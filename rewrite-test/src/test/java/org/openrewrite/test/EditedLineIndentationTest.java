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
package org.openrewrite.test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.EditedLineIndentation.reindented;

class EditedLineIndentationTest {

    @Test
    void lineEditedInPlaceAtAnotherIndentation() {
        assertThat(reindented("a {\n    foo();\n}\n", "a {\n\tbar();\n}\n"))
          .containsExactly(new EditedLineIndentation.Reindented(2, "    foo();", "\tbar();"));
    }

    @Test
    void lineEditedInPlaceAtItsIndentation() {
        assertThat(reindented("a {\n    foo();\n}\n", "a {\n    bar();\n}\n")).isEmpty();
    }

    @Test
    void lineOnlyReindented() {
        assertThat(reindented("a {\n    foo();\n}\n", "a {\n\tfoo();\n}\n")).isEmpty();
    }

    @Test
    void linesAddedAroundTheEdit() {
        assertThat(reindented("a {\n    foo();\n}\n", "a {\n  if (b) {\n      bar();\n  }\n}\n")).isEmpty();
    }

    @Test
    void linesRemovedAroundTheEdit() {
        assertThat(reindented("a {\n    if (b) {\n        foo();\n    }\n}\n", "a {\n  bar();\n}\n")).isEmpty();
    }

    @Test
    void runsOfChangedLinesNotCompared() {
        assertThat(reindented("a {\n    foo();\n    baz();\n}\n", "a {\n    bar();\n  qux();\n}\n")).isEmpty();
    }

    @Test
    void separateEditsComparedSeparately() {
        assertThat(reindented("a {\n    foo();\n    x();\n    baz();\n}\n", "a {\n    bar();\n    x();\n  qux();\n}\n"))
          .containsExactly(new EditedLineIndentation.Reindented(4, "    baz();", "  qux();"));
    }

    @Test
    void unrelatedLineNotCompared() {
        assertThat(reindented("a {\n    foo.bar(baz);\n}\n", "a {\n\tx;\n}\n")).isEmpty();
    }

    @Test
    void lineEditedInsideCodeThatMoved() {
        assertThat(reindented("a {\n    b {\n        foo();\n    }\n}\n", "a {\nb {\n    bar();\n}\n}\n")).isEmpty();
    }

    @Test
    void docCommentLineLosingTheSpaceThatAlignsItsStar() {
        assertThat(reindented("x\n     * foo\ny\n", "x\n    /// foo\ny\n")).isEmpty();
    }

    @Test
    void blankLinesNotCompared() {
        assertThat(reindented("a {\n    \n    foo();\n}\n", "a {\nx\n    foo();\n}\n")).isEmpty();
    }

    @Test
    void carriageReturnsKeptOutOfTheIndentation() {
        assertThat(reindented("a {\r\n    foo();\r\n}\r\n", "a {\r\n    bar();\r\n}\r\n")).isEmpty();
    }
}
