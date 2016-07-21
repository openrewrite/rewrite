package com.netflix.java.refactor

abstract class JavaSourceScanner<out T> {
    abstract fun scan(source: JavaSource): T?
    
    fun name() = javaClass.getAnnotation(AutoRefactor::class.java).value
    fun description() = javaClass.getAnnotation(AutoRefactor::class.java).description
}