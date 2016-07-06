package com.netflix.java.refactor

class Patch2() {
    fun patchFiles(fixes: Collection<LintFix>) =
        fixes.groupBy { it.file }
            .map { 
                val fileText = it.key.readText()
                val sortedFixes = it.value.sortedBy { it.pos.start }
                var source = sortedFixes.foldIndexed("") { i, source, fix ->
                    val prefix = if(i == 0)
                        fileText.substring(0, fix.pos.start)
                    else fileText.substring(sortedFixes[i-1].pos.end, fix.pos.start)
                    source + prefix + fix.changes
                }
                if(sortedFixes.last().pos.end < fileText.length) {
                   source += fileText.substring(sortedFixes.last().pos.end) 
                }
                it.key.writeText(source)
                it.key
            }
}