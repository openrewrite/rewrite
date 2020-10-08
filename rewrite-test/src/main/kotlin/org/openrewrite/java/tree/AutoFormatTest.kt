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
    companion object {
        var dependencies = listOf(
                "package a;\npublic @interface A {}",
                "package a;\npublic @interface B { String value(); }",
                "package a;\npublic @interface C {}"
        )
    }

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
            dependencies = dependencies,
            visitorsMapped = listOf { a ->
                AutoFormat(a.classes[0])
            },
            before = """
                package a;
                
                @A@B("")   @C
                public class D {
                    
                }
            """,
            after = """
                package a;
                
                @A
                @B("")
                @C
                public class D {
                    
                }
            """
    )

    @Test
    fun putClassAnnotationAndModifierOnSeparateLines(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = dependencies,
            visitorsMapped = listOf { a ->
                AutoFormat(a.classes[0])
            },
            before = """
                package a;
                
                @A public class D {
                    
                }
            """,
            after = """
                package a;
                
                @A
                public class D {
                    
                }
            """
    )

    @Test
    fun putClassAnnotationAndKindOnSeparateLines(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = dependencies,
            visitorsMapped = listOf { a ->
                AutoFormat(a.classes[0])
            },
            before = """
                import java.lang.annotation.Documented;
                
                @A class D {
                    
                }
            """,
            after = """
                import java.lang.annotation.Documented;
                
                @A
                class D {
                    
                }
            """
    )

    @Test
    fun putEachMethodAnnotationOnNewLine(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = dependencies,
            visitorsMapped = listOf { a ->
                AutoFormat(a.classes[0].methods[0])
            },
            before = """
                package a;

                public class D {
                    @A@B("")     @C
                    public String toString() {
                    }
                }
            """,
            after = """
                package a;

                public class D {
                    @A
                    @B("")
                    @C
                    public String toString() {
                    }
                }
            """
    )

    @Test
    fun putMethodAnnotationAndVisibilityModifierSeparateLines(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = dependencies,
            visitorsMapped = listOf { a ->
                AutoFormat(a.classes[0].methods[0])
            },
            before = """
                package a;

                public class D {
                    @A@B("foo")     @C public String toString() {
                    }
                    
                    @B("bar")
                    @C static String stringify() {
                    }
                }
            """,
            after = """
                package a;

                public class D {
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
            dependencies = dependencies,
            visitorsMapped = listOf { a ->
                AutoFormat(a.classes[0].methods[1])
            },
            before = """
                package a;

                public class D {
                    @A@B("foo")     @C public String toString() {
                    }
                    
                    @B("bar")
                    @C static String stringify() {
                    }
                }
            """,
            after = """
                package a;

                public class D {
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
            dependencies = dependencies,
            visitorsMapped = listOf { a ->
                AutoFormat(a.classes[0].methods[0])
            },
            before = """
                package a;

                public class D {
                    @B("foo")
                    @A String stringify() {
                    }
                }
            """,
            after = """
                package a;

                public class D {
                    @B("foo")
                    @A
                    String stringify() {
                    }
                }
            """
    )

    @Test
    fun putMethodAnnotationAndNameOnSeparateLines(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = dependencies,
            visitorsMapped = listOf { a ->
                AutoFormat(a.classes[0].methods[0])
            },
            before = """
                package a;

                public class D {
                    @B(Tester.class)
                    @A D() {
                    }
                }
            """,
            after = """
                package a;

                public class D {
                    @B(Tester.class)
                    @A
                    D() {
                    }
                }
            """
    )
}
