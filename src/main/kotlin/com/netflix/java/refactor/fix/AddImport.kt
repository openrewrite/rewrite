package com.netflix.java.refactor.fix

import com.netflix.java.refactor.*
import com.netflix.java.refactor.ast.*
import com.sun.source.tree.ImportTree
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import java.util.*

class AddImport(val clazz: String, val staticMethod: String? = null): RefactoringAstScannerBuilder {
    override fun scanner() = if(packageOwner(clazz).isNotEmpty())
        AddImportScanner(this)
    else NoOpScanner
}

class AddImportScanner(val op: AddImport): FixingScanner() {
    val imports = ArrayList<JCTree.JCImport>()
    var coveredByExistingImport = false
    
    private val packageComparator = PackageComparator()
    
    override fun visitImport(node: ImportTree?, context: Context): List<RefactorFix>? {
        val import = node as JCTree.JCImport
        val importType = import.qualid as JCTree.JCFieldAccess
        imports.add(import)
        
        if (addingStaticImport()) {
            if (importType.selected.matches(op.clazz) && importType.name.toString() == op.staticMethod) {
                coveredByExistingImport = true
            }
            if (importType.selected.matches(op.clazz) && importType.name.toString() == "*") {
                coveredByExistingImport = true
            }
        }
        else {
            if (importType.matches(op.clazz)) {
                coveredByExistingImport = true
            } else if (importType.selected.toString() == packageOwner(op.clazz) && importType.name.toString() == "*") {
                coveredByExistingImport = true
            }
        }
        
        return null
    }

    override fun visitEnd(context: Context): List<RefactorFix> {
        val lastPrior = lastPriorImport()
        val importStatementToAdd = if(addingStaticImport()) {
            "import static ${op.clazz}.${op.staticMethod};"
        } else "import ${op.clazz};"
        
        return if(coveredByExistingImport) {
            emptyList()
        }
        else if(lastPrior == null && imports.isNotEmpty()) {
            listOf(imports.first().insertBefore("$importStatementToAdd\n"))
        }
        else if(lastPrior is JCTree.JCImport) {
            listOf(lastPrior.insertAt("$importStatementToAdd\n"))
        }
        else if(cu.packageName != null) {
            listOf(cu.packageName.insertAt("\n\n$importStatementToAdd"))
        }
        else listOf(cu.insertBefore("$importStatementToAdd\n"))
    }
    
    fun lastPriorImport(): JCTree.JCImport? {
        return imports.lastOrNull { import ->
            // static imports go after all non-static imports
            if(addingStaticImport() && !import.staticImport)
                return@lastOrNull true
            
            // non-static imports should always go before static imports
            if(!addingStaticImport() && import.staticImport)
                return@lastOrNull false
            
            val importType = import.qualid as JCTree.JCFieldAccess
            val comp = packageComparator.compare(importType.selected.toString(), 
                    if(addingStaticImport()) op.clazz else packageOwner(op.clazz))
            if(comp == 0) {
                if(importType.name.toString().compareTo(
                        if(addingStaticImport()) op.staticMethod!! else className(op.clazz)) < 0) 
                    true 
                else false
            }
            else if(comp < 0) true
            else false
        }
    }
    
    fun addingStaticImport() = op.staticMethod is String
}