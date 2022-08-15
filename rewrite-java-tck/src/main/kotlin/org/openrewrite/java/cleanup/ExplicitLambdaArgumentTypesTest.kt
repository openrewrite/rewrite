/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.cleanup

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.TypeUtils

@Suppress(
    "ComparatorCombinators",
    "Convert2MethodRef",
    "ResultOfMethodCallIgnored",
    "CodeBlock2Expr"
)
interface ExplicitLambdaArgumentTypesTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = ExplicitLambdaArgumentTypes()

    @Issue("https://github.com/openrewrite/rewrite/issues/1459")
    @Test
    fun unknownArgumentType(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.function.Predicate;

            class Test {
                static void run(Predicate<WillyWonka> c) {
                }

                static void method() {
                    run(a -> {
                        return a.isEmpty();
                    });
                }
            }
        """
    )

    @Test
    fun oneArgumentExistingExplicitType(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.function.Consumer;

            class Test {
                static void run(Consumer<List<String>> c) {
                }

                static void method() {
                    run(a -> a.size());
                }
            }
        """
    )

    @Test
    fun oneArgumentNoBlock(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.function.Consumer;

            class Test {
                static void run(Consumer<String> c) {
                }

                static void method() {
                    run(q -> q.length());
                }
            }
        """
    )

    @Test
    fun twoArgumentsExistingExplicitType(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.function.BiConsumer;

            class Test {
                static void run(BiConsumer<String, Object> bc) {
                }

                static void method() {
                    run((String a, Object b) -> a.length());
                }
            }
        """
    )

    @Test
    fun twoArgumentsNoBlock(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.function.BiConsumer;

            class Test {
                static void run(BiConsumer<String, Object> bc) {
                }

                static void method() {
                    run((a, b) -> a.length());
                }
            }
        """
    )

    @Test
    fun twoArgumentsWithBlock(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.function.BiPredicate;

            class Test {
                static void run(BiPredicate<String, Object> bc) {
                }

                static void method() {
                    run((a, b) -> {
                        return a.isEmpty();
                    });
                }
            }
        """,
        after = """
            import java.util.function.BiPredicate;

            class Test {
                static void run(BiPredicate<String, Object> bc) {
                }

                static void method() {
                    run((String a, Object b) -> {
                        return a.isEmpty();
                    });
                }
            }
        """
    )

    @Test
    fun handlePrimitiveArrays(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.function.BiFunction;

            class Test {
                private final BiFunction<Integer, byte[], byte[]> func = (a, b) -> {
                    return null;
                };
            }
        """,
        after = """
            import java.util.function.BiFunction;

            class Test {
                private final BiFunction<Integer, byte[], byte[]> func = (Integer a, byte[] b) -> {
                    return null;
                };
            }
        """
    )

    @Test
    fun handleMultiDimensionalPrimitiveArrays(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.function.BiFunction;

            class Test {
                private final BiFunction<Integer, byte[][], byte[][]> func = (a, b) -> {
                    return null;
                };
            }
        """,
        after = """
            import java.util.function.BiFunction;

            class Test {
                private final BiFunction<Integer, byte[][], byte[][]> func = (Integer a, byte[][] b) -> {
                    return null;
                };
            }
        """
    )

    @Test
    fun handleMultiDimensionalFullyQualifiedArrays(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.function.BiFunction;

            class Test {
                private final BiFunction<Integer, Integer[][], Integer[][]> func = (a, b) -> {
                    return null;
                };
            }
        """,
        after = """
            import java.util.function.BiFunction;

            class Test {
                private final BiFunction<Integer, Integer[][], Integer[][]> func = (Integer a, Integer[][] b) -> {
                    return null;
                };
            }
        """
    )

    @Test
    fun oneArgumentWithBlock(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.function.Predicate;

            class Test {
                static void run(Predicate<String> c) {
                }

                static void method() {
                    run(a -> {
                        return a.isEmpty();
                    });
                }
            }
        """,
        after = """
            import java.util.function.Predicate;

            class Test {
                static void run(Predicate<String> c) {
                }

                static void method() {
                    run((String a) -> {
                        return a.isEmpty();
                    });
                }
            }
        """
    )

    @Test
    fun threeArgumentsNoBlock(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                static void run(TriConsumer tc) {
                }

                static void method() {
                    run((a, b, c) -> a.toUpperCase());
                }

                private interface TriConsumer {
                    String method(String a, String b, String c);
                }
            }
        """,
        after = """
            class Test {
                static void run(TriConsumer tc) {
                }

                static void method() {
                    run((String a, String b, String c) -> a.toUpperCase());
                }

                private interface TriConsumer {
                    String method(String a, String b, String c);
                }
            }
        """
    )

    @Test
    fun threeArgumentsWithBlock(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                static void run(TriConsumer tc) {
                }

                static void method() {
                    run((a, b, c) -> {
                        return a.toUpperCase();
                    });
                }

                private interface TriConsumer {
                    String method(String a, String b, String c);
                }
            }
        """,
        after = """
            class Test {
                static void run(TriConsumer tc) {
                }

                static void method() {
                    run((String a, String b, String c) -> {
                        return a.toUpperCase();
                    });
                }

                private interface TriConsumer {
                    String method(String a, String b, String c);
                }
            }
        """
    )

    @Test
    fun threeArgumentsWithBlockPrimitive(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                static void run(TriConsumer tc) {
                }

                static void method() {
                    run((a, b, c) -> {
                        return a + b - c;
                    });
                }

                private interface TriConsumer {
                    int method(int a, int b, int c);
                }
            }
        """,
        after = """
            class Test {
                static void run(TriConsumer tc) {
                }

                static void method() {
                    run((int a, int b, int c) -> {
                        return a + b - c;
                    });
                }

                private interface TriConsumer {
                    int method(int a, int b, int c);
                }
            }
        """
    )

    @Test
    fun threeArgumentsWithBlockGeneric(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                static <T> void run(TriConsumer<T> tc) {
                }

                static void method() {
                    run((a, b, c) -> {
                        return a.toString();
                    });
                }

                private interface TriConsumer<T> {
                    T method(T a, T b, T c);
                }
            }
        """,
        after = """
            class Test {
                static <T> void run(TriConsumer<T> tc) {
                }

                static void method() {
                    run((Object a, Object b, Object c) -> {
                        return a.toString();
                    });
                }

                private interface TriConsumer<T> {
                    T method(T a, T b, T c);
                }
            }
        """
    )

    @Test
    fun noArguments(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.function.Supplier;

            class Test {
                static void run(Supplier<String> s) {
                }

                static void method() {
                    run(() -> {
                        return "example";
                    });
                }
            }
        """
    )

    @Test
    fun arraysSortExample(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.Arrays;

            class Test {
                static void method(String[] arr) {
                    Arrays.sort(arr, (a, b) -> {
                        return a.length() - b.length();
                    });
                }
            }
        """,
        after = """
            import java.util.Arrays;

            class Test {
                static void method(String[] arr) {
                    Arrays.sort(arr, (String a, String b) -> {
                        return a.length() - b.length();
                    });
                }
            }
        """
    )

    @Test
    fun arraysSortExampleWithGeneric(jp: JavaParser) = assertChanged(
        jp,
        before = """
            import java.util.Arrays;

            class Test {
                static <T> void method(T[] arr) {
                    Arrays.sort(arr, (a, b) -> {
                        return a.toString().length() - b.toString().length();
                    });
                }
            }
        """,
        after = """
            import java.util.Arrays;

            class Test {
                static <T> void method(T[] arr) {
                    Arrays.sort(arr, (T a, T b) -> {
                        return a.toString().length() - b.toString().length();
                    });
                }
            }
        """
    )

    @Test
    fun parameterizedTypes(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                package javafx.beans.value;

                public interface ObservableValue<T> {
                    void addListener(ChangeListener<? super T> listener);
                }
            """,
            """
                package javafx.beans.value;
                
                @FunctionalInterface
                public interface ChangeListener<T> {
                    void changed(ObservableValue<? extends T> observable, T oldValue, T newValue);
                }                
            """,
            """
                package example;
                
                import javafx.beans.value.ObservableValue;
                import javafx.beans.value.ChangeListener;

                public class Fred implements ObservableValue<String>{
                    public void addListener(ChangeListener<? super String> listener) {
                    }
                }
            """.trimIndent()
        ),
        before = """
            import javafx.beans.value.ObservableValue;
            import example.Fred;
            
            class Test {
                void foo() {
                    Fred fred = new Fred();
                    fred.addListener((ov, oldState, newState) -> {
                    });
                }
            }
        """,
        after = """
            import javafx.beans.value.ObservableValue;
            import example.Fred;
            
            class Test {
                void foo() {
                    Fred fred = new Fred();
                    fred.addListener((ObservableValue<? extends String> ov, String oldState, String newState) -> {
                    });
                }
            }
        """,
        afterConditions = {u ->
            val fooMethod = u.classes[0].body.statements[0] as J.MethodDeclaration
            val addListenerMethod = fooMethod.body!!.statements[1] as J.MethodInvocation
            val lambda = addListenerMethod.arguments[0] as J.Lambda
            val lambdaType = TypeUtils.asFullyQualified(lambda.type)
            Assertions.assertThat((lambdaType!!.typeParameters[0] as JavaType.FullyQualified).fullyQualifiedName).isEqualTo("java.lang.String")
            val ovVariableDecl = lambda.parameters.parameters[0] as J.VariableDeclarations
            val ovType = TypeUtils.asFullyQualified(ovVariableDecl.variables[0].type)
            val genericType = (ovType!!.typeParameters[0]) as JavaType.GenericTypeVariable
            //This assertion should pass but the argument's type is not being correctly changed to match the signature of the
            //parameterized type.
            Assertions.assertThat((genericType.bounds[0] as JavaType.FullyQualified).fullyQualifiedName).isEqualTo("java.lang.String")
        }
    )

}
