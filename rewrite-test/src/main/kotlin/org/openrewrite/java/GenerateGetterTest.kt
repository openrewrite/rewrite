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
import org.junit.jupiter.api.extension.ExtendWith
import org.openrewrite.RefactorVisitorTest
import org.openrewrite.java.tree.JavaType

@ExtendWith(JavaParserResolver::class)
interface GenerateGetterTest : RefactorVisitorTest {

    @Test
    fun generatesBasicGetter(jp: JavaParser) = assertRefactored(jp,
            visitors = listOf(GenerateGetter.Scoped(JavaType.Class.build("org.example.A"), "foo")),
            before = """ 
                package org.example;
                 
                class A {
                    String foo;
                    
                    String bar;
                }
            """,
            after = """
                package org.example;
                 
                class A {
                    String foo;
                    
                    String bar;
                
                    public String getFoo() {
                        return foo;
                    }
                }
            """
    )

    @Test
    fun generatesInnerClassGetter(jp: JavaParser) = assertRefactored(jp,
            visitors = listOf(GenerateGetter.Scoped(JavaType.Class.build("org.example.A.B"), "foo")),
            before = """ 
                package org.example;
                 
                class A {
                    class B {
                        String foo;
                    }
                }
            """,
            after = """
                package org.example;
                 
                class A {
                    class B {
                        String foo;
                
                        public String getFoo() {
                            return foo;
                        }
                    }
                }
            """
    )

    @Test
    fun getterToInnerClass(jp: JavaParser) = assertRefactored(jp,
            visitors = listOf(GenerateGetter.Scoped(JavaType.Class.build("org.example.A"), "foo")),
            before = """
                package org.example;
                
                import java.util.List;
                 
                class A {
                    Inner foo;
                    
                    public static class Inner { }
                }
            """,
            after = """
                package org.example;
                
                import java.util.List;
                 
                class A {
                    Inner foo;
                    
                    public static class Inner { }
                
                    public Inner getFoo() {
                        return foo;
                    }
                }
            """
    )

    @Test
    fun doesNotDuplicateExistingGetter(jp: JavaParser) = assertUnchanged(jp,
            visitors = listOf(GenerateGetter.Scoped(JavaType.Class.build("org.example.A"), "foo")),
            before = """
                package org.example;
                 
                class A {
                    String foo;
                
                    public String getFoo() {
                        return foo;
                    }
                }
            """
    )

    @Test
    fun handlesGenerics(jp: JavaParser) = assertRefactored(jp,
            visitors = listOf(GenerateGetter.Scoped(JavaType.Class.build("org.example.A"), "foo")),
            before = """ 
                package org.example;
                
                import java.util.List;
                 
                class A<T> {
                    List<T> foo;
                }
            """,
            after = """
                package org.example;
                
                import java.util.List;
                 
                class A<T> {
                    List<T> foo;
                
                    public List<T> getFoo() {
                        return foo;
                    }
                }
            """
    )

    @Test
    fun getterForPrimitive(jp: JavaParser) = assertRefactored(
            jp,
            visitors = listOf(GenerateGetter.Scoped(JavaType.Class.build("org.example.A"), "foo")),
            before = """ 
                package org.example;
                 
                class A {
                    int foo;
                }
            """,
            after = """
                package org.example;
                 
                class A {
                    int foo;
                
                    public int getFoo() {
                        return foo;
                    }
                }
            """
    )
}
