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
import org.openrewrite.RecipeTest
import org.openrewrite.java.tree.J

@ExtendWith(JavaParserResolver::class)
interface GenerateSetterTest : RecipeTest {

    @Test
    fun generatesBasicSetter(jp: JavaParser) = assertChanged(jp,
            visitorsMapped = listOf { a -> GenerateSetter.Scoped(a.classes[0], "foo") },
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
                
                    public void setFoo(String value) {
                        foo = value;
                    }
                }
            """
    )

    @Test
    fun generatesInnerClassSetter(jp: JavaParser) = assertChanged( jp,
            visitorsMapped = listOf { a -> GenerateSetter.Scoped(a.classes[0].body.statements[0] as J.ClassDecl, "foo") },
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
                
                        public void setFoo(String value) {
                            foo = value;
                        }
                    }
                }
            """
    )

    @Test
    fun setterToInnerClass(jp: JavaParser) = assertChanged(jp,
            visitorsMapped = listOf { a -> GenerateSetter.Scoped(a.classes[0], "foo") },
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
                
                    public void setFoo(Inner value) {
                        foo = value;
                    }
                }
            """
    )

    @Test
    fun doesNotDuplicateExistingSetter(jp: JavaParser) = assertUnchanged(jp,
            visitorsMapped = listOf { a -> GenerateSetter.Scoped(a.classes[0], "foo") },
            before = """
                package org.example;
                 
                class A {
                    String foo;
                
                    public void setFoo(String value) {
                        foo = value;
                    }
                }
            """
    )

    @Test
    fun doesNotInterefereWithOverload(jp: JavaParser) = assertChanged(jp,
            visitorsMapped = listOf { a -> GenerateSetter.Scoped(a.classes[0], "foo") },
            before = """ 
                package org.example;
                
                class A {
                    String foo;
                
                    public void setFoo(Integer foo) {
                        this.foo = foo.toString();
                    }
                }
            """,
            after = """
                package org.example;
                
                class A {
                    String foo;
                
                    public void setFoo(Integer foo) {
                        this.foo = foo.toString();
                    }
                
                    public void setFoo(String value) {
                        foo = value;
                    }
                }
            """
    )

    @Test
    fun worksForFieldNamedValue(jp: JavaParser) = assertChanged(jp,
            visitorsMapped = listOf { a -> GenerateSetter.Scoped(a.classes[0], "value") },
            before = """ 
                package org.example;
                 
                class A {
                    String value;
                }
            """,
            after = """
                package org.example;
                 
                class A {
                    String value;
                
                    public void setValue(String value) {
                        this.value = value;
                    }
                }
            """
    )

    @Test
    fun handlesGenerics(jp:JavaParser) = assertChanged(jp,
            visitorsMapped = listOf { a -> GenerateSetter.Scoped(a.classes[0], "foo") },
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
                
                    public void setFoo(List<T> value) {
                        foo = value;
                    }
                }
            """
    )

    @Test
    fun setterForPrimitive(jp: JavaParser) = assertChanged(
            jp,
            visitorsMapped = listOf { a -> GenerateSetter.Scoped(a.classes[0], "foo") },
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
                
                    public void setFoo(int value) {
                        foo = value;
                    }
                }
            """
    )
}
