package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorFix
import com.sun.source.util.TreePathScanner
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type
import com.sun.tools.javac.comp.Attr
import com.sun.tools.javac.comp.AttrContext
import com.sun.tools.javac.comp.Env
import com.sun.tools.javac.comp.Todo
import com.sun.tools.javac.model.JavacElements
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Name
import java.io.File
import java.util.*

/**
 * A separate instance will be created for each file scan, so implementations can safely hold state
 * about a source file without having to clean it up.
 */
interface RefactoringScanner {
    fun scan(cu: JCTree.JCCompilationUnit, context: Context): List<RefactorFix>
}

open class BaseRefactoringScanner :
        TreePathScanner<List<RefactorFix>, BaseRefactoringScanner.Session>(),
        RefactoringScanner {

    data class Session(val cu: JCTree.JCCompilationUnit, val context: Context) {
        companion object {
            val classSymbolTable = HashMap<Name, Symbol.ClassSymbol?>()
        }
        
        val source: File = File(cu.sourcefile.toUri().path)
        val sourceText: CharSequence by lazy { cu.sourcefile.getCharContent(true) }
        val env: Env<AttrContext> by lazy { Todo.instance(context).first { it.toplevel === cu } }
        val attr: Attr by lazy { Attr.instance(context) }
        
        fun classSymbol(name: Name) =
            classSymbolTable.getOrPut(name) {
                (cu.namedImportScope.getElementsByName(name).firstOrNull() ?:
                        cu.starImportScope.getElementsByName(name).firstOrNull()) as Symbol.ClassSymbol?
            }

        fun typeElement(clazz: String): Symbol.ClassSymbol? = JavacElements.instance(context).getTypeElement(clazz)
        fun packageContaining(clazz: String) = typeElement(clazz)?.owner?.toString()

        fun type(tree: JCTree): Type? = attr.attribExpr(tree, env, noType)
        fun types(trees: com.sun.tools.javac.util.List<JCTree>): List<Type> = 
                trees.asIterable().map { attr.attribExpr(it, env, null) }
        
        // In JDK 8, the type argument for attribExpr is optional, but in JDK 7 it is not. Unfortunately, noType is a concrete
        // implementation of a class that is inaccessible from this context, so let's jump through some hoops.
        val noType by lazy {
            val noType = Type::class.java.getField("noType")
            noType.isAccessible = true
            noType.get(null) as Type
        }
    }

    override fun scan(cu: JCTree.JCCompilationUnit, context: Context): List<RefactorFix> {
        val session = Session(cu, context)
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