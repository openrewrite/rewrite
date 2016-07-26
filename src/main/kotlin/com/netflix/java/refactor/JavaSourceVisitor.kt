package com.netflix.java.refactor

/**
 * For convenience in Java code when we don't need to return anything
 */
abstract class JavaSourceVisitor: JavaSourceScanner<Void?> {
    override fun scan(source: JavaSource): Void? {
        visit(source)
        return null
    }
    
    abstract fun visit(source: JavaSource)
}