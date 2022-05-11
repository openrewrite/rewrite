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
package org.openrewrite.java.search

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

interface UriCreatedWithHttpSchemeTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(UriCreatedWithHttpScheme())
    }

    @Disabled
    @Test
    fun findInsecureUri(javaParser: JavaParser) = rewriteRun(
        { spec -> spec.parser(javaParser) },
        java(
            """
                import java.net.URI;
                class Test {
                    void test() {
                        String s = "http://test";
                        if(System.currentTimeMillis() > 0) {
                            System.out.println(URI.create(s));
                        } else {
                            System.out.println(URI.create(s));
                        }
                    }
                }
            """, """
                import java.net.URI;
                class Test {
                    void test() {
                        String s = "https://test";
                        if(System.currentTimeMillis() > 0) {
                            System.out.println(URI.create(s));
                        } else {
                            System.out.println(URI.create(s));
                        }
                    }
                }
            """
        )
    )

    @Disabled
    @Test
    fun replaceBarrierGuard(javaParser: JavaParser) = rewriteRun(
        { spec -> spec.parser(javaParser) },
        java(
            """
                import java.net.URI;
                class Test {
                    void test() {
                        String s = "http://test";
                        s = s.replace("http://", "https://");
                        if(System.currentTimeMillis() > 0) {
                            System.out.println(URI.create(s));
                        } else {
                            System.out.println(URI.create(s));
                        }
                    }
                }
            """
        )
    )
}
