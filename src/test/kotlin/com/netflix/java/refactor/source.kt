package com.netflix.java.refactor

import java.util.regex.Pattern

fun fullyQualifiedName(sourceStr: String): String? {
    val pkgMatcher = Pattern.compile("\\s*package\\s+([\\w\\.]+)").matcher(sourceStr)
    val pkg = if (pkgMatcher.find()) pkgMatcher.group(1) + "." else ""

    val classMatcher = Pattern.compile("\\s*(class|interface|enum)\\s+(\\w+)").matcher(sourceStr)
    return if (classMatcher.find()) pkg + classMatcher.group(2) else null
}
