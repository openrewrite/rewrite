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
import fs from "node:fs";
import path from "node:path";
import ts from "typescript";
import {SyntaxKind as SyntaxKind7, type SourceFile as SourceFile7} from "typescript7/unstable/ast";
import {type Child, childrenOf} from "../../src/javascript/ts7/token-navigation";
import {openSession} from "../../src/javascript/ts7/program";

// TypeScript 7 renumbers every SyntaxKind, so trees are compared by kind *name*. Where 7 renames a
// kind or models a construct differently, the mapping is recorded here rather than skipped: a
// deviation that is not in this table fails the test, which is how new drift gets noticed.
const TS7_EQUIVALENT: Record<string, string> = {
    EndOfFile: "EndOfFileToken",
};

/** TypeScript 6's name for a node 7 models differently, or 7's own name when the two agree. */
function equivalentName(name: string, pos: number, end: number): string {
    // An elision in a binding pattern (`const [a, , b] = ...`) is a zero-width BindingElement in 7
    // where 6 emits an OmittedExpression. Non-empty BindingElements mean the same thing in both.
    if (name === "BindingElement" && pos === end) {
        return "OmittedExpression";
    }
    return TS7_EQUIVALENT[name] ?? name;
}

/** Reverse a SyntaxKind enum to the first name per value, the way parser.ts dispatches. */
function firstNames(kinds: Record<string, unknown>): Map<number, string> {
    const names = new Map<number, string>();
    for (const [name, value] of Object.entries(kinds)) {
        if (typeof value === "number" && !names.has(value)) {
            names.set(value, name);
        }
    }
    return names;
}

const NAMES_6 = firstNames(ts.SyntaxKind as unknown as Record<string, unknown>);
const NAMES_7 = firstNames(SyntaxKind7 as unknown as Record<string, unknown>);

function stream6(node: ts.Node, sourceFile: ts.SourceFile, out: string[]): string[] {
    for (const child of node.getChildren(sourceFile)) {
        out.push(`${NAMES_6.get(child.kind)}@${child.pos}:${child.end}`);
        if (child.kind === ts.SyntaxKind.SyntaxList || child.getChildren(sourceFile).length > 0) {
            stream6(child, sourceFile, out);
        }
    }
    return out;
}

function stream7(node: Child, sourceFile: SourceFile7, out: string[]): string[] {
    for (const child of childrenOf(node, sourceFile)) {
        const name = NAMES_7.get(child.kind)!;
        out.push(`${equivalentName(name, child.pos, child.end)}@${child.pos}:${child.end}`);
        if (childrenOf(child, sourceFile).length > 0) {
            stream7(child, sourceFile, out);
        }
    }
    return out;
}

const CORPUS = fs.readdirSync(path.join(__dirname, "../../src/javascript"))
    .filter(f => f.endsWith(".ts"))
    .map(f => path.join(__dirname, "../../src/javascript", f));

describe("TypeScript 7 AST conformance", () => {
    test("reconstructed token stream matches TypeScript 6 across the parser's own sources", () => {
        const root = "/ts7conformance";
        const sources = new Map(CORPUS.map(f => [`${root}/${path.basename(f)}`, fs.readFileSync(f, "utf8")]));
        const session = openSession(root, sources, {
            target: "esnext", module: "preserve", moduleResolution: "bundler",
            noEmit: true, allowJs: true, strict: false,
        });
        try {
            const mismatches: string[] = [];
            for (const [virtualPath, text] of sources) {
                // The parser turns JSDoc parsing off, so neither side should see JSDoc nodes.
                const sourceFile6 = ts.createSourceFile(virtualPath, text,
                    {languageVersion: ts.ScriptTarget.Latest, jsDocParsingMode: ts.JSDocParsingMode.ParseNone},
                    true, ts.ScriptKind.TS);
                const sourceFile7 = session.project.program.getSourceFile(virtualPath);
                expect(sourceFile7, `TypeScript 7 did not parse ${virtualPath}`).toBeDefined();

                const expected = stream6(sourceFile6, sourceFile6, []);
                const actual = stream7(sourceFile7 as unknown as Child, sourceFile7!, []);
                const at = expected.findIndex((token, i) => token !== actual[i]);
                if (at !== -1 || expected.length !== actual.length) {
                    mismatches.push(`${path.basename(virtualPath)} at #${at}: `
                        + `ts6=${expected[at] ?? "<end>"} ts7=${actual[at] ?? "<end>"}`);
                }
            }
            expect(mismatches).toEqual([]);
        } finally {
            session.close();
        }
    }, 120_000);
});
