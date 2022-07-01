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
package org.openrewrite.java.dataflow2

import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.Test
import org.openrewrite.Cursor
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.Java11Parser
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.MethodMatcher
import org.openrewrite.java.dataflow2.examples.ZipSlipValue
import org.openrewrite.java.dataflow2.examples.ZipSlipValue.NewFileFromZipEntry
import org.openrewrite.java.dataflow2.examples.ZipSlipValue.Safe
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.J.NewClass
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

class ZipSlipTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(ZipSlip())
    }

    @Test
    fun fixesZipSlipUsingFile() = rewriteRun(
        java(
            """
            import java.io.File;
            import java.io.FileOutputStream;
            import java.io.RandomAccessFile;
            import java.io.FileWriter;
            import java.util.zip.ZipEntry;
            public class ZipTest {
              public void m1(ZipEntry entry, File dir) throws Exception {
                String name = entry.getName();
                File file = new File(dir, name);
                FileOutputStream os = new FileOutputStream(file); // ZipSlip
                RandomAccessFile raf = new RandomAccessFile(file, "rw"); // ZipSlip
                FileWriter fw = new FileWriter(file); // ZipSlip
              }
            }
            """,
            """
            import java.io.File;
            import java.io.FileOutputStream;
            import java.io.RandomAccessFile;
            import java.io.FileWriter;
            import java.io.UncheckedIOException;
            import java.util.zip.ZipEntry;
            public class ZipTest {
              public void m1(ZipEntry entry, File dir) throws Exception {
                String name = entry.getName();
                File file = new File(dir, name);
                if (!file.toPath().startsWith(dir.toPath())) {
                    throw new UncheckedIOException("ZipSlip attack detected");
                }
                FileOutputStream os = new FileOutputStream(file); // ZipSlip
                RandomAccessFile raf = new RandomAccessFile(file, "rw"); // ZipSlip
                FileWriter fw = new FileWriter(file); // ZipSlip
              }
            }
            """
        )
    )

    @Test
    fun fixesZipSlipUsingPath() = rewriteRun(
        java(
            """
            import java.io.File;
            import java.io.FileOutputStream;
            import java.io.RandomAccessFile;
            import java.io.FileWriter;
            import java.nio.file.Files;
            import java.util.zip.ZipEntry;
            public class ZipTest {
              public void m1(ZipEntry entry, Path dir) throws Exception {
                String name = entry.getName();
                Path file = dir.resolve(name);
                FileOutputStream os = Files.newOutputStream(file); // ZipSlip
              }
            }
            """,
            """
            import java.io.FileOutputStream;
            import java.io.RandomAccessFile;
            import java.io.FileWriter;
            import java.io.UncheckedIOException;
            import java.nio.file.Files;
            import java.util.zip.ZipEntry;
            public class ZipTest {
              public void m1(ZipEntry entry, Path dir) throws Exception {
                String name = entry.getName();
                Path file = dir.resolve(name);
                if (file.startsWith(dir)) {
                    throw new UncheckedIOException("ZipSlip attack detected");
                }
                FileOutputStream os = Files.newOutputStream(file); // ZipSlip
              }
            }
            """
        )
    )

    @Test
    fun fixesZipSlipUsingString() = rewriteRun(
        java(
            """
            import java.io.File;
            import java.io.FileOutputStream;
            import java.io.RandomAccessFile;
            import java.io.FileWriter;
            import java.nio.file.Files;
            import java.util.zip.ZipEntry;
            public class ZipTest {
              public void m1(ZipEntry entry, File dir) throws Exception {
                String name = entry.getName();
                FileOutputStream os = new FileOutputStream(dir + File.separator + name); // ZipSlip
              }
            }
            """,
            """
            import java.io.FileOutputStream;
            import java.io.RandomAccessFile;
            import java.io.FileWriter;
            import java.io.UncheckedIOException;
            import java.nio.file.Files;
            import java.util.zip.ZipEntry;
            public class ZipTest {
              public void m1(ZipEntry entry, File dir) throws Exception {
                String name = entry.getName();
                File file = new File(dir, name);
                if (!file.toPath().startsWith(dir.toPath())) {
                    throw new UncheckedIOException("ZipSlip attack detected");
                }
                FileOutputStream os = new FileOutputStream(file.toString()); // ZipSlip
              }
            }
            """
        )
    )

    @Test
    fun safeZipSlipPathStartsWith() = rewriteRun(
        java(
            """
            import java.io.FileOutputStream;
            import java.io.File;
            import java.util.zip.ZipEntry;
            public class ZipTest {
              public void m2(ZipEntry entry, File dir) throws Exception {
                String name = entry.getName();
                File file = new File(dir, name);
                File canFile = file.getCanonicalFile();
                String canDir = dir.getCanonicalPath();
                if (!canFile.toPath().startsWith(canDir))
                  throw new Exception();
                FileOutputStream os = new FileOutputStream(file); // OK
              }
            }
            """
        )
    )

    @Test
    fun safeZipSlipPathNormalizedStartsWith() = rewriteRun(
        java(
            """
            public class ZipTest {
              public void m3(ZipEntry entry, File dir) throws Exception {
                String name = entry.getName();
                File file = new File(dir, name);
                if (!file.toPath().normalize().startsWith(dir.toPath()))
                  throw new Exception();
                FileOutputStream os = new FileOutputStream(file); // OK
              }
            }
            """
        )
    )

    @Test
    fun safeZipSlipValidateMethod() = rewriteRun(
        java(
            """
            public class ZipTest {
              private void validate(File tgtdir, File file) throws Exception {
                File canFile = file.getCanonicalFile();
                if (!canFile.toPath().startsWith(tgtdir.toPath()))
                  throw new Exception();
              }
              public void m4(ZipEntry entry, File dir) throws Exception {
                String name = entry.getName();
                File file = new File(dir, name);
                validate(dir, file);
                FileOutputStream os = new FileOutputStream(file); // OK
              }
          }
          """
        )
    )

    @Test
    fun safeZipSlipPathAbsoluteNormalizeStartsWith() = rewriteRun(
        java(
            """
            public class ZipTest {
              public void m5(ZipEntry entry, File dir) throws Exception {
                String name = entry.getName();
                File file = new File(dir, name);
                Path absfile = file.toPath().toAbsolutePath().normalize();
                Path absdir = dir.toPath().toAbsolutePath().normalize();
                if (!absfile.startsWith(absdir))
                  throw new Exception();
                FileOutputStream os = new FileOutputStream(file); // OK
              }
              }
            """
        )
    )

    @Test
    fun safeZipSlipSlipCanonicalPath() = rewriteRun(
        java(
            """
            public class ZipTest {
              public void m6(ZipEntry entry, Path dir) throws Exception {
                String canonicalDest = dir.toFile().getCanonicalPath();
                Path target = dir.resolve(entry.getName());
                String canonicalTarget = target.toFile().getCanonicalPath();
                if (!canonicalTarget.startsWith(canonicalDest + File.separator))
                  throw new Exception();
                OutputStream os = Files.newOutputStream(target); // OK
              }
            }
            """
        )
    )

    @Test
    fun testNewFile() {
        testZipSlip(
            "source1", NewFileFromZipEntry::class.java,
            "import java.io.File; \n" +
                    "import java.io.FileOutputStream; \n" +
                    "import java.io.RandomAccessFile; \n" +
                    "import java.io.FileWriter; \n" +
                    "import java.util.zip.ZipEntry; \n" +
                    "public class ZipTest { \n" +
                    "    public void m1(ZipEntry entry, File dir) throws Exception { \n" +
                    "        String name = entry.getName(); \n" +
                    "        File file = new File(dir, name); \n" +
                    "        FileOutputStream os = new FileOutputStream(file); // ZipSlip \n" +
                    "    } \n" +
                    "} \n" +
                    ""
        )
    }
    @Test
    fun testNewFileWithFlow() {
        testZipSlip(
            "source2", NewFileFromZipEntry::class.java,
            ("import java.io.File; \n" +
                    "import java.io.FileOutputStream; \n" +
                    "import java.io.RandomAccessFile; \n" +
                    "import java.io.FileWriter; \n" +
                    "import java.util.zip.ZipEntry; \n" +
                    "public class ZipTest { \n" +
                    "    public void m1(ZipEntry entry, File dir) throws Exception { \n" +
                    "        String name1 = entry.getName(); \n" +
                    "        String name2 = name1; \n" +
                    "        File file = new File(dir, name2); \n" +
                    "        FileOutputStream os = new FileOutputStream(file); // ZipSlip \n" +
                    "    } \n" +
                    "} \n" +
                    "")
        )
    }
    @Test
    fun testNewFileWithConcatenation() {
        testZipSlip(
            "source3", ZipSlipValue.Unknown::class.java,
            ("import java.io.File; \n" +
                    "import java.io.FileOutputStream; \n" +
                    "import java.io.RandomAccessFile; \n" +
                    "import java.io.FileWriter; \n" +
                    "import java.util.zip.ZipEntry; \n" +
                    "public class ZipTest { \n" +
                    "    public void m1(ZipEntry entry, File dir) throws Exception { \n" +
                    "        String name1 = entry.getName(); \n" +
                    "        String name2 = name1 + \"/\"; \n" +
                    "        File file = new File(dir, name2); \n" +
                    "        FileOutputStream os = new FileOutputStream(file); // ZipSlip \n" +
                    "    } \n" +
                    "} \n" +
                    "")
        )
    }
    @Test
    fun testNewFileWithUnknownMethodCall() {
        testZipSlip(
            "source4", ZipSlipValue.Unknown::class.java,
            ("import java.io.File; \n" +
                    "import java.io.FileOutputStream; \n" +
                    "import java.io.RandomAccessFile; \n" +
                    "import java.io.FileWriter; \n" +
                    "import java.util.zip.ZipEntry; \n" +
                    "public class ZipTest { \n" +
                    "    public void m1(ZipEntry entry, File dir) throws Exception { \n" +
                    "        String name1 = entry.getName(); \n" +
                    "        String name2 = someUnknownMethod(name1); \n" +
                    "        File file = new File(dir, name2); \n" +
                    "        FileOutputStream os = new FileOutputStream(file); // ZipSlip \n" +
                    "    } \n" +
                    "} \n" +
                    "")
        )
    }
    @Test
    fun testNewFileWithGuard() {
        testZipSlip(
            "source5",
            Safe::class.java,
            ("import java.io.File; \n" +
                    "import java.io.FileOutputStream; \n" +
                    "import java.io.RandomAccessFile; \n" +
                    "import java.io.FileWriter; \n" +
                    "import java.util.zip.ZipEntry; \n" +
                    "public class ZipTest { \n" +
                    "    public void m1(ZipEntry entry, File dir) throws Exception { \n" +
                    "        String name = entry.getName(); \n" +
                    "        File file = new File(dir, name); \n" +
                    "        if (!file.toPath().startsWith(dir.toPath())) { \n" +
                    "            //throw new UncheckedIOException(\"ZipSlip attack detected\"); \n" +
                    "            file = null; \n" +
                    "        } \n" +
                    "        FileOutputStream os = new FileOutputStream(file); // ZipSlip \n" +
                    "    } \n" +
                    "} \n" +
                    "")
        )
    }

    fun testZipSlip(name: String, expectedClass: Class<*>, source: String?) {
        println("Processing test $name")
        val cu: J.CompilationUnit = parse(source)
        val visitor = FindConstructorInvocationVisitor()
        visitor.visit(cu, null)
        val newClassCursor = visitor.result
        val newClass = newClassCursor!!.getValue<NewClass>()
        val file = newClass.arguments[0]
        val fileCursor = Cursor(newClassCursor, file)

        // We're interested in the expr() of the output state of 'file'
        val zipSlip = org.openrewrite.java.dataflow2.examples.ZipSlip(DataFlowGraph(cu))
        zipSlip.performAnalysis(fileCursor)
        val state = zipSlip.getStateAfter(file)
        val actual = state.expr()
        println("state.expr() = $actual")
        if (actual is NewFileFromZipEntry) {
            // unsafe, and we know the value of 'dir'
            println(" -> requires a guard")
        } else if (state.expr() === ZipSlipValue.UNKNOWN) {
            println(" -> maybe requires a guard")
        } else if (state.expr() === ZipSlipValue.SAFE) {
            println(" -> does not require a guard")
        } else if (state.expr() === ZipSlipValue.UNSAFE) {
            println(" -> requires a guard")
        }
        AssertionsForClassTypes.assertThat(actual.javaClass)
            .withFailMessage("expected: $expectedClass\n but was: $actual")
            .isEqualTo(expectedClass)
        println()
    }

    fun parse(src: String?): J.CompilationUnit {
        val parser = Java11Parser.Builder().build()
        val ctx: ExecutionContext = InMemoryExecutionContext()
        val cus = parser.parse(ctx, src)
        return cus[0]
    }
}

class FindConstructorInvocationVisitor() : JavaIsoVisitor<Any?>() {
    var m = MethodMatcher("java.io.FileOutputStream <constructor>(java.io.File)")
    var result: Cursor? = null
    override fun visitNewClass(newClass: NewClass, o: Any?): NewClass {
        if (m.matches(newClass)) {
            println("Found constructor invocation " + newClass.print(cursor))
            result = cursor
        }
        return super.visitNewClass(newClass, o)
    }
}
