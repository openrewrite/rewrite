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
            |       new B().singleArg("foo (%s)" + s + 0L);
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
            |       new B().singleArg("foo ({})" + s + 0L);
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
    fun refactorReorderArgumentsWithNoSourceAttachment() {
        val a = java("""
            |package a;
            |public class A {
            |   public void foo(String arg0, Integer arg1) {}
            |   public void foo(Integer arg0, String arg1) {}
            |}
        """)

        val b = java("""
            |import a.*;
            |public class B {
            |   A a;
            |   public void test() {
            |       a.foo("s", 0);
            |   }
            |}
        """)

        parseJava(b, a).refactor()
                .findMethodCalls("a.A foo(..)")
                    .changeArguments()
                        .whereArgNamesAre("s", "n")
                        .reorderByArgName("n", "s")
                        .done()
                    .done()
                .fix()

        assertRefactored(b, """
            |import a.*;
            |public class B {
            |   A a;
            |   public void test() {
            |       a.foo(0, "s");
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

    @Test
    fun refactorReorderArgumentsWhereTheLastArgumentIsVarargAndNotPresentInInvocation() {
        val a = java("""
            |package a;
            |public class A {
            |   public void foo(String s, Object... o) {}
            |}
        """)

        val b = java("""
            |import a.*;
            |public class B {
            |   public void test() {
            |       new A().foo("mystring");
            |   }
            |}
        """)

        parseJava(b, a).refactor()
                .findMethodCalls("a.A foo(..)")
                    .changeArguments()
                        .whereArgNamesAre("s", "o")
                        .reorderByArgName("o", "s")
                        .done()
                    .done()
                .fix()

        assertRefactored(b, """
            |import a.*;
            |public class B {
            |   public void test() {
            |       new A().foo("mystring");
            |   }
            |}
        """)
    }

    @Test
    fun refactorMethodNameWhenMatchingAgainstMethodWithNameThatIsAnAspectjToken() {
        val b = java("""
            |class B {
            |   public void error() {}
            |   public void foo() {}
            |}
        """)
        
        val a = java("""
            |class A {
            |   public void test() {
            |       new B().error();
            |   }
            |}
        """)

        parseJava(a, b).refactor()
                .findMethodCalls("B error()")
                    .changeName("foo")
                    .done()
                .fix()

        assertRefactored(a, """
            |class A {
            |   public void test() {
            |       new B().foo();
            |   }
            |}
        """)
    }

    @Test
    fun refactorInsertArgument() {
        val b = java("""
            |class B {
            |   public void foo() {}
            |   public void foo(String s) {}
            |   public void foo(String s1, String s2) {}
            |   public void foo(String s1, String s2, String s3) {}
            |
            |   public void bar(String s, String s2) {}
            |   public void bar(String s, String s2, String s3) {}
            |}
        """)

        val a = java("""
            |class A {
            |   public void test() {
            |       B b = new B();
            |       b.foo();
            |       b.foo("1");
            |       b.bar("1", "2");
            |   }
            |}
        """)

        parseJava(a, b).refactor()
                .findMethodCalls("B foo(String)")
                    .changeArguments()
                        .insert(0, "\"0\"") // insert at beginning
                        .insert(2, "\"2\"") // insert at end
                        .done()
                    .done()
                .findMethodCalls("B foo()")
                    .changeArguments()
                        .insert(0, "\"0\"")
                        .done()
                    .done()
                .findMethodCalls("B bar(String, String)")
                    .changeArguments()
                        .reorderByArgName("s2", "s") // compatibility of reordering and insertion
                        .insert(0, "\"0\"")
                        .done()
                    .done()
                .fix()

        assertRefactored(a, """
            |class A {
            |   public void test() {
            |       B b = new B();
            |       b.foo("0");
            |       b.foo("0", "1", "2");
            |       b.bar("0", "2", "1");
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