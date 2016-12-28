package com.netflix.rewrite.ast

import com.netflix.rewrite.parse.Parser
import org.junit.Assert
import org.junit.Test

abstract class MemberReferenceTest(p: Parser): Parser by p {

    @Test
    fun staticFunctionReference() {
        val a = parse("""
            import java.util.stream.Stream;

            public class StaticLambdaRef {
                void test() {
                    Stream.of("s").forEach(A::func);
                }
            }

            class A {
                static void func(String s) {}
            }
        """)

        Assert.assertEquals("Stream.of(\"s\").forEach(A::func)", a.classes[0].methods()[0].body!!.statements[0].printTrimmed())
    }
}