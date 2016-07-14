package com.netflix.java.refactor.fix

import com.netflix.java.refactor.*
import com.sun.source.tree.MethodInvocationTree
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeScanner
import com.sun.tools.javac.util.Context
import java.util.*

class ChangeMethodInvocation(signature: String, val tx: RefactorTransaction): FixingOperation {
    override fun scanner() = ChangeMethodInvocationScanner(this)

    var refactorName: String? = null
    val refactorArguments = ArrayList<MethodArgumentMatcher>()
    val matcher = MethodMatcher(signature)
    
    fun refactorName(name: String): ChangeMethodInvocation {
        refactorName = name
        return this
    }
    
    fun refactorArgument(index: Int): MethodArgumentMatcher {
        val matcher = MethodArgumentMatcher(index, this)
        refactorArguments.add(matcher)
        return matcher
    }
    
    fun done(): RefactorTransaction {
        if(tx.autoCommit)
            tx.commit()
        return tx
    }
}

open class MethodArgumentMatcher(val index: Int, val op: ChangeMethodInvocation) {
    var typeConstraint: String? = null
    
    fun isType(clazz: String): MethodArgumentMatcher {
        typeConstraint = clazz
        return this
    }

    fun isType(clazz: Class<*>) = isType(clazz.name)

    var refactorLiterals: ((Any) -> Any)? = null

    fun mapLiterals(transform: (Any) -> Any): MethodArgumentMatcher {
        this.refactorLiterals = transform
        return this
    }
    
    fun done() = op
}

class ChangeMethodInvocationScanner(val op: ChangeMethodInvocation): FixingScanner() {
    override fun visitMethodInvocation(node: MethodInvocationTree, context: Context): List<RefactorFix>? {
        val invocation = node as JCTree.JCMethodInvocation
        if(invocation.meth is JCTree.JCFieldAccess) {
            val meth = (invocation.meth as JCTree.JCFieldAccess)
            
            val args = when(meth.sym) {
                is Symbol.MethodSymbol -> {
                    (meth.sym as Symbol.MethodSymbol).params().map { 
                        val baseType = it.type.toString()
                        if(it.flags() and Flags.VARARGS != 0L) {
                            baseType.substringBefore("[") + "..."
                        }
                        else
                            baseType
                    }.joinToString("")
                }

                // This is a weird case... for some reason the attribution phase will sometimes assign a ClassSymbol to
                // method invocation, making the parameters of the resolved method inaccessible to us. In these cases,
                // we can make a best effort at determining the method's argument types by observing the types that are
                // being passed to it by the code.
                else -> invocation.args.map { it.type.toString() }.joinToString(",")
            } 
            
            
            if(op.matcher.targetTypePattern.matches((meth.sym.owner as Symbol.ClassSymbol).toString()) &&
                    op.matcher.methodNamePattern.matches(meth.name.toString()) &&
                    op.matcher.argumentPattern.matches(args)) {
                return refactorMethod(invocation)
            }
        } else {
            // this is a method invocation on a method in the same class, which we won't be refactoring on ever
        }
        
        return null
    }
    
    fun refactorMethod(invocation: JCTree.JCMethodInvocation): List<RefactorFix> {
        val meth = invocation.meth as JCTree.JCFieldAccess
        val fixes = ArrayList<RefactorFix>()
        
        if(op.refactorName is String) {
            fixes.add(meth.replaceName(op.refactorName!!))
        }
        
        op.refactorArguments.forEach { argRefactor ->
            if(invocation.arguments.length() > argRefactor.index) {
                val argScanner = object: TreeScanner() {
                    override fun visitLiteral(tree: JCTree.JCLiteral) {
                        // prefix and suffix hold the special characters surrounding the values of primitive-ish types,
                        // e.g. the "" around String, the L at the end of a long, etc.
                        val valueMatcher = "(.*)${tree.value}(.*)".toRegex().find(tree.toString())
                        val (prefix, suffix) = valueMatcher!!.groupValues.drop(1)

                        if (argRefactor.typeConstraint?.equals(tree.type.toString()) ?: true) {
                            val transformed = argRefactor.refactorLiterals?.invoke(tree.value) ?: tree.value
                            if (transformed != tree.value) {
                                fixes.add(tree.replace("$prefix$transformed$suffix"))
                            }
                        }
                    }
                }
                
                argScanner.scan(invocation.arguments[argRefactor.index])
            }
        }
        
        return fixes
    }
}
