package com.netflix.java.refactor.search

import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

abstract class HasTypeTest(p: Parser): Parser by p {
    
    @Test
    fun hasType() {
        val a = parse("""
            |import java.util.List;
            |class A {
            |   List list;
            |}
        """)

        assertTrue(a.classes[0].hasType(List::class.java))
    }

    @Test
    fun hasTypeBasedOnStaticImport() {
        val a = parse("""
            |import static java.util.Collections.emptyList;
            |class A {
            |   Object o = emptyList();
            |}
        """)

        assertTrue(a.classes[0].hasType(Collections::class.java))
    }
    
    @Test
    fun hasTypeBasedOnStaticChainedCalls() {
        val a = """
            |package a;
            |public class A { 
            |    public static A none() { return null; }
            |}
        """
        
        val b = """
            |import static a.A.none;
            |class B {
            |   Object o = none().none().none();
            |}
        """

        assertTrue(parse(b, a).classes[0].hasType("a.A"))
    }

    @Test
    fun hasTypeInLocalVariable() {
        val a = parse("""
            |import java.util.List;
            |class A {
            |   public void test() {
            |       List list;
            |   }
            |}
        """)

        assertTrue(a.classes[0].hasType(List::class.java))
    }

    @Test
    fun unresolvableMethodSymbol() {
        val a = parse("""
            public class B {
                public static void baz() {
                    // the parse tree inside this anonymous class will be un-attributed because
                    // A is not a resolvable symbol
                    A a = new A() {
                        @Override public void foo() {
                            bar();
                        }
                    };
                }
                public static void bar() {}
            }
        """)

        a.classes[0].hasType("DoesNotMatter") // doesn't throw an exception
    }
}

class OracleJdkHasTypeTest: HasTypeTest(OracleJdkParser())