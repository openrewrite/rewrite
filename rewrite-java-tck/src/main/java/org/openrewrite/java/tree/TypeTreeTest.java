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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.marker.Quoted;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeTreeTest {

    @Test
    void buildFullyQualifiedClassName() {
        J.FieldAccess name = TypeTree.build("java.util.List");
        assertEquals("java.util.List", name.toString());
        assertEquals("List", name.getSimpleName());
    }

    @Test
    void buildFullyQualifiedClassNameWithSpacing() {
        J.FieldAccess name = TypeTree.build("java . util . List");
        assertEquals("java . util . List", name.toString());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void buildFullyQualifiedInnerClassName() {
        J.FieldAccess name = TypeTree.build("a.Outer.Inner");

        assertEquals("a.Outer.Inner", name.toString());
        assertEquals("Inner", name.getSimpleName());
        assertEquals("a.Outer.Inner", TypeUtils.asFullyQualified(name.getType()).getFullyQualifiedName());

        J.FieldAccess outer = (J.FieldAccess) name.getTarget();
        assertEquals("Outer", outer.getSimpleName());
        assertEquals("a.Outer", TypeUtils.asFullyQualified(outer.getType()).getFullyQualifiedName());
    }

    @Test
    void buildStaticImport() {
        J.FieldAccess name = TypeTree.build("a.A.*");

        assertEquals("a.A.*", name.toString());
        assertEquals("*", name.getSimpleName());
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3845")
    @Test
    void buildEscapedName() {
        J.FieldAccess name = TypeTree.build("foo.bar.`some escaped name`", '`');

        assertTrue(name.getName().getMarkers().findFirst(Quoted.class).isPresent());
        assertEquals("foo.bar.some escaped name", name.toString());
        assertEquals("some escaped name", name.getSimpleName());
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/564")
    @Test
    void buildQuotedNameContainingDelimiter() {
        J.FieldAccess name = TypeTree.build("foo.bar.`$`", '`');

        assertTrue(name.getName().getMarkers().findFirst(Quoted.class).isPresent());
        assertEquals("foo.bar.$", name.toString());
        assertEquals("$", name.getSimpleName());
    }
}
