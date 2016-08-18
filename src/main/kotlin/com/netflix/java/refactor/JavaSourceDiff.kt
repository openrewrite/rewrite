package com.netflix.java.refactor

class JavaSourceDiff(private val source: JavaSource) {
    val before = source.text()
    
    fun gitStylePatch(): String {
        val after = source.text()
        return InMemoryDiffEntry(source.file().toString(), before, after).diff
    }
}