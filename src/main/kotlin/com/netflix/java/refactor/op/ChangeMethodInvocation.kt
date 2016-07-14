package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorFix
import com.netflix.java.refactor.Refactorer
import com.netflix.java.refactor.aspectj.AspectJLexer
import com.netflix.java.refactor.aspectj.RefactorMethodSignatureParser
import com.netflix.java.refactor.aspectj.RefactorMethodSignatureParserBaseVisitor
import com.sun.source.tree.MethodInvocationTree
import com.sun.tools.javac.code.Flags
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.tree.TreeScanner
import com.sun.tools.javac.util.Context
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.TerminalNode
import java.util.*

class ChangeMethodInvocation(signature: String, val containingRule: Refactorer): RefactorOperation {
    override fun scanner() = ChangeMethodInvocationScanner(this)
    
    lateinit var targetTypePattern: Regex
    lateinit var methodNamePattern: Regex
    lateinit var argumentPattern: Regex
    
    var refactorName: String? = null
    val refactorArguments = ArrayList<MethodArgumentMatcher>()
    
    init {
        val parser = RefactorMethodSignatureParser(CommonTokenStream(AspectJLexer(ANTLRInputStream(signature))))

        object: RefactorMethodSignatureParserBaseVisitor<Void>() {
            override fun visitMethodPattern(ctx: RefactorMethodSignatureParser.MethodPatternContext): Void? {
                targetTypePattern = TypeVisitor().visitTargetTypePattern(ctx.targetTypePattern()).toRegex()
                methodNamePattern = ctx.simpleNamePattern().children // all TerminalNode instances
                        .map { it.toString().aspectjToRegexSyntax() }
                        .joinToString("")
                        .toRegex()
                argumentPattern = FormalParameterVisitor().visitFormalParametersPattern(ctx.formalParametersPattern()).toRegex()
                return null
            }
        }.visit(parser.methodPattern())
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
            
            
            if(op.targetTypePattern.matches((meth.sym.owner as Symbol.ClassSymbol).toString()) &&
                    op.methodNamePattern.matches(meth.name.toString()) &&
                    op.argumentPattern.matches(args)) {
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

/**
 * See https://eclipse.org/aspectj/doc/next/progguide/semantics-pointcuts.html#type-patterns
 * 
 * An embedded * in an identifier matches any sequence of characters, but 
 * does not match the package (or inner-type) separator ".".
 * 
 * The ".." wildcard matches any sequence of characters that start and end with a ".", so it can be used to pick out all 
 * types in any subpackage, or all inner types. e.g. <code>within(com.xerox..*)</code> picks out all join points where 
 * the code is in any declaration of a type whose name begins with "com.xerox.".
 */
fun String.aspectjToRegexSyntax() = this
        .replace("[", "\\[").replace("]", "\\]")
        .replace("([^\\.])*.([^\\.])*", "$1\\.$2")
        .replace("*", "[^\\.]*")
        .replace("..", "\\.(.+\\.)?")

class TypeVisitor : RefactorMethodSignatureParserBaseVisitor<String>() {
    override fun visitClassNameOrInterface(ctx: RefactorMethodSignatureParser.ClassNameOrInterfaceContext): String {
        return ctx.children // all TerminalNode instances
                .map { 
                    it.text.aspectjToRegexSyntax()
                }
                .joinToString("")
                .let { className ->
                    if(!className.contains('.')) {
                        try {
                            Class.forName("java.lang.${className.substringBefore("\\[")}", false, TypeVisitor::class.java.classLoader)
                            return@let "java.lang.$className"
                        } catch(ignore: ClassNotFoundException) {
                        }
                    }
                    className
                }
    }

    override fun visitPrimitiveType(ctx: RefactorMethodSignatureParser.PrimitiveTypeContext): String {
        return ctx.text
    }
}

/**
 * The wildcard .. indicates zero or more parameters, so:
 *
 * <code>execution(void m(..))</code>
 * picks out execution join points for void methods named m, of any number of arguments, while
 *
 * <code>execution(void m(.., int))</code>
 * picks out execution join points for void methods named m whose last parameter is of type int.
 */
class FormalParameterVisitor: RefactorMethodSignatureParserBaseVisitor<String>() {
    private val arguments = ArrayList<Argument>()
    
    private sealed class Argument {
        abstract val regex: String

        object DotDot: Argument() {
            override val regex = "([^,]+,)*([^,]+)"
        }
        
        class FormalType(ctx: RefactorMethodSignatureParser.FormalTypePatternContext): Argument() {
            override val regex: String by lazy { 
                val baseType = TypeVisitor().visitFormalTypePattern(ctx)
                if(variableArgs) "$baseType..." else baseType
            }
            var variableArgs = false
        }
    }

    override fun visitTerminal(node: TerminalNode): String? {
        if(node.text == "...") {
            (arguments.last() as Argument.FormalType).variableArgs = true
        }
        return super.visitTerminal(node)
    }
    
    override fun visitDotDot(ctx: RefactorMethodSignatureParser.DotDotContext): String? {
        arguments.add(Argument.DotDot)
        return super.visitDotDot(ctx)
    }
    
    override fun visitFormalTypePattern(ctx: RefactorMethodSignatureParser.FormalTypePatternContext): String? {
        arguments.add(Argument.FormalType(ctx))
        return super.visitFormalTypePattern(ctx)
    }

    override fun visitFormalParametersPattern(ctx: RefactorMethodSignatureParser.FormalParametersPatternContext): String {
        super.visitFormalParametersPattern(ctx)
        return arguments.mapIndexed { i, argument -> 
            // Note: the AspectJ grammar doesn't allow for multiple ..'s in one formal parameter pattern
            when(argument) {
                is Argument.DotDot -> {
                    if(i > 0)
                        "(,${argument.regex})?"
                    else "(${argument.regex},)?"
                }
                is Argument.FormalType -> argument.regex
            }
        }.joinToString("")
    }
}