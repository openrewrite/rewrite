package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorFix
import com.netflix.java.refactor.RefactorRule
import com.sun.source.tree.MethodInvocationTree
import com.sun.tools.javac.tree.JCTree

class ChangeMethodInvocation(val signature: String, val containingRule: RefactorRule): RefactorOperation {
    override val scanner = ChangeMethodInvocationScanner(this)
    
    var targetTypeConstraint: String? = null
    var refactorName: String? = null

    fun whereTargetIsType(clazz: String): ChangeMethodInvocation {
        targetTypeConstraint = clazz
        return this
    }
    
    fun whereTargetIsType(clazz: Class<Any>) = whereTargetIsType(clazz.name)
    
    fun refactorName(name: String): ChangeMethodInvocation {
        refactorName = name
        return this
    }
    
    fun done() = containingRule
}

class MethodArgumentMatcher {
    var typeConstraint: String? = null
    
    fun isType(clazz: String): MethodArgumentMatcher {
        typeConstraint = clazz
        return this
    }

    fun isType(clazz: Class<Any>) = isType(clazz.name)
}

class ChangeMethodInvocationScanner(val op: ChangeMethodInvocation): BaseRefactoringScanner() {
    override fun visitMethodInvocation(node: MethodInvocationTree, session: Session): List<RefactorFix>? {
        val meth = node as JCTree.JCMethodInvocation
        
        return null
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
        .whereTargetIsType(ILog.class)
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
