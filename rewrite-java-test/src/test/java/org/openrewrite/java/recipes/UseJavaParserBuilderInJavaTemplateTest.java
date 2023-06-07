/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class UseJavaParserBuilderInJavaTemplateTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseJavaParserBuilderInJavaTemplate())
          .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath())
            .dependsOn(
              """
                package org.openrewrite.java;
                
                import org.openrewrite.Cursor;
                import java.util.function.Supplier;
                
                public class JavaTemplate {
                    public static Builder builder(Supplier<Cursor> parentScope, String code) {
                        return new Builder();
                    }
                    public static class Builder {
                        public Builder javaParser(Supplier<JavaParser> javaParser) {
                            return this;
                        }
                        public Builder javaParser(JavaParser.Builder<?, ?> javaParser) {
                            return this;
                        }
                    }
                }
                """));
    }

    @DocumentExample
    @Test
    void useBuilder() {
        rewriteRun(
          java(
            """
              import org.openrewrite.java.*;
              class MyTest {
                  Object o = JavaTemplate.builder(() -> null, "")
                    .javaParser(() -> JavaParser.fromJavaVersion().build());
              }
              """,
            """
              import org.openrewrite.java.*;
              class MyTest {
                  Object o = JavaTemplate.builder(() -> null, "")
                    .javaParser(JavaParser.fromJavaVersion());
              }
              """
          )
        );
    }
}
