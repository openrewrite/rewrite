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
}