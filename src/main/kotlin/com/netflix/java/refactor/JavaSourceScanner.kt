package com.netflix.java.refactor

interface JavaSourceScanner<P, out R> {
    fun scan(source: JavaSource<P>): R
}