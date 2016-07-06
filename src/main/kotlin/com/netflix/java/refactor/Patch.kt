package com.netflix.java.refactor

import java.io.File
import java.util.*

class Patch(val relativeTo: File, val patchName: String = "refactor.patch") {
    fun patchToFile(fixes: Collection<LintFix>): File {
        val patch = File(patchName)
        patch.writeText(patchAsString(fixes))
        return patch
    }
    
    fun patchAsString(fixes: Collection<LintFix>): String {
        val patchSets = fixes.groupBy { it.file }
                .mapValues { it.value.sortedBy { it.pos.lineNumber } }
        
        // TODO deal with overlaps
        
        var combinedPatch = ""
        
        patchSets.entries.forEachIndexed { i, filePatchFix ->
            val (file, patchFixes) = filePatchFix
            val newlineAtEndOfOriginal = file.readText().last() == '\n'
            var firstLineOfContext = 1
            var beforeLineCount = 0
            var afterLineCount = 0
            
            // generate just this patch
            val lines = listOf("").plus(file.readLines()) // the extra empty line is so we don't have to do a bunch of zero-based conversions for line arithmetic
            
            val patch = mutableListOf<String>()
            patchFixes.sortedWith(Comparator { f1, f2 -> f1.pos.lineNumber.compareTo(f2.pos.lineNumber) }).forEachIndexed { j, fix ->
                val lastFix = j == patchFixes.size - 1
                
                // 'before' context
                if(fix.pos.lineNumber > 0) {
                    val beforeContext = (
                        if(j == 0) {
                            val firstLine = Math.max(fix.pos.lineNumber - 3, 1)
                            lines.subList(firstLine, fix.pos.lineNumber)
                        } else {
                            lines.subList(patchFixes[j-1].pos.lastLineNumber + 1, fix.pos.lineNumber)
                        }
                    ).map { " $it" }.dropWhile { j == 0 && it.isBlank() }
                    
                    if(j == 0)
                        firstLineOfContext = Math.min(firstLineOfContext, fix.pos.lineNumber)
                }

                // - lines (lines being replaced, deleted)
                val changed = lines.subList(fix.pos.lineNumber, fix.pos.lastLineNumber + 1).map { "-$it" }
                patch.addAll(changed)
                beforeLineCount += changed.size
                
                if(j == 0 && fix.pos.lastLineNumber + 1 == lines.size && !newlineAtEndOfOriginal && changed.last() != "\n") {
                    patch += "\\ No newline at end of file"
                }
                
                // + lines (to be included in new file)
                val changeLines = fix.changes.split("\n").withIndex().toList()
                val changes = changeLines.map {
                    val (k, line) = it
                    var changedLine = line
                    if(k == 0) {
                        val affected = lines[fix.pos.lineNumber]
                        changedLine = affected.substring(0, 
                                if(fix.pos.columnNumber < 0) affected.length + fix.pos.columnNumber
                                else fix.pos.columnNumber - 1) + line
                    }
                    if(k == changeLines.size - 1) {
                        val affected = lines[fix.pos.lastLineNumber]
                        changedLine += affected.substring(if(fix.pos.lastColumnNumber < 0) affected.length + fix.pos.lastColumnNumber + 1
                            else fix.pos.lastColumnNumber - 1)
                    }
                    if(changedLine.isNotBlank()) "+$changedLine" else null
                }.filterNotNull()
                
                patch.addAll(changes)
                afterLineCount += changes.size
                
                // 'after' context
                if(fix.pos.lastLineNumber < lines.size - 1 && lastFix) {
                    val lastLineOfContext = Math.min(fix.pos.lastLineNumber + 3 + 1, lines.size)
                    val afterContext = lines.subList(fix.pos.lastLineNumber + 1, lastLineOfContext)
                        .map { " $it" }
                        .dropLastWhile { it.isBlank() }
                    
                    beforeLineCount += afterContext.size
                    afterLineCount += afterContext.size
                    
                    patch.addAll(afterContext)
 
                    if(lastLineOfContext == lines.size && !newlineAtEndOfOriginal) {
                        patch += "\\ No newline at end of file"
                    }
                }
                else if(lastFix && fix.changes.last() != '\n' && !newlineAtEndOfOriginal) {
                    patch += "\\ No newline at end of file"
                }
                
                // combine it with all the other patches
                if(i > 0) {
                    combinedPatch += "\n"
                }
            }
          
            val relativePath = relativeTo.toPath().relativize(file.toPath()).toString()
            val diffHeader = """
                |diff --git a/$relativePath b/$relativePath
                |--- a/$relativePath
                |+++ b/$relativePath
                |@@ -$firstLineOfContext,$beforeLineCount +${if(afterLineCount == 0) 0 else firstLineOfContext},$afterLineCount @@
                |""".trimMargin()
            
            combinedPatch += diffHeader + patch.joinToString("\n")
        }
        
        combinedPatch += "\n"
        return combinedPatch
    }
}
