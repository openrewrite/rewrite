package com.netflix.java.refactor.op

import com.netflix.java.refactor.RefactorTest
import org.junit.Test
import java.net.URL

class RefactoringScannerTest: RefactorTest() {
    
    @Test
    fun scannerIsAbleToIdentifyTypesFromExternalDependencies() {
        val a = java("""
            |package a;
            |import org.testng.annotations.*;
            |public class A {
            |   @Test
            |   public void test() {}
            |}
        """)

        val testngDownloaded = URL("http://repo1.maven.org/maven2/org/testng/testng/6.9.9/testng-6.9.9.jar").openStream().readBytes()
        val testng = temp.newFile("testng-6.9.9.jar")
        testng.outputStream().use { it.write(testngDownloaded) }
        
        refactor(a, emptyList(), listOf(testng))
                .changeType("org.testng.annotations.Test", "org.junit", "Test")

        assertRefactored(a, """
            |package a;
            |import org.junit.Test;
            |public class A {
            |   @Test
            |   public void test() {}
            |}
        """)
    }
}