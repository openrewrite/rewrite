package com.netflix.java.refactor.fix

import com.netflix.java.refactor.RefactorTest
import org.junit.Test
import java.io.File

class ChangeMethodInvocationTest: RefactorTest() {
    
    @Test
    fun refactorMethodNameForMethodWithSingleArg() {
        val a = java("""
            |class A {
            |   public void test() {
            |       new B().singleArg("boo");
            |   }
            |}
        """)

        refactor(a, b())
                .changeMethod("B singleArg(String)")
                    .refactorName("bar")
                    .done()
        
        assertRefactored(a, """
            |class A {
            |   public void test() {
            |       new B().bar("boo");
            |   }
            |}
        """)
    }

    @Test
    fun refactorMethodNameForMethodWithArrayArg() {
        val a = java("""
            |class A {
            |   public void test() {
            |       new B().arrArg(new String[] {"boo"});
            |   }
            |}
        """)

        refactor(a, b())
                .changeMethod("B arrArg(String[])")
                .refactorName("bar")
                .done()

        assertRefactored(a, """
            |class A {
            |   public void test() {
            |       new B().bar(new String[] {"boo"});
            |   }
            |}
        """)
    }

    @Test
    fun refactorMethodNameForMethodWithVarargArg() {
        val a = java("""
            |class A {
            |   public void test() {
            |       new B().varargArg("boo", "again");
            |   }
            |}
        """)

        refactor(a, b())
                .changeMethod("B varargArg(String...)")
                    .refactorName("bar")
                    .done()

        assertRefactored(a, """
            |class A {
            |   public void test() {
            |       new B().bar("boo", "again");
            |   }
            |}
        """)
    }
    
    @Test
    fun transformStringArgument() {
        val a = java("""
            |class A {
            |   public void test() {
            |       String s = "bar";
            |       new B().singleArg("foo %s " + s + 0L);
            |   }
            |}
        """)

        refactor(a, b())
                .changeMethod("B singleArg(String)")
                    .refactorArgument(0)
                        .isType(String::class.java)
                        .mapLiterals { s -> s.toString().replace("%s", "{}") }
                    .done()
                .done()

        assertRefactored(a, """
            |class A {
            |   public void test() {
            |       String s = "bar";
            |       new B().singleArg("foo {} " + s + 0L);
            |   }
            |}
        """)
    }
    
    @Test
    fun refactorTargetToStatic() {
        val a = java("""
            |package a;
            |public class A {
            |   public void foo() {}
            |}
        """)
        
        val b = java("""
            |package b;
            |public class B {
            |   public static void foo() {}
            |}
        """)
        
        val c = java("""
            |import a.*;
            |class C {
            |   public void test() {
            |       new A().foo();
            |   }
            |}
        """)
        
        refactor(c, a, b).changeMethod("a.A foo()")
                .refactorTargetToStatic("b.B")
                .done()
        
        assertRefactored(c, """
            |import a.*;
            |import b.B;
            |class C {
            |   public void test() {
            |       B.foo();
            |   }
            |}
        """)
    }
    
    @Test
    fun refactorStaticTargetToStatic() {
        val a = java("""
            |package a;
            |public class A {
            |   public static void foo() {}
            |}
        """)

        val b = java("""
            |package b;
            |public class B {
            |   public static void foo() {}
            |}
        """)

        val c = java("""
            |import static a.A.*;
            |class C {
            |   public void test() {
            |       foo();
            |   }
            |}
        """)

        refactor(c, a, b).changeMethod("a.A foo()")
                .refactorTargetToStatic("b.B")
                .done()

        assertRefactored(c, """
            |import b.B;
            |import static a.A.*;
            |class C {
            |   public void test() {
            |       B.foo();
            |   }
            |}
        """)
    }

    @Test
    fun refactorExplicitStaticToVariable() {
        val a = java("""
            |package a;
            |public class A {
            |   public void foo() {}
            |}
        """)

        val b = java("""
            |package b;
            |public class B {
            |   public static void foo() {}
            |}
        """)

        val c = java("""
            |import a.*;
            |import b.B;
            |public class C {
            |   A a;
            |   public void test() {
            |       B.foo();
            |   }
            |}
        """)

        refactor(c, a, b).changeMethod("b.B foo()")
                .refactorTargetToVariable("a")
                .done()

        assertRefactored(c, """
            |import a.*;
            |import b.B;
            |public class C {
            |   A a;
            |   public void test() {
            |       a.foo();
            |   }
            |}
        """)
    }

    @Test
    fun refactorStaticImportToVariable() {
        val a = java("""
            |package a;
            |public class A {
            |   public void foo() {}
            |}
        """)

        val b = java("""
            |package b;
            |public class B {
            |   public static void foo() {}
            |}
        """)

        val c = java("""
            |import a.*;
            |import static b.B.*;
            |public class C {
            |   A a;
            |   public void test() {
            |       foo();
            |   }
            |}
        """)

        refactor(c, a, b).changeMethod("b.B foo()")
                .refactorTargetToVariable("a")
                .done()

        assertRefactored(c, """
            |import a.*;
            |import static b.B.*;
            |public class C {
            |   A a;
            |   public void test() {
            |       a.foo();
            |   }
            |}
        """)
    }

    private fun b(): File = java("""
                |class B {
                |   public void singleArg(String s) {}
                |   public void arrArg(String[] s) {}
                |   public void varargArg(String... s) {}
                |}
            """)
}