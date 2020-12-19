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
import org.openrewrite.RefactorVisitorTest

interface RemoveImportTest : RefactorVisitorTest {

    @Test
    fun removeNamedImport(jp: JavaParser) = assertRefactored(
            jp,
            visitors = listOf(
                RemoveImport().apply { setType("java.util.List") }
            ),
            before = """
                import java.util.List;
                class A {}
            """,
            after = "class A {}"
    )

    @Test
    fun leaveImportIfRemovedTypeIsStillReferredTo(jp: JavaParser) = assertUnchanged(
            jp,
            visitors = listOf(
                RemoveImport().apply { setType("java.util.List") }
            ),
            before = """
                import java.util.List;
                class A {
                   List list;
                }
            """
    )

    @Test
    fun removeStarImportIfNoTypesReferredTo(jp: JavaParser) = assertRefactored(
            jp,
            visitors = listOf(
                RemoveImport().apply { setType("java.util.List") }
            ),
            before = """
                import java.util.*;
                class A {}
            """,
            after = "class A {}"
    )

    @Test
    fun replaceStarImportWithNamedImportIfOnlyOneReferencedTypeRemains(jp: JavaParser) = assertRefactored(
            jp,
            visitors = listOf(
                RemoveImport().apply { setType("java.util.List") }
            ),
            before = """
                import java.util.*;
                class A {
                   Collection c;
                }
            """,
            after = """
                import java.util.Collection;
                class A {
                   Collection c;
                }
            """
    )

    @Test
    fun leaveStarImportInPlaceIfMoreThanTwoTypesStillReferredTo(jp: JavaParser) = assertUnchanged(
            jp,
            visitors = listOf(
                RemoveImport().apply { setType("java.util.List") }
            ),
            before = """
                import java.util.*;
                class A {
                   Collection c;
                   Set s;
                }
            """
    )

    @Test
    fun removeStarStaticImport(jp: JavaParser) = assertRefactored(
            jp,
            visitors = listOf(
                RemoveImport().apply { setType("java.util.Collections") }
            ),
            before = """
                import static java.util.Collections.*;
                class A {}
            """,
            after = "class A {}"
    )

    @Test
    fun leaveStarStaticImportIfReferenceStillExists(jp: JavaParser) = assertUnchanged(
            jp,
            visitors = listOf(
                RemoveImport().apply { setType("java.util.Collections") }
            ),
            before = """
                import static java.util.Collections.*;
                class A {
                   Object o = emptyList();
                }
            """
    )

    @Test
    fun leaveNamedStaticImportIfReferenceStillExists(jp: JavaParser) = assertRefactored(
            jp,
            visitors = listOf(
                RemoveImport().apply { setType("java.util.Collections") }
            ),
            before = """
                import static java.util.Collections.emptyList;
                import static java.util.Collections.emptySet;
                class A {
                   Object o = emptyList();
                }
            """,
            after = """
                import static java.util.Collections.emptyList;
                class A {
                   Object o = emptyList();
                }
            """
    )

    @Test
    fun leaveNamedStaticImportOnFieldIfReferenceStillExists(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(
                """
                    package foo;
                    public class B {
                        public static final String STRING = "string";
                        public static final String STRING2 = "string2";
                    }
                """,
                """
                    package foo;
                    public class C {
                        public static final String ANOTHER = "string";
                    }
                """
            ),
            visitors = listOf(
                RemoveImport().apply { setType("foo.B") },
                RemoveImport().apply { setType("foo.C") }
            ),
            before = """
                import static foo.B.STRING;
                import static foo.B.STRING2;
                import static foo.C.*;
                public class A {
                    String a = STRING;
                }
            """,
            after = """
                import static foo.B.STRING;
                public class A {
                    String a = STRING;
                }
            """
    )

    @Test
    fun removeImportForChangedMethodArgument(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(
                """
                    package b;
                    public interface B {
                        void doSomething();
                    }
                """,
                """
                    package c;
                    public interface C {
                        void doSomething();
                    }
                """
            ),
            visitors = listOf(
                ChangeType().apply { setType("b.B"); setTargetType("c.C") }
            ),
            before = """
                import b.B;
                
                class A {
                    void foo(B arg) {
                        arg.doSomething();
                    }
                }
            """,
            after = """
                import c.C;
                
                class A {
                    void foo(C arg) {
                        arg.doSomething();
                    }
                }
            """
    )
}
