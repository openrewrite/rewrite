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

interface AddAnnotationTest {
    companion object {
        private const val annot = """
            package b;
            public @interface MyAnnotation {
            }
        """
    }

    @Test
    fun addAnnotationToClass(jp: JavaParser) {
        """
            package a;
            
            public class UsersController {
                class Inner {
                }
            }
        """
                .whenParsedBy(jp)
                .whenVisitedByMapped { a -> AddAnnotation.Scoped(a.classes[0], "lombok.RequiredArgsConstructor") }
                .whenVisitedByMapped { a -> AddAnnotation.Scoped(a.classes[0].body.statements[0], "lombok.RequiredArgsConstructor") }
                .isRefactoredTo("""
                    package a;
                    
                    import lombok.RequiredArgsConstructor;
                    
                    @RequiredArgsConstructor
                    public class UsersController {
                        @RequiredArgsConstructor
                        class Inner {
                        }
                    }
                """)
    }

    @Test
    fun addAnnotationToField(jp: JavaParser) {
        """
            package a;
            
            public class UsersController {
                private final UserService userService;
                NameService nameService;
            }
        """
                .whenParsedBy(jp)
                .whichDependsOn(annot)
                .whenVisitedByMapped { a -> AddAnnotation.Scoped(a.classes[0].fields[0], "b.MyAnnotation") }
                .whenVisitedByMapped { a -> AddAnnotation.Scoped(a.classes[0].fields[1], "b.MyAnnotation") }
                .isRefactoredTo("""
                    package a;
                    
                    import b.MyAnnotation;
                    
                    public class UsersController {
                        @MyAnnotation
                        private final UserService userService;
                    
                        @MyAnnotation
                        NameService nameService;
                    }
                """)
    }

    @Test
    fun addAnnotationToMethod(jp: JavaParser) {
        """
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
                .whenParsedBy(jp)
                .whichDependsOn(annot)
                .whenVisitedByMany { a -> a.classes[0].methods.map { m -> AddAnnotation.Scoped(m, "b.MyAnnotation") } }
                .isRefactoredTo("""
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
                """)
    }

    @Test
    fun addAnnotationToMethodParameters(jp: JavaParser) {
        """
            package a;
            
            public class UsersController {
                public void getUsers(Integer maxUsers) {
                }
            }
        """
                .whenParsedBy(jp)
                .whichDependsOn(annot)
                .whenVisitedByMapped { a -> AddAnnotation.Scoped(a.classes[0].methods[0].params.params[0],
                        "b.MyAnnotation") }
                .isRefactoredTo("""
                    package a;
                    
                    import b.MyAnnotation;
                    
                    public class UsersController {
                        public void getUsers(@MyAnnotation Integer maxUsers) {
                        }
                    }
                """)
    }
}
