package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorFix
import com.sun.source.util.TreePathScanner
import com.sun.tools.javac.model.JavacElements
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
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
    }

    override fun scan(cu: JCTree.JCCompilationUnit, context: Context, source: File): List<RefactorFix> {
        val session = Session(cu, context, source)
        return scan(cu, session).plus(visitEnd(session) ?: emptyList())
    }

    open fun visitEnd(session: Session): List<RefactorFix>? = null

    override fun reduce(r1: List<RefactorFix>?, r2: List<RefactorFix>?) = (r1 ?: emptyList()).plus(r2 ?: emptyList())

    protected fun JCTree.replace(changes: String, session: Session) =
            RefactorFix(this.startPosition..this.getEndPosition(session.cu.endPositions), changes, session.source)

    protected fun JCTree.insertAfter(changes: String, session: Session): RefactorFix {
        val end = this.getEndPosition(session.cu.endPositions)
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