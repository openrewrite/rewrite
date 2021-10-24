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
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface FixSerializableFieldsTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = FixSerializableFields(false, null)

    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion().build()

    val models @Language("java") get() =
        """
            import java.io.Serializable;
            
            public class A {
                int value1;
            }
            public class B {
                A aValue;
            }
            public class C implements Serializable {
                int intValue;
                String stringValue;
            }
    """.trimIndent()

    @Test
    fun markTransient(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(models),
        before = """
            import java.io.Serializable;
            import java.io.DataInputStream;
            
            class Example implements Serializable {
                private DataInputStream nonSerializable;
                C cValue;
                public void test() {
                }
            }
        """,
        after = """
            import java.io.Serializable;
            import java.io.DataInputStream;

            class Example implements Serializable {
                private transient DataInputStream nonSerializable;
                C cValue;
                public void test() {
                }
            }
        """
    )

    @Test
    fun markAsTransientArray(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(models),
        before = """
            import java.io.Serializable;
            import java.io.DataInputStream;

            class Example implements Serializable {
                private DataInputStream[] nonSerializable;
                C cValue;
                public void test() {
                }
            }
        """,
        after = """
            import java.io.Serializable;
            import java.io.DataInputStream;

            class Example implements Serializable {
                private transient DataInputStream[] nonSerializable;
                C cValue;
                public void test() {
                }
            }
        """
    )

    @Test
    fun markAsTransientList(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(models),
        before = """
            import java.io.DataInputStream;
            import java.io.Serializable;
            import java.util.List;

            class Example implements Serializable {
                private List<DataInputStream> aValue;
                private List<C> cValue;
                public void test() {
                }
            }
        """,
        after = """
            import java.io.DataInputStream;
            import java.io.Serializable;
            import java.util.List;

            class Example implements Serializable {
                private transient List<DataInputStream> aValue;
                private List<C> cValue;
                public void test() {
                }
            }
        """
    )

    @Test
    fun markAsTransientMap(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(models),
        before = """
            import java.io.DataInputStream;
            import java.io.Serializable;
            import java.util.Map;

            class Example implements Serializable {
                private Map<String, DataInputStream> aMap;
                private Map<String, C> cMap;
                public void test() {
                }
            }
        """,
        after = """
            import java.io.DataInputStream;
            import java.io.Serializable;
            import java.util.Map;

            class Example implements Serializable {
                private transient Map<String, DataInputStream> aMap;
                private Map<String, C> cMap;
                public void test() {
                }
            }
        """
    )

    @Test
    fun dontMarkStaticFields(jp: JavaParser) = assertUnchanged(
        jp,
        dependsOn = arrayOf(models),
        before = """
            import java.io.Serializable;

            class Example implements Serializable {
                private static A aValue;
                C cValue;
                public void test() {
                }
            }
        """
    )

    @Test
    fun dontModifyClassThatIsNotSerializable(jp: JavaParser) = assertUnchanged(
        jp,
        dependsOn = arrayOf(models),
        before = """
            class Example {
                private A aValue;
                C cValue;
                public void test() {
                }
            }
        """
    )

    @Test
    fun makeSerializable(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(models),
        before = """
            import java.io.DataInputStream;
            import java.io.Serializable;
            
            class Example implements Serializable {
                private DataInputStream nonSerializable;
                C cValue;
                public void test() {
                }
            }
        """,
        after = """
            import java.io.DataInputStream;
            import java.io.Serializable;

            class Example implements Serializable {
                private transient DataInputStream nonSerializable;
                C cValue;
                public void test() {
                }
            }
        """
    )

    @Test
    fun makeSerializableArray(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                import java.io.Serializable;
                import java.io.DataInputStream;
                
                class Example implements Serializable {
                    private A[] nonSerializable;
                    C cValue;
                    public void test() {
                    }
                }
            """
        ),
        before= models,
        after = """
            import java.io.Serializable;

            public class A implements Serializable {
                int value1;
            }
            public class B {
                A aValue;
            }
            public class C implements Serializable {
                int intValue;
                String stringValue;
            }
        """
    )

    @Test
    fun makeSerializableList(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                import java.io.Serializable;
                import java.io.DataInputStream;
                import java.util.List;
                
                class Example implements Serializable {
                    private List<A> nonSerializable;
                    C cValue;
                    public void test() {
                    }
                }
            """),
        before= models,
        after = """
            import java.io.Serializable;

            public class A implements Serializable {
                int value1;
            }
            public class B {
                A aValue;
            }
            public class C implements Serializable {
                int intValue;
                String stringValue;
            }
        """
    )

    @Test
    fun makeSerializableMap(jp: JavaParser) = assertChanged(
        jp,
        dependsOn = arrayOf(
            """
                import java.io.Serializable;
                import java.io.DataInputStream;
                import java.util.Map;
                
                class Example implements Serializable {
                    private Map<String,A> nonSerializable;
                    C cValue;
                    public void test() {
                    }
                }
            """
        ),
        before= models,
        after = """
            import java.io.Serializable;

            public class A implements Serializable {
                int value1;
            }
            public class B {
                A aValue;
            }
            public class C implements Serializable {
                int intValue;
                String stringValue;
            }
        """
    )

    @Test
    fun makeExclusionTransient(jp: JavaParser) = assertChanged(
        jp,
        recipe = FixSerializableFields(false, listOf("A")),
        dependsOn = arrayOf(models),
        before = """
            import java.io.Serializable;
            
            class Example implements Serializable {
                private A nonSerializable;
                C cValue;
                public void test() {
                }
            }
        """,
        after = """
            import java.io.Serializable;
            
            class Example implements Serializable {
                private transient A nonSerializable;
                C cValue;
                public void test() {
                }
            }
        """
    )

    @Test
    fun doNotChangeSerializableGenerics(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.io.Serializable;
            import java.util.Map;
            
            class A<TTTT extends Serializable> implements Serializable {
                private Map<String, TTTT> items;
                private TTTT item;
            }
        """
    )
}
