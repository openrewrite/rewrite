package com.netflix.java.refactor.search

import com.netflix.java.refactor.parse.OracleJdkParser
import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class FindTypeTest(p: Parser): Parser by p {

    val a1 = """
        package a;
        public class A1 extends Exception {
            public static void stat() {}
        }
    """

    @Test
    fun simpleName() {
        val b = parse("""
            |import a.A1;
            |
            |public class B extends A1 {}
        """, a1)

        assertEquals(1, b.findType("a.A1").size)
    }

    @Test
    fun fullyQualifiedName() {
        val b = parse("public class B extends a.A1 {}", a1)
        assertEquals(1, b.findType("a.A1").size)
    }

    @Test
    fun annotation() {
        val a1 = "public @interface A1 {}"
        val b = parse("@A1 public class B {}", a1)
        assertEquals(1, b.findType("A1").size)
    }

    @Test
    fun array() { // array types and new arrays
        val b = parse("""
            |import a.A1;
            |public class B {
            |   A1[] a = new A1[0];
            |}
        """, a1)

        assertEquals(2, b.findType("a.A1").size)
    }

    @Test
    fun classDecl() {
        val i1 = "public interface I1 {}"

        val b = parse("""
            |import a.A1;
            |public class B extends A1 implements I1 {}
        """, a1, i1)

        assertEquals(1, b.findType("a.A1").size)
        assertEquals(1, b.findType("I1").size)
    }

    @Test
    fun method() {
        val b = parse("""
            |import a.A1;
            |public class B {
            |   public A1 foo() throws A1 { return null; }
            |}
        """, a1)

        assertEquals(2, b.findType("a.A1").size)
    }

    @Test
    fun methodInvocationTypeParametersAndWildcard() {
        val b = parse("""
            |import a.A1;
            |import java.util.List;
            |public class B {
            |   public <T extends A1> T generic(T n, List<? super A1> in) { return null; }
            |   public void test() {
            |       A1.stat();
            |       this.<A1>generic(null, null);
            |   }
            |}
        """, a1)

        b.findType("a.A1").map { b.cursor(it) }

        assertEquals(4, b.findType("a.A1").size)
    }

    @Test
    fun multiCatch() {
        val b = parse("""
            |import a.A1;
            |public class B {
            |   public void test() {
            |       try {}
            |       catch(A1 | RuntimeException e) {}
            |   }
            |}
        """, a1)

        assertEquals(1, b.findType("a.A1").size)
    }

    @Test
    fun multiVariable() {
        val b = parse("""
            |import a.A1;
            |public class B {
            |   A1 f1, f2;
            |}
        """, a1)

        assertEquals(1, b.findType("a.A1").size)
    }

    @Test
    fun newClass() {
        val b = parse("""
            |import a.A1;
            |public class B {
            |   A1 a = new A1();
            |}
        """, a1)

        assertEquals(2, b.findType("a.A1").size)
    }

    @Test
    fun paramaterizedType() {
        val b = parse("""
            |import a.A1;
            |public class B {
            |   Map<A1, A1> m;
            |}
        """, a1)

        assertEquals(2, b.findType("a.A1").size)
    }

    @Test
    fun typeCast() {
        val b = parse("""
            |import a.A1;
            |public class B {
            |   A1 a = (A1) null;
            |}
        """, a1)

        assertEquals(2, b.findType("a.A1").size)
    }
}

class OracleJdkFindTypeTest: FindTypeTest(OracleJdkParser())