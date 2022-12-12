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

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.marker.JavaVersion
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import java.util.*

@Suppress(
    "ConstantConditions",
    "StatementWithEmptyBody",
    "EmptyTryBlock",
    "CatchMayIgnoreException",
    "UnusedAssignment",
    "ResultOfMethodCallIgnored",
    "BooleanMethodNameMustStartWithQuestion",
    "PointlessBooleanExpression",
    "UseOfObsoleteCollectionType",
    "UnnecessaryLocalVariable",
    "EmptyFinallyBlock",
    "ClassInitializerMayBeStatic",
    "FunctionName",
    "ParameterCanBeLocal"
)
interface RemoveUnusedLocalVariablesTest : RewriteTest, JavaRecipeTest {
    override val recipe: Recipe
        get() = RemoveUnusedLocalVariables(arrayOf())

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(RemoveUnusedLocalVariables(arrayOf()))
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/841")
    fun ignoreSuppressWarnings() = assertUnchanged(
        before = """
            class Test {
                static int method(int x) {
                    int a = 0;
                    @SuppressWarnings("unused") int b = 0;
                    return a + 1;
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1332")
    @Suppress("MethodMayBeStatic")
    fun ignoreVariablesNamed() = assertUnchanged(
        recipe = RemoveUnusedLocalVariables(arrayOf("unused", "ignoreMe")),
        before = """
            class Test {
                void method(Object someData) {
                    int unused = writeDataToTheDB(someData);
                    int ignoreMe = writeDataToTheDB(someData);
                }

                int writeDataToTheDB(Object save) {
                    return 1;
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1278")
    @Suppress("MethodMayBeStatic")
    fun keepRightHandSideStatement() = assertUnchanged(
        before = """
            class Test {
                void method(Object someData) {
                    int doNotRemoveMe = writeDataToTheDB(someData);
                    int doNotRemoveMeEither = 1 + writeDataToTheDB(someData);
                }

                int writeDataToTheDB(Object save) {
                    return 1;
                }
            }
        """
    )

    @Test
    fun keepStatementWhenSideEffectInInitialization() = assertUnchanged(
        before = """
            class Test {
                void method(Object someData) {
                    // Don't write code like this.... Please
                    int a = null == (someData = null) ? 0 : 9;
                }
            }
        """
    )

    @Test
    fun keepStatementWhenSideEffectInAccess() = assertUnchanged(
        before = """
            class Test {
                void method(Object someData) {
                    String a = "";
                    while((a = reader.nexLine()) != null) {
                        System.out.println(a);
                    }
                }
            }
        """
    )

    @Test
    fun removeUnusedLocalVariables() = assertChanged(
        before = """
            class Test {
                static int method(int x) {
                    int a = 0;
                    int b = 0;
                    return a;
                }
            }
        """,
        after = """
            class Test {
                static int method(int x) {
                    int a = 0;
                    return a;
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1742")
    @Test
    fun preserveComment() = assertChanged(
        before = """
            class Test {
                Long method() {
                    // Keep comment
                    String foo;
                    return Long.parseLong("123");
                }
            }
        """,
        after = """
            class Test {
                Long method() {
                    // Keep comment
                    return Long.parseLong("123");
                }
            }
        """
    )

    @Test
    fun removeUnusedLocalVariablesReassignedButNeverUsed() = assertChanged(
        before = """
            class Test {
                static int method() {
                    int isRead = -1;
                    int notRead = 0;
                    notRead = 1;
                    notRead += 1;
                    notRead = isRead + 1;
                    return isRead + 1;
                }
            }
        """,
        after = """
            class Test {
                static int method() {
                    int isRead = -1;
                    return isRead + 1;
                }
            }
        """
    )

    @Test
    fun ignoreClassFields() = assertUnchanged(
        before = """
            class Test {
                int a = 0;
                int b = 1;

                int method(int x) {
                    b = 2;
                    return x + 1;
                }
            }
        """
    )

    @Test
    fun removeUnusedLocalVariablesInClassInitializer() = assertChanged(
        before = """
            class Test {
                static {
                    int unused = 0;
                    int used = 1;
                    System.out.println(used);
                }

                {
                    int unused = 0;
                    int used = 1;
                    System.out.println(used);
                }
            }
        """,
        after = """
            class Test {
                static {
                    int used = 1;
                    System.out.println(used);
                }

                {
                    int used = 1;
                    System.out.println(used);
                }
            }
        """
    )

    @Test
    fun handleLocalVariablesShadowingClassFields() = assertChanged(
        before = """
            class Test {
                int a = 0;
                int unused = 1;

                static int method(int x) {
                    int unused = 2;
                    return x + 1;
                }
            }
        """,
        after = """
            class Test {
                int a = 0;
                int unused = 1;

                static int method(int x) {
                    return x + 1;
                }
            }
        """
    )

    @Test
    fun localVariableUnusedIncrementOperation() = assertChanged(
        before = """
            class Test {
                static boolean isTrue() {
                    return false;
                }

                static int method(int x) {
                    int a = 0;
                    int b = 99;
                    a++;
                    for (int i = 0; isTrue(); i++) {
                        a++;
                    }
                    return b++;
                }
            }
        """,
        after = """
            class Test {
                static boolean isTrue() {
                    return false;
                }

                static int method(int x) {
                    int b = 99;
                    for (int i = 0; isTrue(); i++) {
                    }
                    return b++;
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/apache/dubbo/blob/747282cdf851c144af562d3f92e10349cc315e36/dubbo-metadata/dubbo-metadata-definition-protobuf/src/test/java/org/apache/dubbo/metadata/definition/protobuf/model/GooglePB.java#L938-L944")
    fun keepLocalVariablesAssignmentOperationToOtherVariables() = assertUnchanged(
        before = """
            class Test {
                static int method() {
                    int dataSize = 0;
                    int size = 0;
                    for (int j = 0; j < 10; j++) {
                        dataSize += 1;
                    }
                    size += dataSize;
                    return size;
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/blob/706a172ed5449214a4a08637a27dbe768fb4eecd/rewrite-core/src/main/java/org/openrewrite/internal/StringUtils.java#L55-L65")
    fun keepLocalVariableAssignmentOperation() = assertUnchanged(
        before = """
            class Test {
                static boolean method() {
                    boolean a = false;
                    return a |= false;
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/blob/706a172ed5449214a4a08637a27dbe768fb4eecd/rewrite-core/src/main/java/org/openrewrite/internal/StringUtils.java#L55-L65")
    fun removeUnusedLocalVariableBitwiseAssignmentOperation() = assertChanged(
        before = """
            class Test {
                static boolean method() {
                    boolean a = false;
                    boolean b = false;
                    b &= true;
                    return a |= false;
                }
            }
        """,
        after = """
            class Test {
                static boolean method() {
                    boolean a = false;
                    return a |= false;
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/blob/706a172ed5449214a4a08637a27dbe768fb4eecd/rewrite-core/src/main/java/org/openrewrite/internal/StringUtils.java#L55-L65")
    fun keepLocalVariableBitwiseAssignmentOperationWithinExpression() = assertUnchanged(
        before = """
            class Test {
                static boolean method(String string) {
                    boolean a = false;
                    for (char c : string.toCharArray()) {
                        if (false || (a |= !Character.isWhitespace(c))) {
                            break;
                        }
                    }
                    return a;
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/blob/706a172ed5449214a4a08637a27dbe768fb4eecd/rewrite-core/src/main/java/org/openrewrite/internal/StringUtils.java#L55-L65")
    fun handleInstanceOf() = assertUnchanged(
        before = """
            import java.util.Stack;

            class Test {
                static boolean method(Stack<Object> typeStack) {
                    for (Object e = typeStack.pop(); ; e = typeStack.pop()) {
                        if (e instanceof String) {
                            break;
                        }
                    }
                    return true;
                }
            }
        """
    )

    @Test
    fun removeUnusedLocalVariablesFromMultiVariablesDeclaration() = assertChanged(
        before = """
            class Test {
                static int method(int x) {
                    int a = 0, b = 0, c = 0, d = 0;
                    return b + c;
                }
            }
        """,
        after = """
            class Test {
                static int method(int x) {
                    int b = 0, c = 0;
                    return b + c;
                }
            }
        """
    )

    @Test
    fun keepLocalVariablesWhenUsedAsMethodInvocationArgument() = assertUnchanged(
        before = """
            class Test {
                static void method() {
                    int a = 0;
                    System.out.println(a);
                }
            }
        """
    )

    @Test
    fun keepLocalVariablesWhenMethodInvocationsCalledOnThem() = assertUnchanged(
        before = """
            class Test {
                void method() {
                    Worker worker = new Worker();
                    worker.doWork();
                }

                class Worker {
                    void doWork() {
                        //
                    }
                }
            }
        """
    )

    @Test
    fun ignoreClassVariables() = assertUnchanged(
        before = """
            class Test {
                static int someClassVariable = 0;
                int someInstanceVariable = 0;

                static void method() {
                    // do nothing
                }
            }
        """
    )

    @Test
    fun ignoreAnonymousClassVariables() = assertUnchanged(
        before = """
            import java.io.File;

            class Test {
                static File method(File dir) {
                    final File src = new File(dir, "child") {
                        private static final long serialVersionUID = 1L;
                    };
                    return src;
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/apache/dubbo/blob/747282cdf851c144af562d3f92e10349cc315e36/dubbo-rpc/dubbo-rpc-api/src/main/java/org/apache/dubbo/rpc/RpcStatus.java#L108-L118")
    fun forLoopWithExternalIncrementLogic() = assertUnchanged(
        before = """
            class Test {
                static void method() {
                    for (int i; ; ) {
                        i = 41;
                        if (i == 42) {
                            break;
                        }
                    }
                }
            }
        """
    )

    @Test
    fun forLoopIncrementVariableReadInEvaluationCondition() = assertUnchanged(
        before = """
            class Test {
                static void method() {
                    for (int j = 0; j < 10; j++) {
                    }
                }
            }
        """
    )

    @Test
    fun removeUnusedLocalVariablesWithinTryCatch() = assertChanged(
        before = """
            class Test {
                static void method() {
                    try {
                        int a = 0;
                        int b = 1;
                        System.out.println(b);
                    } catch (Exception e) {
                        int a = 3;
                        int b = 4;
                        System.out.println(a);
                    } finally {
                        int a = 5;
                        int b = 6;
                    }
                }
            }
        """,
        after = """
            class Test {
                static void method() {
                    try {
                        int b = 1;
                        System.out.println(b);
                    } catch (Exception e) {
                        int a = 3;
                        System.out.println(a);
                    } finally {
                    }
                }
            }
        """
    )

    @Test
    fun ignoreTryWithResourceUnusedVariables() = assertUnchanged(
        before = """
            import java.util.stream.Stream;

            class Test {
                static void method() {
                    try (Stream<Object> unused = Stream.of()) {
                        // do nothing
                    } catch (Exception e) {
                        // do nothing
                    }
                }
            }
        """
    )

    @Test
    fun handleVariablesReadWithinTry() = assertChanged(
        before = """
            class Test {
                static void assertEquals(Object expected, Object actual) {
                    // do nothing
                }

                static void method() {
                    Object used, unused;
                    try {
                        used = new Object();
                        assertEquals(used, null);
                        unused = new Object();
                    } catch (Exception e) {
                        // do nothing
                    }
                }
            }
        """,
        after = """
            class Test {
                static void assertEquals(Object expected, Object actual) {
                    // do nothing
                }

                static void method() {
                    Object used;
                    try {
                        used = new Object();
                        assertEquals(used, null);
                    } catch (Exception e) {
                        // do nothing
                    }
                }
            }
        """
    )

    @Test
    fun ignoreUnusedTryCatchExceptionVariableDeclaration() = assertUnchanged(
        before = """
            class Test {
                static void method() {
                    try {
                        // do nothing
                    } catch (Exception e) {
                        // do nothing
                    }
                }
            }
        """
    )

    @Test
    fun ignoreUnusedLambdaExpressionParameters() = assertUnchanged(
        before = """
            import java.util.function.BinaryOperator;
            import java.util.function.UnaryOperator;

            class Test {
                static BinaryOperator<UnaryOperator<Object>> method() {
                    return (a, b) -> input -> {
                        Object o = a.apply(input);
                        o.toString();
                        return o;
                    };
                }
            }
        """
    )

    @Test
    fun ignoreUnusedLambdaExpressionParametersForEach() = assertUnchanged(
        before = """
            import java.util.List;

            class Test {
                static void method(List<Object> list) {
                    list.forEach(item -> {
                        // do nothing with "item"
                    });
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/txazo/spring-cloud-sourcecode/blob/5ffe615558e76f3bb37f19026ece5cbaff4d0404/eureka-client/src/main/java/com/netflix/discovery/converters/jackson/builder/StringInterningAmazonInfoBuilder.java#L114-L124")
    fun recognizeUsedVariableWithinWhileLoop() = assertChanged(
        before = """
            class Test {
                TestToken testToken;

                static void method(Test tp) {
                    int isUsed = 0;
                    TestToken token = tp.nextToken();
                    while ((token = tp.nextToken()) != TestToken.END_TOKEN) {
                        // do anything except read the value of "token"
                        tp.nextToken();
                        int unused = 11;
                        unused = isUsed;
                        System.out.println(isUsed);
                    }
                }

                TestToken nextToken() {
                    return this.testToken;
                }

                enum TestToken {
                    START_TOKEN,
                    END_TOKEN
                }
            }
        """,
        after = """
            class Test {
                TestToken testToken;

                static void method(Test tp) {
                    int isUsed = 0;
                    TestToken token = tp.nextToken();
                    while ((token = tp.nextToken()) != TestToken.END_TOKEN) {
                        // do anything except read the value of "token"
                        tp.nextToken();
                        System.out.println(isUsed);
                    }
                }

                TestToken nextToken() {
                    return this.testToken;
                }

                enum TestToken {
                    START_TOKEN,
                    END_TOKEN
                }
            }
        """
    )

    @Test
    @Issue("This still causes SonarQube to warn, but there isn't much that can be done in these cases. Maybe change to a while loop?")
    fun ignoreForLoopIncrementVariableNeverRead() = assertUnchanged(
        before = """
            class Test {
                static boolean isTrue() {
                    return true;
                }

                static void method() {
                    for (int j = 0; isTrue(); j++) {
                    }
                }
            }
        """
    )

    @Test
    @Issue("This still causes SonarQube to warn, but there isn't much that can be done in these cases. Maybe change to a forEach?")
    fun ignoreEnhancedForLoops() = assertUnchanged(
        before = """
            import java.util.List;

            class Test {
                static void method(List<String> list) {
                    for (String s : list) {
                        // do nothing
                    }
                }
            }
        """
    )

    @Test
    fun `remove file getter methods as they do not side effect`() = assertChanged(
        before = """
            import java.io.File;

            class Test {
                static void method(File file) {
                    String canonicalPath = file.getCanonicalPath();
                }
            }
        """,
        after = """
            import java.io.File;

            class Test {
                static void method(File file) {
                }
            }
        """
    )

    @Test
    fun `remove file getter methods when chained as they do not side effect`() = assertChanged(
        before = """
            import java.io.File;

            class Test {
                static void method(File file) {
                    String canonicalPath = file.getParentFile().getCanonicalPath();
                }
            }
        """,
        after = """
            import java.io.File;

            class Test {
                static void method(File file) {
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1743")
    @Test
    fun assignmentWithinExpression() = rewriteRun(
        java("""
            class A {
                void foo() {
                    String foo;
                    Long.parseLong(foo = "123");
                }
            }
        """,
        """
            class A {
                void foo() {
                    Long.parseLong("123");
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2509")
    @Test
    fun recordCompactConstructor() = rewriteRun(
        { spec -> spec.beforeRecipe { sf ->
            val javaRuntimeVersion = System.getProperty("java.runtime.version")
            val javaVendor = System.getProperty("java.vm.vendor")
            if (JavaVersion(UUID.randomUUID(), javaRuntimeVersion, javaVendor, javaRuntimeVersion, javaRuntimeVersion).majorVersion != 17) {
                spec.recipe(Recipe.noop())
            }
        }},
        java("""
            public record MyRecord(
               boolean bar,
               String foo
            ) {
               public MyRecord {
                  if (foo == null) {
                      foo = "defaultValue";
                  }
              }
            }
        """)
    )
}
