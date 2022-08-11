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
package org.openrewrite.java.dataflow

import org.junit.jupiter.api.Test
import org.openrewrite.Cursor
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.MethodMatcher
import org.openrewrite.java.tree.Expression
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("FunctionName")
interface DataflowRealWorldExamplesTest: RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        val zipEntryGetMethod = MethodMatcher("java.util.zip.ZipEntry getName()", true)
        spec.recipe(RewriteTest.toRecipe {
            FindLocalFlowPaths(object : LocalFlowSpec<Expression, Expression>() {
                override fun isSource(expr: Expression, cursor: Cursor) =
                    zipEntryGetMethod.matches(expr)

                override fun isSink(expr: Expression, cursor: Cursor) = true
            })
        })
        spec.expectedCyclesThatMakeChanges(1).cycles(1)
    }

    @Test
    fun `example infowangxin_springmvc`() = rewriteRun(
        java(
            """
            import java.io.BufferedInputStream;
            import java.io.BufferedOutputStream;
            import java.io.File;
            import java.io.FileInputStream;
            import java.io.FileOutputStream;
            import java.io.IOException;
            import java.io.InputStream;
            import java.io.OutputStream;
            import java.util.Enumeration;
            import java.util.List;
            import java.util.zip.CRC32;
            import java.util.zip.CheckedOutputStream;
            import java.util.zip.Deflater;
            import java.util.zip.ZipEntry;
            import java.util.zip.ZipFile;
            import java.util.zip.ZipOutputStream;

            class ZipUtil {
                    /**
                     * 解压缩
                     *
                     * @param zipfile
                     *            File 需要解压缩的文件
                     * @param descDir
                     *            String 解压后的目标目录
                     */
                    @SuppressWarnings({ "rawtypes", "resource" })
                    public static void unZipFiles(File zipfile, String descDir) {
                        try {
                            ZipFile zf = new ZipFile(zipfile);
                            ZipEntry entry = null;
                            String zipEntryName = null;
                            InputStream in = null;
                            OutputStream out = null;
                            byte[] buf1 = null;
                            int len;
                            for (Enumeration entries = zf.entries(); entries.hasMoreElements();) {
                                entry = ((ZipEntry) entries.nextElement());
                                zipEntryName = entry.getName();
                                in = zf.getInputStream(entry);
                                out = new FileOutputStream(descDir + zipEntryName);
                                buf1 = new byte[1024];
                                len = 0;
                                while ((len = in.read(buf1)) > 0) {
                                    out.write(buf1, 0, len);
                                }
                                in.close();
                                out.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
            }
            """,
            """
            import java.io.BufferedInputStream;
            import java.io.BufferedOutputStream;
            import java.io.File;
            import java.io.FileInputStream;
            import java.io.FileOutputStream;
            import java.io.IOException;
            import java.io.InputStream;
            import java.io.OutputStream;
            import java.util.Enumeration;
            import java.util.List;
            import java.util.zip.CRC32;
            import java.util.zip.CheckedOutputStream;
            import java.util.zip.Deflater;
            import java.util.zip.ZipEntry;
            import java.util.zip.ZipFile;
            import java.util.zip.ZipOutputStream;

            class ZipUtil {
                    /**
                     * 解压缩
                     *
                     * @param zipfile
                     *            File 需要解压缩的文件
                     * @param descDir
                     *            String 解压后的目标目录
                     */
                    @SuppressWarnings({ "rawtypes", "resource" })
                    public static void unZipFiles(File zipfile, String descDir) {
                        try {
                            ZipFile zf = new ZipFile(zipfile);
                            ZipEntry entry = null;
                            String zipEntryName = null;
                            InputStream in = null;
                            OutputStream out = null;
                            byte[] buf1 = null;
                            int len;
                            for (Enumeration entries = zf.entries(); entries.hasMoreElements();) {
                                entry = ((ZipEntry) entries.nextElement());
                                zipEntryName = /*~~>*/entry.getName();
                                in = zf.getInputStream(entry);
                                out = new FileOutputStream(descDir + /*~~>*/zipEntryName);
                                buf1 = new byte[1024];
                                len = 0;
                                while ((len = in.read(buf1)) > 0) {
                                    out.write(buf1, 0, len);
                                }
                                in.close();
                                out.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
            }
            """
        )
    )
}
