package com.netflix.java.refactor

class JavaSourceDiff(private val source: JavaSource) {
    private val before = source.text()
    
    fun gitStylePatch() = InMemoryDiffEntry(source.file().toString(), before, source.text()).diff  
}