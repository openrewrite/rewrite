package com.netflix.java.refactor

import com.netflix.java.refactor.find.*
import com.sun.tools.javac.tree.JCTree

class JavaSource(internal val cu: CompilationUnit) {
    var changedFile = false

    fun file() = cu.source()
    fun text() = cu.source().readText()
    fun classes() = cu.jcCompilationUnit.defs.filterIsInstance<JCTree.JCClassDecl>().map { it.name.toString() }
    
    internal var lastCommitChangedFile = false

    fun refactor(): RefactorTransaction {
        val tx = RefactorTransaction(this)

        if(lastCommitChangedFile) {
            cu.reparse()
            lastCommitChangedFile = false
        }
        return tx
    }
    
    fun hasType(clazz: Class<*>): Boolean = HasType(clazz.name).scanner().scan(cu)
    
    fun findFields(clazz: Class<*>): List<Field> = FindFields(clazz.name).scanner().scan(cu)
    fun findMethods(signature: String): List<Method> = FindMethods(signature).scanner().scan(cu)
}