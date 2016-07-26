package com.netflix.java.refactor.ast

import com.netflix.java.refactor.AbstractRefactorTest
import org.junit.Test
import java.net.URL

class AstScannerTest : AbstractRefactorTest() {
    
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
        
        parseJava(a, emptyList(), listOf(testng)).refactor()
                .changeType("org.testng.annotations.Test", "org.junit.Test")
                .fix()

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