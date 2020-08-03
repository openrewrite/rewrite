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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.firstMethodStatement

interface TryCatchTest {
    
    @Test
    fun tryFinally(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                public void test() {
                    try {
                    }
                    finally {
                    }
                }
            }
        """)[0]
        
        val tryable = a.firstMethodStatement() as J.Try
        assertEquals(0, tryable.catches.size)
        assertTrue(tryable.finally is J.Try.Finally)
    }
    
    @Test
    fun tryCatchNoFinally(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                public void test() {
                    try {
                    }
                    catch(Throwable t) {
                    }
                }
            }
        """)[0]

        val tryable = a.firstMethodStatement() as J.Try
        assertEquals(1, tryable.catches.size)
    }
    
    @Test
    fun tryWithResources(jp: JavaParser) {
        val a = jp.parse("""
            import java.io.*;
            public class A {
                File f;
                public void test() {
                    try(FileInputStream fis = new FileInputStream(f)) {
                    }
                    catch(IOException e) {
                    }
                }
            }
        """)[0]

        val tryable = a.firstMethodStatement() as J.Try
        assertEquals(1, tryable.resources?.decls?.size ?: -1)
    }

    @Test
    fun formatTryWithResources(jp: JavaParser) {
        val a = jp.parse("""
            import java.io.*;
            public class A {
                File f;
                public void test() {
                    try(FileInputStream fis = new FileInputStream(f)) { }
                }
            }
        """)[0]

        val tryable = a.firstMethodStatement() as J.Try
        assertEquals("try(FileInputStream fis = new FileInputStream(f)) { }",
                tryable.printTrimmed())
    }

    @Test
    fun formatMultiCatch(jp: JavaParser) {
        val a = jp.parse("""
            import java.io.*;
            public class A {
                File f;
                public void test() {
                    try(FileInputStream fis = new FileInputStream(f)) {}
                    catch(FileNotFoundException | RuntimeException e) {}
                }
            }
        """)[0]

        val multiCatch = (a.firstMethodStatement() as J.Try).catches[0].param.tree.typeExpr as J.MultiCatch
        assertEquals("FileNotFoundException | RuntimeException", multiCatch.printTrimmed())
    }

    @Test
    fun formatTryCatchFinally(jp: JavaParser) {
        val a = jp.parse("""
            public class A {
                public void test() {
                    try {}
                    catch(Exception e) {}
                    catch(RuntimeException e) {}
                    catch(Throwable t) {}
                    finally {}
                }
            }
        """)[0]

        val tryable = a.firstMethodStatement() as J.Try
        assertEquals("try {}\ncatch(Exception e) {}\ncatch(RuntimeException e) {}\ncatch(Throwable t) {}\nfinally {}", tryable.printTrimmed())
    }
}
