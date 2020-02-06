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
package com.netflix.rewrite.tree

import com.netflix.rewrite.asClass
import com.netflix.rewrite.parse.Parser
import com.netflix.rewrite.tree.visitor.AstVisitor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CountDownLatch

abstract class MemberReferenceTest(p: Parser): Parser by p {

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

        object: AstVisitor<Unit?>() {
            override fun defaultTo(t: Tree?): Nothing? = null

            override fun visitMemberReference(memberRef: Tr.MemberReference): Unit? {
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