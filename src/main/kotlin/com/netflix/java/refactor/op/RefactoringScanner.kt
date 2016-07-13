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
    fun scan(cu: JCTree.JCCompilationUnit, context: Context): List<RefactorFix>
}

open class BaseRefactoringScanner :
        TreePathScanner<List<RefactorFix>, Context>(),
        RefactoringScanner {

    protected lateinit var cu: JCTree.JCCompilationUnit
    protected val source: File by lazy { File(cu.sourcefile.toUri().path) }
    protected val sourceText: CharSequence by lazy { cu.sourcefile.getCharContent(true) }
    
    override fun scan(cu: JCTree.JCCompilationUnit, context: Context): List<RefactorFix> {
        this.cu = cu
        return super.scan(cu, context).plus(visitEnd(context))
    }

    open fun visitEnd(context: Context): List<RefactorFix> = emptyList()

    override fun reduce(r1: List<RefactorFix>?, r2: List<RefactorFix>?) = (r1 ?: emptyList()).plus(r2 ?: emptyList())

    protected fun JCTree.replace(changes: String) =
            RefactorFix(this.startPosition..this.getEndPosition(cu.endPositions), changes, source)
    
    protected fun JCTree.JCFieldAccess.replaceName(changes: String): RefactorFix {
        val nameStart = this.selected.getEndPosition(cu.endPositions) + 1
        return RefactorFix(nameStart..nameStart + this.name.toString().length, changes, source)
    }

    protected fun JCTree.insertAfter(changes: String): RefactorFix {
        val end = this.getEndPosition(cu.endPositions)+1
        return RefactorFix(end..end, changes, source)
    }

    protected fun JCTree.insertBefore(changes: String) =
            RefactorFix(this.startPosition..this.startPosition, changes, source)

    protected fun JCTree.delete(): RefactorFix {
        var end = this.getEndPosition(cu.endPositions)
        if(sourceText.length > end && sourceText[end] == '\n') {
            end++ // delete the newline too
        }
        return RefactorFix(this.startPosition..end, null, source)
    }

    protected fun Context.packageContaining(clazz: String) =
            JavacElements.instance(this).getTypeElement(clazz)?.owner?.toString()
}

class CompositeScanner(vararg val scanners: RefactoringScanner): RefactoringScanner {
    override fun scan(cu: JCTree.JCCompilationUnit, context: Context) =
        scanners.flatMap { it.scan(cu, context) }
}

class IfThenScanner(val ifFixesResultFrom: RefactoringScanner, then: Array<RefactoringScanner>): RefactoringScanner {
    private val compositeThenRun = CompositeScanner(*then)
    
    override fun scan(cu: JCTree.JCCompilationUnit, context: Context): List<RefactorFix> {
        val fixes = ifFixesResultFrom.scan(cu, context)
        return if(fixes.isNotEmpty()) fixes.plus(compositeThenRun.scan(cu, context)) else emptyList()
    }
}
