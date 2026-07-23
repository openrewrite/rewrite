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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.javascript.tree.JS;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every newline the JavaScript printer emits must be reachable by an in-process {@link JavaScriptVisitor}
 * walk (whitespace, comments, and literal value sources), otherwise LST-walking tools such as line counters
 * silently undercount. Regression coverage for the {@code ExpressionStatement}/{@code StatementExpression}
 * prefix and {@code TrailingComma} suffix gaps.
 */
class JavaScriptVisitorTest {

    static long nl(String s) {
        return s == null ? 0 : s.chars().filter(c -> c == '\n').count();
    }

    long visitorNewlines(SourceFile cu) {
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
        return n.get();
    }

    void assertComplete(String src) {
        SourceFile cu = JavaScriptParser.builder().build()
                .parseInputs(singletonList(Parser.Input.fromString(Paths.get("test.tsx"), src)), null, new InMemoryExecutionContext())
                .findFirst()
                .orElseThrow();
        assertThat(cu).as(() -> "did not parse to a JS.CompilationUnit:\n" + src).isInstanceOf(JS.CompilationUnit.class);
        String printed = cu.printAll();
        assertThat(visitorNewlines(cu))
                .as(() -> "visitor missed newlines the printer emits for:\n" + src)
                .isEqualTo((int) nl(printed));
    }

    @Test
    void visitorSeesEveryPrintedNewline() {
        for (String src : SNIPPETS) {
            assertComplete(src);
        }
    }

    static final List<String> SNIPPETS = List.of(
            // directive prologue (ExpressionStatement.prefix)
            "function f() {\n    'use strict';\n    foo();\n}\n",
            "(function () {\n    'use strict';\n    foo();\n})();\n",
            // trailing commas (TrailingComma.suffix) across many positions
            "const a = [\n    1,\n    2,\n    3,\n];\n",
            "const o = {\n    a: 1,\n    b: 2,\n};\n",
            "function g(\n    a,\n    b,\n) {\n    return a + b;\n}\n",
            "g(\n    1,\n    2,\n);\n",
            "import {\n    a,\n    b,\n} from './m';\n",
            "export {\n    a,\n    b,\n};\n",
            "const { a, b, } = o;\n",
            // imports / exports
            "import foo from './foo';\nimport { bar } from './bar';\n\nexport const x = 1;\n",
            // arrow functions, async, generators
            "const h = async (x) => {\n    await x;\n    return x;\n};\n",
            "function* gen() {\n    yield 1;\n    yield 2;\n}\n",
            // classes
            "class C extends B {\n    constructor() {\n        super();\n        this.x = 1;\n    }\n\n    m() {\n        return this.x;\n    }\n}\n",
            // control flow
            "function f(x) {\n    if (x > 0) {\n        return 1;\n    } else {\n        return 2;\n    }\n    for (let i = 0; i < 10; i++) {\n        console.log(i);\n    }\n}\n",
            "try {\n    risky();\n} catch (e) {\n    handle(e);\n} finally {\n    cleanup();\n}\n",
            "switch (x) {\n    case 1:\n        a();\n        break;\n    default:\n        b();\n}\n",
            // template literals (J.Literal.valueSource) incl. multi-line
            "const s = `line one\nline two\nline three`;\n",
            "const t = `a${x}b${y}c`;\n",
            "const u = tag`multi\nline`;\n",
            // JSX text and children
            "const el = (\n    <div className=\"a\">\n        Hello\n        {name}\n    </div>\n);\n",
            // comments
            "// header\n/* block\n   comment */\nfunction f() {\n    return 1; // trailing\n}\n",
            "/**\n * jsdoc\n * @param x\n */\nfunction f(x) {\n    return x;\n}\n",
            // destructuring, spread, default params
            "function f({ a, b } = {}, ...rest) {\n    return [a, b, ...rest];\n}\n",
            // TypeScript-ish
            "interface Thing {\n    id: number;\n    name: string;\n}\n",
            "type Pair = {\n    a: number;\n    b: number;\n};\n",
            "enum E {\n    A,\n    B,\n    C,\n}\n",
            // no trailing newline
            "const x = 1;\nconst y = 2;"
    );
}
