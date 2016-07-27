package com.netflix.java.refactor.fix

import com.netflix.java.refactor.AbstractRefactorTest
import org.junit.Test
import java.io.File

class ChangeMethodInvocationTest: AbstractRefactorTest() {
    
    @Test
    fun refactorMethodNameForMethodWithSingleArg() {
        val a = java("""
            |class A {
            |   public void test() {
            |       new B().singleArg("boo");
            |   }
            |}
        """)

        parseJava(a, b()).refactor()
                .findMethodCalls("B singleArg(String)")
                    .changeName("bar")
                    .done()
                .fix()
        
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

        parseJava(a, b()).refactor()
                .findMethodCalls("B arrArg(String[])")
                    .changeName("bar")
                    .done()
                .fix()

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

        parseJava(a, b()).refactor()
                .findMethodCalls("B varargArg(String...)")
                    .changeName("bar")
                    .done()
                .fix()

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

        parseJava(a, b()).refactor()
                .findMethodCalls("B singleArg(String)")
                    .changeArguments()
                        .arg(String::class.java)
                            .changeLiterals { s -> s.toString().replace("%s", "{}") }
                            .done()
                        .done()
                    .done()
                .fix()

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
        
        parseJava(c, a, b).refactor()
                .findMethodCalls("a.A foo()")
                    .changeTarget("b.B")
                    .done()
                .fix()
        
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

        parseJava(c, a, b).refactor()
                .findMethodCalls("a.A foo()")
                    .changeTarget("b.B")
                    .done()
                .fix()

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

        parseJava(c, a, b).refactor()
                .findMethodCalls("b.B foo()")
                    .changeTargetToVariable("a")
                    .done()
                .fix()

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

        parseJava(c, a, b).refactor()
                .findMethodCalls("b.B foo()")
                    .changeTargetToVariable("a")
                    .done()
                .fix()

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

    @Test
    fun refactorReorderArguments() {
        val a = java("""
            |package a;
            |public class A {
            |   public void foo(String s, Integer n) {}
            |   public void foo(Integer n, String s) {}
            |}
        """)

        val b = java("""
            |import a.*;
            |public class B {
            |   A a;
            |   public void test() {
            |       a.foo("mystring", 0);
            |   }
            |}
        """)

        parseJava(b, a).refactor()
                .findMethodCalls("a.A foo(..)")
                    .changeArguments()
                        .arg(String::class.java)
                            .changeLiterals { s -> "anotherstring" }
                            .done()
                        .reorderByArgName("n", "s")
                        .done()
                    .done()
                .fix()

        assertRefactored(b, """
            |import a.*;
            |public class B {
            |   A a;
            |   public void test() {
            |       a.foo(0, "anotherstring");
            |   }
            |}
        """)
    }

    @Test
    fun refactorReorderArgumentsWhereOneOfTheOriginalArgumentsIsAVararg() {
        val a = java("""
            |package a;
            |public class A {
            |   public void foo(String s, Integer n, Object... o) {}
            |   public void bar(String s, Object... o) {}
            |}
        """)

        val b = java("""
            |import a.*;
            |public class B {
            |   A a;
            |   public void test() {
            |       a.foo("mystring", 0, "a");
            |   }
            |}
        """)

        parseJava(b, a).refactor()
                .findMethodCalls("a.A foo(..)")
                    .changeName("bar")
                    .changeArguments()
                        .reorderByArgName("s", "o", "n")
                        .done()
                    .done()
                .fix()

        assertRefactored(b, """
            |import a.*;
            |public class B {
            |   A a;
            |   public void test() {
            |       a.bar("mystring", "a", 0);
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