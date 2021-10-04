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
@file:Suppress("ResultOfMethodCallIgnored")

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

interface UseFilesCreateTempDirectoryTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = UseFilesCreateTempDirectory()

    @Test
    fun useFilesCreateTempDirectory() = assertChanged(
        before = """
            import java.io.File;
            import java.io.IOException;
            
            class A {
                void b() throws IOException {
                    File tempDir;
                    tempDir = File.createTempFile("abc", ".");
                    tempDir.delete();
                    tempDir.mkdir();
                    System.out.println(tempDir.getAbsolutePath());
                }
            }
        """,
        after = """
            import java.io.File;
            import java.io.IOException;
            import java.nio.file.Files;
            
            class A {
                void b() throws IOException {
                    File tempDir;
                    tempDir = Files.createTempDirectory("abc").toFile();
                    System.out.println(tempDir.getAbsolutePath());
                }
            }
        """
    )

    @Test
    fun useFilesCreateTempDirectoryWithParentDir() = assertChanged(
        before = """
            import java.io.File;
            import java.io.IOException;
            import java.nio.file.Files;
            
            class A {
                File testData = Files.createTempDirectory("").toFile();
                void b() throws IOException {
                    File tmpDir = File.createTempFile("test", "dir", testData);
                    tmpDir.delete();
                    tmpDir.mkdir();
                    System.out.println(tmpDir.getAbsolutePath());
                }
            }
        """,
        after = """
            import java.io.File;
            import java.io.IOException;
            import java.nio.file.Files;
            
            class A {
                File testData = Files.createTempDirectory("").toFile();
                void b() throws IOException {
                    File tmpDir = Files.createTempDirectory(testData.toPath(), "test").toFile();
                    System.out.println(tmpDir.getAbsolutePath());
                }
            }
        """
    )

    @Test
    fun useFilesCreateTempDirectory2() = assertChanged(
        before = """
            import java.io.File;
            import java.io.IOException;
            
            class A {
                void b() throws IOException {
                    File tempDir = File.createTempFile("abc", ".");
                    tempDir.delete();
                    tempDir.mkdir();
                    System.out.println(tempDir.getAbsolutePath());
                    tempDir = File.createTempFile("def", ".");
                    tempDir.delete();
                    tempDir.mkdir();
                    System.out.println(tempDir.getAbsolutePath());
                }
            }
        """,
        after = """
            import java.io.File;
            import java.io.IOException;
            import java.nio.file.Files;
            
            class A {
                void b() throws IOException {
                    File tempDir = Files.createTempDirectory("abc").toFile();
                    System.out.println(tempDir.getAbsolutePath());
                    tempDir = Files.createTempDirectory("def").toFile();
                    System.out.println(tempDir.getAbsolutePath());
                }
            }
        """
    )

    @Test
    fun onlySupportAssignmentToJIdentifier() = assertChanged(
        dependsOn = arrayOf(
            """
                package abc;
                import java.io.File;
                public class C {
                    public static File FILE;
                }
            """),
        before = """
            package abc;
            import java.io.File;
            import java.io.IOException;
            
            class A {
                void b() throws IOException {
                    C.FILE = File.createTempFile("cfile", ".");
                    File tempDir = File.createTempFile("abc", ".");
                    tempDir.delete();
                    tempDir.mkdir();
                }
            }
        """,
        after = """
            package abc;
            import java.io.File;
            import java.io.IOException;
            import java.nio.file.Files;
            
            class A {
                void b() throws IOException {
                    C.FILE = File.createTempFile("cfile", ".");
                    File tempDir = Files.createTempDirectory("abc").toFile();
                }
            }
        """
    )

}