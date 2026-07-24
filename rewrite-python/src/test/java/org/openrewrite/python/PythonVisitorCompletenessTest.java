/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.python;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "No remote client/server available")
class PythonVisitorCompletenessTest {

    static long nl(String s) {
        return s == null ? 0 : s.chars().filter(c -> c == '\n').count();
    }

    long visitorNewlines(SourceFile cu) {
        AtomicInteger n = new AtomicInteger();
        new PythonVisitor<AtomicInteger>() {
            @Override
            public Space visitSpace(Space s, Space.Location l, AtomicInteger a) {
                a.addAndGet((int) nl(s.getWhitespace()));
                for (Comment c : s.getComments()) {
                    a.addAndGet((int) (c instanceof TextComment ? nl(((TextComment) c).getText()) : nl(c.printComment(getCursor()))));
                    a.addAndGet((int) nl(c.getSuffix()));
                }
                return super.visitSpace(s, l, a);
            }

            @Override
            public J visitLiteral(J.Literal lit, AtomicInteger a) {
                a.addAndGet((int) nl(lit.getValueSource()));
                return super.visitLiteral(lit, a);
            }
        }.visit(cu, n);
        return n.get();
    }

    void check(String src) {
        SourceFile cu = PythonParser.builder().build().parse(new InMemoryExecutionContext(), src).findFirst().orElseThrow();
        String printed = cu.printAll();
        assertThat(visitorNewlines(cu)).as(() -> "snippet:\n" + src).isEqualTo((int) nl(printed));
    }

    @Test
    void completeness() {
        for (String s : SNIPPETS) {
            check(s);
        }
    }

    static final List<String> SNIPPETS = List.of(
      // imports
      "import sys\n",
      "import os, sys\n",
      "import os.path as p\n",
      "from collections import OrderedDict\n",
      "from a.b.c import (\n    d,\n    e,\n    f,\n)\n",
      "from . import x\n",
      "from foo import *\n",
      // module docstring
      "\"\"\"Module docstring.\n\nMulti line.\n\"\"\"\nx = 1\n",
      // simple assignments
      "x = 1\ny = 2\n",
      "a: int = 3\n",
      "a = b = c = 0\n",
      "x += 1\n",
      "x, y = 1, 2\n",
      "d = {\n    'a': 1,\n    'b': 2,\n}\n",
      "lst = [\n    1,\n    2,\n    3,\n]\n",
      "s = {\n    1,\n    2,\n}\n",
      "t = (\n    1,\n    2,\n)\n",
      // def
      "def f():\n    pass\n",
      "def f(a, b=1, *args, **kwargs):\n    return a + b\n",
      "def f(a: int, b: str = 'x') -> bool:\n    return True\n",
      "def f(\n    a,\n    b,\n    c,\n):\n    pass\n",
      "def f(a, *, b, c):\n    pass\n",
      // function docstring
      "def f():\n    \"\"\"Docstring.\n\n    Details.\n    \"\"\"\n    return 1\n",
      // class
      "class A:\n    pass\n",
      "class A(B, C):\n    x = 1\n",
      "class A(\n    B,\n    C,\n):\n    pass\n",
      "class A:\n    \"\"\"Class docstring.\"\"\"\n    def m(self):\n        return self\n",
      // decorators
      "@foo\ndef f():\n    pass\n",
      "@a.b.c\ndef f():\n    pass\n",
      "@a.b(c, d)\ndef f():\n    pass\n",
      "@property\ndef x(self):\n    return self._x\n",
      "@x.setter\ndef x(self, v):\n    self._x = v\n",
      "@decorator(\n    arg1,\n    arg2,\n)\nclass A:\n    pass\n",
      // if/elif/else
      "if x:\n    pass\n",
      "if x:\n    a = 1\nelif y:\n    a = 2\nelse:\n    a = 3\n",
      "if (\n    x and\n    y\n):\n    pass\n",
      // for/while/else
      "for i in range(10):\n    print(i)\n",
      "for i in range(10):\n    print(i)\nelse:\n    print('done')\n",
      "while x:\n    x -= 1\n",
      "while x:\n    break\nelse:\n    pass\n",
      // try/except/finally
      "try:\n    x = 1\nexcept ValueError:\n    pass\n",
      "try:\n    x = 1\nexcept ValueError as e:\n    print(e)\nexcept (TypeError, KeyError):\n    pass\nelse:\n    pass\nfinally:\n    pass\n",
      "try:\n    pass\nexcept* ValueError:\n    pass\n",
      // with
      "with open('f') as fp:\n    data = fp.read()\n",
      "with open('a') as a, open('b') as b:\n    pass\n",
      "with (\n    open('a') as a,\n    open('b') as b,\n):\n    pass\n",
      // lambda
      "f = lambda x: x + 1\n",
      "f = lambda x, y=1, *a, **k: x\n",
      // comprehensions
      "xs = [x for x in range(10)]\n",
      "xs = [x for x in range(10) if x > 5]\n",
      "d = {k: v for k, v in items}\n",
      "s = {x for x in range(10)}\n",
      "g = (x for x in range(10))\n",
      "m = [\n    x\n    for x in range(10)\n    if x > 2\n    if x < 8\n]\n",
      "xs = [x async for x in aiter()]\n",
      // f-strings
      "x = f'{a}'\n",
      "x = f'{a!r:>{width}}'\n",
      "x = f'{a=}'\n",
      "x = f'''\nmulti {a}\nline {b}\n'''\n",
      // strings multiline
      "x = '''\nabc\ndef\n'''\n",
      "x = \"line1\" \\\n    \"line2\"\n",
      // comments
      "# leading comment\nx = 1\n",
      "x = 1  # trailing comment\n",
      "def f():\n    # body comment\n    pass\n",
      "x = [\n    1,  # one\n    2,  # two\n]\n",
      // async/await
      "async def f():\n    await g()\n",
      "async def f():\n    async with a() as b:\n        pass\n",
      "async def f():\n    async for x in y():\n        pass\n",
      // match/case
      "match x:\n    case 1:\n        pass\n    case _:\n        pass\n",
      "match p:\n    case Point(x=0, y=0):\n        pass\n    case Point(x=0, y=y):\n        pass\n    case [a, b]:\n        pass\n    case {'k': v}:\n        pass\n    case A() | B():\n        pass\n    case _:\n        pass\n",
      "match x:\n    case [1, 2, *rest]:\n        pass\n    case str() as s if len(s) > 0:\n        pass\n",
      // global/nonlocal
      "def f():\n    global x\n    x = 1\n",
      "def f():\n    def g():\n        nonlocal y\n        y = 1\n",
      "global a, b, c\n",
      // assert
      "assert x\n",
      "assert x, 'message'\n",
      "assert (\n    x and y\n), 'msg'\n",
      // yield
      "def f():\n    yield 1\n",
      "def f():\n    yield\n",
      "def f():\n    x = yield 1\n",
      "def f():\n    yield from gen()\n",
      // del/pass/raise
      "del x\n",
      "del x, y\n",
      "raise\n",
      "raise ValueError('x')\n",
      "raise ValueError('x') from err\n",
      // slices
      "x = a[1:2]\n",
      "x = a[1:2:3]\n",
      "x = a[:]\n",
      "x = a[::2]\n",
      "x = a[i:j, k]\n",
      // ternary / walrus
      "x = a if cond else b\n",
      "if (n := len(a)) > 10:\n    pass\n",
      // return / annotated
      "def f() -> 'SomeType':\n    return None\n",
      "x: list[int] = []\n",
      // star expressions
      "a, *b = [1, 2, 3]\n",
      "f(*args, **kwargs)\n",
      "x = [*a, *b]\n",
      "d = {**a, **b}\n",
      // type alias (3.12)
      "type Alias = int\n",
      // no trailing newline
      "x = 1",
      "def f():\n    pass",
      "class A:\n    pass",
      "if x:\n    pass\nelse:\n    pass",
      // union types
      "def f(x: int | str) -> int | None:\n    return None\n",
      // nested / blank lines
      "def outer():\n\n    def inner():\n        pass\n\n    return inner\n",
      "x = 1\n\n\ny = 2\n",
      "class A:\n\n    x = 1\n\n\n    def m(self):\n        pass\n",
      // decorated property with multiple decorators
      "class A:\n    @staticmethod\n    @wraps(f)\n    def m():\n        pass\n",
      // trailing commas across many constructs (exercise TrailingComma.suffix)
      "foo(\n    a,\n    b,\n)\n",
      "def f(\n    a,\n    b,\n):\n    pass\n",
      "d = {\n    'a': 1,\n}\n",
      "s = {\n    1,\n}\n",
      "lst = [\n    1,\n]\n",
      "t = (\n    1,\n)\n",
      "class A(\n    B,\n):\n    pass\n",
      "x = a[\n    1,\n    2,\n]\n",
      "@deco(\n    x,\n)\ndef f():\n    pass\n",
      "from m import (\n    a,\n    b,\n)\n",
      "with (\n    open('a') as a,\n    open('b') as b,\n):\n    pass\n",
      // multi-line Py-specific spaces (exercise previously short-circuited spaces)
      "x = (\n    a\n    | b\n    | c\n)\n",
      "def f():\n    del (\n        a,\n        b,\n    )\n",
      "g = (\n    x\n    for x in items\n    if x\n)\n",
      "match x:\n    case [\n        1,\n        2,\n    ]:\n        pass\n"
    );
}
