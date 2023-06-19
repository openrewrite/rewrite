package org.openrewrite.java;

import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.RecipeSpec;
import org.junit.jupiter.api.Test;

import static org.openrewrite.java.Assertions.java;

class AddScopeToInjectedClassTest implements RewriteTest {

  /**  @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddScopeToInjectedClass())
                .cycles(1);
             //   .expectedCyclesThatMakeChanges(1);
    }*/

    @DocumentExample
    @Test
    void scopeRequired() {
        rewriteRun(spec -> spec.recipe(new AddScopeToInjectedClass())
                .cycles(1),
                java("""
        package com.sample.service;

        public class Bar{}
        """,
                        """
                        package com.sample.service;

                        import javax.enterprise.context.Dependent;

                        @Dependent
                        public class Bar{}
                        """),

        java("""
        package com.sample;

        import javax.inject.Inject;
        import com.sample.service.Bar;

        public class Foo{

            @javax.inject.Inject
            Bar service;
        }
        """));
    }



}