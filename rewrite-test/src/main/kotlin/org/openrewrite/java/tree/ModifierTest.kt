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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser

interface ModifierTest {
    @Test
    fun addStaticModifierToClass(jp: JavaParser) {
        assertEquals("private static final ", jp.modifiersOnClass("private final ")
                .withModifiers("static").printMods())
        assertEquals("private static ", jp.modifiersOnClass("private ")
                .withModifiers("static").printMods())
        assertEquals("\tstatic ", jp.modifiersOnClass("\t")
                .withModifiers("static").printMods())
    }

    @Test
    fun addFinalModifierToClass(jp: JavaParser) {
        assertEquals("private static final ", jp.modifiersOnClass("private static ")
                .withModifiers("final").printMods())
        assertEquals("private final ", jp.modifiersOnClass("private ")
                .withModifiers("final").printMods())
        assertEquals("\tfinal ", jp.modifiersOnClass("\t")
                .withModifiers("final").printMods())
    }

    @Test
    fun addVisibilityModifierToClass(jp: JavaParser) {
        assertEquals("public ", jp.modifiersOnClass("protected ")
                .withModifiers("public").printMods())
        assertEquals("\tpublic final ", jp.modifiersOnClass("\tfinal ")
                .withModifiers("public").printMods())
        assertEquals("\tpublic ", jp.modifiersOnClass("\t")
                .withModifiers("public").printMods())
    }

    @Test
    fun addStaticModifierToField(jp: JavaParser) {
        assertEquals("private static final ", jp.modifiersOnField("private final ")
                .withModifiers("static").printMods())
        assertEquals("private static ", jp.modifiersOnField("private ")
                .withModifiers("static").printMods())
        assertEquals("\tstatic ", jp.modifiersOnField("\t")
                .withModifiers("static").printMods())
    }

    @Test
    fun addFinalModifierToField(jp: JavaParser) {
        assertEquals("private static final ", jp.modifiersOnField("private static ")
                .withModifiers("final").printMods())
        assertEquals("private final ", jp.modifiersOnField("private ")
                .withModifiers("final").printMods())
        assertEquals("\tfinal ", jp.modifiersOnField("\t")
                .withModifiers("final").printMods())
    }

    @Test
    fun addVisibilityModifierToField(jp: JavaParser) {
        assertEquals("public ", jp.modifiersOnField("protected ")
                .withModifiers("public").printMods())
        assertEquals("\tpublic final ", jp.modifiersOnField("\tfinal ")
                .withModifiers("public").printMods())
        assertEquals("\tpublic ", jp.modifiersOnField("\t")
                .withModifiers("public").printMods())
    }

    @Test
    fun addMultipleModifiersToField(jp: JavaParser) {
        assertEquals("\tprivate final ", jp.modifiersOnField("\t")
                .withModifiers("private", "final").printMods())
        assertEquals("private final ", jp.modifiersOnField("private ")
                .withModifiers("private", "final").printMods())
    }

    @Test
    fun addStaticModifierToMethod(jp: JavaParser) {
        assertEquals("private static final ", jp.modifiersOnMethod("private final ")
                .withModifiers("static").printMods())
        assertEquals("private static ", jp.modifiersOnMethod("private ")
                .withModifiers("static").printMods())
        assertEquals("\tstatic ", jp.modifiersOnMethod("\t")
                .withModifiers("static").printMods())
    }

    @Test
    fun addFinalModifierToMethod(jp: JavaParser) {
        assertEquals("private static final ", jp.modifiersOnMethod("private static ")
                .withModifiers("final").printMods())
        assertEquals("private final ", jp.modifiersOnMethod("private ")
                .withModifiers("final").printMods())
        assertEquals("\tfinal ", jp.modifiersOnMethod("\t")
                .withModifiers("final").printMods())
    }

    @Test
    fun addVisibilityModifierToMethod(jp: JavaParser) {
        assertEquals("public ", jp.modifiersOnMethod("protected ")
                .withModifiers("public").printMods())
        assertEquals("\tpublic final ", jp.modifiersOnMethod("\tfinal ")
                .withModifiers("public").printMods())
        assertEquals("\tpublic ", jp.modifiersOnMethod("\t")
                .withModifiers("public").printMods())
    }

    private fun JavaParser.modifiersOnClass(modifiers: String): J.ClassDecl =
            reset().parse("public class A {${modifiers}class B {}}")[0].classes[0].body.statements[0] as J.ClassDecl

    private fun JavaParser.modifiersOnField(modifiers: String): J.VariableDecls =
            reset().parse("public class A {${modifiers}int n;")[0].classes[0].body.statements[0] as J.VariableDecls

    private fun JavaParser.modifiersOnMethod(modifiers: String): J.MethodDecl =
            reset().parse("public class A {${modifiers}void foo() {}")[0].classes[0].body.statements[0] as J.MethodDecl

    private fun J.ClassDecl.printMods() = print().substringBefore("class")
    private fun J.VariableDecls.printMods() = print().substringBefore("int")
    private fun J.MethodDecl.printMods() = print().substringBefore("void")
}
