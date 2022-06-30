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
package org.openrewrite.java

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.tree.J
import java.util.stream.Collectors.toList

interface RenameVariableTest : JavaRecipeTest {
    fun renameVariableTest(targetClassName: String, hasName: String, toName: String): Recipe =
        toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J {
                    if (classDecl.simpleName.equals(targetClassName)) {
                        val variableDecls =
                            classDecl.body.statements.stream().filter { s -> s is J.VariableDeclarations }
                                .map { s -> s as J.VariableDeclarations }
                                .collect(toList())

                        val namedVariables: MutableList<J.VariableDeclarations.NamedVariable> = mutableListOf()
                        variableDecls.forEach { namedVariables.addAll(it.variables) }

                        for (namedVariable in namedVariables) {
                            if (namedVariable.simpleName.equals(hasName)) {
                                doAfterVisit(RenameVariable(namedVariable, toName))
                            }
                        }
                    }
                    return super.visitClassDeclaration(classDecl, p)
                }
            }
        }

    @Test
    fun doNotChangeToJavaKeyword(jp: JavaParser) = assertUnchanged(
        jp,
        recipe = renameVariableTest("A", "val", "int"),
        before = """
            package org.openrewrite;

            public class A {
                int fooA() {
                    val++;
                    // creates new scope owned by for loop.
                    for (int val = 0; val < 10; ++val) {
                        int x = val + 1;
                    }
                    val++;
                }
                // val is scoped to classDeclaration regardless of statement order.
                public int val = 1;
            }
        """
    )

    @Test
    fun doNotRenameForLoopVariables(jp: JavaParser) = assertChanged(
        jp,
        recipe = renameVariableTest("A", "val", "VALUE"),
        before = """
            package org.openrewrite;

            public class A {
                int fooA() {
                    val++;
                    // creates new scope owned by for loop.
                    for (int val = 0; val < 10; ++val) {
                        int x = val + 1;
                    }
                    val++;
                }
                // val is scoped to classDeclaration regardless of statement order.
                public int val = 1;
            }
        """,
        after = """
            package org.openrewrite;

            public class A {
                int fooA() {
                    VALUE++;
                    // creates new scope owned by for loop.
                    for (int val = 0; val < 10; ++val) {
                        int x = val + 1;
                    }
                    VALUE++;
                }
                // val is scoped to classDeclaration regardless of statement order.
                public int VALUE = 1;
            }
        """
    )

    @Suppress("UnusedAssignment")
    @Test
    fun renameForLoopVariables(jp: JavaParser) = assertChanged(
        jp,
        recipe = renameVariableTest("A", "val", "VALUE"),
        before = """
            package org.openrewrite;

            public class A {
                int fooA() {
                    // refers to class declaration scope.
                    for (val = 0; val < 10; ++val) {
                        int x = val + 1;
                        int val = 10;
                        val++;
                    }
                }
                // val is scoped to classDeclaration regardless of statement order.
                public int val = 1;
            }
        """,
        after = """
            package org.openrewrite;

            public class A {
                int fooA() {
                    // refers to class declaration scope.
                    for (VALUE = 0; VALUE < 10; ++VALUE) {
                        int x = VALUE + 1;
                        int val = 10;
                        val++;
                    }
                }
                // val is scoped to classDeclaration regardless of statement order.
                public int VALUE = 1;
            }
        """
    )

    @Test
    fun doNotRenameInnerClassVariables(jp: JavaParser) = assertChanged(
        jp,
        recipe = renameVariableTest("A", "val", "VALUE"),
        before = """
            package org.openrewrite;

            public class A {
                // Scoped to ClassDeclaration regardless of statement order.
                public int val = 1;

                class X {
                    int val = 0;
                }
            }
        """,
        after = """
            package org.openrewrite;

            public class A {
                // Scoped to ClassDeclaration regardless of statement order.
                public int VALUE = 1;

                class X {
                    int val = 0;
                }
            }
        """
    )

    @Test
    fun renameInnerClassVariables(jp: JavaParser) = assertChanged(
        jp,
        recipe = renameVariableTest("A", "val", "VALUE"),
        before = """
            package org.openrewrite;

            public class A {
                public int val = 0;

                class X {
                    int foo() {
                        val = 10;
                        return val;
                    }
                }
            }
        """,
        after = """
            package org.openrewrite;

            public class A {
                public int VALUE = 0;

                class X {
                    int foo() {
                        VALUE = 10;
                        return VALUE;
                    }
                }
            }
        """
    )

    @Test
    fun renameTry(jp: JavaParser) = assertChanged(
        jp,
        recipe = renameVariableTest("A", "val", "VALUE"),
        before = """
            package org.openrewrite;

            import java.io.FileInputStream;
            import java.io.FileDescriptor;

            public class A {
                public int val = 0;

                int foo() {
                    try (FileInputStream fs = new FileInputStream("file")) {
                        FileDescriptor fd = fs.getFD();
                        val++;
                    } catch (Exception ex) {
                        throw new RuntimeException("" + val, ex);
                    }
                    return val;
                }
            }
        """,
        after = """
            package org.openrewrite;

            import java.io.FileInputStream;
            import java.io.FileDescriptor;

            public class A {
                public int VALUE = 0;

                int foo() {
                    try (FileInputStream fs = new FileInputStream("file")) {
                        FileDescriptor fd = fs.getFD();
                        VALUE++;
                    } catch (Exception ex) {
                        throw new RuntimeException("" + VALUE, ex);
                    }
                    return VALUE;
                }
            }
        """
    )

    @Test
    fun renameCatchWithoutResource(jp: JavaParser) = assertChanged(
        jp,
        recipe = renameVariableTest("A", "val", "VALUE"),
        before = """
            package org.openrewrite;

            import java.io.FileDescriptor;
            import java.io.FileInputStream;

            public class A {
                public int val = 0;

                int foo() {
                    try {
                        val++;
                        String val = "1234";
                        Integer.valueOf(val);
                    } catch (Exception ex) {
                        throw new RuntimeException("" + val, ex);
                    }
                    return val;
                }
            }
        """,
        after = """
            package org.openrewrite;

            import java.io.FileDescriptor;
            import java.io.FileInputStream;

            public class A {
                public int VALUE = 0;

                int foo() {
                    try {
                        VALUE++;
                        String val = "1234";
                        Integer.valueOf(val);
                    } catch (Exception ex) {
                        throw new RuntimeException("" + VALUE, ex);
                    }
                    return VALUE;
                }
            }
        """
    )

    @Test
    fun renameCatchWithTryResource(jp: JavaParser) = assertChanged(
        jp,
        recipe = renameVariableTest("A", "val", "VALUE"),
        before = """
            package org.openrewrite;

            import java.io.FileDescriptor;
            import java.io.FileInputStream;

            public class A {
                public int val = 0;

                int foo() {
                    FileDescriptor fd;
                    try (FileInputStream val = new FileInputStream("file")) {
                        fd = val.getFD();
                    } catch (Exception ex) {
                        throw new RuntimeException("" + val, ex);
                    }
                    return fd.hashCode();
                }
            }
        """,
        after = """
            package org.openrewrite;

            import java.io.FileDescriptor;
            import java.io.FileInputStream;

            public class A {
                public int VALUE = 0;

                int foo() {
                    FileDescriptor fd;
                    try (FileInputStream val = new FileInputStream("file")) {
                        fd = val.getFD();
                    } catch (Exception ex) {
                        throw new RuntimeException("" + VALUE, ex);
                    }
                    return fd.hashCode();
                }
            }
        """
    )

    @Suppress("UnnecessaryLocalVariable")
    @Test
    fun renameVariablesInLambda(jp: JavaParser) = assertChanged(
        jp,
        recipe = renameVariableTest("A", "val", "VALUE"),
        before = """
            package org.openrewrite;

            import java.util.function.BiFunction;

            public class A {
                public Integer val = 1;

                int onlyChangeInScope() {
                    BiFunction<String, Integer, Integer> x = (String k, Integer v) -> 
                    val == null ? 42 : val + 41;

                    // Class scope.
                    val = 10;
                    // Method scope.
                    int val = 10;
                    return val;
                }
            }
        """,
        after = """
            package org.openrewrite;

            import java.util.function.BiFunction;

            public class A {
                public Integer VALUE = 1;

                int onlyChangeInScope() {
                    BiFunction<String, Integer, Integer> x = (String k, Integer v) -> 
                    VALUE == null ? 42 : VALUE + 41;

                    // Class scope.
                    VALUE = 10;
                    // Method scope.
                    int val = 10;
                    return val;
                }
            }
        """
    )

    @Test
    fun doNotRenameVariablesInLambda(jp: JavaParser) = assertChanged(
        jp,
        recipe = renameVariableTest("A", "val", "VALUE"),
        before = """
            package org.openrewrite;

            import java.util.function.BiFunction;

            public class A {
                public Integer val = 1;

                int onlyChangeInScope() {
                    BiFunction<String, Integer, Integer> x = (String k, Integer val) -> 
                    val == null ? 42 : val + 41;

                    // Class scope.
                    val = 10;
                }
            }
        """,
        after = """
            package org.openrewrite;

            import java.util.function.BiFunction;

            public class A {
                public Integer VALUE = 1;

                int onlyChangeInScope() {
                    BiFunction<String, Integer, Integer> x = (String k, Integer val) -> 
                    val == null ? 42 : val + 41;

                    // Class scope.
                    VALUE = 10;
                }
            }
        """
    )

    @Suppress("UnusedAssignment")
    @Test
    fun renameSwitchCases(jp: JavaParser) = assertChanged(
        jp,
        recipe = renameVariableTest("A", "val", "VALUE"),
        before = """
            package org.openrewrite;

            public class A {
                public int val = 1;

                int newScopeDoNotChange() {
                    switch (val) {
                        case 1:
                            val++;
                            break;
                        case 2:
                            int val = 100;
                            val += 2;
                            break;
                        case 3:
                            val = 0;
                            break;
                        default:
                            break;
                    }
                    return val;
                }
            }
        """,
        after = """
            package org.openrewrite;

            public class A {
                public int VALUE = 1;

                int newScopeDoNotChange() {
                    switch (VALUE) {
                        case 1:
                            VALUE++;
                            break;
                        case 2:
                            int val = 100;
                            val += 2;
                            break;
                        case 3:
                            VALUE = 0;
                            break;
                        default:
                            break;
                    }
                    return VALUE;
                }
            }
        """
    )

    @Suppress("UnnecessaryLocalVariable")
    @Test
    fun renameMethodVariables(jp: JavaParser) = assertChanged(
        jp,
        recipe = renameVariableTest("A", "val", "VALUE"),
        before = """
            package org.openrewrite;

            public class A {
                public int val = 1;

                int newScopeDoNotChange(int val) {
                    return val;
                }
                int onlyChangeInScope() {
                    // Class scope.
                    val = 10;
                    // Method scope.
                    int val = 10;
                    return val;
                }
            }
        """,
        after = """
            package org.openrewrite;

            public class A {
                public int VALUE = 1;

                int newScopeDoNotChange(int val) {
                    return val;
                }
                int onlyChangeInScope() {
                    // Class scope.
                    VALUE = 10;
                    // Method scope.
                    int val = 10;
                    return val;
                }
            }
        """
    )

    @Suppress("UnnecessaryLocalVariable")
    @Issue("https://github.com/openrewrite/rewrite/pull/1603")
    @Test
    fun renameFieldAccessVariables(jp: JavaParser) = assertChanged(
        jp,
        recipe = renameVariableTest("A", "val", "VALUE"),
        dependsOn =  arrayOf("""
            class ClassWithPublicField {
                public int publicField = 10;
            }
        """),
        before = """
            public class A {
                public ClassWithPublicField val = new ClassWithPublicField();

                int getNumberTwice() {
                    val.publicField = this.val.publicField + 10;
                    return val.publicField;
                }
            }
        """,
        after = """
            public class A {
                public ClassWithPublicField VALUE = new ClassWithPublicField();

                int getNumberTwice() {
                    VALUE.publicField = this.VALUE.publicField + 10;
                    return VALUE.publicField;
                }
            }
        """
    )

    @Suppress("UnnecessaryLocalVariable")
    @Issue("https://github.com/openrewrite/rewrite/pull/1603")
    @Test
    fun renameLocalFieldAccessInStaticMethod(jp: JavaParser) = assertChanged(
        jp,
        recipe = renameVariableTest("A", "val", "VALUE"),
        before = """
            public class A {
                private int val;

                static A getInstance() {
                    A a = new A();
                    a.val = 12;
                    return a;
                }
            }
        """,
        after = """
            public class A {
                private int VALUE;

                static A getInstance() {
                    A a = new A();
                    a.VALUE = 12;
                    return a;
                }
            }
        """
    )

    @Suppress("StatementWithEmptyBody", "ConstantConditions", "UnusedAssignment")
    @Test
    fun renameVariable(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaVisitor<ExecutionContext>() {
                override fun visitVariableDeclarations(multiVariable: J.VariableDeclarations, p: ExecutionContext): J {
                    if (cursor.dropParentUntil { it is J }.getValue<J>() is J.MethodDeclaration) {
                        doAfterVisit(RenameVariable(multiVariable.variables[0], "n2"))
                    } else if (cursor
                            .dropParentUntil { it is J }
                            .dropParentUntil { it is J }
                            .getValue<J>() !is J.ClassDeclaration
                    ) {
                        doAfterVisit(RenameVariable(multiVariable.variables[0], "n1"))
                    }
                    return super.visitVariableDeclarations(multiVariable, p)
                }
            }
        },
        before = """
            public class B {
                int n;
            
                {
                    n++; // do not change.
                    int n;
                    n = 1;
                    n /= 2;
                    if(n + 1 == 2) {}
                    n++;
                }
               
                public int foo(int n) {
                    return n + this.n;
                }
            }
        """,
        after = """
            public class B {
                int n;
            
                {
                    n++; // do not change.
                    int n1;
                    n1 = 1;
                    n1 /= 2;
                    if(n1 + 1 == 2) {}
                    n1++;
                }
               
                public int foo(int n2) {
                    return n2 + this.n;
                }
            }
        """
    )
}
