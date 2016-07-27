package com.netflix.java.refactor

import com.netflix.java.refactor.find.*
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.tree.JCTree

class JavaSource(internal val cu: CompilationUnit) {
    var changedFile = false

    fun file() = cu.source()
    fun text() = cu.source().readText()
    fun classes() = cu.jcCompilationUnit.defs
            .filterIsInstance<JCTree.JCClassDecl>()
            .map { it.sym }
            .filterIsInstance<Symbol.ClassSymbol>()
            .map { it.toString() }
    
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

    /**
     * Find fields defined on this class, but do not include inherited fields up the type hierarchy 
     */
    fun findFields(clazz: Class<*>): List<Field> = FindFields(clazz.name, false).scanner().scan(cu)

    /**
     * Find fields defined both on this class and visible inherited fields up the type hierarchy
     */
    fun findFieldsIncludingInherited(clazz: Class<*>): List<Field> = FindFields(clazz.name, true).scanner().scan(cu)
    
    fun findMethodCalls(signature: String): List<Method> = FindMethods(signature).scanner().scan(cu)
}