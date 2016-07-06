package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorFix
import com.sun.source.tree.ImportTree
import com.sun.tools.javac.tree.JCTree
import java.util.*

class AddImport(val pkg: String, val clazz: String): RefactorOperation {
    override val scanner = AddImportScanner(this)
}

class AddImportScanner(val op: AddImport): BaseRefactoringScanner() {
    val imports = ArrayList<JCTree.JCImport>()
    var coveredByExistingImport = false
    
    override fun visitImport(node: ImportTree?, p: Session?): List<RefactorFix>? {
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

    override fun visitEnd(session: Session): List<RefactorFix>? {
        val lastPrior = lastPriorImport(session)
        
        return if(coveredByExistingImport) {
            null
        }
        else if(lastPrior == null && imports.isNotEmpty()) {
            listOf(imports.first().insertBefore("import ${op.pkg}.${op.clazz};\n", session))
        }
        else if(lastPrior is JCTree.JCImport) {
            listOf(lastPrior.insertAfter("import ${op.pkg}.${op.clazz};\n", session))
        }
        else if(session.cu.packageName != null) {
            listOf(session.cu.packageName.insertAfter("\n\nimport ${op.pkg}.${op.clazz};", session))
        }
        else listOf(session.cu.insertBefore("import ${op.pkg}.${op.clazz};\n", session))
    }
    
    fun lastPriorImport(session: Session): JCTree.JCImport? {
        return imports.lastOrNull { import ->
            val importType = import.qualid as JCTree.JCFieldAccess
            when(packageComparator.compare(importType.selected.toString(), op.pkg)) {
                0 -> {
                    if(importType.name.toString().compareTo(op.clazz) < 0) 
                        true 
                    else false
                }
                -1 -> true
                else -> false
            }
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