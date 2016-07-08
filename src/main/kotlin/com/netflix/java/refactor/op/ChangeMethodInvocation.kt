package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorFix
import com.netflix.java.refactor.RefactorRule
import com.netflix.java.refactor.aspectj.AspectJLexer
import com.netflix.java.refactor.aspectj.RefactorMethodSignatureParser
import com.netflix.java.refactor.aspectj.RefactorMethodSignatureParserBaseVisitor
import com.sun.source.tree.MethodInvocationTree
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeScanner
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import java.util.*

class ChangeMethodInvocation(signature: String, val containingRule: RefactorRule): RefactorOperation {
    override val scanner = ChangeMethodInvocationScanner(this)
    
    var targetTypePattern: Regex? = null
    var methodNamePattern: Regex? = null
    var refactorName: String? = null
    val refactorArguments = ArrayList<MethodArgumentMatcher>()
    
    init {
        val parser = RefactorMethodSignatureParser(CommonTokenStream(AspectJLexer(ANTLRInputStream(signature))))

        /*
        An embedded * in an identifier matches any sequence of characters, but does not match the package (or inner-type) separator ".".
        An embedded .. in an identifier matches any sequence of characters that starts and ends with the package (or inner-type) separator ".".
         */

        object: RefactorMethodSignatureParserBaseVisitor<Void>() {
            override fun visitMethodPattern(ctx: RefactorMethodSignatureParser.MethodPatternContext): Void? {
                targetTypePattern = TypeVisitor().visitTypePattern(ctx.typePattern())
                methodNamePattern = ctx.simpleNamePattern().Identifier(0).toString().replace("*", ".*").toRegex()
                return super.visitMethodPattern(ctx)
            }
        }.visit(parser.methodPattern())
    }
    
    class TypeVisitor: RefactorMethodSignatureParserBaseVisitor<Regex>() {
        override fun visitSimpleTypePattern(ctx: RefactorMethodSignatureParser.SimpleTypePatternContext) =
            ctx.dottedNamePattern().type()[0].classOrInterfaceType().Identifier(0).toString().replace("..", ".*").toRegex()
    }
    
    fun refactorName(name: String): ChangeMethodInvocation {
        refactorName = name
        return this
    }
    
    fun refactorArgument(index: Int): MethodArgumentMatcher {
        val matcher = MethodArgumentMatcher(index, this)
        refactorArguments.add(matcher)
        return matcher
    }
    
    fun done() = containingRule
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

class ChangeMethodInvocationScanner(val op: ChangeMethodInvocation): BaseRefactoringScanner() {
    override fun visitMethodInvocation(node: MethodInvocationTree, session: Session): List<RefactorFix>? {
        val invocation = node as JCTree.JCMethodInvocation
        if(invocation.meth is JCTree.JCFieldAccess) {
            val meth = (invocation.meth as JCTree.JCFieldAccess)
            
            if(op.targetTypePattern?.matches(session.type(meth.selected).toString()) ?: true &&
                    op.methodNamePattern?.matches(meth.name.toString()) ?: true) {
                return refactorMethod(invocation, session)
            }
        } else {
            // this is a method invocation on a method in the same class, which we won't be refactoring on ever
        }
        
        return null
    }
    
    fun refactorMethod(invocation: JCTree.JCMethodInvocation, session: Session): List<RefactorFix> {
        val meth = invocation.meth as JCTree.JCFieldAccess
        val fixes = ArrayList<RefactorFix>()
        
        if(op.refactorName is String) {
            fixes.add(meth.replaceName(op.refactorName!!, session))
        }
        
        op.refactorArguments.forEach { argRefactor ->
            if(invocation.arguments.length() > argRefactor.index) {
                object: TreeScanner() {
                    override fun visitLiteral(tree: JCTree.JCLiteral) {
                        // prefix and suffix hold the special characters surrounding the values of primitive-ish types,
                        // e.g. the "" around String, the L at the end of a long, etc.
                        val (prefix, suffix) = "(.*)${tree.value}(.*)".toRegex().find(tree.toString())!!.groupValues.drop(1)
                        
                        if(argRefactor.typeConstraint?.equals(session.type(tree).toString()) ?: true) {
                            val transformed = argRefactor.refactorLiterals?.invoke(tree.value) ?: tree.value
                            if(transformed != tree.value) {
                                fixes.add(tree.replace("$prefix$transformed$suffix", session))
                            }
                        }
                    }
                }.scan(invocation.arguments[argRefactor.index])
            }
        }
        
        return fixes
    }
}
