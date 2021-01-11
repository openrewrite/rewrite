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
import org.openrewrite.RecipeTest

interface OrderDeclarationsTest : RecipeTest {
    @Test
    fun orderDeclarations(jp: JavaParser) = assertChanged(
            jp,
            visitors = listOf(OrderDeclarations()),
            before = """
                class A {
                    private final int unsettable = 1;
                    private String s;
                    private static final A internalA = new A();
                    public static final A a = new A();
                    private A() {}
                    public A(String s) {
                        this.s = s;
                    }
                    public void setS(String s) {
                        this.s = s;
                    }
                    @Override
                    public boolean equals(Object obj) {
                        return true;
                    }
                    @Override
                    public int hashCode() {
                        return 42;
                    }
                    @Override
                    public String toString() {
                        return "a";
                    }
                    private void internalDetail() {}
                    public String getS() {
                        return this.s;
                    }
                    public A withS(String s) {
                        return new A(s);
                    }
                }
            """,
            after = """ 
                class A {
                    public static final A a = new A();
                
                    private static final A internalA = new A();
                
                    private final int unsettable = 1;
                
                    private String s;
                
                    public A(String s) {
                        this.s = s;
                    }
                
                    private A() {}
                
                    private void internalDetail() {}
                
                    public String getS() {
                        return this.s;
                    }
                
                    public void setS(String s) {
                        this.s = s;
                    }
                
                    public A withS(String s) {
                        return new A(s);
                    }
                
                    @Override
                    public boolean equals(Object obj) {
                        return true;
                    }
                
                    @Override
                    public int hashCode() {
                        return 42;
                    }
                
                    @Override
                    public String toString() {
                        return "a";
                    }
                }
            """
    )
}
