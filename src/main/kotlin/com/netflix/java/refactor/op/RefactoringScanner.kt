package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorFix
import com.sun.source.util.TreePathScanner
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.comp.Attr
import com.sun.tools.javac.comp.AttrContext
import com.sun.tools.javac.comp.Env
import com.sun.tools.javac.comp.Todo
import com.sun.tools.javac.model.JavacElements
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Name
import java.io.File

/**
 * A separate instance will be created for each file scan, so implementations can safely hold state
 * about a source file without having to clean it up.
 */
interface RefactoringScanner {
    fun scan(cu: JCTree.JCCompilationUnit, context: Context, source: File): List<RefactorFix>
}

open class BaseRefactoringScanner :
        TreePathScanner<List<RefactorFix>, BaseRefactoringScanner.Session>(),
        RefactoringScanner {

    data class Session(val cu: JCTree.JCCompilationUnit, val context: Context, val source: File) {
        val sourceText: String by lazy { source.readText() }
        val env: Env<AttrContext> by lazy { Todo.instance(context).first { it.toplevel === cu } }
        val attr: Attr by lazy { Attr.instance(context) }
        
        fun classSymbol(name: Name) = (cu.namedImportScope.getElementsByName(name).firstOrNull() ?: 
                cu.starImportScope.getElementsByName(name).firstOrNull()) as Symbol.ClassSymbol?

        fun type(tree: JCTree) = attr.attribExpr(tree, env)
        fun types(trees: com.sun.tools.javac.util.List<JCTree>) = trees.asIterable().map { attr.attribExpr(it, env) }
    }

    override fun scan(cu: JCTree.JCCompilationUnit, context: Context, source: File): List<RefactorFix> {
        val session = Session(cu, context, source)
        return scan(cu, session).plus(visitEnd(session) ?: emptyList())
    }

    open fun visitEnd(session: Session): List<RefactorFix>? = null

    override fun reduce(r1: List<RefactorFix>?, r2: List<RefactorFix>?) = (r1 ?: emptyList()).plus(r2 ?: emptyList())

    protected fun JCTree.replace(changes: String, session: Session) =
            RefactorFix(this.startPosition..this.getEndPosition(session.cu.endPositions), changes, session.source)
    
    protected fun JCTree.JCFieldAccess.replaceName(changes: String, session: Session): RefactorFix {
        val nameStart = this.selected.getEndPosition(session.cu.endPositions) + 1
        return RefactorFix(nameStart..nameStart + this.name.toString().length, changes, session.source)
    }

    protected fun JCTree.insertAfter(changes: String, session: Session): RefactorFix {
        val end = this.getEndPosition(session.cu.endPositions)+1
        return RefactorFix(end..end, changes, session.source)
    }

    protected fun JCTree.insertBefore(changes: String, session: Session) =
            RefactorFix(this.startPosition..this.startPosition, changes, session.source)

    protected fun JCTree.delete(session: Session): RefactorFix {
        var end = this.getEndPosition(session.cu.endPositions)
        if(session.sourceText.length > end && session.sourceText[end] == '\n') {
            end++ // delete the newline too
        }
        return RefactorFix(this.startPosition..end, null, session.source)
    }
    
    protected fun typeElement(clazz: String, session: Session) = 
            JavacElements.instance(session.context).getTypeElement(clazz)
}

class CompositeScanner(vararg val scanners: RefactoringScanner): RefactoringScanner {
    override fun scan(cu: JCTree.JCCompilationUnit, context: Context, source: File) =
        scanners.flatMap { it.scan(cu, context, source) }
}

class IfThenScanner(val ifFixed: RefactoringScanner, vararg thenRun: RefactoringScanner): RefactoringScanner {
    private val compositeThenRun = CompositeScanner(*thenRun)
    
    override fun scan(cu: JCTree.JCCompilationUnit, context: Context, source: File): List<RefactorFix> {
        val fixes = ifFixed.scan(cu, context, source)
        return if(fixes.isNotEmpty()) fixes.plus(compositeThenRun.scan(cu, context, source)) else emptyList()
    }
}