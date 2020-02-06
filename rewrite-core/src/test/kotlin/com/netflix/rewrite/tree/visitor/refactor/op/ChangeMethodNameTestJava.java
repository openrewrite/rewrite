package com.netflix.rewrite.tree.visitor.refactor.op;

import com.netflix.rewrite.parse.OpenJdkParser;
import com.netflix.rewrite.parse.Parser;
import com.netflix.rewrite.tree.Tr;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChangeMethodNameTestJava {
    final Parser parser = new OpenJdkParser();

    @Test
    public void refactorMethodName() {
        String a = "class A {{ B.foo(0); }}";
        String b = "class B { static void foo(int n) {} }";

        final Tr.CompilationUnit cu = parser.parse(a, /* which depends on */ b);

        Tr.CompilationUnit fixed = cu.refactor()
                .changeMethodName(cu.findMethodCalls("B foo(int)"), "bar")
                .fix();

        assertEquals(fixed.print(), "class A {{ B.bar(0); }}");
    }

    @Test
    public void refactorMethodNameDiff() {
        String a = "class A {\n   {\n      B.foo(0);\n   }\n}";
        String b = "class B { static void foo(int n) {} }";

        final Tr.CompilationUnit cu = parser.parse(a, /* which depends on */ b);

        String diff = cu.refactor()
                .changeMethodName(cu.findMethodCalls("B foo(int)"), "bar")
                .diff();

        System.out.println(diff);
    }
}
