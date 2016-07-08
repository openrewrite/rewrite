package com.netflix.java.refactor

import java.io.File

data class RefactorFix(val position: IntRange,
                       val changes: String?,
                       val source: File) {
    val lineNumber: Int by lazy {
        source.readText().substring(0, position.start).count { it == '\n' } + 1
    }
}