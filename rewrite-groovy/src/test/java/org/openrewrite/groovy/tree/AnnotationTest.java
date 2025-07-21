/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings({"GroovyUnusedAssignment", "GrUnnecessarySemicolon"})
class AnnotationTest implements RewriteTest {

    @Test
    void simple() {
        rewriteRun(
          groovy(
            """
              @Foo
              class Test implements Runnable {
                  @java.lang.Override
                  void run() {}
              }
              """
          )
        );
    }

    @Test
    void simpleFQN() {
        rewriteRun(
          groovy(
            """
              @org.springframework.stereotype.Service
              class Test {}
              """
          )
        );
    }

    @Test
    void withParentheses() {
        rewriteRun(
          groovy(
            """
              @Foo()
              class Test {}
              """
          )
        );
    }

    @Test
    void inline() {
        rewriteRun(
          groovy(
            """
              @Foo class Test implements Runnable {
                  @Override void run() {}
              }
              """
          )
        );
    }

    @Test
    void withProperties() {
        rewriteRun(
          groovy(
            """
              @Foo(value = "A", version = "1.0")
              class Test {}
              """
          )
        );
    }

    @Test
    void withConstantProperty() {
        rewriteRun(
          groovy(
            """
              @Foo(value = Test.VERSION)
              class Test {
                  static final String VERSION = "1.23"
              }
              """
          )
        );
    }

    @Test
    void withStaticallyImportedConstantProperty() {
        rewriteRun(
          groovy(
            """
              import static java.io.File.separator
              @Deprecated(since = separator)
              class Test {
              }
              """
          )
        );
    }

    @Test
    void withImplicitValueProperty() {
        rewriteRun(
          groovy(
            """
              @Foo( "A" )
              class Test {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4055")
    @Test
    void nested() {
        rewriteRun(
          groovy(
            """
              @Foo(bar = @Bar(@Baz(baz = @Qux("1.0"))))
              class Test {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4254")
    @Test
    void groovyTransformAnnotation() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.EqualsAndHashCode
              import groovy.transform.ToString
              
              @Foo
              @ToString
              @EqualsAndHashCode
              @Bar
              class Test {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4254")
    @Test
    void groovyTransformImmutableAnnotation() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.Immutable
              import groovy.transform.TupleConstructor
              
              @Foo
              @TupleConstructor
              @Immutable
              @Bar
              class Test {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4254")
    @Test
    void groovyTransformImmutableFQNAnnotation() {
        rewriteRun(
          groovy(
            """
              @groovy.transform.Immutable
              class Test {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4853")
    @Test
    void annotationOnVariable() {
        rewriteRun(
          groovy(
            """
              @Foo def a = "a"
              """
          )
        );
    }

    @Test
    void groovyTransformFieldAnnotationOnVariable() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.Field
              
              @Field def a = [1, 2, 3]
              """
          )
        );
    }

    @Test
    void groovyTransformFieldFQNAnnotationOnVariable() {
        rewriteRun(
          groovy(
            """
              @groovy.transform.Field def a = [1, 2, 3]
              """
          )
        );
    }

    @Test
    void groovyTransformFieldFQNAnnotationOnVariableWithReference() {
        rewriteRun(
          groovy(
            """
              def z = 1 + 2
              @groovy.transform.Field def a = z
              """
          )
        );
    }

    @Test
    void groovyTransformFieldFQNAnnotationOnVariableWithMethodInvocation() {
        rewriteRun(
          groovy(
            """
              @groovy.transform.Field def a = callSomething()
              """
          )
        );
    }
}
