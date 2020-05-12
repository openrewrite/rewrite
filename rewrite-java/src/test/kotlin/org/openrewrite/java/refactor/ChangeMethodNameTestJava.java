/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.refactor;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChangeMethodNameTestJava {
    private final JavaParser parser = new JavaParser();

    @Test
    public void refactorMethodName() {
        String a = "class A {{ B.foo(0); }}";
        String b = "class B { static void foo(int n) {} }";

        final J.CompilationUnit cu = parser.parse(a, /* which depends on */ b);

        J.CompilationUnit fixed = cu.refactor()
                .visit(new ChangeMethodName(cu.findMethodCalls("B foo(int)").get(0), "bar"))
                .fix().getFixed();

        assertEquals(fixed.print(), "class A {{ B.bar(0); }}");
    }

    @Test
    public void refactorMethodNameDiff() {
        String a = "class A {\n   {\n      B.foo(0);\n   }\n}";
        String b = "class B { static void foo(int n) {} }";

        final J.CompilationUnit cu = parser.parse(a, /* which depends on */ b);

        String diff = cu.refactor()
                .visit(new ChangeMethodName(cu.findMethodCalls("B foo(int)").get(0), "bar"))
                .fix()
                .diff();

        System.out.println(diff);
    }
}
