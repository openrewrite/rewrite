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
import org.openrewrite.whenParsedBy

interface RemoveImportTest {

    @Test
    fun removeNamedImport(jp: JavaParser) {
        """
            import java.util.List;
            class A {}
        """
                .whenParsedBy(jp)
                .whenVisitedBy(RemoveImport().apply { setType("java.util.List") })
                .isRefactoredTo("class A {}")
    }

    @Test
    fun leaveImportIfRemovedTypeIsStillReferredTo(jp: JavaParser) {
        """
            import java.util.List;
            class A {
               List list;
            }
        """
                .whenParsedBy(jp)
                .whenVisitedBy(RemoveImport().apply { setType("java.util.List") })
                .isUnchanged()
    }

    @Test
    fun removeStarImportIfNoTypesReferredTo(jp: JavaParser) {
        """
            import java.util.*;
            class A {}
        """
                .whenParsedBy(jp)
                .whenVisitedBy(RemoveImport().apply { setType("java.util.List") })
                .isRefactoredTo("class A {}")
    }

    @Test
    fun replaceStarImportWithNamedImportIfOnlyOneReferencedTypeRemains(jp: JavaParser) {
        """
            import java.util.*;
            class A {
               Collection c;
            }
        """
                .whenParsedBy(jp)
                .whenVisitedBy(RemoveImport().apply { setType("java.util.List") })
                .isRefactoredTo("""
                    import java.util.Collection;
                    class A {
                       Collection c;
                    }
                """)
    }

    @Test
    fun leaveStarImportInPlaceIfMoreThanTwoTypesStillReferredTo(jp: JavaParser) {
        """
            import java.util.*;
            class A {
               Collection c;
               Set s;
            }
        """
                .whenParsedBy(jp)
                .whenVisitedBy(RemoveImport().apply { setType("java.util.List") })
                .isUnchanged()
    }

    @Test
    fun removeStarStaticImport(jp: JavaParser) {
        """
            import static java.util.Collections.*;
            class A {}
        """
                .whenParsedBy(jp)
                .whenVisitedBy(RemoveImport().apply { setType("java.util.Collections") })
                .isRefactoredTo("class A {}")
    }

    @Test
    fun leaveStarStaticImportIfReferenceStillExists(jp: JavaParser) {
        """
            import static java.util.Collections.*;
            class A {
               Object o = emptyList();
            }
        """
                .whenParsedBy(jp)
                .whenVisitedBy(RemoveImport().apply { setType("java.util.Collections") })
                .isUnchanged()
    }

    @Test
    fun leaveNamedStaticImportIfReferenceStillExists(jp: JavaParser) {
        """
            import static java.util.Collections.emptyList;
            import static java.util.Collections.emptySet;
            class A {
               Object o = emptyList();
            }
        """
                .whenParsedBy(jp)
                .whenVisitedBy(RemoveImport().apply { setType("java.util.Collections") })
                .isRefactoredTo("""
                    import static java.util.Collections.emptyList;
                    class A {
                       Object o = emptyList();
                    }
                """)
    }

    @Test
    fun leaveNamedStaticImportOnFieldIfReferenceStillExists(jp: JavaParser) {
        """
            import static foo.B.STRING;
            import static foo.B.STRING2;
            import static foo.C.*;
            public class A {
                String a = STRING;
            }
        """
                .whenParsedBy(jp)
                .whichDependsOn("""
                    package foo;
                    public class B {
                        public static final String STRING = "string";
                        public static final String STRING2 = "string2";
                    }
                """)
                .whichDependsOn("""
                    package foo;
                    public class C {
                        public static final String ANOTHER = "string";
                    }
                """)
                .whenVisitedBy(RemoveImport().apply { setType("foo.B") })
                .whenVisitedBy(RemoveImport().apply { setType("foo.C") })
                .isRefactoredTo("""
                    import static foo.B.STRING;
                    public class A {
                        String a = STRING;
                    }
                """)
    }

    @Test
    fun removeImportForChangedMethodArgument(jp: JavaParser) {
        """
            import b.B;
            
            class A {
                void foo(B arg) {
                    arg.doSomething();
                }
            }
        """
                .whenParsedBy(jp)
                .whichDependsOn("""
                    package b;
                    public interface B {
                        void doSomething();
                    }
                """)
                .whichDependsOn("""
                    package c;
                    public interface C {
                        void doSomething();
                    }
                """)
                .whenVisitedBy(ChangeType().apply { setType("b.B"); setTargetType("c.C") })
                .isRefactoredTo("""
                    import c.C;
                    
                    class A {
                        void foo(C arg) {
                            arg.doSomething();
                        }
                    }
                """)
    }
}
