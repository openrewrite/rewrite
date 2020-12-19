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

interface GenerateNewBeanUsingPropertiesTest: RefactorVisitorTest {
    @Test
    fun generateNewBeanWithProperties(jp: JavaParser) = assertRefactored(
            jp,
            dependencies = listOf("""
                package bean;
                public class Address {
                    private String street;
                    private String zip;
                    
                    public void setStreet(String street) {
                        this.street = street;
                    }
                    
                    public void setZip(String zip) {
                        this.zip = zip;
                    }
                }
            """),
            visitorsMapped = listOf { a ->
                val method = a.classes[0].methods[0]
                val body = method.body!!
                GenerateNewBeanUsingProperties.Scoped(
                        body,
                        body.statements[0],
                        "bean.Address",
                        null,
                        "street", (method.params.params[0] as J.VariableDecls).vars[0].name,
                        "zip", (method.params.params[1] as J.VariableDecls).vars[0].name
                ).apply {
                }
            },
            before = """
                public class A {
                    public void printAddress(String street, String zip) {
                        System.out.println(address.toString());
                    }
                }
            """,
            after = """
                public class A {
                    public void printAddress(String street, String zip) {
                        Address address = new Address();
                        address.setStreet(street);
                        address.setZip(zip);
                        System.out.println(address.toString());
                    }
                }
            """
    )
}
