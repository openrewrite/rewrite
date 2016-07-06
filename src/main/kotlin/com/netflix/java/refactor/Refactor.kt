package com.netflix.java.refactor

import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.ImportTree
import com.sun.source.util.TreePathScanner
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.model.JavacElements
import com.sun.tools.javac.tree.JCTree
import com.sun.tools.javac.util.Context
import com.sun.tools.javac.util.Name
import java.io.File

class Refactor {
    fun methodInvocation(source: String) = SourceScanner.MethodInvocation.MethodInvocationRuleBuilder(source)
    fun refactorType(from: Class<Any>, to: Class<Any>) = SourceScanner.TypeReference(from.name, to.name)
}

sealed class SourceScanner : TreePathScanner<List<LintFix>, RuleContext>() {
    fun run(rule: LintRule, sources: Iterable<File>): Collection<LintFix> {
        val parser = AstParser()
        val filesToCompilationUnit = sources.zip(parser.parseFiles(sources))
        return filesToCompilationUnit.flatMap { 
            val (file, cu) = it
            scan(cu, RuleContext(rule, file, parser.context))
        }
    }
    
    class TypeReference(val from: String, val to: String): SourceScanner() {
        override fun visitImport(node: ImportTree, ruleContext: RuleContext): List<LintFix>? {
            val importType = (node as JCTree.JCImport).qualid as JCTree.JCFieldAccess
            return if(importType.toString() == from) {
                listOf(LintFix(ruleContext, importType.positionIn(), to))
            } else null
        }
        
        override fun visitIdentifier(node: IdentifierTree, ruleContext: RuleContext): List<LintFix>? {
            val ident = node as JCTree.JCIdent
            val import = cu().namedImportScope.getElementsByName(ident.name).firstOrNull() ?:
                cu().starImportScope.getElementsByName(ident.name).firstOrNull()
            
            return if(import is Symbol.ClassSymbol && import.fullname.toString() == from) {
                val toElem = JavacElements.instance(ruleContext.context).getTypeElement(to)
                listOf(LintFix(ruleContext, ident.positionIn(), toElem.name.toString()))
            } else null
        }
    }
    
    class MethodInvocation(): SourceScanner() {
        class MethodInvocationRuleBuilder(source: String) {
            fun refactorArguments(): MethodArgumentFixer = MethodArgumentFixer()
            fun renameTarget(identifier: String): MethodInvocationRuleBuilder = this
            fun renameMethod(name: String): MethodInvocationRuleBuilder = this

            inner class MethodArgumentFixer {
                fun done(): MethodInvocationRuleBuilder = this@MethodInvocationRuleBuilder
                fun <T> argument(index: Int, arg: (T) -> T): MethodArgumentFixer = this
                fun dropArgument(at: Int): MethodArgumentFixer = this
                fun dropArguments(from: Int, to: Int): MethodArgumentFixer = this
            }

            fun build() = SourceScanner.MethodInvocation()
        }
    }

    protected fun cu() = currentPath.compilationUnit as JCTree.JCCompilationUnit
    protected fun JCTree.positionIn() = positionIn(cu())

    override fun reduce(r1: List<LintFix>?, r2: List<LintFix>?) = (r1 ?: emptyList()).plus(r2 ?: emptyList())

    /**
     * TODO look at com.sun.tools.java.comp.Resolve for a generalized name resolution mechanism
     * look at com.sun.tools.java.comp.Enter for generation of Env<AttrContext>
     */
    protected fun Context.isType(name: Name): Boolean {
        val elements = JavacElements.instance(this)
        val cu = currentPath.compilationUnit as JCTree.JCCompilationUnit
        return cu.starImportScope.getElementsByName(name).iterator().hasNext() ||
                cu.namedImportScope.getElementsByName(name).iterator().hasNext() ||
                elements.getTypeElement(name.toString()) != null ||
                cu.packageName != null && elements.getTypeElement(cu.packageName.toString() + "." + name.toString()) != null
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

      linter.refactorType(ILog.class, org.slf4j.Logger.class),
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
