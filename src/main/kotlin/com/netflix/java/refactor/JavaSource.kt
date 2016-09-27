package com.netflix.java.refactor

import com.netflix.java.refactor.find.*
import com.netflix.java.refactor.find.Annotation
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.tree.JCTree
import java.nio.file.Files

class JavaSource<P>(val cu: CompilationUnit, val datum: P) {
    var changedFile = false

    fun file() = cu.source()
    fun text() = String(Files.readAllBytes(cu.source()))

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
    fun hasType(clazz: String): Boolean = HasType(clazz).scanner().scan(cu)

    fun hasImport(clazz: Class<*>): Boolean = HasImport(clazz.name).scanner().scan(cu)
    fun hasImport(clazz: String): Boolean = HasImport(clazz).scanner().scan(cu)

    fun findAnnotations(clazz: String): List<Annotation> = FindAnnotations(clazz).scanner().scan(cu)
    fun findAnnotations(clazz: Class<*>): List<Annotation> = FindAnnotations(clazz.name).scanner().scan(cu)

    /**
     * Find fields defined on this class, but do not include inherited fields up the type hierarchy
     */
    fun findFields(clazz: Class<*>): List<Field> = FindFields(clazz.name, false).scanner().scan(cu)
    fun findFields(clazz: String): List<Field> = FindFields(clazz, false).scanner().scan(cu)

    /**
     * Find fields defined both on this class and visible inherited fields up the type hierarchy
     */
    fun findFieldsIncludingInherited(clazz: Class<*>): List<Field> = FindFields(clazz.name, true).scanner().scan(cu)
    fun findFieldsIncludingInherited(clazz: String): List<Field> = FindFields(clazz, true).scanner().scan(cu)

    fun findMethodCalls(signature: String): List<Method> = FindMethods(signature).scanner().scan(cu)

    fun diff(body: JavaSource<*>.() -> Unit): String {
        val before = text()
        this.body()
        val after = text()
        return InMemoryDiffEntry(file().toString(), before, after).diff
    }

    fun beginDiff() = JavaSourceDiff(this)
}