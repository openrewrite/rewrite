/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.javascript;

/**
 * Every newline the JavaScript printer emits must be reachable by an in-process {@link JavaScriptVisitor}
 * walk (whitespace, comments, and literal value sources), otherwise LST-walking tools such as line counters
 * silently undercount. Regression coverage for the {@code ExpressionStatement}/{@code StatementExpression}
 * prefix and {@code TrailingComma} suffix gaps.
 * <p>
 * {@code rewriteRun} already verifies the source round-trips (parse+print is idempotent), so the visitor's
 * newline count must equal the newline count of the source itself.
 */
class JavaScriptVisitorTest implements RewriteTest {

    static long nl(String s) {
        return s == null ? 0 : s.chars().filter(c -> c == '\n').count();
    }

    void assertVisitorSeesEveryNewline(String src) {
        rewriteRun(javascript(src, spec -> spec.afterRecipe(cu -> {
            AtomicInteger n = new AtomicInteger();
            new JavaScriptVisitor<AtomicInteger>() {
                @Override
                public Space visitSpace(Space space, Space.Location loc, AtomicInteger a) {
                    a.addAndGet((int) nl(space.getWhitespace()));
                    for (Comment c : space.getComments()) {
                        a.addAndGet((int) (c instanceof TextComment ? nl(((TextComment) c).getText()) : nl(c.printComment(getCursor()))));
                        a.addAndGet((int) nl(c.getSuffix()));
                    }
                    return super.visitSpace(space, loc, a);
                }

                @Override
                public J visitLiteral(J.Literal literal, AtomicInteger a) {
                    a.addAndGet((int) nl(literal.getValueSource()));
                    return super.visitLiteral(literal, a);
                }
            }.visit(cu, n);
            assertThat((long) n.get())
                    .as(() -> "visitor missed newlines the printer emits for:\n" + src)
                    .isEqualTo(nl(src));
        })));
    }

    @Test
    void directiveProloguePrefix() {
        assertVisitorSeesEveryNewline("function f() {\n    'use strict';\n    foo();\n}");
        assertVisitorSeesEveryNewline("(function () {\n    'use strict';\n    foo();\n})();");
    }

    @Test
    void trailingCommaSuffix() {
        assertVisitorSeesEveryNewline("const a = [\n    1,\n    2,\n];");
        assertVisitorSeesEveryNewline("const o = {\n    a: 1,\n    b: 2,\n};");
        assertVisitorSeesEveryNewline("g(\n    1,\n    2,\n);");
        assertVisitorSeesEveryNewline("import {\n    a,\n    b,\n} from './m';");
    }

    @Test
    void commentsClassesAndTemplates() {
        assertVisitorSeesEveryNewline("// header\n/* block\n   comment */\nclass C {\n    m() {\n        return 1; // trailing\n    }\n}");
        assertVisitorSeesEveryNewline("const s = `line one\nline two\nline three`;");
    }
}
