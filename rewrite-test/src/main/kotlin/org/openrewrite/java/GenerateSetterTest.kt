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

@ExtendWith(JavaParserResolver::class)
interface GenerateSetterTest : RefactorVisitorTest {

    @Test
    fun generatesBasicSetter(jp: JavaParser) = assertRefactored(jp,
            visitors = listOf(GenerateSetter().apply {
                setField("foo")
                setType("org.example.A")
            }),
            before = """ 
                package org.example;
                 
                class A {
                    String foo;
                    
                    String bar;
                }
            """.trimIndent(),
            after = """
                package org.example;
                 
                class A {
                    String foo;
                
                    public void setFoo(String value) {
                        foo = value;
                    }
                    
                    String bar;
                }
            """.trimIndent()
    )

    @Test
    fun generatesInnerClassSetter(jp: JavaParser) = assertRefactored( jp,
            visitors = listOf(GenerateSetter().apply {
                setField("foo")
                setType("org.example.A.B")
            }),
            before = """ 
                package org.example;
                 
                class A {
                    class B {
                        String foo;
                    }
                }
            """.trimIndent(),
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
            """.trimIndent()
    )

    @Test
    fun doesNotDuplicateExistingSetter(jp: JavaParser) = assertUnchanged(jp,
            visitors = listOf(GenerateSetter().apply {
                setField("foo")
                setType("org.example.A")
            }),
            before = """
                package org.example;
                 
                class A {
                    String foo;
                
                    public void setFoo(String value) {
                        foo = value;
                    }
                }
            """.trimIndent()
    )

    @Test
    fun worksForFieldNamedValue(jp: JavaParser) = assertRefactored(jp,
            visitors = listOf(GenerateSetter().apply {
                setField("value")
                setType("org.example.A")
            }),
            before = """ 
                package org.example;
                 
                class A {
                    String value;
                }
            """.trimIndent(),
            after = """
                package org.example;
                 
                class A {
                    String value;
                
                    public void setValue(String value) {
                        this.value = value;
                    }
                }
            """.trimIndent()
    )
}
