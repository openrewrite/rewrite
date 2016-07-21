package com.netflix.java.refactor

import com.netflix.java.refactor.ast.FixingOperation
import com.netflix.java.refactor.find.*
import com.netflix.java.refactor.fix.*
import java.util.*

class JavaSource(val cu: CompilationUnit) {
    var changedFile = false

    fun file() = cu.source()
    fun text() = cu.source().readText()
    
    internal var lastCommitChangedFile = false

    fun refactor(): RefactorTransaction {
        val tx = RefactorTransaction(this)

        if(lastCommitChangedFile) {
            cu.reparse()
            lastCommitChangedFile = false
        }
        return tx
    }
    
    fun hasType(clazz: Class<*>): Boolean =
        HasType(clazz.name).scanner().scan(cu)
    
    fun findField(clazz: Class<*>): Field =
        FindField(clazz.name).scanner().scan(cu)
    
    fun hasField(clazz: Class<*>) = findField(clazz).exists
    
    fun findMethod(signature: String): Method =
        FindMethod(signature).scanner().scan(cu)
    
    fun hasMethod(signature: String) = findMethod(signature).exists
    
    fun changeType(from: String, to: String) {
        refactor().changeType(from, to).fix()
    }
    
    fun changeType(from: Class<*>, to: Class<*>) {
        refactor().changeType(from, to).fix()
    }

    fun changeMethod(signature: String): ChangeMethodInvocation = refactor().changeMethod(signature)

    fun changeField(clazz: Class<*>): ChangeField = refactor().changeField(clazz)
    fun changeField(clazz: String): ChangeField = refactor().changeField(clazz)
    
    fun removeImport(clazz: String) {
        refactor().removeImport(clazz).fix()
    }
    
    fun removeImport(clazz: Class<*>) {
        refactor().removeImport(clazz).fix()
    }

    fun addImport(clazz: String) {
        refactor().addImport(clazz).fix()
    }

    fun addImport(clazz: Class<*>) {
        refactor().addImport(clazz).fix()
    }
    
    fun addStaticImport(clazz: String, method: String) {
        refactor().addStaticImport(clazz, method).fix()
    }
    
    fun addStaticImport(clazz: Class<*>, method: String) {
        refactor().addStaticImport(clazz, method).fix()
    }
}


class RefactorTransaction(val refactorer: JavaSource) {
    private val ops = ArrayList<FixingOperation>()
    
    fun changeType(from: String, to: String): RefactorTransaction {
        ops.add(ChangeType(from, to))
        return this
    }

    fun changeType(from: Class<*>, to: Class<*>) = changeType(from.name, to.name)

    fun changeMethod(signature: String): ChangeMethodInvocation {
        val changeMethod = ChangeMethodInvocation(signature, this)
        ops.add(changeMethod)
        return changeMethod
    }

    fun changeField(clazz: Class<*>): ChangeField = changeField(clazz.name)
    
    fun changeField(clazz: String): ChangeField {
        val changeField = ChangeField(clazz, this)
        ops.add(changeField)
        return changeField
    }

    fun removeImport(clazz: String): RefactorTransaction {
        ops.add(RemoveImport(clazz))
        return this
    }

    fun removeImport(clazz: Class<*>) = removeImport(clazz.name)

    fun addImport(clazz: String): RefactorTransaction {
        ops.add(AddImport(clazz))
        return this
    }

    fun addImport(clazz: Class<*>) = addImport(clazz.name)
    
    fun addStaticImport(clazz: String, method: String): RefactorTransaction {
        ops.add(AddImport(clazz, method))
        return this
    }
    
    fun addStaticImport(clazz: Class<*>, method: String) = addStaticImport(clazz.name, method)
    
    fun fix() {
        val fixes = ops.flatMap { it.scanner().scan(refactorer.cu) }
        
        if(fixes.isNotEmpty()) {
            try {
                val sourceText = refactorer.text()
                val sortedFixes = fixes.sortedBy { it.position.last }.sortedBy { it.position.start }
                var source = sortedFixes.foldIndexed("") { i, source, fix ->
                    val prefix = if (i == 0)
                        sourceText.substring(0, fix.position.first)
                    else sourceText.substring(sortedFixes[i - 1].position.last, fix.position.start)
                    source + prefix + (fix.changes ?: "")
                }
                if (sortedFixes.last().position.last < sourceText.length) {
                    source += sourceText.substring(sortedFixes.last().position.last)
                }
                refactorer.file().writeText(source)
            } catch(t: Throwable) {
                // TODO how can we throw a better exception?
                t.printStackTrace()
            }
        }
        
        if(fixes.isNotEmpty()) {
            refactorer.changedFile = true
            refactorer.lastCommitChangedFile = true
        }
    }
}