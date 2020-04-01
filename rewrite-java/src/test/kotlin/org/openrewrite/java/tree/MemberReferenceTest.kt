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