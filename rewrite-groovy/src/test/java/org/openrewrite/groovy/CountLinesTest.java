package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

public class CountLinesTest implements RewriteTest {

    @Test
    void countsLines() {
        rewriteRun(
          groovy(
            """
              package com.whatever
              
              import java.util.List
              
              // comments don't count
              class A {
                  
                  
                  List<String> foo() {
                  }
              }
              """,
            spec -> spec.afterRecipe(cu ->
              assertThat(CountLinesVisitor.countLines(cu)).isEqualTo(4))
          )
        );
    }
}

