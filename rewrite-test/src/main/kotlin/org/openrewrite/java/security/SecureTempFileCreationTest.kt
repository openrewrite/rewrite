/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.security

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface SecureTempFileCreationTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = SecureTempFileCreation()

    @Test
    fun twoArgCreateTempFile(jp: JavaParser) = assertChanged(
        before = """
            import java.io.File;
            
            public class A {
                File tempDir = File.createTempFile("hello", "world");
            }
        """,
        after = """
            import java.io.File;
            import java.nio.file.Files;
            
            public class A {
                File tempDir = Files.createTempFile("hello", "world").toFile();
            }
        """
    )

    @Test
    fun threeArgCreateTempFile(jp: JavaParser) = assertChanged(
        before = """
            import java.io.File;
            
            public class A {
                File tempDir = File.createTempFile("hello", "world", new File("."));
            }
        """,
        after = """
            import java.io.File;
            import java.nio.file.Files;
            
            public class A {
                File tempDir = Files.createTempFile(new File(".").toPath(), "hello", "world").toFile();
            }
        """
    )
}
