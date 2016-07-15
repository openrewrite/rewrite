package com.netflix.java.refactor.fix

import com.netflix.java.refactor.*
import com.sun.source.tree.MethodInvocationTree
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeScanner
import com.sun.tools.javac.util.Context
import java.util.*

class ChangeMethodInvocation(signature: String, val tx: RefactorTransaction) : FixingOperation {
    override fun scanner(): RefactoringScanner<List<RefactorFix>> =
        if (refactorTargetToStatic is String) {
            IfThenScanner(ifFixesResultFrom = ChangeMethodInvocationScanner(this),
                    then = arrayOf(
                            AddImport(refactorTargetToStatic!!).scanner()
                    ))
        } else {
            ChangeMethodInvocationScanner(this)
        }

    val matcher = MethodMatcher(signature)
    
    var refactorName: String? = null
    val refactorArguments = ArrayList<MethodArgumentMatcher>()
    var refactorTargetToStatic: String? = null
    var refactorTargetToVariable: String? = null

    fun refactorName(name: String): ChangeMethodInvocation {
        refactorName = name
        return this
    }

    fun refactorArgument(index: Int): MethodArgumentMatcher {
        val matcher = MethodArgumentMatcher(index, this)
        refactorArguments.add(matcher)
        return matcher
    }

    fun refactorTargetToStatic(clazz: String): ChangeMethodInvocation {
        refactorTargetToStatic = clazz
        return this
    }
    
    fun refactorTargetToStatic(clazz: Class<*>) = refactorTargetToStatic(clazz.name)

    fun refactorTargetToVariable(variable: String): ChangeMethodInvocation {
        refactorTargetToVariable = variable
        return this
    }
    
    fun done(): RefactorTransaction {
        if (tx.autoCommit)
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

class ChangeMethodInvocationScanner(val op: ChangeMethodInvocation) : FixingScanner() {
    override fun visitMethodInvocation(node: MethodInvocationTree, context: Context): List<RefactorFix>? {
        val invocation = node as JCTree.JCMethodInvocation
        if(op.matcher.matches(invocation)) {
            return refactorMethod(invocation)
        }
        return null
    }

    fun refactorMethod(invocation: JCTree.JCMethodInvocation): List<RefactorFix> {
        val meth = invocation.meth as JCTree.JCFieldAccess
        val fixes = ArrayList<RefactorFix>()

        if (op.refactorName is String) {
            fixes.add(meth.replaceName(op.refactorName!!))
        }

        op.refactorArguments.forEach { argRefactor ->
            if (invocation.arguments.length() > argRefactor.index) {
                val argScanner = object : TreeScanner() {
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
        
        if(op.refactorTargetToStatic is String) {
            fixes.add(meth.selected.replace(className(op.refactorTargetToStatic!!)))
        }
        
        if(op.refactorTargetToVariable is String) {
            fixes.add(meth.selected.replace(op.refactorTargetToVariable!!))
        }

        return fixes
    }
}
