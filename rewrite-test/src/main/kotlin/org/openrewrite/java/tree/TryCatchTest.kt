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
package org.openrewrite.java.tree

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaTreeTest
import org.openrewrite.java.JavaTreeTest.NestingLevel.Block
import java.util.concurrent.atomic.AtomicInteger

interface TryCatchTest : JavaTreeTest {

    @Test
    fun catchRightPadding(jp: JavaParser) {
        val j = jp.parse(
            """
            class Test {
                void method() {
                    try {
                        String foo;
                    } catch( Exception e ) {
                    //
                    }
                }
            }
        """.trimIndent()
        )[0]

        object : JavaIsoVisitor<Int>() {
            override fun visitCatch(c: J.Try.Catch, p: Int): J.Try.Catch {
                assertThat(c.parameter.padding.tree.after.whitespace).isEqualTo(" ")
                return super.visitCatch(c, p)
            }
        }.visit(j, 0)
    }

    @Test
    fun tryFinally(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            try {
            }
            finally {
            }
        """
    )

    @Test
    fun tryCatchNoFinally(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            try {
            }
            catch(Throwable t) {
            }
        """
    )

    @Test
    fun tryWithResources(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            File f = new File("file.txt");
            try (FileInputStream fis = new FileInputStream(f)) {
            }
            catch(IOException e) {
            }
        """, "java.io.*"
    )

    @Test
    fun tryWithResourcesSemiTerminated(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            File f = new File("file.txt");
            try (FileInputStream fis = new FileInputStream(f) ; ) {
            }
            catch(IOException e) {
            }
        """, "java.io.*"
    )

    @Test
    fun multiCatch(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            File f = new File("file.txt");
            try(FileInputStream fis = new FileInputStream(f)) {}
            catch(FileNotFoundException | RuntimeException e) {}
        """, "java.io.*"
    )

    @Test
    fun multipleResources(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            File f = new File("file.txt");
            try(FileInputStream fis = new FileInputStream(f); FileInputStream fis2 = new FileInputStream(f)) {}
            catch(RuntimeException | IOException e) {}
        """, "java.io.*"
    )

    @Test
    fun tryCatchFinally(jp: JavaParser) = assertParsePrintAndProcess(
        jp, Block, """
            try {}
            catch(Exception e) {}
            catch(RuntimeException e) {}
            catch(Throwable t) {}
            finally {}
        """
    )

    @Test
    @Issue("#530 Try-with-resource navigation")
    fun tryWithResourcesTypeAttributed(jp: JavaParser) {
        val source = """
            package com.example;
            import java.io.*;
            public class Testing {
                public void foo() {
                    File f = new File("file.txt");
                    try(FileInputStream fis = new FileInputStream(f); FileInputStream fis2 = new FileInputStream(f)) {
                        String foo;
                    } catch(RuntimeException | IOException e) {
                        //
                    }
                }
            }
        """.trimIndent()
        val cu: J.CompilationUnit = jp.parse(source).iterator().next()

        //This is testing the navigation into the try-with-resources and ensures the visitor correctly visits the
        //variable declarations of each resource by counting the number of on new class events.
        val counter = AtomicInteger()
        val countingVisitor = object : JavaIsoVisitor<AtomicInteger>() {
            override fun visitNewClass(newClass: J.NewClass, c: AtomicInteger): J.NewClass {
                counter.incrementAndGet()
                return super.visitNewClass(newClass, c)
            }
        }
        countingVisitor.visit(cu, counter)
        assertThat(counter.get()).isEqualTo(3)
    }
}
