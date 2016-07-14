package com.netflix.java.refactor.fix

import com.netflix.java.refactor.FixingOperation
import com.netflix.java.refactor.FixingScanner
import com.netflix.java.refactor.RefactorFix
import com.sun.source.tree.ImportTree
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import java.util.*

class AddImport(val pkg: String, val clazz: String): FixingOperation {
    override fun scanner() = AddImportScanner(this)
}

class AddImportScanner(val op: AddImport): FixingScanner() {
    val imports = ArrayList<JCTree.JCImport>()
    var coveredByExistingImport = false
    
    override fun visitImport(node: ImportTree?, context: Context): List<RefactorFix>? {
        val import = node as JCTree.JCImport
        val importType = import.qualid as JCTree.JCFieldAccess
        imports.add(import)
        
        if(importType.toString() == "${op.pkg}.${op.clazz}") {
            coveredByExistingImport = true
        }
        else if(importType.name.toString() == "*" && importType.selected.toString() == op.pkg) {
            coveredByExistingImport = true
        }
        
        return null
    }

    override fun visitEnd(context: Context): List<RefactorFix> {
        val lastPrior = lastPriorImport()
        
        return if(coveredByExistingImport) {
            emptyList()
        }
        else if(lastPrior == null && imports.isNotEmpty()) {
            listOf(imports.first().insertBefore("import ${op.pkg}.${op.clazz};\n"))
        }
        else if(lastPrior is JCTree.JCImport) {
            listOf(lastPrior.insertAfter("import ${op.pkg}.${op.clazz};\n"))
        }
        else if(cu.packageName != null) {
            listOf(cu.packageName.insertAfter("\n\nimport ${op.pkg}.${op.clazz};"))
        }
        else listOf(cu.insertBefore("import ${op.pkg}.${op.clazz};\n"))
    }
    
    fun lastPriorImport(): JCTree.JCImport? {
        return imports.lastOrNull { import ->
            val importType = import.qualid as JCTree.JCFieldAccess
            val comp = packageComparator.compare(importType.selected.toString(), op.pkg)
            if(comp == 0) {
                if(importType.name.toString().compareTo(op.clazz) < 0) 
                    true 
                else false
            }
            else if(comp < 0) true
            else false
        }
    }
    
    val packageComparator = Comparator<String> { p1, p2 ->
        val p1s = p1.split(".")
        val p2s = p2.split(".")
        
        p1s.forEachIndexed { i, fragment ->
            if(p2s.size < i + 1) return@Comparator 1
            if(fragment != p2s[i]) return@Comparator fragment.compareTo(p2s[i])
        }
        
        if(p1s.size < p2s.size) -1 else 0
    }
}