package com.netflix.java.refactor.fix

import com.netflix.java.refactor.RefactorFix
import com.netflix.java.refactor.RefactorTransaction
import com.netflix.java.refactor.ast.*
import com.netflix.java.refactor.find.Argument
import com.netflix.java.refactor.find.Method
import com.sun.source.tree.LiteralTree
import com.sun.source.tree.MethodInvocationTree
import com.sun.source.util.TreePath
import com.sun.source.util.TreePathScanner
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import java.util.*
import java.util.regex.Pattern

class ChangeMethodInvocation(signature: String, val tx: RefactorTransaction) : RefactoringAstScannerBuilder {
    override fun scanner(): AstScanner<List<RefactorFix>> =
            if (refactorTargetToStatic is String) {
                IfThenScanner(ifFixesResultFrom = ChangeMethodInvocationScanner(this),
                        then = arrayOf(
                                AddImport(refactorTargetToStatic!!).scanner()
                        ))
            } else {
                ChangeMethodInvocationScanner(this)
            }

    internal val matcher = MethodMatcher(signature)

    internal var refactorName: String? = null
    internal var refactorArguments: RefactorArguments? = null
    internal var refactorTargetToStatic: String? = null
    internal var refactorTargetToVariable: String? = null
    internal var refactorNameBuilder: ((Method) -> String)? = null

    fun changeName(name: String): ChangeMethodInvocation {
        refactorName = name
        return this
    }

    fun changeName(nameBuilder: (Method) -> String): ChangeMethodInvocation {
        refactorNameBuilder = nameBuilder
        return this
    }

    fun changeArguments(): RefactorArguments {
        refactorArguments = RefactorArguments(this)
        return refactorArguments!!
    }

    /**
     * Change to a static method invocation on clazz
     */
    fun changeTarget(clazz: String): ChangeMethodInvocation {
        refactorTargetToStatic = clazz
        return this
    }

    /**
     * Change to a static method invocation on clazz
     */
    fun changeTarget(clazz: Class<*>) = changeTarget(clazz.name)

    /**
     * Change the target to a named variable
     */
    fun changeTargetToVariable(variable: String): ChangeMethodInvocation {
        refactorTargetToVariable = variable
        return this
    }

    fun done() = tx
}

class RefactorArguments(val op: ChangeMethodInvocation) {
    internal val individualArgumentRefactors = ArrayList<RefactorArgument>()
    internal var reorderArguments: List<String>? = null
    internal var argumentNames: List<String>? = null
    internal val insertions = ArrayList<InsertArgument>()
    internal val deletions = ArrayList<DeleteArgument>()

    fun arg(clazz: String): RefactorArgument {
        val arg = RefactorArgument(this, typeConstraint = clazz)
        individualArgumentRefactors.add(arg)
        return arg
    }

    fun arg(clazz: Class<*>) = arg(clazz.name)

    fun arg(pos: Int): RefactorArgument {
        val arg = RefactorArgument(this, posConstraint = pos)
        individualArgumentRefactors.add(arg)
        return arg
    }

    fun whereArgNamesAre(vararg name: String): RefactorArguments {
        this.argumentNames = name.toList()
        return this
    }

    fun reorderByArgName(vararg name: String): RefactorArguments {
        reorderArguments = name.toList()
        return this
    }

    fun insert(pos: Int, value: String): RefactorArguments {
        insertions.add(InsertArgument(pos, value))
        return this
    }

    fun delete(pos: Int): RefactorArguments {
        deletions.add(DeleteArgument(pos))
        return this
    }

    fun done() = op
}

data class InsertArgument(val pos: Int, val value: String)

data class DeleteArgument(val pos: Int)

open class RefactorArgument(val op: RefactorArguments,
                            val typeConstraint: String? = null,
                            val posConstraint: Int? = null) {
    internal var refactorLiterals: ((Any) -> Any)? = null

    fun changeLiterals(transform: (Any) -> Any): RefactorArgument {
        this.refactorLiterals = transform
        return this
    }

    fun done() = op
}

class ChangeMethodInvocationScanner(val op: ChangeMethodInvocation) : FixingScanner() {
    override fun visitMethodInvocation(node: MethodInvocationTree, context: Context): List<RefactorFix>? {
        val invocation = node as JCTree.JCMethodInvocation
        val nestedFixes = super.visitMethodInvocation(node, context) ?: emptyList()
        return nestedFixes + if (op.matcher.matches(invocation)) {
            refactorMethod(invocation)
        } else emptyList()
    }

    fun refactorMethod(invocation: JCTree.JCMethodInvocation): List<RefactorFix> {
        val meth = invocation.meth
        val fixes = ArrayList<RefactorFix>()
        val methSym = when (meth) {
            is JCTree.JCFieldAccess -> meth.sym
            is JCTree.JCIdent -> meth.sym
            else -> null
        }

        val newName =
                if (op.refactorName is String) {
                    op.refactorName
                } else if (op.refactorNameBuilder != null) {
                    op.refactorNameBuilder!!(Method(invocation.meth.toString(), invocation.source(), invocation.args.map { Argument(it.source()) }))
                } else {
                    null
                }

        if (newName is String) {
            when (meth) {
                is JCTree.JCFieldAccess -> {
                    val nameStart = meth.selected.getEndPosition(cu.endPositions) + 1
                    fixes.add(RefactorFix(nameStart..nameStart + meth.name.toString().length, newName, source))
                }
                is JCTree.JCIdent -> {
                    fixes.add(meth.replace(newName))
                }
            }
        }

        if (op.refactorArguments is RefactorArguments) {
            if (op.refactorArguments?.reorderArguments != null) {
                val reorders = op.refactorArguments!!.reorderArguments!!
                val paramNames = when (methSym) {
                    is Symbol.MethodSymbol -> methSym.params().map { it.name.toString() }
                    else -> null
                }

                if (paramNames != null) {
                    var argPos = 0
                    reorders.forEachIndexed { paramPos, reorder ->
                        if (invocation.arguments.size <= argPos) {
                            // this is a weird case, there are not enough arguments in the invocation to satisfy the reordering specification
                            // TODO what to do?
                            return@forEachIndexed
                        }

                        if (paramNames[paramPos] != reorder) {
                            var swaps = invocation.arguments.filterIndexed { j, swap -> paramNames[Math.min(j, paramNames.size - 1)] == reorder }

                            // when no source is attached, we must define names first
                            if (swaps.isEmpty()) {
                                val pos = op.refactorArguments?.argumentNames?.indexOf(reorder) ?: -1
                                if (pos >= 0 && pos < invocation.arguments.size) {
                                    swaps = if (pos < op.refactorArguments!!.argumentNames!!.size - 1) {
                                        listOf(invocation.args[pos])
                                    } else {
                                        // this is a varargs argument, grab them all
                                        invocation.arguments.drop(pos)
                                    }
                                }
                            }

                            swaps.forEach { swap ->
                                fixes.add(invocation.arguments[argPos].replace(swap.changesToArgument(argPos) ?: swap.source()))
                                argPos++
                            }
                        } else argPos++
                    }
                } else {
                    // TODO what do we do when the method symbol is not present?
                }
            } else {
                invocation.arguments.forEachIndexed { i, arg ->
                    arg.changesToArgument(i)?.let { changes ->
                        fixes.add(arg.replace(changes))
                    }
                }
            }

            op.refactorArguments?.insertions?.forEach { insertion ->
                if (invocation.arguments.isEmpty()) {
                    val argStart = sourceText.indexOf('(', invocation.methodSelect.getEndPosition(cu.endPositions)) + 1
                    fixes.add(insertAt(argStart, "${if (insertion.pos > 0) ", " else ""}${insertion.value}"))
                } else if (invocation.arguments.size <= insertion.pos) {
                    fixes.add(insertAt(invocation.arguments.last().getEndPosition(cu.endPositions), ", ${insertion.value}"))
                } else {
                    fixes.add(insertAt(invocation.arguments[insertion.pos].startPosition, "${insertion.value}, "))
                }
            }

            op.refactorArguments?.deletions?.forEach { deletion ->
                fixes.add(invocation.arguments[deletion.pos].delete())
            }
        }

        if (op.refactorTargetToStatic is String) {
            when (meth) {
                is JCTree.JCFieldAccess ->
                    fixes.add(meth.selected.replace(className(op.refactorTargetToStatic!!)))
                is JCTree.JCIdent ->
                    fixes.add(meth.insertBefore(className(op.refactorTargetToStatic!! + ".")))
            }
        }

        if (op.refactorTargetToVariable is String) {
            when (meth) {
                is JCTree.JCFieldAccess ->
                    fixes.add(meth.selected.replace(op.refactorTargetToVariable!!))
                is JCTree.JCIdent ->
                    fixes.add(meth.insertBefore(op.refactorTargetToVariable!! + "."))
            }
        }

        return fixes
    }

    private inner class ChangeArgumentScanner : TreePathScanner<List<RefactorFix>, RefactorArgument>() {
        override fun visitLiteral(node: LiteralTree, refactor: RefactorArgument): List<RefactorFix> {
            val literal = node as JCTree.JCLiteral
            val value = literal.value

            // prefix and suffix hold the special characters surrounding the values of primitive-ish types,
            // e.g. the "" around String, the L at the end of a long, etc.
            val valueMatcher = "(.*)${Pattern.quote(value.toString())}(.*)".toRegex().find(node.toString().replace("\\", ""))
            return when (valueMatcher) {
                is MatchResult -> {
                    val (prefix, suffix) = valueMatcher.groupValues.drop(1)

                    val transformed = refactor.refactorLiterals?.invoke(value) ?: value
                    if (transformed != value.toString()) listOf(literal.replace("$prefix$transformed$suffix")) else emptyList()
                }
                else -> {
                    // this should never happen
                    emptyList()
                }
            }
        }

        override fun reduce(r1: List<RefactorFix>?, r2: List<RefactorFix>?): List<RefactorFix> =
                (r1 ?: emptyList()).plus(r2 ?: emptyList())
    }

    fun JCTree.JCExpression.changesToArgument(pos: Int): String? {
        val refactor = op.refactorArguments?.individualArgumentRefactors?.find { it.posConstraint == pos } ?:
                op.refactorArguments?.individualArgumentRefactors?.find { this.type?.matches(it.typeConstraint) ?: false }

        return if (refactor is RefactorArgument) {
            val fixes = ChangeArgumentScanner().scan(TreePath.getPath(cu, this), refactor) ?: emptyList()

            // aggregate all the fixes to this argument into one "change" replacement rule
            return if (fixes.isNotEmpty()) {
                val sortedFixes = fixes.sortedBy { it.position.last }.sortedBy { it.position.start }
                var fixedArg = sortedFixes.foldIndexed("") { i, source, fix ->
                    val prefix = if (i == 0)
                        sourceText.substring(this.startPosition, fix.position.first)
                    else sourceText.substring(sortedFixes[i - 1].position.last, fix.position.start)
                    source + prefix + (fix.changes ?: "")
                }
                if (sortedFixes.last().position.last < sourceText.length) {
                    fixedArg += sourceText.substring(sortedFixes.last().position.last, this.getEndPosition(cu.endPositions))
                }

                fixedArg
            } else null
        } else null
    }
}
