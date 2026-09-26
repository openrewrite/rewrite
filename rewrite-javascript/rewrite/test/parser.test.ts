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
import {ExecutionContext, Parser, ParserInput, ParserSourceReader, readSourceSync} from "../src";
import {isParseError} from "../src/parse-error";
import {PlainText} from "../src/text";
import {JavaScriptParser} from "../src/javascript";
import {randomId} from "../src/uuid";
import {emptyMarkers} from "../src/markers";
import {SourceFile} from "../src/tree";

/** Parses to whatever text it was constructed with, so its output need not match its input. */
class FixedTextParser extends Parser {
    constructor(private readonly parsesTo: string, ctx?: ExecutionContext) {
        super({ctx});
    }

    async* parse(...inputs: ParserInput[]): AsyncGenerator<SourceFile> {
        for (const input of inputs) {
            const parsed: PlainText = {
                kind: PlainText.Kind.PlainText,
                id: randomId(),
                markers: emptyMarkers,
                sourcePath: this.relativePath(input),
                text: this.parsesTo,
                snippets: []
            };
            yield await this.requirePrintEqualsInput(parsed, input);
        }
    }
}


describe("parse source reader utility", () => {
    const sourceJson = {text: `  { "type": "object" }`, sourcePath: "source.json"};

    test("whitespace", () => {
        const reader = new ParserSourceReader(sourceJson);
        expect(reader.whitespace()).toEqual("  ");
        expect(reader.cursor).toEqual(2);
    })

    test("source before a token", () => {
        const reader = new ParserSourceReader(sourceJson);
        expect(reader.sourceBefore("{")).toEqual("  ");
        expect(reader.cursor).toEqual(3);
    });

    test("read in memory source file", () => {
        expect(readSourceSync(sourceJson)).toEqual(`  { "type": "object" }`);
    });
});

describe("print-equals-input check", () => {
    const input = {text: "hello", sourcePath: "a.txt"};

    test("the option key is the one peers send", () => {
        expect(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT).toBe("org.openrewrite.requirePrintEqualsInput");
    });

    test("a parse that prints back to its input passes through", async () => {
        const parsed = await new FixedTextParser("hello").parseOne(input);
        expect(isParseError(parsed)).toBe(false);
    });

    test("a parse that loses source becomes a ParseError carrying the tree that lost it", async () => {
        const parsed = await new FixedTextParser("hell").parseOne(input);
        expect(isParseError(parsed)).toBe(true);
        if (isParseError(parsed)) {
            expect(parsed.markers.markers[0]).toMatchObject({message: expect.stringContaining("a.txt is not print idempotent.")});
            expect(parsed.erroneous).toMatchObject({kind: PlainText.Kind.PlainText, text: "hell"});
        }
    });

    test.each([
        ["the option off as a string keeps the tree", "false", false],
        ["the option off as a boolean keeps the tree", false, false],
        ["the option off as a digit keeps the tree", "0", false],
        ["the option off in upper case keeps the tree", "FALSE", false],
        ["an unparseable option reports the loss", "nonsense", true],
    ])("%s", async (_, option, expectsParseError) => {
        const ctx = new ExecutionContext({[ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT]: option});
        const parsed = await new FixedTextParser("hell", ctx).parseOne(input);
        expect(isParseError(parsed)).toBe(expectsParseError);
    });
});

describe("literals the LST cannot hold verbatim", () => {
    test("a lone surrogate still parses, held as the escape it prints back as", async () => {
        const parsed = await new JavaScriptParser({}).parseOne(
            {text: 'const s = "\uD800";', sourcePath: "a.ts"});
        expect(isParseError(parsed)).toBe(false);
    });
});
