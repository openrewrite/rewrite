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
package org.openrewrite.java.tree

import org.junit.jupiter.api.Test
import org.openrewrite.RefactorVisitorTest
import org.openrewrite.java.AutoFormat
import org.openrewrite.java.JavaParser

interface AutoFormatTest : RefactorVisitorTest {
    @Test
    fun methodDeclaration(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { a ->
                AutoFormat(a.classes[0].methods[0])
            },
            before = """
                import java.util.*;
                
                public class A {
                    List<String> l = new ArrayList<>();
                    
                @Deprecated
                public void method() {
                  if(true) {
                    l.add("value");
                  }
                }
                }
            """,
            after = """
                import java.util.*;
                
                public class A {
                    List<String> l = new ArrayList<>();
                    
                    @Deprecated
                    public void method() {
                        if(true) {
                            l.add("value");
                        }
                    }
                }
            """
    )

    @Test
    fun putEachClassAnnotationOnNewLine(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(
                "public interface Tester {}"
            ),
            visitorsMapped = listOf { a ->
                AutoFormat(a.classes[0])
            },
            before = """
                import lombok.Data;
                import org.junit.jupiter.api.Tag;
                import java.lang.annotation.Documented;
                
                @Documented@Tag(Tester.class)   @Data
                public class B {
                    
                }
            """,
            after = """
                import lombok.Data;
                import org.junit.jupiter.api.Tag;
                import java.lang.annotation.Documented;
                
                @Documented
                @Tag(Tester.class)
                @Data
                public class B {
                    
                }
            """
    )

    @Test
    fun putClassAnnotationAndModifierOnSeparateLines(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { a ->
                AutoFormat(a.classes[0])
            },
            before = """
                import java.lang.annotation.Documented;
                
                @Documented private class B {
                    
                }
            """,
            after = """
                import java.lang.annotation.Documented;
                
                @Documented
                private class B {
                    
                }
            """
    )

    @Test
    fun putClassAnnotationAndKindOnSeparateLines(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { a ->
                AutoFormat(a.classes[0])
            },
            before = """
                import java.lang.annotation.Documented;
                
                @Documented class B {
                    
                }
            """,
            after = """
                import java.lang.annotation.Documented;
                
                @Documented
                class B {
                    
                }
            """
    )

    @Test
    fun putEachMethodAnnotationOnNewLine(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(
                "public interface Tester {}"
            ),
            visitorsMapped = listOf { a ->
                AutoFormat(a.classes[0].methods[0])
            },
            before = """
                import org.junit.jupiter.api.Tag;
                import lombok.Data;

                public class K {
                    @Override@Tag(Tester.class)     @Data
                    public String toString() {
                    }
                }
            """,
            after = """
                import org.junit.jupiter.api.Tag;
                import lombok.Data;

                public class K {
                    @Override
                    @Tag(Tester.class)
                    @Data
                    public String toString() {
                    }
                }
            """
    )

    @Test
    fun putMethodAnnotationAndVisibilityModifierSeparateLines(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(
                    "package a;\npublic @interface A {}",
                    "package a;\npublic @interface B { String value(); }",
                    "package a;\npublic @interface C {}"
            ),
            visitorsMapped = listOf { a ->
                AutoFormat(a.classes[0].methods[0])
            },
            before = """
                package a;

                public class K {
                    @A@B("foo")     @C public String toString() {
                    }
                    
                    @B("bar")
                    @C static String stringify() {
                    }
                }
            """,
            after = """
                package a;

                public class K {
                    @A
                    @B("foo")
                    @C
                    public String toString() {
                    }
                    
                    @B("bar")
                    @C static String stringify() {
                    }
                }
            """
    )

    @Test
    fun putMethodAnnotationAndVisibilityModifierSeparateLines2(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(
                    "package a;\npublic @interface A {}",
                    "package a;\npublic @interface B { String value(); }",
                    "package a;\npublic @interface C {}"
            ),
            visitorsMapped = listOf { a ->
                AutoFormat(a.classes[0].methods[1])
            },
            before = """
                package a;

                public class K {
                    @A@B("foo")     @C public String toString() {
                    }
                    
                    @B("bar")
                    @C static String stringify() {
                    }
                }
            """,
            after = """
                package a;

                public class K {
                    @A@B("foo")     @C public String toString() {
                    }
                    
                    @B("bar")
                    @C
                    static String stringify() {
                    }
                }
            """
    )

    @Test
    fun putMethodAnnotationAndReturnTypeOnSeparateLines(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(
                    "public interface Tester {}"
            ),
            visitorsMapped = listOf { a ->
                AutoFormat(a.classes[0].methods[0])
            },
            before = """
                import org.junit.jupiter.api.Tag;
                import lombok.Data;

                public class K {
                    @Tag(Tester.class)
                    @Data String stringify() {
                    }
                }
            """,
            after = """
                import org.junit.jupiter.api.Tag;
                import lombok.Data;

                public class K {
                    @Tag(Tester.class)
                    @Data
                    String stringify() {
                    }
                }
            """
    )

    @Test
    fun putMethodAnnotationAndNameOnSeparateLines(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(
                    "public interface Tester {}"
            ),
            visitorsMapped = listOf { a ->
                AutoFormat(a.classes[0].methods[0])
            },
            before = """
                import org.junit.jupiter.api.Tag;
                import lombok.Data;

                public class K {
                    @Tag(Tester.class)
                    @Data K() {
                    }
                }
            """,
            after = """
                import org.junit.jupiter.api.Tag;
                import lombok.Data;

                public class K {
                    @Tag(Tester.class)
                    @Data
                    K() {
                    }
                }
            """
    )
}
