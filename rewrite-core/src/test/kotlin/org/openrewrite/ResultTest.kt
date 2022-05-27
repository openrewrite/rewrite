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
package org.openrewrite

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class ResultTest {
    private val filePath = Paths.get("com/netflix/MyJavaClass.java")

    private fun ab(which: String) = "$which/" + filePath.toString().replace("\\", "/")

    @Test
    fun idempotent() {
        val diff = Result.InMemoryDiffEntry(
            Paths.get("com/netflix/MyJavaClass.java"),
            Paths.get("com/netflix/MyJavaClass.java"),
            null,
            "public class A {}",
            "public class A {}",
            emptySet()
        )

        assertThat(diff.diff).isEmpty()
    }

    @Test
    fun singleLineChange() {
        val diff = Result.InMemoryDiffEntry(
            filePath, filePath, null,
            """
                |public void test() {
                |   logger.infof("some %s", 1);
                |}
                |
            """.trimMargin(),
            """
                |public void test() {
                |   logger.info("some {}", 1);
                |}
                |
            """.trimMargin(),
            setOf(object : Recipe() {
                override fun getName(): String = "logger.Fix"
                override fun getDisplayName(): String {
                    return name
                }
            })
        ).diff

        assertThat(
            """
            |diff --git ${ab("a")} ${ab("b")}
            |index 3490cbf..5d64ae4 100644
            |--- ${ab("a")}
            |+++ ${ab("b")}
            |@@ -1,3 +1,3 @@ logger.Fix
            | public void test() {
            |-   logger.infof("some %s", 1);
            |+   logger.info("some {}", 1);
            | }
            |
        """.trimMargin()
        ).isEqualTo(diff)
    }

    @Test
    fun multipleChangesMoreThanThreeLinesApart() {
        val diff = Result.InMemoryDiffEntry(
            filePath, filePath, null,
            """
                |public void test() {
                |   logger.infof("some %s", 1);
                |   System.out.println("1");
                |   System.out.println("2");
                |   System.out.println("3");
                |   System.out.println("4");
                |   System.out.println("5");
                |   System.out.println("6");
                |   System.out.println("7");
                |   System.out.println("8");
                |   logger.infof("some %s", 2);
                |}
                |
            """.trimMargin(),
            """
                |public void test() {
                |   logger.info("some {}", 1);
                |   System.out.println("1");
                |   System.out.println("2");
                |   System.out.println("3");
                |   System.out.println("4");
                |   System.out.println("5");
                |   System.out.println("6");
                |   System.out.println("7");
                |   System.out.println("8");
                |   logger.info("some %s", 2);
                |}
                |
            """.trimMargin(),

            setOf(
                object : Recipe() {
                    override fun getName(): String = "logger.Fix1"
                    override fun getDisplayName(): String {
                        return name
                    }
                },
                object : Recipe() {
                    override fun getName(): String = "logger.Fix2"
                    override fun getDisplayName(): String {
                        return name
                    }
                }
            )
        ).diff

        assertThat(
            """
                |diff --git ${ab("a")} ${ab("b")}
                |index c17f051..bb2dfba 100644
                |--- ${ab("a")}
                |+++ ${ab("b")}
                |@@ -1,5 +1,5 @@ logger.Fix1, logger.Fix2
                | public void test() {
                |-   logger.infof("some %s", 1);
                |+   logger.info("some {}", 1);
                |    System.out.println("1");
                |    System.out.println("2");
                |    System.out.println("3");
                |@@ -8,5 +8,5 @@
                |    System.out.println("6");
                |    System.out.println("7");
                |    System.out.println("8");
                |-   logger.infof("some %s", 2);
                |+   logger.info("some %s", 2);
                | }
                |
            """.trimMargin()
        ).isEqualTo(diff)
    }

    @Test
    fun addFile() {
        val diff = Result.InMemoryDiffEntry(
            null, filePath, null,
            "",
            """
                |public void test() {
                |   logger.info("Hello Fred");
                |}
                |
            """.trimMargin(),
            setOf(
                object : Recipe() {
                    override fun getName(): String = "logger.Fix1"
                    override fun getDisplayName(): String {
                        return name
                    }
                }
            )).diff

        assertThat(diff).isEqualTo(
            """
                |diff --git a/com/netflix/MyJavaClass.java b/com/netflix/MyJavaClass.java
                |new file mode 100644
                |index 0000000..efeb105
                |--- /dev/null
                |+++ b/com/netflix/MyJavaClass.java
                |@@ -0,0 +1,3 @@ logger.Fix1
                |+public void test() {
                |+   logger.info("Hello Fred");
                |+}
                |
            """.trimMargin()
        )
    }

    @Test
    fun deleteFile() {
        val diff = Result.InMemoryDiffEntry(
            filePath, null, null,
            """
                |public void test() {
                |   logger.info("Hello Fred");
                |}
                |
            """.trimMargin(),
            "",
            setOf(
                object : Recipe() {
                    override fun getName(): String = "logger.Fix1"
                    override fun getDisplayName(): String {
                        return name
                    }
                }
            )).diff

        assertThat(diff).isEqualTo(
            """
                |diff --git a/com/netflix/MyJavaClass.java b/com/netflix/MyJavaClass.java
                |deleted file mode 100644
                |index efeb105..0000000
                |--- a/com/netflix/MyJavaClass.java
                |+++ /dev/null
                |@@ -1,3 +0,0 @@ logger.Fix1
                |-public void test() {
                |-   logger.info("Hello Fred");
                |-}
                |
           """.trimMargin()
        )

    }

}
