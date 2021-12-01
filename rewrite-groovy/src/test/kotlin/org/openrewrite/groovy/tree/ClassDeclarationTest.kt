/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy.tree

import org.junit.jupiter.api.Test

class ClassDeclarationTest : GroovyTreeTest {

    @Test
    fun multipleClassDeclarationsInOneCompilationUnit() = assertParsePrintAndProcess(
        """
            public class A {
                int n
                
                def sum(int m) {
                    n+m
                }
            }
            class B {}
        """
    )

    @Test
    fun implements() = assertParsePrintAndProcess(
        """
            public interface B {}
            interface C {}
            class A implements B, C {}
        """
    )

    @Test
    fun extends() = assertParsePrintAndProcess(
        """
            public class Test {}
            class A extends Test {}
        """
    )

    @Test
    fun modifierOrdering() = assertParsePrintAndProcess(
        """
            public abstract class A {}
        """
    )

    @Test
    fun interfaceExtendsInterface() = assertParsePrintAndProcess(
        """
            interface A {}
            interface C {}
            interface B extends A , C {}
        """
    )

    @Test
    fun transitiveInterfaces() = assertParsePrintAndProcess(
        """
        interface A {}
        interface B extends A {}
        interface C extends B {}
        """
    )

    @Test
    fun annotation() = assertParsePrintAndProcess(
        """
            @interface A{}
        """
    )
}
