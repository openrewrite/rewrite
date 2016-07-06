package com.netflix.java.refactor

import com.netflix.java.refactor.op.ChangeType
import com.netflix.java.refactor.op.RefactorOperation
import com.netflix.java.refactor.op.RemoveImport
import java.io.File
import java.util.*

class RefactorRule(val id: String,
                   val description: String = "") {

    private val ops = ArrayList<RefactorOperation>()
    
    fun changeType(from: String, to: String): RefactorRule {
        ops.add(ChangeType(from, to))
        return this
    }

    fun changeType(from: Class<Any>, to: Class<Any>) = changeType(from.name, to.name)

    fun removeImport(clazz: String): RefactorRule {
        ops.add(RemoveImport(clazz))
        return this
    }
    
    fun removeImport(clazz: Class<Any>) = removeImport(clazz.name)
    
    /**
     * Perform refactoring on a set of source files
     */
    fun refactor(vararg sources: File): List<RefactorFix> =
        ops.flatMap { op ->
            val parser = AstParser()
            val filesToCompilationUnit = sources.zip(parser.parseFiles(sources.toList()))
            filesToCompilationUnit.flatMap {
                val (file, cu) = it
                op.scanner.scan(cu, parser.context, file)
            }
        }
    
    fun refactorAndFix(vararg sources: File): List<RefactorFix> {
        val fixes = refactor(*sources)
        fixes.groupBy { it.source }
                .forEach {
                    val fileText = it.key.readText()
                    val sortedFixes = it.value.sortedBy { it.position.first }
                    var source = sortedFixes.foldIndexed("") { i, source, fix ->
                        val prefix = if(i == 0)
                            fileText.substring(0, fix.position.first)
                        else fileText.substring(sortedFixes[i-1].position.last, fix.position.start)
                        source + prefix + (fix.changes ?: "")
                    }
                    if(sortedFixes.last().position.last < fileText.length) {
                        source += fileText.substring(sortedFixes.last().position.last)
                    }
                    it.key.writeText(source)
                }
        return fixes
    }
}

/*
class ILogLintRule {
  static Linter linter = new Linter();

  // generate a properties file with this name in META-INF/lint-rules
  @LintRule("ilog-to-slf4j", description = "we want to use SLF4J", severity = Level.Warning)
  static List<LintRule> iLogToSlf4j() {
    return Arrays.asList(
      linter.methodInvocation("logger.info(contains('%s'))")

      linter.methodInvocation("logger.info(s)")
        .whereArgument("s").isType(String.class)

      linter.methodInvocation("logger.info(any, any, ...)")
        .whereVariableIsType(ILog.class)
        .refactorArguments()
          .argument(0, ...)
          .argument(1, ...)
          .done()
        .refactorSomethingElse()
        .build(),

      linter.changeType(ILog.class, org.slf4j.Logger.class),
      linter.refactorFieldType(ILog.class, org.slf4j.Logger.class), // is this useful?

      linter.staticMethodInvocation("Log.getLogger(any)")
        .refactorTargetTypeTo(org.slf4j.Logger.class)
    );
  }

  @LintRule("ilog-to-juli")
  static List<LintRule> iLogToJuli() {

  }
}
*/
