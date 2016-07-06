package com.netflix.java.refactor

import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import java.io.File

enum class Level {
    Info, Trivial, Warning, Error
}

data class LintRule(val id: String, 
                    val description: String, 
                    val level: Level = Level.Warning)

data class Position(val start: Int, // 0-based
                    val end: Int, // 0-based
                    val lineNumber: Int, // 1-based, inclusive
                    val lastLineNumber: Int, // 1-based, inclusive
                    val columnNumber: Int, // 1-based, inclusive
                    val lastColumnNumber: Int) // 1-based, exclusive

data class RuleContext(val rule: LintRule,
                       val source: File,
                       val context: Context)

data class LintFix(val ruleContext: RuleContext,
                   val pos: Position,
                   val changes: String)

fun JCTree.positionIn(cu: JCTree.JCCompilationUnit): Position {
    val rawSource = cu.sourcefile.getCharContent(true)

    val startPos = this.startPosition
    val endPos = this.getEndPosition(cu.endPositions)

    val beforeStatement = rawSource.substring(0, startPos)
    val statement = rawSource.substring(startPos, endPos)

    val lineNumber = beforeStatement.count { it == '\n' } + 1
    val lastLineNumber = lineNumber + statement.count { it == '\n' }
    val columnNumber = startPos - beforeStatement.lastIndexOf('\n')

    return Position(
            startPos,
            endPos,
            lineNumber,
            lastLineNumber,
            columnNumber,
            lastColumnNumber = endPos - (statement.lastIndexOf('\n') + startPos + 1) +
                    if (lineNumber == lastLineNumber) columnNumber else 0
    )
}