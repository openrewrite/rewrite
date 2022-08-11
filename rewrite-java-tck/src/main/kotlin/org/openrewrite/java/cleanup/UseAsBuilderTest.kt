/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.JavaParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

interface UseAsBuilderTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(UseAsBuilder("org.openrewrite.java.JavaParser.Builder", true,
            "org.openrewrite.java.JavaParser fromJavaVersion()"))
    }

    @Suppress("rawtypes")
    @Test
    fun useAsBuilder(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec ->
            spec.parser(jp.classpath(JavaParser.runtimeClasspath()).build())
        },
        java(
            """
                import org.openrewrite.java.JavaParser;
                class Test {
                    void test() {
                        int a = 0;
                        JavaParser.Builder builder = JavaParser.fromJavaVersion();
                        String b = "rewrite-java";
                        int c = 0;
                        builder = builder.classpath(b);
                        int d = 0;
                    }
                }
            """,
            """
                import org.openrewrite.java.JavaParser;
                class Test {
                    void test() {
                        int a = 0;
                        String b = "rewrite-java";
                        int c = 0;
                        JavaParser.Builder builder = JavaParser.fromJavaVersion()
                                .classpath(b);
                        int d = 0;
                    }
                }
            """
        )
    )
}
