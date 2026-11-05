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
import {Recipe} from "../recipe";
import {Cursor, isTree, rootCursor, SourceFile, Tree} from "../tree";
import {Result} from "../run";
import {MarkerPrinter, PrintOutputCapture, TreePrinters} from "../print";
import {isParseError} from "../parse-error";
import {MarkersKind, ParseExceptionResult} from "../markers";
import {TreeVisitor} from "../visitor";
import {J} from "../java";
import {JavaScriptVisitor, JS} from "../javascript";
import {Json, JsonVisitor} from "../json";
import {Yaml, YamlVisitor} from "../yaml";
import {typeName} from "./probes";

const MAX_PER_FILE = 5;
const SNIPPET_LENGTH = 60;
const STRICT_NUMBER = /^-?(0|[1-9][0-9]*)(\.[0-9]+)?([eE][+-]?[0-9]+)?$/;

/** How the check reads the trees of a language, as Java's `TreeShapeValidationService` does. */
interface Language {
    /** The visitor of the language, whose order of visiting the check compares nodes in. */
    readonly visitor: abstract new () => TreeVisitor<any, any>

    /** What tells a node apart from another of its kind with the same children, such as an identifier's name. */
    value(tree: Tree): string | undefined

    /** Whether the comparison leaves out a node and what it holds, as with an empty element the parser adds. */
    skip(tree: Tree): boolean

    /** What a node has that the parser accepts but the language doesn't, such as a duplicate key. */
    invalid(tree: Tree): string[]
}

let languages: { [kind: string]: Language } | undefined;

/**
 * The language of a source file of `kind`, from a table built on first use, since the modules of the languages import
 * the harness and may still be loading when it loads.
 */
function languageOf(kind: string): Language | undefined {
    languages ??= {
        [JS.Kind.CompilationUnit]: {
            visitor: JavaScriptVisitor,
            value: javaScriptValue,
            skip: tree => tree.kind === J.Kind.Empty,
            invalid: () => []
        },
        [Json.Kind.Document]: {
            visitor: JsonVisitor,
            value: jsonValue,
            skip: tree => tree.kind === Json.Kind.Empty,
            invalid: jsonInvalid
        },
        [Yaml.Kind.Documents]: {
            visitor: YamlVisitor,
            value: yamlValue,
            skip: () => false,
            invalid: yamlInvalid
        }
    };
    return languages[kind];
}

interface Node {
    readonly tree: Tree
    readonly cursor: Cursor
    readonly children: Node[]
}

/**
 * Finds nodes in a recipe's output that its parser builds differently from the text they print, which recipes that
 * run after it don't match the way they match parsed code.
 */
export class TreeShapes {
    constructor(private readonly recipe: Recipe) {
    }

    /** @param freshParse The fresh parse of the text each source the recipe left prints. */
    async assertParserShaped(results: Result[], freshParse: Map<SourceFile, SourceFile>): Promise<void> {
        const found: string[] = [];
        for (const {before, after} of results) {
            const language = after && languageOf(after.kind);
            if (!after || after === before || !language) {
                continue;
            }
            const text = await printed(after);
            const reparsed = freshParse.get(after);
            if (!reparsed || text.trim() === "" || before && await printed(before) === text) {
                continue;
            }
            const problems = await this.problems(language, after, before, reparsed, text);
            if (problems.length > 0) {
                found.push(`  ${after.sourcePath}:`, ...problems.map(problem => `    ${problem}`));
            }
        }
        if (found.length > 0) {
            throw new Error([
                "Write code with a template or the format's own parser, or build the nodes the way its parser " +
                `builds the text they print. The output of ${this.recipe.name} holds nodes that its parser makes ` +
                "differently of the same text:",
                ...found,
                "A recipe that runs after it matches the tree, not the text, so it misses or mistakes a node that " +
                "parsed code never holds. If the output is meant to differ from parsed code, set " +
                "spec.checkParserShapedTrees = false."
            ].join("\n"));
        }
    }

    private async problems(language: Language, after: SourceFile, before: SourceFile | undefined,
                           reparsed: SourceFile, text: string): Promise<string[]> {
        if (isParseError(reparsed)) {
            const exception = reparsed.markers.markers
                .find(m => m.kind === MarkersKind.ParseExceptionResult) as ParseExceptionResult | undefined;
            return [`the text it prints doesn't parse: ${exception ? firstLine(exception.message) : "a ParseError"}`];
        } else if (reparsed.kind !== after.kind || await printed(reparsed) !== text) {
            // a parser that doesn't print back what it read is the parser's to fix
            return [];
        }
        const problems: string[] = [];
        const shape = await skeleton(language, after);
        await compare(language, after, shape, await skeleton(language, reparsed), problems);
        // a file that already broke its language's rules follows other ones
        if (!before || invalid(language, await skeleton(language, before)).length === 0) {
            for (const problem of invalid(language, shape)) {
                if (problems.length < MAX_PER_FILE) {
                    problems.push(problem);
                }
            }
        }
        return problems;
    }
}

async function compare(language: Language, sourceFile: SourceFile, node: Node, parsed: Node,
                       problems: string[]): Promise<void> {
    if (problems.length >= MAX_PER_FILE) {
        return;
    }
    if (node.tree.kind !== parsed.tree.kind) {
        problems.push(`${await snippet(sourceFile, node)} is a ${typeName(node.tree.kind)} where the parser makes a ` +
            typeName(parsed.tree.kind));
        return;
    }
    const value = language.value(node.tree);
    const parsedValue = language.value(parsed.tree);
    if (value !== parsedValue) {
        problems.push(`${await snippet(sourceFile, node)} has ${value} where the parser makes ${parsedValue}`);
    }
    if (node.children.length !== parsed.children.length) {
        problems.push(`${await snippet(sourceFile, node)} holds ${kinds(node.children)} where the parser makes ` +
            kinds(parsed.children));
        return;
    }
    for (let i = 0; i < node.children.length; i++) {
        await compare(language, sourceFile, node.children[i], parsed.children[i], problems);
    }
}

function kinds(nodes: Node[]): string {
    return nodes.length === 0 ? "nothing" : nodes.map(node => typeName(node.tree.kind)).join(", ");
}

/** The nodes of a source file, each with the nodes it holds, leaving out those the language skips. */
async function skeleton(language: Language, sourceFile: SourceFile): Promise<Node> {
    const roots: Node[] = [];
    const path: Node[] = [];
    const Visitor = language.visitor as unknown as new () => TreeVisitor<any, any>;
    const visitor = new class extends Visitor {
        protected override async preVisit(tree: Tree, _p: any): Promise<Tree | undefined> {
            if (language.skip(tree)) {
                this.stopAfterPreVisit();
                return tree;
            }
            const ancestors = this.cursor.asArray().filter(value => isTree(value)).length - 1;
            path.length = Math.min(path.length, ancestors);
            const node: Node = {tree, cursor: this.cursor, children: []};
            (path.length === 0 ? roots : path[path.length - 1].children).push(node);
            path.push(node);
            return tree;
        }
    };
    await visitor.visit(sourceFile, undefined);
    return roots[0] ?? {tree: sourceFile, cursor: rootCursor(), children: []};
}

function invalid(language: Language, node: Node): string[] {
    return [...language.invalid(node.tree), ...node.children.flatMap(child => invalid(language, child))];
}

async function snippet(sourceFile: SourceFile, node: Node): Promise<string> {
    let shown: string;
    try {
        shown = (await TreePrinters.printer(sourceFile)
            .print(node.tree, new PrintOutputCapture(MarkerPrinter.SANITIZED), node.cursor.parent))
            .trim().replace(/\s+/g, " ");
    } catch {
        shown = "?";
    }
    return `\`${shown.length > SNIPPET_LENGTH ? `${shown.substring(0, SNIPPET_LENGTH)}...` : shown}\``;
}

function javaScriptValue(tree: Tree): string | undefined {
    switch (tree.kind) {
        case J.Kind.Identifier:
            return `the name \`${(tree as J.Identifier).simpleName}\``;
        case J.Kind.Literal: {
            const literal = tree as J.Literal;
            // the parser keeps the source of a literal with surrogate escapes in place of its value
            const value = literal.unicodeEscapes?.length ? "a value with surrogate escapes" :
                literal.value === null || literal.value === undefined ? `the value ${literal.value}` :
                    `the ${typeof literal.value} value ${quoted(literal.value)}`;
            return `${value} of type ${literal.type?.keyword ?? "none"}`;
        }
        case J.Kind.Modifier: {
            const modifier = tree as J.Modifier;
            return `the modifier ${modifier.type === J.ModifierType.LanguageExtension ? modifier.keyword : modifier.type}`;
        }
        case J.Kind.Primitive:
            return `the type ${(tree as J.Primitive).type.keyword}`;
        case J.Kind.Binary:
        case J.Kind.Unary:
        case J.Kind.AssignmentOperation:
        case JS.Kind.Binary:
        case JS.Kind.AssignmentOperation:
            return `the operator ${(tree as J.Binary).operator.element}`;
        case JS.Kind.TypeOperator:
            return `the operator ${(tree as JS.TypeOperator).operator}`;
    }
    return undefined;
}

function quoted(value: string | number | bigint | boolean): string {
    let shown = String(value).replace(/\\/g, "\\\\").replace(/\n/g, "\\n").replace(/\r/g, "\\r")
        .replace(/\t/g, "\\t");
    if (shown.length > 40) {
        shown = `${shown.substring(0, 40)}...`;
    }
    return typeof value === "string" ? `"${shown}"` : shown;
}

function jsonValue(tree: Tree): string | undefined {
    if (tree.kind === Json.Kind.Literal) {
        const value = (tree as Json.Literal).value;
        return value === null || value === undefined ? `the value ${value}` : `the ${typeof value} value \`${value}\``;
    } else if (tree.kind === Json.Kind.Identifier) {
        return `the name \`${(tree as Json.Identifier).name}\``;
    }
    return undefined;
}

function jsonInvalid(tree: Tree): string[] {
    const found: string[] = [];
    if (tree.kind === Json.Kind.Object) {
        const keys = new Set<string>();
        for (const member of (tree as Json.Object).members.map(m => m.element)) {
            if (member.kind === Json.Kind.Member) {
                const key = (member as Json.Member).key.element;
                const name = key.kind === Json.Kind.Identifier ? key.name : String(key.value);
                if (keys.has(name)) {
                    found.push(`the key \`${name}\` appears twice in one object`);
                }
                keys.add(name);
            }
        }
    } else if (tree.kind === Json.Kind.Member) {
        const key = (tree as Json.Member).key.element;
        if (key.kind === Json.Kind.Identifier) {
            found.push(`the key \`${key.name}\` has no quotes, which JSON doesn't allow`);
        }
    } else if (tree.kind === Json.Kind.Literal) {
        const {source, value} = tree as Json.Literal;
        if (source.startsWith("'")) {
            found.push(`\`${source}\` is in single quotes, which JSON doesn't allow`);
        } else if (typeof value === "number" && !STRICT_NUMBER.test(source)) {
            found.push(`\`${source}\` isn't a number JSON allows`);
        }
    }
    return found;
}

function yamlValue(tree: Tree): string | undefined {
    if (tree.kind === Yaml.Kind.Scalar) {
        const scalar = tree as Yaml.Scalar;
        // a block scalar's value holds the whitespace up to the next node, which the parser splits differently
        const block = scalar.style === Yaml.ScalarStyle.LITERAL || scalar.style === Yaml.ScalarStyle.FOLDED;
        return `the ${scalar.style} value \`${block ? scalar.value.replace(/\s+$/, "") : scalar.value}\``;
    } else if (tree.kind === Yaml.Kind.Anchor) {
        return `the anchor \`${(tree as Yaml.Anchor).key}\``;
    } else if (tree.kind === Yaml.Kind.Alias) {
        return `the alias of \`${(tree as Yaml.Alias).anchor.key}\``;
    }
    return undefined;
}

function yamlInvalid(tree: Tree): string[] {
    if (tree.kind !== Yaml.Kind.Mapping) {
        return [];
    }
    const found: string[] = [];
    const keys = new Set<string>();
    for (const {key} of (tree as Yaml.Mapping).entries) {
        const name = key.kind === Yaml.Kind.Scalar ? (key as Yaml.Scalar).value : `*${(key as Yaml.Alias).anchor.key}`;
        if (keys.has(name)) {
            found.push(`the key \`${name}\` appears twice in one mapping, which YAML doesn't allow`);
        }
        keys.add(name);
    }
    return found;
}

function printed(sourceFile: SourceFile): Promise<string> {
    return TreePrinters.printer(sourceFile).print(sourceFile, new PrintOutputCapture(MarkerPrinter.SANITIZED));
}

function firstLine(message: string): string {
    const line = message.trim().split("\n")[0];
    return line.length > 200 ? `${line.substring(0, 200)}...` : line;
}
