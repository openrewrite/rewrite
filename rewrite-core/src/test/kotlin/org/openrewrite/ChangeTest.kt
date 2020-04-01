package org.openrewrite

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class ChangeTest {
    @Test
    fun idempotent() {
        val diff = Change.InMemoryDiffEntry(Paths.get("com/netflix/MyJavaClass.java"), null,
                "public class A {}",
                "public class A {}")

        assertThat(diff.diff).isEmpty()
    }

    @Test
    fun singleLineChange() {
        val diff = Change.InMemoryDiffEntry(Paths.get("com/netflix/MyJavaClass.java"), null,
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
            """.trimMargin()
        ).diff

        assertThat("""
            |diff --git a/com/netflix/MyJavaClass.java b/com/netflix/MyJavaClass.java
            |index 3490cbf..5d64ae4 100644
            |--- a/com/netflix/MyJavaClass.java
            |+++ b/com/netflix/MyJavaClass.java
            |@@ -1,3 +1,3 @@
            | public void test() {
            |-   logger.infof("some %s", 1);
            |+   logger.info("some {}", 1);
            | }
            |
        """.trimMargin()).isEqualTo(diff)
    }

    @Test
    fun multipleChangesMoreThanThreeLinesApart() {
        val diff = Change.InMemoryDiffEntry(Paths.get("com/netflix/MyJavaClass.java"), null,
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
            """.trimMargin()
        ).diff

        assertThat("""
                |diff --git a/com/netflix/MyJavaClass.java b/com/netflix/MyJavaClass.java
                |index c17f051..bb2dfba 100644
                |--- a/com/netflix/MyJavaClass.java
                |+++ b/com/netflix/MyJavaClass.java
                |@@ -1,5 +1,5 @@
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
        """.trimMargin()).isEqualTo(diff)
    }
}