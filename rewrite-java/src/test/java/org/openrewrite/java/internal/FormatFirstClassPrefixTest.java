/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.internal;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class FormatFirstClassPrefixTest {
    @Test
    void classWithNoTopLevelClasses() {
        J.CompilationUnit cu = parse("interface Augmentation {}");
        J.ClassDeclaration declaration = cu.getClasses().get(0);

        // Module augmentation can contain an interface without any top-level classes.
        assertThat(new FormatFirstClassPrefix<>().visit(declaration, null,
          new Cursor(null, cu.withClasses(Collections.emptyList()))))
          .isSameAs(declaration);
    }

    @Test
    void firstTopLevelClassIsFormatted() {
        J.CompilationUnit cu = parse("public   class A {}");
        J.ClassDeclaration declaration = cu.getClasses().get(0);

        J result = new FormatFirstClassPrefix<>().visit(declaration, null, new Cursor(null, cu));

        assertThat(result).isNotNull();
        assertThat(result.printTrimmed(new Cursor(null, cu))).isEqualTo("public class A {}");
    }

    @Test
    void laterTopLevelClassIsUnchanged() {
        J.CompilationUnit cu = parse("class A {} class B{}");
        J.ClassDeclaration declaration = cu.getClasses().get(1);

        assertThat(new FormatFirstClassPrefix<>().visit(declaration, null, new Cursor(null, cu)))
          .isSameAs(declaration);
    }

    @Test
    void differentInstanceWithSameIdIsUnchanged() {
        J.CompilationUnit cu = parse("class A {}");
        J.ClassDeclaration declaration = cu.getClasses().get(0).withPrefix(Space.format("\n\n"));

        assertThat(new FormatFirstClassPrefix<>().visit(declaration, null, new Cursor(null, cu)))
          .isSameAs(declaration);
    }

    @Test
    void nestedClassIsUnchanged() {
        J.CompilationUnit cu = parse("class A { class Nested{} }");
        J.ClassDeclaration outer = cu.getClasses().get(0);
        J.ClassDeclaration nested = (J.ClassDeclaration) outer.getBody().getStatements().get(0);

        assertThat(new FormatFirstClassPrefix<>().visit(nested, null,
          new Cursor(new Cursor(null, cu), outer)))
          .isSameAs(nested);
    }

    private static J.CompilationUnit parse(String source) {
        return (J.CompilationUnit) JavaParser.fromJavaVersion().build().parse(source)
          .findFirst().orElseThrow(IllegalStateException::new);
    }
}
