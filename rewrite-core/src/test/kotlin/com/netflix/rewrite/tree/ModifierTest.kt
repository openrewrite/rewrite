/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.tree

import com.netflix.rewrite.Parser
import org.junit.Assert.assertEquals
import org.junit.Test

class ModifierTest {
    @Test
    fun addStaticModifierToClass() {
        assertEquals("private static final ", modifiersOnClass("private final ")
                .withModifiers("static").printMods())
        assertEquals("private static ", modifiersOnClass("private ")
                .withModifiers("static").printMods())
        assertEquals("\tstatic ", modifiersOnClass("\t")
                .withModifiers("static").printMods())
    }

    @Test
    fun addFinalModifierToClass() {
        assertEquals("private static final ", modifiersOnClass("private static ")
                .withModifiers("final").printMods())
        assertEquals("private final ", modifiersOnClass("private ")
                .withModifiers("final").printMods())
        assertEquals("\tfinal ", modifiersOnClass("\t")
                .withModifiers("final").printMods())
    }

    @Test
    fun addVisibilityModifierToClass() {
        assertEquals("public ", modifiersOnClass("protected ")
                .withModifiers("public").printMods())
        assertEquals("\tpublic final ", modifiersOnClass("\tfinal ")
                .withModifiers("public").printMods())
        assertEquals("\tpublic ", modifiersOnClass("\t")
                .withModifiers("public").printMods())
    }

    @Test
    fun addStaticModifierToField() {
        assertEquals("private static final ", modifiersOnField("private final ")
                .withModifiers("static").printMods())
        assertEquals("private static ", modifiersOnField("private ")
                .withModifiers("static").printMods())
        assertEquals("\tstatic ", modifiersOnField("\t")
                .withModifiers("static").printMods())
    }

    @Test
    fun addFinalModifierToField() {
        assertEquals("private static final ", modifiersOnField("private static ")
                .withModifiers("final").printMods())
        assertEquals("private final ", modifiersOnField("private ")
                .withModifiers("final").printMods())
        assertEquals("\tfinal ", modifiersOnField("\t")
                .withModifiers("final").printMods())
    }

    @Test
    fun addVisibilityModifierToField() {
        assertEquals("public ", modifiersOnField("protected ")
                .withModifiers("public").printMods())
        assertEquals("\tpublic final ", modifiersOnField("\tfinal ")
                .withModifiers("public").printMods())
        assertEquals("\tpublic ", modifiersOnField("\t")
                .withModifiers("public").printMods())
    }

    @Test
    fun addMultipleModifiersToField() {
        assertEquals("\tprivate final ", modifiersOnField("\t")
                .withModifiers("private", "final").printMods())
        assertEquals("private final ", modifiersOnField("private ")
                .withModifiers("private", "final").printMods())
    }

    @Test
    fun addStaticModifierToMethod() {
        assertEquals("private static final ", modifiersOnMethod("private final ")
                .withModifiers("static").printMods())
        assertEquals("private static ", modifiersOnMethod("private ")
                .withModifiers("static").printMods())
        assertEquals("\tstatic ", modifiersOnMethod("\t")
                .withModifiers("static").printMods())
    }

    @Test
    fun addFinalModifierToMethod() {
        assertEquals("private static final ", modifiersOnMethod("private static ")
                .withModifiers("final").printMods())
        assertEquals("private final ", modifiersOnMethod("private ")
                .withModifiers("final").printMods())
        assertEquals("\tfinal ", modifiersOnMethod("\t")
                .withModifiers("final").printMods())
    }

    @Test
    fun addVisibilityModifierToMethod() {
        assertEquals("public ", modifiersOnMethod("protected ")
                .withModifiers("public").printMods())
        assertEquals("\tpublic final ", modifiersOnMethod("\tfinal ")
                .withModifiers("public").printMods())
        assertEquals("\tpublic ", modifiersOnMethod("\t")
                .withModifiers("public").printMods())
    }

    private fun modifiersOnClass(modifiers: String): Tr.ClassDecl =
            Parser().parse("public class A {${modifiers}class B {}}").classes[0].body.statements[0] as Tr.ClassDecl

    private fun modifiersOnField(modifiers: String): Tr.VariableDecls =
            Parser().parse("public class A {${modifiers}int n;").classes[0].body.statements[0] as Tr.VariableDecls

    private fun modifiersOnMethod(modifiers: String): Tr.MethodDecl =
            Parser().parse("public class A {${modifiers}void foo() {}").classes[0].body.statements[0] as Tr.MethodDecl

    private fun Tr.ClassDecl.printMods() = print().substringBefore("class")
    private fun Tr.VariableDecls.printMods() = print().substringBefore("int")
    private fun Tr.MethodDecl.printMods() = print().substringBefore("void")
}
