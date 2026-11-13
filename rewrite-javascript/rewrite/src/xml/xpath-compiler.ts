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

type TokenType =
    "/" | "//" | "::" | "[" | "]" | "(" | ")" | "@" | ".." | "." | "," |
    "=" | "!=" | "<=" | ">=" | "<" | ">" | "*" |
    "and" | "or" | "text" | "comment" | "node" | "processing-instruction" |
    "NUMBER" | "STRING" | "QNAME" | "NCNAME" | "EOF";

interface Token {
    readonly type: TokenType
    readonly text: string
    readonly offset: number
}

const KEYWORDS = new Set<TokenType>(["and", "or", "text", "comment", "node", "processing-instruction"]);
const NODE_TYPES = new Set<TokenType>(["text", "comment", "node", "processing-instruction"]);
const STEP_STARTS = new Set<TokenType>([".", "..", "@", "*", "QNAME", "NCNAME", ...KEYWORDS]);

function isNameStartChar(c: number): boolean {
    return (c >= 0x61 && c <= 0x7A) || (c >= 0x41 && c <= 0x5A) || c === 0x5F ||
        (c >= 0xC0 && c <= 0xD6) || (c >= 0xD8 && c <= 0xF6) || (c >= 0xF8 && c <= 0x2FF) ||
        (c >= 0x370 && c <= 0x37D) || (c >= 0x37F && c <= 0x1FFF) || (c >= 0x200C && c <= 0x200D) ||
        (c >= 0x2070 && c <= 0x218F) || (c >= 0x2C00 && c <= 0x2FEF) || (c >= 0x3001 && c <= 0xD7FF) ||
        (c >= 0xF900 && c <= 0xFDCF) || (c >= 0xFDF0 && c <= 0xFFFD);
}

function isNameChar(c: number): boolean {
    return isNameStartChar(c) || c === 0x2D || c === 0x2E || (c >= 0x30 && c <= 0x39) || c === 0xB7 ||
        (c >= 0x300 && c <= 0x36F) || (c >= 0x203F && c <= 0x2040);
}

function isDigit(c: number): boolean {
    return c >= 0x30 && c <= 0x39;
}

function syntaxError(expression: string, offset: number, message: string): Error {
    return new Error(`Syntax error at offset ${offset}: ${message}. Original input: '${expression}'`);
}

function tokenize(expression: string): Token[] {
    const tokens: Token[] = [];
    let i = 0;
    const push = (type: TokenType, length: number) => {
        tokens.push({type, text: expression.substring(i, i + length), offset: i});
        i += length;
    };
    while (i < expression.length) {
        const c = expression[i];
        const next = expression[i + 1];
        switch (c) {
            case " ":
            case "\t":
            case "\r":
            case "\n":
                i++;
                continue;
            case "/":
                push(next === "/" ? "//" : "/", next === "/" ? 2 : 1);
                continue;
            case ".":
                push(next === "." ? ".." : ".", next === "." ? 2 : 1);
                continue;
            case "<":
            case ">":
                if (next === "=") {
                    push(c === "<" ? "<=" : ">=", 2);
                } else {
                    push(c, 1);
                }
                continue;
            case ":":
            case "!":
                if (next !== (c === ":" ? ":" : "=")) {
                    throw syntaxError(expression, i, `token recognition error at: '${c}'`);
                }
                push(c === ":" ? "::" : "!=", 2);
                continue;
            case "[":
            case "]":
            case "(":
            case ")":
            case "@":
            case ",":
            case "=":
            case "*":
                push(c, 1);
                continue;
            case "'":
            case "\"": {
                const end = expression.indexOf(c, i + 1);
                if (end === -1) {
                    throw syntaxError(expression, i, `token recognition error at: '${expression.substring(i)}'`);
                }
                push("STRING", end + 1 - i);
                continue;
            }
        }
        const code = expression.charCodeAt(i);
        if (isDigit(code)) {
            let end = i;
            while (end < expression.length && isDigit(expression.charCodeAt(end))) {
                end++;
            }
            if (expression[end] === "." && isDigit(expression.charCodeAt(end + 1))) {
                end++;
                while (end < expression.length && isDigit(expression.charCodeAt(end))) {
                    end++;
                }
            }
            push("NUMBER", end - i);
        } else if (isNameStartChar(code)) {
            let end = scanName(expression, i);
            let type: TokenType = "NCNAME";
            if (expression[end] === ":" && isNameStartChar(expression.charCodeAt(end + 1))) {
                end = scanName(expression, end + 1);
                type = "QNAME";
            } else if (KEYWORDS.has(expression.substring(i, end) as TokenType)) {
                type = expression.substring(i, end) as TokenType;
            }
            push(type, end - i);
        } else {
            throw syntaxError(expression, i, `token recognition error at: '${c}'`);
        }
    }
    tokens.push({type: "EOF", text: "", offset: expression.length});
    return tokens;
}

function scanName(expression: string, start: number): number {
    let end = start + 1;
    while (end < expression.length && isNameChar(expression.charCodeAt(end))) {
        end++;
    }
    return end;
}

interface Span {
    readonly start: number
    readonly end: number
}

interface OrExpr extends Span {
    readonly operands: AndExpr[]
}

interface AndExpr extends Span {
    readonly operands: EqualityExpr[]
}

interface EqualityExpr extends Span {
    readonly operands: RelationalExpr[]
    readonly operators: TokenType[]
}

interface RelationalExpr extends Span {
    readonly operands: PathExpr[]
    readonly operators: TokenType[]
}

type PathExpr = FilterPathExpr | LocationPathExpr;

interface FilterPathExpr extends Span {
    readonly kind: "function" | "bracketed" | "literal" | "number"
    readonly functionCall?: FunctionCall
    readonly expr?: OrExpr
    readonly token?: Token
    readonly predicates: OrExpr[]
    readonly trailingSeparator?: "/" | "//"
    readonly trailingPath?: RelativeLocationPath
}

interface LocationPathExpr extends Span {
    readonly kind: "location"
    readonly absolute?: "/" | "//"
    readonly relativePath?: RelativeLocationPath
}

interface RelativeLocationPath extends Span {
    readonly steps: Step[]
    readonly separators: ("/" | "//")[]
}

type Step =
    { readonly kind: "abbreviated", readonly dotdot: boolean, readonly predicates: OrExpr[] } |
    { readonly kind: "attribute", readonly name: string, readonly predicates: OrExpr[] } |
    { readonly kind: "node", readonly axis?: string, readonly nodeTest: NodeTest, readonly predicates: OrExpr[] };

type NodeTest = { readonly name: string, readonly nodeType?: never } | { readonly nodeType: string, readonly name?: never };

interface FunctionCall {
    readonly name: string
    readonly args: OrExpr[]
}

class Parser {
    private pos = 0;

    constructor(private readonly expression: string, private readonly tokens: Token[]) {
    }

    parse(): OrExpr {
        const expr = this.orExpr();
        if (this.peek().type !== "EOF") {
            this.fail(`extraneous input '${this.peek().text}'`);
        }
        return expr;
    }

    text(span: Span): string {
        return this.tokens.slice(span.start, span.end).map(t => t.text).join("");
    }

    private peek(ahead: number = 0): Token {
        return this.tokens[Math.min(this.pos + ahead, this.tokens.length - 1)];
    }

    private next(): Token {
        return this.tokens[this.pos++];
    }

    private accept(...types: TokenType[]): Token | undefined {
        return types.includes(this.peek().type) ? this.next() : undefined;
    }

    private expect(...types: TokenType[]): Token {
        const token = this.accept(...types);
        if (!token) {
            this.fail(`mismatched input '${this.peek().text || "<EOF>"}' expecting ${types.map(t => `'${t}'`).join(" or ")}`);
        }
        return token!;
    }

    private fail(message: string): never {
        throw syntaxError(this.expression, this.peek().offset, message);
    }

    private orExpr(): OrExpr {
        const start = this.pos;
        const operands = [this.andExpr()];
        while (this.accept("or")) {
            operands.push(this.andExpr());
        }
        return {start, end: this.pos, operands};
    }

    private andExpr(): AndExpr {
        const start = this.pos;
        const operands = [this.equalityExpr()];
        while (this.accept("and")) {
            operands.push(this.equalityExpr());
        }
        return {start, end: this.pos, operands};
    }

    private equalityExpr(): EqualityExpr {
        const start = this.pos;
        const operands = [this.relationalExpr()];
        const operators: TokenType[] = [];
        for (let op = this.accept("=", "!="); op; op = this.accept("=", "!=")) {
            operators.push(op.type);
            operands.push(this.relationalExpr());
        }
        return {start, end: this.pos, operands, operators};
    }

    private relationalExpr(): RelationalExpr {
        const start = this.pos;
        const operands = [this.pathExpr()];
        const operators: TokenType[] = [];
        for (let op = this.accept("<", ">", "<=", ">="); op; op = this.accept("<", ">", "<=", ">=")) {
            operators.push(op.type);
            operands.push(this.pathExpr());
        }
        return {start, end: this.pos, operands, operators};
    }

    private pathExpr(): PathExpr {
        const start = this.pos;
        const token = this.peek();
        let filter: Omit<FilterPathExpr, "start" | "end" | "predicates">;
        if ((token.type === "NCNAME" || NODE_TYPES.has(token.type)) && this.peek(1).type === "(") {
            filter = {kind: "function", functionCall: this.functionCall()};
        } else if (token.type === "(") {
            this.next();
            const expr = this.orExpr();
            this.expect(")");
            filter = {kind: "bracketed", expr};
        } else if (token.type === "STRING" || token.type === "NUMBER") {
            this.next();
            filter = {kind: token.type === "STRING" ? "literal" : "number", token};
        } else {
            return this.locationPath();
        }
        const predicates = this.predicates();
        const separator = this.accept("/", "//");
        const trailingPath = separator && this.relativeLocationPath();
        return {
            ...filter,
            start,
            end: this.pos,
            predicates,
            trailingSeparator: separator?.type as "/" | "//" | undefined,
            trailingPath
        };
    }

    private functionCall(): FunctionCall {
        const name = this.next().text;
        this.expect("(");
        const args: OrExpr[] = [];
        if (!this.accept(")")) {
            do {
                args.push(this.orExpr());
            } while (this.accept(","));
            this.expect(")");
        }
        return {name, args};
    }

    private locationPath(): LocationPathExpr {
        const start = this.pos;
        const absolute = this.accept("/", "//");
        let relativePath: RelativeLocationPath | undefined;
        if (absolute?.type !== "/" || STEP_STARTS.has(this.peek().type)) {
            relativePath = this.relativeLocationPath();
        }
        return {kind: "location", start, end: this.pos, absolute: absolute?.type as "/" | "//" | undefined, relativePath};
    }

    private relativeLocationPath(): RelativeLocationPath {
        const start = this.pos;
        const steps = [this.step()];
        const separators: ("/" | "//")[] = [];
        for (let separator = this.accept("/", "//"); separator; separator = this.accept("/", "//")) {
            separators.push(separator.type as "/" | "//");
            steps.push(this.step());
        }
        return {start, end: this.pos, steps, separators};
    }

    private step(): Step {
        const abbreviated = this.accept(".", "..");
        if (abbreviated) {
            return {kind: "abbreviated", dotdot: abbreviated.type === "..", predicates: []};
        }
        if (this.accept("@")) {
            const name = this.expect("QNAME", "NCNAME", "*", ...KEYWORDS).text;
            return {kind: "attribute", name, predicates: this.predicates()};
        }
        let axis: string | undefined;
        if (this.peek().type === "NCNAME" && this.peek(1).type === "::") {
            axis = this.next().text;
            this.next();
        }
        const nodeTest = this.nodeTest();
        return {kind: "node", axis, nodeTest, predicates: this.predicates()};
    }

    private nodeTest(): NodeTest {
        const name = this.accept("*", "QNAME", "NCNAME", ...(this.peek(1).type === "(" ? [] : [...KEYWORDS]));
        if (name) {
            return {name: name.text};
        }
        const nodeType = this.expect(...NODE_TYPES);
        this.expect("(");
        this.expect(")");
        return {nodeType: nodeType.text};
    }

    private predicates(): OrExpr[] {
        const predicates: OrExpr[] = [];
        while (this.accept("[")) {
            predicates.push(this.orExpr());
            this.expect("]");
        }
        return predicates;
    }
}

export type ComparisonOp = "=" | "!=" | "<" | "<=" | ">" | ">=";

export type FunctionType =
    "position" | "last" | "local-name" | "namespace-uri" | "contains" | "starts-with" | "ends-with" |
    "string-length" | "substring-before" | "substring-after" | "count" | "text" | "not" | "comment" | "node" |
    "processing-instruction";

const FUNCTIONS = new Set<string>([
    "position", "last", "local-name", "namespace-uri", "contains", "starts-with", "ends-with", "string-length",
    "substring-before", "substring-after", "count", "text", "not", "comment", "node", "processing-instruction"
]);

export type CompiledExpr =
    { readonly type: "numeric", readonly value: number } |
    { readonly type: "string", readonly value: string } |
    { readonly type: "comparison", readonly left: CompiledExpr, readonly op: ComparisonOp, readonly right: CompiledExpr } |
    { readonly type: "and" | "or", readonly left: CompiledExpr, readonly right: CompiledExpr } |
    { readonly type: "function", readonly functionType: FunctionType, readonly args: CompiledExpr[] } |
    { readonly type: "attribute" | "child", readonly name: string } |
    { readonly type: "path", readonly steps: CompiledExpr[], readonly pathSteps?: CompiledStep[] } |
    { readonly type: "absolutePath", readonly path: string } |
    { readonly type: "parent" | "self" };

export type StepType = "dot" | "dotdot" | "axis" | "attribute" | "nodeType" | "nodeTest";

export type AxisType = "parent" | "self" | "child" | "other";

export type NodeTypeTest = "text" | "comment" | "node" | "processing-instruction" | "unknown";

export interface CompiledStep {
    readonly type: StepType
    readonly isDescendant: boolean
    readonly name?: string
    readonly axis: AxisType
    readonly nodeType: NodeTypeTest
    readonly predicates: CompiledExpr[]
}

export interface CompiledFilterExpr {
    readonly path: string
    readonly predicates: CompiledExpr[]
    readonly trailingPath?: CompiledStep[]
    readonly trailingIsDescendant: boolean
}

export type CompiledXPath =
    { readonly type: "path", readonly steps: CompiledStep[], readonly absolute: boolean } |
    { readonly type: "boolean", readonly expr: CompiledExpr } |
    { readonly type: "filter", readonly filter: CompiledFilterExpr };

const CACHE_SIZE = 256;
const cache = new Map<string, CompiledXPath>();

export function compileXPath(expression: string): CompiledXPath {
    let compiled = cache.get(expression);
    if (!compiled) {
        compiled = new Compiler(expression).compile();
        if (cache.size >= CACHE_SIZE) {
            cache.delete(cache.keys().next().value!);
        }
        cache.set(expression, compiled);
    }
    return compiled;
}

function isBacktrack(step: CompiledStep): boolean {
    return step.type === "dotdot" || (step.type === "axis" && step.axis === "parent");
}

class Compiler {
    private readonly parser: Parser;

    constructor(expression: string) {
        this.parser = new Parser(expression, tokenize(expression));
    }

    compile(): CompiledXPath {
        const orExpr = this.parser.parse();
        const single = orExpr.operands.length === 1 &&
            orExpr.operands[0].operands.length === 1 &&
            orExpr.operands[0].operands[0].operands.length === 1 &&
            orExpr.operands[0].operands[0].operands[0].operands.length === 1 ?
            orExpr.operands[0].operands[0].operands[0].operands[0] : undefined;
        if (single?.kind === "location") {
            return this.locationPath(single);
        } else if (single) {
            return this.filterExprAsXPath(single);
        }
        return {type: "boolean", expr: this.orExpr(orExpr)};
    }

    private locationPath(locationPath: LocationPathExpr): CompiledXPath {
        const relativePath = locationPath.relativePath;
        const steps = relativePath ?
            relativePath.steps.map((s, i) => this.step(s, i > 0 && relativePath.separators[i - 1] === "//")) :
            [];
        return {type: "path", steps: normalizeParentSteps(steps), absolute: locationPath.absolute === "/"};
    }

    private filterExprAsXPath(pathExpr: FilterPathExpr): CompiledXPath {
        if (pathExpr.kind === "bracketed") {
            return {
                type: "filter",
                filter: {
                    path: this.parser.text(pathExpr.expr!),
                    predicates: pathExpr.predicates.map(p => this.orExpr(p)),
                    trailingPath: pathExpr.trailingPath && this.relativeSteps(pathExpr.trailingPath),
                    trailingIsDescendant: pathExpr.trailingPath !== undefined && pathExpr.trailingSeparator === "//"
                }
            };
        }
        return {type: "boolean", expr: this.pathExpr(pathExpr)};
    }

    private step(step: Step, isDescendant: boolean): CompiledStep {
        const predicates = step.predicates.map(p => this.orExpr(p));
        const base = {isDescendant, axis: "other" as AxisType, nodeType: "unknown" as NodeTypeTest, predicates};
        switch (step.kind) {
            case "abbreviated":
                return {...base, type: step.dotdot ? "dotdot" : "dot"};
            case "attribute":
                return {...base, type: "attribute", name: step.name};
        }
        if (step.axis !== undefined && step.axis !== "child") {
            const axis: AxisType = step.axis === "parent" || step.axis === "self" ?
                step.axis : "other";
            return {...base, type: "axis", name: nodeTestName(step.nodeTest), axis,
                nodeType: step.nodeTest.nodeType as NodeTypeTest ?? "unknown"};
        }
        if (step.nodeTest.nodeType !== undefined) {
            return {...base, type: "nodeType", nodeType: step.nodeTest.nodeType as NodeTypeTest};
        }
        return {...base, type: "nodeTest", name: nodeTestName(step.nodeTest)};
    }

    private orExpr(orExpr: OrExpr): CompiledExpr {
        return orExpr.operands.map(a => this.andExpr(a))
            .reduce((left, right) => ({type: "or", left, right}));
    }

    private andExpr(andExpr: AndExpr): CompiledExpr {
        return andExpr.operands.map(e => this.equalityExpr(e))
            .reduce((left, right) => ({type: "and", left, right}));
    }

    private equalityExpr(equalityExpr: EqualityExpr): CompiledExpr {
        let result = this.relationalExpr(equalityExpr.operands[0]);
        for (let i = 1; i < equalityExpr.operands.length; i++) {
            const op = equalityExpr.operators[i - 1] as ComparisonOp;
            result = {type: "comparison", left: result, op, right: this.relationalExpr(equalityExpr.operands[i])};
        }
        return result;
    }

    private relationalExpr(relationalExpr: RelationalExpr): CompiledExpr {
        let result = this.pathExpr(relationalExpr.operands[0]);
        for (let i = 1; i < relationalExpr.operands.length; i++) {
            const op = relationalExpr.operators[i - 1] as ComparisonOp;
            result = {type: "comparison", left: result, op, right: this.pathExpr(relationalExpr.operands[i])};
        }
        return result;
    }

    private pathExpr(pathExpr: PathExpr): CompiledExpr {
        switch (pathExpr.kind) {
            case "location":
                if (pathExpr.absolute) {
                    return {type: "absolutePath", path: this.parser.text(pathExpr)};
                }
                return this.relativePathAsExpr(pathExpr.relativePath!);
            case "function":
                return this.functionCall(pathExpr.functionCall!);
            case "bracketed":
                return this.orExpr(pathExpr.expr!);
            case "literal":
                return {type: "string", value: stripQuotes(pathExpr.token!.text)};
            case "number": {
                const text = pathExpr.token!.text;
                const value = Number(text);
                return {type: "numeric", value};
            }
        }
    }

    private relativeSteps(relativePath: RelativeLocationPath): CompiledStep[] {
        return relativePath.steps.map((s, i) => this.step(s, i > 0 && relativePath.separators[i - 1] === "//"));
    }

    private relativePathAsExpr(relativePath: RelativeLocationPath): CompiledExpr {
        const step = relativePath.steps[0];
        if (relativePath.steps.length === 1 && step.predicates.length === 0 &&
            (step.kind !== "node" || step.axis === undefined)) {
            if (step.kind === "abbreviated") {
                return {type: step.dotdot ? "parent" : "self"};
            }
            if (step.kind === "attribute") {
                return {type: "attribute", name: step.name};
            }
            if (step.nodeTest.name !== undefined) {
                return {type: "child", name: step.nodeTest.name};
            }
        }
        return {type: "path", steps: [], pathSteps: this.relativeSteps(relativePath)};
    }

    private functionCall(functionCall: FunctionCall): CompiledExpr {
        if (!FUNCTIONS.has(functionCall.name)) {
            throw new Error(`Unsupported XPath function: ${functionCall.name}()`);
        }
        return {
            type: "function",
            functionType: functionCall.name as FunctionType,
            args: functionCall.args.map(a => this.orExpr(a))
        };
    }
}

function nodeTestName(nodeTest: NodeTest): string {
    return nodeTest.nodeType ?? nodeTest.name!;
}

function stripQuotes(s: string): string {
    if (s.length >= 2 && (s[0] === "'" || s[0] === "\"") && s[s.length - 1] === s[0]) {
        return s.substring(1, s.length - 1);
    }
    return s;
}

function normalizeParentSteps(steps: CompiledStep[]): CompiledStep[] {
    const isLeading = (index: number) => steps.slice(0, index).every(isBacktrack);
    if (!steps.some((s, i) => i > 0 && isBacktrack(s) && !isLeading(i))) {
        return steps;
    }
    const result: CompiledStep[] = [];
    let i = 0;
    while (i < steps.length) {
        if (!isBacktrack(steps[i]) || isLeading(i)) {
            result.push(steps[i++]);
            continue;
        }
        const parentStart = i;
        while (i < steps.length && isBacktrack(steps[i])) {
            i++;
        }
        const parentCount = i - parentStart;
        const predicateStart = parentStart - parentCount;
        if (predicateStart < 0 || predicateStart >= result.length) {
            continue;
        }
        const predicateSteps = result.splice(predicateStart, Math.min(parentCount, result.length - predicateStart));
        const children: CompiledExpr[] = predicateSteps.map(s => ({type: "child", name: s.name!}));
        const predicate: CompiledExpr = children.length === 1 ? children[0] : {type: "path", steps: children};
        if (predicateStart > 0 && predicateStart <= result.length) {
            const anchor = result[predicateStart - 1];
            result[predicateStart - 1] = {...anchor, predicates: [...anchor.predicates, predicate]};
        }
    }
    return result;
}
