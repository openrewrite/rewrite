package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorFix
import com.sun.source.util.TreePathScanner
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import java.io.File

interface RefactoringScanner {
    fun scan(cu: JCTree.JCCompilationUnit, context: Context, source: File): List<RefactorFix>
}

open class RefactoringScannerInternal:
        TreePathScanner<List<RefactorFix>, RefactoringScannerInternal.Session>(),
        RefactoringScanner {

    data class Session(val context: Context, val source: File)

    override fun scan(cu: JCTree.JCCompilationUnit, context: Context, source: File) =
        scan(cu, Session(context, source))

    override fun reduce(r1: List<RefactorFix>?, r2: List<RefactorFix>?) = (r1 ?: emptyList()).plus(r2 ?: emptyList())

    protected fun cu() = currentPath.compilationUnit as JCTree.JCCompilationUnit

    protected fun JCTree.replace(changes: String, session: Session) =
            RefactorFix(this.startPosition..this.getEndPosition(cu().endPositions), changes, session.source)

    protected fun JCTree.insertAfter(changes: String, session: Session): RefactorFix {
        val end = this.getEndPosition(cu().endPositions)
        return RefactorFix(end..end, changes, session.source)
    }

    protected fun JCTree.insertBefore(changes: String, session: Session) =
            RefactorFix(this.startPosition..this.startPosition, changes, session.source)

    protected fun JCTree.delete(session: Session) =
            RefactorFix(this.startPosition..this.getEndPosition(cu().endPositions), null, session.source)
}