package org.openrewrite.scala;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;

public class TupleAssignDebugTest implements RewriteTest {
    @Test
    @org.junit.jupiter.api.Disabled("Known issue: Scala 3 compiler AST spans include equals sign in tuple assignment LHS")
    void debugTupleAssignment() {
        rewriteRun(
          scala(
            """
            object Test {
              var (a, b) = (1, 2)
              (a, b) = (3, 4)
            }
            """
          )
        );
    }
    
    @Test
    void simpleTupleDeclaration() {
        rewriteRun(
          scala(
            """
            object Test {
              val (a, b) = (1, 2)
            }
            """
          )
        );
    }
}