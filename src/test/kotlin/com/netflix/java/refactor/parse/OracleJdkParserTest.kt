package com.netflix.java.refactor.parse

import com.netflix.java.refactor.ast.asClass
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.URL

class OracleJdkParserTest {
    @JvmField @Rule val temp = TemporaryFolder()

    @Test
    fun parserIsAbleToIdentifyTypesFromExternalDependencies() {
        val testngDownloaded = URL("http://repo1.maven.org/maven2/org/testng/testng/6.9.9/testng-6.9.9.jar").openStream().readBytes()
        val testng = temp.newFile("testng-6.9.9.jar")
        testng.outputStream().use { it.write(testngDownloaded) }

        val a = OracleJdkParser(listOf(testng.toPath())).parse("""
            |package a;
            |import org.testng.annotations.*;
            |public class A {
            |   @Test
            |   public void test() {}
            |}
        """)

        assertEquals("org.testng.annotations.Test", a.classes[0].methods()[0].annotations[0].type.asClass()?.fullyQualifiedName)
    }
}