package com.netflix.java.refactor.op

import com.netflix.java.refactor.Refactorer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.URL

class RefactoringScannerTest {
    @JvmField @Rule
    val temp = TemporaryFolder()
    
    @Test
    fun scannerIsAbleToIdentifyTypesFromExternalDependencies() {
        val a = temp.newFile("A.java")
        a.writeText("""
            |package a;
            |import org.testng.annotations.*;
            |public class A {
            |   @Test
            |   public void test() {}
            |}
        """.trimMargin())

        val testngDownloaded = URL("http://repo1.maven.org/maven2/org/testng/testng/6.9.9/testng-6.9.9.jar").openStream().readBytes()
        val testng = temp.newFile("testng-6.9.9.jar")
        testng.outputStream().use { it.write(testngDownloaded) }
        
        Refactorer().changeType("org.testng.annotations.Test", "org.junit", "Test")
            .refactorAndFix(listOf(a), listOf(testng))

        assertEquals("""
            |package a;
            |import org.junit.Test;
            |public class A {
            |   @Test
            |   public void test() {}
            |}
        """.trimMargin(), a.readText())
    }
}