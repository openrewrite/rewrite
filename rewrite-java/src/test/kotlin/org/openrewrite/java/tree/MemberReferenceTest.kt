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
import org.junit.jupiter.api.Test
import org.openrewrite.Tree
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaSourceVisitor
import org.openrewrite.java.asClass
import java.util.concurrent.CountDownLatch

open class MemberReferenceTest : JavaParser() {

    @Test
    fun staticFunctionReference() {
        val a = parse("""
            import java.util.stream.Stream;

            public class StaticLambdaRef {
                void test() {
                    Stream.of("s").forEach(A :: func);
                }
            }

            class A {
                static void func(String s) {}
            }
        """)

        val memberRefLatch = CountDownLatch(1)

        object: JavaSourceVisitor<Unit?>() {
            override fun defaultTo(t: Tree?): Nothing? = null

            override fun visitMemberReference(memberRef: J.MemberReference): Unit? {
                memberRefLatch.countDown()
                assertEquals("java.util.function.Consumer", memberRef.type.asClass()?.fullyQualifiedName)
                assertEquals("A :: func", memberRef.printTrimmed())
                return null
            }
        }.visit(a)

        assertEquals(0L, memberRefLatch.count)
    }

    @Test
    fun constructorMethodReference() {
        val a = parse("""
            import java.util.*;
            import java.util.stream.*;
            public class A {
                Stream<Integer> n = Stream.of(1, 2);
                Set<Integer> n2 = n.collect(HashSet<Integer>::new, HashSet::add);
            }
        """)

        val collect = a.classes[0].fields[1].vars[0].initializer!!
        assertEquals("n.collect(HashSet<Integer>::new, HashSet::add)", collect.printTrimmed())
    }
}