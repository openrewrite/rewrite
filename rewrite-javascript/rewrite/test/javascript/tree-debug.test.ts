/*
 * Copyright 2025 the original author or authors.
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

import {
    formatCursorMessages,
    formatSpace,
    formatWhitespace,
    JavaScriptParser,
    JS,
    LstDebugPrinter,
    LstDebugVisitor
} from "../../src/javascript";
import {Cursor, ExecutionContext} from "../../src";
import {J} from "../../src/java";

describe("LST Debug Utilities", () => {

    describe("formatWhitespace", () => {
        test("empty string returns ''", () => {
            expect(formatWhitespace("")).toBe("''");
        });

        test("undefined returns ''", () => {
            expect(formatWhitespace(undefined)).toBe("''");
        });

        test("single space shows · (implicit ₁)", () => {
            const result = formatWhitespace(" ");
            expect(result).toBe("'·'");
        });

        test("multiple spaces show count subscript", () => {
            const result = formatWhitespace("    ");
            expect(result).toBe("'·₄'");
        });

        test("newline shows \\n (implicit ₁)", () => {
            const result = formatWhitespace("\n");
            expect(result).toBe("'\\n'");
        });

        test("newline + spaces shows \\n·₂", () => {
            const result = formatWhitespace("\n  ");
            expect(result).toBe("'\\n·₂'");
        });

        test("tabs show - with count", () => {
            const result = formatWhitespace("\t");
            expect(result).toBe("'-'");
        });

        test("multiple tabs show count subscript", () => {
            const result = formatWhitespace("\t\t");
            expect(result).toBe("'-₂'");
        });

        test("mixed whitespace with multiple newlines", () => {
            const result = formatWhitespace("\n    \n  ");
            expect(result).toBe("'\\n·₄\\n·₂'");
        });

        test("two newlines show \\n₂", () => {
            const result = formatWhitespace("\n\n");
            expect(result).toBe("'\\n₂'");
        });

        test("carriage return shows \\r", () => {
            const result = formatWhitespace("\r\n");
            expect(result).toBe("'\\r\\n'");
        });

        test("spaces and tabs mixed", () => {
            const result = formatWhitespace("  \t\t");
            expect(result).toBe("'·₂-₂'");
        });

        test("10 spaces shows ·₁₀", () => {
            const result = formatWhitespace("          "); // 10 spaces
            expect(result).toBe("'·₁₀'");
        });

        test("12 spaces shows ·₁₂", () => {
            const result = formatWhitespace("            "); // 12 spaces
            expect(result).toBe("'·₁₂'");
        });
    });

    describe("formatSpace", () => {
        test("undefined space", () => {
            expect(formatSpace(undefined)).toBe("<undefined>");
        });

        test("empty space", () => {
            const space: J.Space = {
                kind: J.Kind.Space,
                comments: [],
                whitespace: ""
            };
            expect(formatSpace(space)).toBe("''");
        });

        test("space with whitespace only", () => {
            const space: J.Space = {
                kind: J.Kind.Space,
                comments: [],
                whitespace: "\n    "
            };
            expect(formatSpace(space)).toBe("'\\n·₄'");
        });

        test("space with single line comment only", () => {
            const space: J.Space = {
                kind: J.Kind.Space,
                comments: [{multiline: false, text: "comment", suffix: ""} as any],
                whitespace: ""
            };
            expect(formatSpace(space)).toBe("//comment");
        });

        test("space with multiline comment only", () => {
            const space: J.Space = {
                kind: J.Kind.Space,
                comments: [{multiline: true, text: "comment", suffix: ""} as any],
                whitespace: ""
            };
            expect(formatSpace(space)).toBe("/*comment*/");
        });

        test("space with comment and suffix", () => {
            const space: J.Space = {
                kind: J.Kind.Space,
                comments: [{multiline: false, text: "comment", suffix: "\n"} as any],
                whitespace: ""
            };
            expect(formatSpace(space)).toBe("//comment'\\n'");
        });

        test("space with multiple comments", () => {
            const space: J.Space = {
                kind: J.Kind.Space,
                comments: [
                    {multiline: false, text: "c1", suffix: "\n"} as any,
                    {multiline: false, text: "c2", suffix: ""} as any
                ],
                whitespace: ""
            };
            expect(formatSpace(space)).toBe("//c1'\\n' + //c2");
        });

        test("space with comments and trailing whitespace", () => {
            const space: J.Space = {
                kind: J.Kind.Space,
                comments: [{multiline: false, text: "comment", suffix: "\n"} as any],
                whitespace: "    "
            };
            expect(formatSpace(space)).toBe("//comment'\\n' + '·₄'");
        });
    });

    describe("formatCursorMessages", () => {
        test("undefined cursor returns <no messages>", () => {
            expect(formatCursorMessages(undefined)).toBe("<no messages>");
        });

        test("cursor with no messages returns <no messages>", () => {
            const cursor = new Cursor({}, undefined);
            expect(formatCursorMessages(cursor)).toBe("<no messages>");
        });

        test("cursor with string message", () => {
            const cursor = new Cursor({}, undefined);
            cursor.messages.set("key", "value");
            expect(formatCursorMessages(cursor)).toBe('⟨key=value⟩');
        });

        test("cursor with number message", () => {
            const cursor = new Cursor({}, undefined);
            cursor.messages.set("indent", 4);
            expect(formatCursorMessages(cursor)).toBe("⟨indent=4⟩");
        });

        test("cursor with array message (indentContext)", () => {
            const cursor = new Cursor({}, undefined);
            cursor.messages.set("indentContext", [8, "block"]);
            expect(formatCursorMessages(cursor)).toBe('⟨indentContext=[8, "block"]⟩');
        });

        test("cursor with multiple messages", () => {
            const cursor = new Cursor({}, undefined);
            cursor.messages.set("indent", 4);
            cursor.messages.set("kind", "block");
            const result = formatCursorMessages(cursor);
            expect(result).toContain("indent=4");
            expect(result).toContain('kind=block');
            expect(result).toMatch(/^⟨.*⟩$/);
        });
    });

    describe("AstDebugPrinter", () => {
        const parser = new JavaScriptParser();

        async function parse(source: string): Promise<JS.CompilationUnit> {
            const gen = parser.parse({text: source, sourcePath: 'test.ts'});
            return (await gen.next()).value as JS.CompilationUnit;
        }

        test("prints simple tree structure", async () => {
            const source = `const x = 1;`;
            const cu = await parse(source);

            // Capture console.info output
            const logs: string[] = [];
            const originalInfo = console.info;
            console.info = (msg: string) => logs.push(msg);

            try {
                const printer = new LstDebugPrinter({includeCursorMessages: false});
                printer.print(cu);

                const output = logs.join('\n');
                expect(output).toContain("CompilationUnit");
                expect(output).toContain("prefix:");
            } finally {
                console.info = originalInfo;
            }
        });

        test("prints cursor messages when enabled", async () => {
            const source = `const x = 1;`;
            const cu = await parse(source);

            const logs: string[] = [];
            const originalInfo = console.info;
            console.info = (msg: string) => logs.push(msg);

            try {
                const cursor = new Cursor(cu, undefined);
                cursor.messages.set("indentContext", [0, "block"]);

                const printer = new LstDebugPrinter({includeCursorMessages: true});
                printer.print(cu, cursor);

                const output = logs.join('\n');
                expect(output).toContain("indentContext");
            } finally {
                console.info = originalInfo;
            }
        });

        test("prints label as comment before output", async () => {
            const source = `const x = 1;`;
            const cu = await parse(source);

            const logs: string[] = [];
            const originalInfo = console.info;
            console.info = (msg: string) => logs.push(msg);

            try {
                const printer = new LstDebugPrinter({});
                printer.print(cu, undefined, "before visiting method invocation");

                const output = logs.join('\n');
                expect(output).toContain("// before visiting method invocation");
                // Label should be first line
                expect(logs[0]).toContain("// before visiting method invocation");
            } finally {
                console.info = originalInfo;
            }
        });

        test("log prints single node without recursion", async () => {
            const source = `class Person { name: string; }`;
            const cu = await parse(source);

            const logs: string[] = [];
            const originalInfo = console.info;
            console.info = (msg: string) => logs.push(msg);

            try {
                const printer = new LstDebugPrinter({includeCursorMessages: false});
                printer.log(cu, undefined, "testing log");

                // Output is flushed as a single string with newlines
                expect(logs.length).toBe(1);
                const output = logs[0];
                const lines = output.split('\n');
                expect(lines[0]).toBe("// testing log");
                expect(lines[1]).toContain("CompilationUnit{");
                // Should NOT contain child nodes like ClassDeclaration
                expect(output).not.toContain("ClassDeclaration");
            } finally {
                console.info = originalInfo;
            }
        });

        test("log shows cursor messages on separate line", async () => {
            const source = `const x = 1;`;
            const cu = await parse(source);

            const logs: string[] = [];
            const originalInfo = console.info;
            console.info = (msg: string) => logs.push(msg);

            try {
                const cursor = new Cursor(cu, undefined);
                cursor.messages.set("indent", 4);
                cursor.messages.set("kind", "block");

                const printer = new LstDebugPrinter({includeCursorMessages: true});
                printer.log(cu, cursor);

                const output = logs.join('\n');
                expect(output).toContain("CompilationUnit{");
                // Cursor messages appear on separate line with arrow prefix
                expect(output).toContain("⤷ ⟨");
                expect(output).toContain("indent=4");
            } finally {
                console.info = originalInfo;
            }
        });
    });

    describe("AstDebugVisitor", () => {
        const parser = new JavaScriptParser();

        async function parse(source: string): Promise<JS.CompilationUnit> {
            const gen = parser.parse({text: source, sourcePath: 'test.ts'});
            return (await gen.next()).value as JS.CompilationUnit;
        }

        test("visits and prints tree structure", async () => {
            const source = `const x = 1;`;
            const cu = await parse(source);

            const logs: string[] = [];
            const originalInfo = console.info;
            console.info = (msg: string) => logs.push(msg);

            try {
                const visitor = new LstDebugVisitor({}, {printPreVisit: true, printPostVisit: false});
                await visitor.visit(cu, new ExecutionContext());

                const output = logs.join('\n');
                // Kind names include the namespace prefix (JS$ or J$)
                expect(output).toContain("JS$CompilationUnit");
                expect(output).toContain("J$VariableDeclarations");
            } finally {
                console.info = originalInfo;
            }
        });

        test("shows identifier names inline", async () => {
            const source = `const myVar = 1;`;
            const cu = await parse(source);

            const logs: string[] = [];
            const originalInfo = console.info;
            console.info = (msg: string) => logs.push(msg);

            try {
                const visitor = new LstDebugVisitor({}, {printPreVisit: true, printPostVisit: false});
                await visitor.visit(cu, new ExecutionContext());

                const output = logs.join('\n');
                // Identifier should show the name inline with property path and braces
                expect(output).toContain('name: J$Identifier{"myVar"');
            } finally {
                console.info = originalInfo;
            }
        });

        test("shows literal values inline", async () => {
            const source = `const x = "hello";`;
            const cu = await parse(source);

            const logs: string[] = [];
            const originalInfo = console.info;
            console.info = (msg: string) => logs.push(msg);

            try {
                const visitor = new LstDebugVisitor({}, {printPreVisit: true, printPostVisit: false});
                await visitor.visit(cu, new ExecutionContext());

                const output = logs.join('\n');
                // Literal should show the value inline with braces
                // With intersection types, it's printed directly (not under element:)
                expect(output).toContain('J$Literal{"hello"');
            } finally {
                console.info = originalInfo;
            }
        });

        test("shows property path with array index", async () => {
            const source = `const x = 1, y = 2;`;
            const cu = await parse(source);

            const logs: string[] = [];
            const originalInfo = console.info;
            console.info = (msg: string) => logs.push(msg);

            try {
                const visitor = new LstDebugVisitor({}, {printPreVisit: true, printPostVisit: false});
                await visitor.visit(cu, new ExecutionContext());

                const output = logs.join('\n');
                // Should show array indices for variables with braces
                expect(output).toContain('name: J$Identifier{"x"');
                expect(output).toContain('name: J$Identifier{"y"');
            } finally {
                console.info = originalInfo;
            }
        });

        test("shows primitive element values in LeftPadded", async () => {
            const source = `const x = 1 + 2;`;
            const cu = await parse(source);

            const logs: string[] = [];
            const originalInfo = console.info;
            console.info = (msg: string) => logs.push(msg);

            try {
                const visitor = new LstDebugVisitor({}, {printPreVisit: true, printPostVisit: false});
                await visitor.visit(cu, new ExecutionContext());

                const output = logs.join('\n');
                // Should show operator value in LeftPadded
                expect(output).toContain('LeftPadded{before=');
                expect(output).toContain('element="Addition"');
            } finally {
                console.info = originalInfo;
            }
        });

        test("shows boolean element values in RightPadded", async () => {
            const source = `class Foo { static x = 1; }`;
            const cu = await parse(source);

            const logs: string[] = [];
            const originalInfo = console.info;
            console.info = (msg: string) => logs.push(msg);

            try {
                const visitor = new LstDebugVisitor({}, {printPreVisit: true, printPostVisit: false});
                await visitor.visit(cu, new ExecutionContext());

                const output = logs.join('\n');
                // Should show static boolean value in RightPadded (false for block.static, true would be for static block)
                expect(output).toContain('static: RightPadded{after=');
                expect(output).toMatch(/element=(true|false)/);
            } finally {
                console.info = originalInfo;
            }
        });

        test("shows markers like TrailingComma in RightPadded", async () => {
            // Array with trailing comma
            const source = `const arr = [1, 2,];`;
            const cu = await parse(source);

            const logs: string[] = [];
            const originalInfo = console.info;
            console.info = (msg: string) => logs.push(msg);

            try {
                const visitor = new LstDebugVisitor({}, {printPreVisit: true, printPostVisit: false});
                await visitor.visit(cu, new ExecutionContext());

                const output = logs.join('\n');
                // Should show TrailingComma marker on the last element's RightPadded
                expect(output).toContain('markers=[TrailingComma]');
            } finally {
                console.info = originalInfo;
            }
        });
    });
});
