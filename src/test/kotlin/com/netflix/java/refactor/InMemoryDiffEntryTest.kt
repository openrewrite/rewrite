package com.netflix.java.refactor

import org.junit.Test
import org.junit.Assert.assertEquals

class InMemoryDiffEntryTest {
    
    @Test
    fun idempotent() {
        val diff = InMemoryDiffEntry("com/netflix/MyJavaClass.java",
                "public class A {}",
                "public class A {}")
        
        assertEquals("", diff.diff)
    }
    
    @Test
    fun singleLineChange() {
        val diff = InMemoryDiffEntry("com/netflix/MyJavaClass.java",
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
        
        assertEquals("""
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
        """.trimMargin(), diff)
    }
}