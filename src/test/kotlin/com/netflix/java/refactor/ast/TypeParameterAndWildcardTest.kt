package com.netflix.java.refactor.ast

import com.netflix.java.refactor.parse.Parser
import org.junit.Assert.assertEquals
import org.junit.Test

abstract class TypeParameterAndWildcardTest(p: Parser): Parser by p {
    val bc = listOf("public interface B {}", "public interface C {}")

    @Test
    fun extendsAndSuper() {
        val a = parse("""
            import java.util.List;
            public class A {
                public <P extends B> void foo(List<P> out, List<? super C> in) {}
            }
        """, whichDependOn = bc)

        assertEquals("public <P extends B> void foo(List<P> out, List<? super C> in) {}",
                a.classes[0].methods()[0].printTrimmed())
    }

    @Test
    fun multipleExtends() {
        val a = parse("public class A< T extends  B & C > {}", whichDependOn = bc)
        assertEquals("public class A< T extends  B & C > {}", a.printTrimmed())
    }

    @Test
    fun wildcardExtends() {
        val a = parse("""
            import java.util.*;
            public class A {
                List< ?  extends  B > bs;
            }
        """, whichDependOn = "public class B {}")

        val typeParam = a.classes[0].fields()[0].typeExpr as Tr.ParameterizedType
        assertEquals("List< ?  extends  B >", typeParam.print())
    }

    @Test
    fun emptyWildcard() {
        val a = parse("""
            import java.util.*;
            public class A {
                List< ? > a;
            }
        """)

        val typeParam = a.classes[0].fields()[0].typeExpr as Tr.ParameterizedType
        assertEquals("List< ? >", typeParam.print())
    }
}