package com.netflix.rewrite.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RewritePluginTest {

    @JvmField @Rule
    val temp = TemporaryFolder()

    @Test
    fun rewriteCode() {
        val projectDir = temp.root

        File(projectDir, "build.gradle").writeText("""
            plugins {
                id 'java'
                id 'netflix.rewrite'
            }

            repositories { mavenCentral() }
            dependencies { testCompile 'junit:junit:4.11' }
        """)

        val sourceFolder = File(projectDir, "src/test/java")
        sourceFolder.mkdirs()
        val test = File(sourceFolder, "MyTest.java")
        test.writeText("""
            |import org.junit.Test;
            |import static org.junit.Assert.*;
            |
            |public class MyTest {
            |    @Test
            |    public void test() {
            |        assertEquals(2, 1+1);
            |    }
            |}
        """.trimMargin())

        println(runTaskAndFail(projectDir, "lintSource").output)
        println(runTaskAndFail(projectDir, "fixSourceLint").output)

        assertEquals("""
            |import org.junit.Test;
            |import static org.assertj.core.api.Assertions.*;
            |
            |public class MyTest {
            |    @Test
            |    public void test() {
            |        assertThat(1+1).isEqualTo(2);
            |    }
            |}
        """.trimMargin(), test.readText())
    }

    private fun runTaskAndFail(projectDir: File?, task: String): BuildResult {
        return GradleRunner.create()
                .withDebug(true)
                .withProjectDir(projectDir)
                .withArguments(task)
                .withPluginClasspath()
                .buildAndFail()
    }
}