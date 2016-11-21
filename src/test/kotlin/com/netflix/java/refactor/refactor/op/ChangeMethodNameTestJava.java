package com.netflix.java.refactor.refactor.op;

import com.netflix.java.refactor.ast.Tr;
import com.netflix.java.refactor.parse.OracleJdkParser;
import com.netflix.java.refactor.parse.Parser;
import com.netflix.java.refactor.refactor.Refactor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChangeMethodNameTestJava {
    Parser parser = new OracleJdkParser();

    @Test
    public void refactorMethodName() {
        String a = "class A {{ B.foo(0); }}";
        String b = "class B { static void foo(int n) {} }";

        final Tr.CompilationUnit cu = parser.parse(a, /* which depends on */ b);

        Tr.CompilationUnit fixed = cu.refactor(refactor -> {
            for (Tr.MethodInvocation inv : cu.findMethodCalls("B foo(int)")) {
                refactor.changeName(inv, "bar");
            }
        }).fix();

        assertEquals(fixed.print(), "class A {{ B.bar(0); }}");
    }

    @Test
    public void refactorMethodNameDiff() {
        String a = "class A {\n   {\n      B.foo(0);\n   }\n}";
        String b = "class B { static void foo(int n) {} }";

        final Tr.CompilationUnit cu = parser.parse(a, /* which depends on */ b);

        String diff = cu.refactor(refactor -> {
            for (Tr.MethodInvocation inv : cu.findMethodCalls("B foo(int)")) {
                refactor.changeName(inv, "bar");
            }
        }).diff();

        System.out.println(diff);
    }
}
