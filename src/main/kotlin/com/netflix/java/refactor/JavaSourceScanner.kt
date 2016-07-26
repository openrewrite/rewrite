package com.netflix.java.refactor

interface JavaSourceScanner<out T> {
    fun scan(source: JavaSource): T
}