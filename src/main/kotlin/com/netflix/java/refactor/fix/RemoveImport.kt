package com.netflix.java.refactor.fix

import com.netflix.java.refactor.RefactorFix
import com.netflix.java.refactor.ast.FixingScanner
import com.netflix.java.refactor.ast.MethodMatcher
import com.netflix.java.refactor.ast.RefactoringAstScannerBuilder
import com.netflix.java.refactor.ast.matches
import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.ImportTree
import com.sun.source.tree.MethodInvocationTree
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import java.util.*

class RemoveImport(val clazz: String) : RefactoringAstScannerBuilder {
    override fun scanner() = RemoveImportScanner(this)
}

class RemoveImportScanner(val op: RemoveImport) : FixingScanner() {
    val methodMatcher = MethodMatcher("${op.clazz} *(..)")
    
    var namedImport: JCTree.JCImport? = null
    var starImport: JCTree.JCImport? = null

    var referencedTypes = ArrayList<Symbol.ClassSymbol>()
    var referencedMethods = ArrayList<Symbol.MethodSymbol>()

    var staticNamedImports = ArrayList<JCTree.JCImport>()
    var staticStarImport: JCTree.JCImport? = null

    
    override fun visitImport(node: ImportTree, context: Context): List<RefactorFix>? {
        val import = node as JCTree.JCImport
        val importType = import.qualid as JCTree.JCFieldAccess
        if (import.isStatic) {
            val method = importType.name.toString()
            if (importType.selected.toString() == op.clazz) {
                if (method == "*")
                    staticStarImport = import
                else
                    staticNamedImports.add(import)
            }
        } else {
            if (importType.toString() == op.clazz) {
                namedImport = import
            } else if (importType.name.toString() == "*" && importType.selected.toString() == context.packageContaining(op.clazz)) {
                starImport = import
            }
        }

        return null
    }

    override fun visitIdentifier(node: IdentifierTree, context: Context): List<RefactorFix>? {
        val ident = node as JCTree.JCIdent
        if (ident.sym is Symbol.ClassSymbol) {
            val sym = ident.sym as Symbol.ClassSymbol
            if (sym.owner.toString() == context.packageContaining(op.clazz)) {
                referencedTypes.add(sym)
            }
        }
        return null
    }

    override fun visitMethodInvocation(node: MethodInvocationTree, p: Context): List<RefactorFix>? {
        val invocation = node as JCTree.JCMethodInvocation
        if(methodMatcher.matches(invocation)) {
            val meth = invocation.meth
            val methSym = when (meth) {
                is JCTree.JCFieldAccess -> meth.sym
                is JCTree.JCIdent -> meth.sym
                else -> null
            }
            
            if(methSym is Symbol.MethodSymbol) {
                if(methSym.owner.toString() == op.clazz)
                   referencedMethods.add(methSym)       
            }
        }
        return super.visitMethodInvocation(node, p)
    }

    override fun visitEnd(context: Context): List<RefactorFix> =
        classImportDeletions() + staticImportDeletions()

    private fun classImportDeletions() = 
        if (namedImport is JCTree.JCImport && referencedTypes.none { it.toString() == op.clazz }) {
            listOf(namedImport!!.delete())
        } else if (starImport is JCTree.JCImport && referencedTypes.isEmpty()) {
            listOf(starImport!!.delete())
        } else if (starImport is JCTree.JCImport && referencedTypes.size == 1) {
            listOf(starImport!!.replace("import ${referencedTypes[0].className()};"))
        } else emptyList()
    
    private fun staticImportDeletions(): ArrayList<RefactorFix> {
        val staticImportFixes = ArrayList<RefactorFix>()
        if(staticStarImport is JCTree.JCImport && referencedMethods.isEmpty()) {
            staticImportFixes.add(staticStarImport!!.delete())
        }
        staticNamedImports.forEach { staticImport ->
            val method = (staticImport.qualid as JCTree.JCFieldAccess).name.toString()
            if(referencedMethods.none { ref -> ref.name.toString() == method })
                staticImportFixes.add(staticImport.delete())
        }
        return staticImportFixes
    }
}