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

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

@Suppress("MethodMayBeStatic", "FunctionName")
interface MissingOverrideAnnotationTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = MissingOverrideAnnotation(null, null)

    companion object {
        @Language("java")
        private const val supportingParents: String = """
            interface TestInterface {
                void testInterface();
            }

            interface TestInterface0 {
                void testInterface0();
            }

            interface TestInterfaceExtension extends TestInterface0 {
                void testInterfaceExtension();
            }

            class TestParentParent {
                public void testParentParent() {
                }
            }

            class TestParent extends TestParentParent {
                public void testParent() {
                }
            }

            abstract class AbstractTestParent {
                abstract boolean isAbstractBoolean();

                boolean isBoolean() {
                    return true;
                }
            }
        """
    }

    @Test
    fun `when a method overrides from a parent`() = assertChanged(
        dependsOn = arrayOf(supportingParents),
        before = """
            class Test extends TestParentParent {

                public void testParentParent() {
                }

                public void localMethod() {
                }
            }
        """,
        after = """
            class Test extends TestParentParent {

                @Override
                public void testParentParent() {
                }

                public void localMethod() {
                }
            }
        """
    )

    @Test
    fun `when a method overrides multiple layers of parents`() = assertChanged(
        dependsOn = arrayOf(supportingParents),
        before = """
            class Test extends TestParent {

                public void testParent() {
                }

                public void localMethod() {
                }
            }
        """,
        after = """
            class Test extends TestParent {

                @Override
                public void testParent() {
                }

                public void localMethod() {
                }
            }
        """
    )

    @Test
    fun `when a method implements an interface`() = assertChanged(
        dependsOn = arrayOf(supportingParents),
        before = """
            class Test implements TestInterface {

                public void testInterface() {
                }

                public void localMethod() {
                }
            }
        """,
        after = """
            class Test implements TestInterface {

                @Override
                public void testInterface() {
                }

                public void localMethod() {
                }
            }
        """
    )

    @Test
    fun `when methods are implemented from multiple interfaces`() = assertChanged(
        dependsOn = arrayOf(supportingParents),
        before = """
            class Test implements TestInterface, TestInterface0 {

                public void testInterface() {
                }

                public void localMethod() {
                }

                public void testInterface0() {
                }
            }
        """,
        after = """
            class Test implements TestInterface, TestInterface0 {

                @Override
                public void testInterface() {
                }

                public void localMethod() {
                }

                @Override
                public void testInterface0() {
                }
            }
        """
    )

    @Test
    fun `when methods are implemented from multiple layers of interfaces`() = assertChanged(
        dependsOn = arrayOf(supportingParents),
        before = """
            class Test implements TestInterfaceExtension {

                public void testInterfaceExtension() {
                }

                public void localMethod() {
                }

                public void testInterface0() {
                }
            }
        """,
        after = """
            class Test implements TestInterfaceExtension {

                @Override
                public void testInterfaceExtension() {
                }

                public void localMethod() {
                }

                @Override
                public void testInterface0() {
                }
            }
        """
    )

    @Test
    fun `when a method overrides from a parent and a method implements an interface`() = assertChanged(
        dependsOn = arrayOf(supportingParents),
        before = """
            class Test extends TestParent implements TestInterface {

                public void testParent() {
                }

                public void localMethod() {
                }

                public void testInterface() {
                }
            }
        """,
        after = """
            class Test extends TestParent implements TestInterface {

                @Override
                public void testParent() {
                }

                public void localMethod() {
                }

                @Override
                public void testInterface() {
                }
            }
        """
    )

    @Test
    fun `when the method is static`() = assertUnchanged(
        dependsOn = arrayOf(
            """
            import java.util.Collection;
            import java.util.Collections;

            class TestBase {
                protected static Collection<Object[]> parameters() {
                    return Collections.emptyList();
                }
            }
        """.trimIndent()
        ),
        before = """
            import java.util.Collection;
            import java.util.Collections;

            class Test extends TestBase {
                protected static Collection<Object[]> parameters() {
                    return Collections.emptyList();
                }
            }
        """
    )

    @Test
    fun `when the parent is abstract and the method to override is abstract`() = assertUnchanged(
        dependsOn = arrayOf(supportingParents),
        before = """
            class Test extends AbstractTestParent {

                public boolean isAbstractBoolean() {
                    return false;
                }
            }
        """
    )

    @Test
    fun `when the parent is abstract and the method to override is not abstract`() = assertChanged(
        dependsOn = arrayOf(supportingParents),
        before = """
            class Test extends AbstractTestParent {

                public boolean isAbstractBoolean() {
                    return false;
                }

                public boolean isBoolean() {
                    return true;
                }
            }
        """,
        after = """
            class Test extends AbstractTestParent {

                public boolean isAbstractBoolean() {
                    return false;
                }

                @Override
                public boolean isBoolean() {
                    return true;
                }
            }
        """
    )

    @Test
    fun `when a method already has an @Override annotation`() = assertUnchanged(
        dependsOn = arrayOf(supportingParents),
        before = """
            class Test extends TestParent {

                @Override
                public void testParent() {
                }
            }
        """
    )

    @Test
    @Disabled("Override annotation is not being attributed in Java 8, causing infinite cycles when adding annotations to anonymous classes. Only appears in Java 8.")
    fun `when ignoreAnonymousClassMethods is true and a method overrides within an anonymous class`() = assertUnchanged(
        recipe = MissingOverrideAnnotation(null, true),
        dependsOn = arrayOf(supportingParents),
        before = """
            class Test {
                public void method() {
                    TestParent t = new TestParent() {
                        public void testParent() {
                        }
                    };
                }
            }
        """
    )

    @Test
    @Disabled("Override annotation is not being attributed in Java 8, causing infinite cycles when adding annotations to anonymous classes. Only appears in Java 8.")
    fun `when ignoreAnonymousClassMethods is false and a method overrides within an anonymous class`() = assertChanged(
        recipe = MissingOverrideAnnotation(null, false),
        dependsOn = arrayOf(supportingParents),
        before = """
            class Test {
                public void method() {
                    TestParent t = new TestParent() {
                        public void testParent() {
                        }
                    };
                }
            }
        """,
        after = """
            class Test {
                public void method() {
                    TestParent t = new TestParent() {
                        @Override
                        public void testParent() {
                        }
                    };
                }
            }
        """
    )

    @Test
    fun `when ignoreObjectMethods is true and a method overrides from the base Object class`() = assertUnchanged(
        recipe = MissingOverrideAnnotation(true, null),
        before = """
            class Test {
                public String toString() {
                    return super.toString();
                }
            }
        """
    )

    @Test
    fun `when ignoreObjectMethods is false and a method overrides from the base Object class`() = assertChanged(
        recipe = MissingOverrideAnnotation(false, null),
        before = """
            class Test {

                public String toString() {
                    return super.toString();
                }
            }
        """,
        after = """
            class Test {

                @Override
                public String toString() {
                    return super.toString();
                }
            }
        """
    )

}
