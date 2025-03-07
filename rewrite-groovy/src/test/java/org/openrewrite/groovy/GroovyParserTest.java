package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class GroovyParserTest {

    @Test
    void groovyPackageDefinition() {
        assertThatCode(() -> {
            GroovyParser.builder().build()
              .parse(
                """
                  package org.openrewrite.groovy
                  
                  class A {
                      static void main(String[] args) {
                         String name = "John"
                         println(name)
                      }
                   }
                  """,
                """
                  package org.openrewrite.groovy;
                  
                  class B {
                      static void main(String[] args) {
                         String name = "Doe"
                         println(name)
                      }
                   }
                  """
              );
        }).doesNotThrowAnyException();

    }

}