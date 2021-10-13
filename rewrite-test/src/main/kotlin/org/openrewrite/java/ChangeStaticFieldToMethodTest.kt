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
package org.openrewrite.java

import org.junit.jupiter.api.Test

interface ChangeStaticFieldToMethodTest : JavaRecipeTest {
    override val recipe: ChangeStaticFieldToMethod
        get() = ChangeStaticFieldToMethod("java.util.Collections", "EMPTY_LIST",
                "com.acme.Lists", "of")

    @Test
    fun migratesQualifiedField(jp: JavaParser) = assertChanged(
            jp,
            before = """
            import java.util.Collections;
            import java.util.List;
            
            class A {
                public List<String> empty() {
                    return Collections.EMPTY_LIST;
                }
            }
            """,
            after = """
            import com.acme.Lists;

            import java.util.List;
            
            class A {
                public List<String> empty() {
                    return Lists.of();
                }
            }
            """
    )

    @Test
    fun migratesStaticImportedField(jp: JavaParser) = assertChanged(
            jp,
            before = """
            import static java.util.Collections.EMPTY_LIST;
            
            class A {
                public Object empty() {
                    return EMPTY_LIST;
                }
            }
            """,
            after = """
            import com.acme.Lists;
            
            class A {
                public Object empty() {
                    return Lists.of();
                }
            }
            """
    )

    @Test
    fun migratesFullyQualifiedField(jp: JavaParser) = assertChanged(
            jp,
            before = """
            class A {
                public Object empty() {
                    return java.util.Collections.EMPTY_LIST;
                }
            }
            """,
            after = """
            import com.acme.Lists;
            
            class A {
                public Object empty() {
                    return Lists.of();
                }
            }
            """
    )

    @Test
    fun migratesFieldInitializer() = assertChanged(
            before = """
            import java.util.Collections;

            class A {
                private final Object collection = Collections.EMPTY_LIST;
            }
            """,
            after = """
            import com.acme.Lists;

            class A {
                private final Object collection = Lists.of();
            }
            """
    )

    @Test
    fun ignoresUnrelatedFields(jp: JavaParser) = assertUnchanged(
            jp,
            before = """
            import java.util.Collections;
            
            class A {
                static Object EMPTY_LIST = null;
                public Object empty1() {
                    return A.EMPTY_LIST;
                }
                public Object empty2() {
                    return EMPTY_LIST;
                }
            }
            """
    )
}
