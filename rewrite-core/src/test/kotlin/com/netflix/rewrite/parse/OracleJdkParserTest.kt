/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.parse

import com.netflix.rewrite.ast.Type
import com.netflix.rewrite.ast.asClass
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
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

    @Test
    fun relativizeSourcePaths() {
        val packageFolder = File(temp.root, "src/main/java/com/netflix/test")
        packageFolder.mkdirs()

        val aSrc = File(packageFolder, "A.java")
        aSrc.writeText("public class A {}")

        val a = OracleJdkParser().parse(listOf(aSrc.toPath()), temp.root.toPath())
        assertEquals("src/main/java/com/netflix/test/A.java", a.first().sourcePath)
    }

    @Test
    fun partialTypeAttribution() {
        // DNE = does not exist
        val a = OracleJdkParser().parse("""
            |package a;
            |import b.B;
            |import dne.DNE;
            |public class A extends DNE {
            |    B b = new B();
            |
            |    public DNE foo() {
            |        B b2 = new B();
            |    }
            |
            |    public Consumer<DNE> bar() {
            |        return dne -> {
            |            B b = new B();
            |        };
            |    }
            |
            |    class C extends DNE {
            |        B b = new B();
            |    }
            |}
        """, "package b; public class B {}")

        // still able to find references to B even when "cannot find symbols" abound!
        assertTrue(a.findType("b.B").map { a.cursor(it)?.enclosingVariableDecl() }.filterNotNull().size >= 4)
    }

    @Test
    fun parserIncludesTypeInterfaces() {
        val a = OracleJdkParser().parse("""
            |package a;
            |import java.util.HashSet;
            |import java.util.Set;
            |public class A {
            |   Set<String> set = new HashSet<String>();
            }
        """)

        val hashSet = a.findType("java.util.HashSet")[1]    // first element is the import
        assertNotNull(hashSet.type.asClass())
        assertTrue(hashSet.type.asClass()!!.interfaces.any { (it as Type.ShallowClass).fullyQualifiedName == "java.util.Set" })
    }
}
