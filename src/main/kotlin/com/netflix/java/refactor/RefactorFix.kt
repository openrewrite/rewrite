package com.netflix.java.refactor

import java.io.File

data class RefactorFix(val position: IntRange,
                       val changes: String?,
                       val source: File)