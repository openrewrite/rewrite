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
@file:Suppress("ClassInitializerMayBeStatic", "EmptyTryBlock")

package org.openrewrite.java.tree

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.openrewrite.Issue
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.JavaParserResolver
import org.openrewrite.test.RewriteTest

@ExtendWith(JavaParserResolver::class)
class TryCatchJava17Test: RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/763")
    @Test
    fun tryWithResourcesIdentifier() = rewriteRun(
        java(
        """
            import java.io.InputStream;
            class A {
                {
                    InputStream in;
                    try (in) {
                    }
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1027")
    @Test
    fun tryWithResourcesIdentifierAndVariables() = rewriteRun(
        java("""
            import java.io.File;
            import java.io.FileInputStream;
            import java.util.Scanner;
            
            class A {
                void a() throws Exception {
                    FileInputStream fis = new FileInputStream(new File("file.txt"));
                    try (fis; Scanner sc = new Scanner("")) {
                    }
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1027")
    @Test
    fun tryWithResourcesIdentifierAndSemicolon() = rewriteRun(
        java("""
            import java.io.File;
            import java.io.FileInputStream;
            import java.util.Scanner;

            class A {
                void a() throws Exception {
                    FileInputStream fis = new FileInputStream(new File("file.txt"));
                    try (fis;) {
                    }
                }
            }
        """)
    )
}
