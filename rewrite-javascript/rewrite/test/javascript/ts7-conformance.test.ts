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
import {SyntaxKind as SyntaxKind7, type Node as Node7, type SourceFile as SourceFile7} from "typescript7/unstable/ast";
import {SymbolFlags as SymbolFlags7} from "typescript7/unstable/sync";
import {typeAtLocation} from "../../src/javascript/ts7/checker";
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

// Source covering the constructs type-mapping.ts branches on. The checker moved to Go, so agreement
// here is what says its answers still describe the same types.
const TYPED_SOURCE = `
export interface Shape { area(): number; name?: string }
export class Circle implements Shape {
    constructor(private radius: number) {}
    area(): number { return Math.PI * this.radius ** 2; }
    static of(r: number): Circle { return new Circle(r); }
}
export abstract class Base<T> { abstract value(): T; }
export class Impl extends Base<string> { value(): string { return "x"; } }
export type Mixed = string | number | boolean | null;
export type Both = Shape & { extra: number };
export enum Color { Red, Green = 3, Blue }
export const c = Circle.of(2);
export const n: number = c.area();
export const arr: Array<Circle> = [c];
export function generic<T extends Shape>(x: T): T[] { return [x]; }
export const lambda = (a: number, b: string) => a + b.length;
`;

const NAMED_FLAGS = ["Class", "Interface", "Enum", "TypeAlias", "Alias", "ValueModule", "NamespaceModule",
    "TypeParameter", "Function", "Method", "Property", "Variable", "BlockScopedVariable", "Type"];

// Resolving a type-position name through its type node yields the instantiated type, where
// TypeScript 6 reports the generic declaration or the constructor behind it. Both describe the same
// reference and the instantiated form is what the LST wants, so these pairs are recorded as
// agreement; a rendering outside this table is a failure.
const TS7_MORE_SPECIFIC: Record<string, string> = {
    "typeof Base": "Base<string>",   // class Impl extends Base<string>
    "T[]": "Circle[]",               // const arr: Array<Circle>
};

describe("TypeScript 7 type attribution", () => {
    test("types and symbol flags agree with TypeScript 6 for every identifier", () => {
        const root = "/ts7types";
        const file = `${root}/typed.ts`;

        const options6 = {
            target: ts.ScriptTarget.Latest, module: ts.ModuleKind.Preserve,
            moduleResolution: ts.ModuleResolutionKind.Bundler, strict: false, noEmit: true,
        };
        const host = ts.createCompilerHost(options6);
        const baseGetSourceFile = host.getSourceFile.bind(host);
        const baseFileExists = host.fileExists.bind(host);
        const baseReadFile = host.readFile.bind(host);
        // Only the probe file is virtual; lib.d.ts still comes off disk, or nothing resolves.
        host.getSourceFile = (name, languageVersion, ...rest) => name === file
            ? ts.createSourceFile(name, TYPED_SOURCE,
                {languageVersion: ts.ScriptTarget.Latest, jsDocParsingMode: ts.JSDocParsingMode.ParseNone},
                true, ts.ScriptKind.TS)
            : baseGetSourceFile(name, languageVersion, ...rest);
        host.fileExists = name => name === file || baseFileExists(name);
        host.readFile = name => (name === file ? TYPED_SOURCE : baseReadFile(name));
        const program6 = ts.createProgram([file], options6, host);
        const checker6 = program6.getTypeChecker();
        const sourceFile6 = program6.getSourceFile(file)!;

        const session = openSession(root, new Map([[file, TYPED_SOURCE]]), {
            target: "esnext", module: "preserve", moduleResolution: "bundler", strict: false, noEmit: true,
        });
        try {
            const checker7 = session.project.checker;
            const sourceFile7 = session.project.program.getSourceFile(file)!;

            const identifiers6: ts.Identifier[] = [];
            (function walk(n: ts.Node) {
                if (ts.isIdentifier(n)) identifiers6.push(n);
                ts.forEachChild(n, walk);
            })(sourceFile6);
            const identifiers7: Node7[] = [];
            (function walk(n: Node7) {
                if (n.kind === SyntaxKind7.Identifier) identifiers7.push(n);
                n.forEachChild(child => { walk(child); return undefined; });
            })(sourceFile7 as unknown as Node7);

            expect(identifiers7.length).toBe(identifiers6.length);

            const typeMismatches: string[] = [];
            const flagMismatches: string[] = [];
            for (let i = 0; i < identifiers6.length; i++) {
                const node6 = identifiers6[i], node7 = identifiers7[i];
                expect(node7.pos, `identifier #${i} is at a different offset`).toBe(node6.pos);

                const type6 = checker6.getTypeAtLocation(node6);
                const type7 = typeAtLocation(checker7, node7);
                const rendered6 = type6 ? checker6.typeToString(type6) : "<none>";
                const rendered7 = type7 ? checker7.typeToString(type7) : "<none>";
                if (rendered6 !== rendered7 && TS7_MORE_SPECIFIC[rendered6] !== rendered7) {
                    typeMismatches.push(`${node6.text}@${node6.pos}: ts6="${rendered6}" ts7="${rendered7}"`);
                }

                const symbol6 = checker6.getSymbolAtLocation(node6);
                const symbol7 = checker7.getSymbolAtLocation(node7);
                const named = (flags: number | undefined, table: Record<string, number>) => flags === undefined
                    ? "<none>"
                    : NAMED_FLAGS.filter(name => flags & table[name]).join("|");
                const flags6 = named(symbol6?.flags, ts.SymbolFlags as unknown as Record<string, number>);
                const flags7 = named(symbol7?.flags, SymbolFlags7 as unknown as Record<string, number>);
                if (flags6 !== flags7) {
                    flagMismatches.push(`${node6.text}@${node6.pos}: ts6=[${flags6}] ts7=[${flags7}]`);
                }
            }
            expect(typeMismatches).toEqual([]);
            expect(flagMismatches).toEqual([]);
        } finally {
            session.close();
        }
    }, 120_000);
});
