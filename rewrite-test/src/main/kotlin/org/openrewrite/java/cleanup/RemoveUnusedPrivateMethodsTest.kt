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
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface RemoveUnusedPrivateMethodsTest : JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .classpath("junit-jupiter-params")
            .build()

    override val recipe: Recipe
        get() = RemoveUnusedPrivateMethods()

    @Test
    fun removeUnusedPrivateMethods() = assertChanged(
        before = """
            class Test {
                private void unused() {
                }
            
                public void dontRemove() {
                    dontRemove2();
                }
                
                private void dontRemove2() {
                }
            }
        """,
        after = """
            class Test {
            
                public void dontRemove() {
                    dontRemove2();
                }
                
                private void dontRemove2() {
                }
            }
        """
    )

    @Test
    fun doNotRemoveCustomizedSerialization() = assertUnchanged(
        before = """
            class Test implements java.io.Serializable {
                private void writeObject(java.io.ObjectOutputStream out) {}
                private void readObject(java.io.ObjectInputStream in) {}
                private void readObjectNoData() {}
            }
        """
    )

    @Test
    fun doNotRemoveMethodsWithAnnotations() = assertUnchanged(
        before = """
            import org.junit.jupiter.params.provider.MethodSource;
            import java.util.stream.Stream;

            class Test {
                @MethodSource("sourceExample")
                void test(String input) {
                }
                private Stream<Arguments> sourceExample() {
                    return null;
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1536")
    @Test
    fun privateMethodWithBoundedGenericTypes() = assertUnchanged(
        before = """
            public class TestClass {
                void method() {
                    checkMethodInUse("String", "String");
                }

                private static void checkMethodInUse(String arg0, String arg1) {
                }

                private static <T> void checkMethodInUse(String arg0, T arg1) {
                }
            }
        """
    )
}
