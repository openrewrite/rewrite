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
import org.openrewrite.java.tree.J

interface RemoveAnnotationTest : RefactorVisitorTest {
    companion object {
        private const val annot = """
            package b;
            public @interface MyAnnotation {
            }
        """
    }

    @Test
    fun removeAnnotationFromClass(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf(
                    { a -> RemoveAnnotation.Scoped(a.classes[0], "lombok.RequiredArgsConstructor") },
                    { a -> RemoveAnnotation.Scoped(a.classes[0].body.statements[0], "lombok.RequiredArgsConstructor") }
            ),
            before = """
                package a;
                
                import lombok.RequiredArgsConstructor;
                
                @RequiredArgsConstructor
                public class UsersController {
                    @RequiredArgsConstructor
                    class Inner {
                    }
                }
            """,
            after = """
                package a;
                
                public class UsersController {
                    class Inner {
                    }
                }
            """
    )

    @Test
    fun removeAnnotationFromField(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf(
                    { a -> RemoveAnnotation.Scoped((a as J.CompilationUnit).classes[0].fields[0], "b.MyAnnotation") },
                    { a -> RemoveAnnotation.Scoped((a as J.CompilationUnit).classes[0].fields[1], "b.MyAnnotation") }
            ),
            before = """
                package a;
                
                import b.MyAnnotation;
                
                public class UsersController {
                    @MyAnnotation
                    private final UserService userService;
                
                    @MyAnnotation
                    NameService nameService;
                }
            """,
            after = """
                package a;
                
                public class UsersController {
                    private final UserService userService;
                    NameService nameService;
                }
            """
    )

    @Test
    fun removeAnnotationFromMethod(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(annot),
            visitorsMappedToMany = listOf { a: J.CompilationUnit ->
                a.classes[0].methods.map { m -> RemoveAnnotation.Scoped(m, "b.MyAnnotation") }
            },
            before = """
                package a;
                
                import b.MyAnnotation;
                
                public class UsersController {
                    @MyAnnotation
                    UsersController() {
                    }
                
                    @MyAnnotation
                    public void onInit() {
                    }
                
                    @MyAnnotation
                    void onInit2() {
                    }
                    
                    @MyAnnotation
                    <T> T onInit3() {
                        return null;
                    }
                }
            """,
            after = """
package a;

public class UsersController {
    
    UsersController() {
    }

    
    public void onInit() {
    }

    
    void onInit2() {
    }
    
    
    <T> T onInit3() {
        return null;
    }
}
            """
    )

    @Test
    fun removeAnnotationFromMethodParameters(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf(annot),
            visitorsMapped = listOf { a ->
                RemoveAnnotation.Scoped(a.classes[0].methods[0].params.params[0],
                        "b.MyAnnotation")
            },
            before = """
                package a;
                
                import b.MyAnnotation;
                
                public class UsersController {
                    public void getUsers(@MyAnnotation Integer maxUsers) {
                    }
                }
            """,
            after = """
                package a;
                
                public class UsersController {
                    public void getUsers( Integer maxUsers) {
                    }
                }
            """
    )
}
