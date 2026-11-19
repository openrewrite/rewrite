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
import {Cursor, isTree, Tree} from "../tree";
import {getMemberKeyName, Json} from "./tree";

/**
 * Matches a cursor location against a JsonPath expression, such as `$.sap.ui5.dependencies.libs.*`.
 *
 * A port of `org.openrewrite.json.JsonPathMatcher`, supporting the same subset of JsonPath with the same results:
 * `$`, `.key`, `['key']`, `*`, `..`, array indexes and slices, and `?()` filters using `==`, `!=`, `=~`,
 * `contains`, `!`, `&&` and `||`.
 */
export class JsonPathMatcher {
    private readonly jsonPath: string;
    private parsed?: JsonPathNode;

    constructor(jsonPath: string) {
        this.jsonPath = jsonPath.endsWith(".") && jsonPath.length > 1 ?
            jsonPath.substring(0, jsonPath.length - 1) :
            jsonPath;
    }

    /**
     * What the expression selects, evaluated along the path from the document root to the cursor: a
     * `Json` element, an array of them, or `undefined` when nothing matches.
     *
     * @throws Error when the expression is not valid JsonPath
     */
    find<T = unknown>(cursor: Cursor): T | undefined {
        return this.find0(cursor, resolvedAncestors(cursor)) as T | undefined;
    }

    /**
     * Whether the element at the cursor is one that the expression selects.
     *
     * @throws Error when the expression is not valid JsonPath
     */
    matches(cursor: Cursor): boolean {
        const result = this.find0(cursor, resolvedAncestors(cursor));
        if (result === undefined) {
            return false;
        }
        return Array.isArray(result) ?
            result.some(r => javaEquals(r, cursor.value)) :
            javaEquals(result, cursor.value);
    }

    private find0(cursor: Cursor, cursorPath: Tree[]): unknown {
        if (cursorPath.length === 0) {
            return undefined;
        }
        const start = this.jsonPath.startsWith(".") && !this.jsonPath.startsWith("..") ?
            cursor.value :
            cursorPath[0];
        const parsed = this.parse();
        return new Evaluator(cursorPath, start, false).visit(parsed) ?? undefined;
    }

    private parse(): JsonPathNode {
        if (!this.parsed) {
            this.parsed = new JsonPathParser(this.jsonPath).parse();
        }
        return this.parsed;
    }
}

function resolvedAncestors(cursor: Cursor): Tree[] {
    const path: Tree[] = [];
    for (let c: Cursor | undefined = cursor; c; c = c.parent) {
        if (isTree(c.value)) {
            path.unshift(c.value);
        }
    }
    return path;
}

function javaEquals(a: any, b: any): boolean {
    if (a === b) {
        return true;
    }
    if (Array.isArray(a) && Array.isArray(b)) {
        return a.length === b.length && a.every((e, i) => javaEquals(e, b[i]));
    }
    return isTree(a) && isTree(b) && a.kind === b.kind && a.id === b.id;
}

interface JsonPathNode {
    kind: "jsonPath";
    root: boolean;
    startsWithBracket: boolean;
    expressions: ExpressionNode[];
}

interface ExpressionNode {
    kind: "expression";
    child: DotOperatorNode | RecursiveDescentNode | BracketNode;
}

interface DotOperatorNode {
    kind: "dotOperator";
    child: BracketNode | PropertyNode | WildcardNode;
}

interface RecursiveDescentNode {
    kind: "recursiveDescent";
    first: boolean;
    child: DotOperatorNode;
}

interface BracketNode {
    kind: "bracket";
    properties: PropertyNode[];
    slice?: SliceNode;
    indexes?: string[];
    filter?: FilterNode;
}

interface PropertyNode {
    kind: "property";
    name: string;
}

interface WildcardNode {
    kind: "wildcard";
}

interface SliceNode {
    kind: "slice";
    first?: number;
    last?: number;
    start?: number;
    end?: number;
}

interface FilterNode {
    kind: "filter";
    expressions: FilterExpressionNode[];
}

interface FilterExpressionNode {
    kind: "filterExpression";
    child: BinaryNode | RegexNode | ContainsNode | UnaryNode | NegationNode;
}

type Operand = BinaryNode | RegexNode | ContainsNode | NegationNode | UnaryNode | LiteralNode;

interface BinaryNode {
    kind: "binary";
    operator: "&&" | "||" | "==" | "!=";
    left: Operand;
    right: Operand;
}

interface RegexNode {
    kind: "regex";
    unary: UnaryNode;
    pattern: RegExp;
}

interface ContainsNode {
    kind: "contains";
    unaryFirst: boolean;
    unary: UnaryNode;
    literal: LiteralNode;
}

interface NegationNode {
    kind: "negation";
    filterExpression?: FilterExpressionNode;
    unary?: UnaryNode;
}

interface UnaryNode {
    kind: "unary";
    at: boolean;
    name?: string;
    jsonPath?: JsonPathNode;
    /** The text of the expression's third token, which Java's `contains` takes as the key to look up. */
    thirdToken?: string;
    lastToken: string;
}

interface LiteralNode {
    kind: "literal";
    value: string | boolean | null | JsonPathNumber;
}

type PathNode = JsonPathNode | ExpressionNode | DotOperatorNode | RecursiveDescentNode | BracketNode |
    PropertyNode | WildcardNode | SliceNode | FilterNode | FilterExpressionNode | Operand;

const DECIMAL = /^([+-]?)(\d*)(?:\.(\d*))?(?:[eE]([+-]?\d+))?$/;

class JsonPathNumber {
    private readonly canonical: string;

    constructor(readonly text: string) {
        const [, sign, int, fraction = "", exponent = "0"] = DECIMAL.exec(text)!;
        const digits = (int + fraction).replace(/^0+/, "");
        const significant = digits.replace(/0+$/, "");
        this.canonical = significant.length === 0 ? "0" :
            `${sign === "-" ? "-" : ""}${significant}e${Number(exponent) - fraction.length + digits.length - significant.length}`;
    }

    equals(other: JsonPathNumber): boolean {
        return this.canonical === other.canonical;
    }

    toString(): string {
        return this.text;
    }
}

function literalValue(literal: Json.Literal): unknown {
    if (typeof literal.value === "number" && Number.isFinite(literal.value) &&
        /\d/.test(literal.source) && DECIMAL.test(literal.source)) {
        return new JsonPathNumber(literal.source);
    }
    return literal.value ?? null;
}

function is<K extends keyof typeof Json.Kind>(o: any, kind: K): boolean {
    return o != null && typeof o === "object" && !Array.isArray(o) && o.kind === Json.Kind[kind];
}

function membersOf(object: Json.Object): Json[] {
    return object.members.map(m => m.element);
}

function valuesOf(array: Json.Array): Json[] {
    return array.values.map(v => v.element);
}

function keyOf(member: Json.Member): string | undefined {
    const key = getMemberKeyName(member);
    return key === undefined ? undefined : escapeUnpairedSurrogates(key);
}

function flatten(results: any[]): any[] {
    return results.flatMap(r => Array.isArray(r) ? r : [r]);
}

function getResultFromList(results: any): any {
    if (Array.isArray(results)) {
        if (results.length === 0) {
            return undefined;
        } else if (results.length === 1) {
            return results[0];
        }
    }
    return results;
}

function collectDescendants(node: any, descendants: any[]): void {
    if (Array.isArray(node)) {
        for (const o of node) {
            collectDescendants(o, descendants);
        }
        return;
    }
    for (const child of childrenOf(node)) {
        descendants.push(child);
        collectDescendants(child, descendants);
    }
}

function childrenOf(node: any): Json[] {
    if (is(node, "Member")) {
        return childrenOf((node as Json.Member).value);
    } else if (is(node, "Object")) {
        return membersOf(node);
    } else if (is(node, "Array")) {
        return valuesOf(node);
    }
    return [];
}

function notEmpty(result: any): boolean {
    return result != null && (!Array.isArray(result) || result.length > 0);
}

class Evaluator {
    constructor(private readonly cursorPath: Tree[],
                private scope: any,
                private readonly isRecursiveDescent: boolean) {
    }

    visit(node: PathNode): any {
        switch (node.kind) {
            case "jsonPath":
                return this.visitJsonPath(node);
            case "expression":
            case "dotOperator":
            case "filterExpression":
                return this.visitChildren([node.child]);
            case "filter":
                return this.visitChildren(node.expressions);
            case "recursiveDescent":
                return this.visitRecursiveDescent(node);
            case "bracket":
                return this.visitBracketOperator(node);
            case "property":
                return this.visitProperty(node);
            case "wildcard":
                return this.visitWildcard(node);
            case "slice":
                return this.visitSlice(node);
            case "binary":
                return this.visitBinaryExpression(node);
            case "regex":
                return this.visitRegexExpression(node);
            case "contains":
                return this.visitContainsExpression(node);
            case "negation":
                return this.visitNegationExpression(node);
            case "unary":
                return this.visitUnaryExpression(node);
            case "literal":
                return node.value;
        }
    }

    private visitChildren(children: PathNode[]): any {
        let result = this.scope;
        for (const child of children) {
            if (this.scope == null) {
                break;
            }
            result = this.scope = this.visit(child);
        }
        return result;
    }

    private visitJsonPath(node: JsonPathNode): any {
        if (node.root || node.startsWithBracket) {
            this.scope = this.cursorPath.find(t => is(t, "Object")) ??
                (this.cursorPath.find(t => is(t, "Document") && is((t as Json.Document).value, "Object")) as Json.Document | undefined)?.value;
        }
        return this.visitChildren(node.expressions);
    }

    private visitRecursiveDescent(node: RecursiveDescentNode): any {
        if (this.scope == null) {
            return undefined;
        }
        const wildcard = node.child.child.kind === "wildcard";
        if (node.first) {
            const results: any[] = [];
            for (const path of this.cursorPath) {
                const result = new Evaluator(this.cursorPath, path, false).visit(node.child);
                if (wildcard && Array.isArray(result)) {
                    results.push(...result);
                } else if (result != null) {
                    results.push(result);
                }
            }
            return results;
        } else if (wildcard) {
            const descendants: any[] = [];
            collectDescendants(this.scope, descendants);
            return getResultFromList(descendants);
        }
        return new Evaluator(this.cursorPath, this.scope, true).visit(node.child);
    }

    private visitBracketOperator(node: BracketNode): any {
        if (node.properties.length > 0) {
            if (node.properties.length === 1) {
                return this.visitProperty(node.properties[0]);
            }
            return node.properties.map(p => this.visitProperty(p));
        } else if (node.slice) {
            return this.visitSlice(node.slice);
        } else if (node.indexes) {
            return this.visitIndexes(node.indexes);
        } else if (node.filter) {
            return this.visit(node.filter);
        }
        return undefined;
    }

    private elementsInScope(): any[] | undefined {
        if (Array.isArray(this.scope)) {
            return this.scope;
        } else if (is(this.scope, "Array")) {
            return valuesOf(this.scope);
        } else if (is(this.scope, "Member")) {
            this.scope = this.scope.value;
            return this.elementsInScope();
        }
        return [];
    }

    private visitSlice(node: SliceNode): any {
        const results = this.elementsInScope()!;
        let start = 0;
        let limit = Infinity;
        if (node.first !== undefined) {
            limit = node.first;
        } else if (node.last !== undefined) {
            start = Math.max(0, results.length - node.last);
        } else if (node.start !== undefined) {
            start = node.start;
            limit = node.end !== undefined ? node.end + 1 : limit;
        }
        return results.slice(start, start + limit);
    }

    private visitIndexes(indexes: string[]): any {
        const results = this.elementsInScope()!;
        const selected: any[] = [];
        for (const index of indexes) {
            for (let i = 0; i < results.length; i++) {
                if (index === String(i)) {
                    selected.push(results[i]);
                }
            }
        }
        return getResultFromList(selected);
    }

    private mapScope(elements: any[], visit: () => any): any[] {
        return elements
            .map(o => {
                this.scope = o;
                return visit();
            })
            .filter(r => r != null);
    }

    private visitProperty(node: PropertyNode): any {
        const scope = this.scope;
        if (is(scope, "Member")) {
            const member = scope as Json.Member;
            const key = keyOf(member);
            if (this.isRecursiveDescent) {
                const matches: any[] = [];
                if (key === node.name) {
                    matches.push(member);
                }
                this.scope = member.value;
                const result = getResultFromList(this.visitProperty(node));
                if (result != null) {
                    matches.push(result);
                }
                return getResultFromList(matches);
            } else if (is(member.value, "Literal")) {
                return key === node.name ? member : undefined;
            }
            this.scope = member.value;
            return this.visitProperty(node);
        } else if (is(scope, "Object")) {
            const object = scope as Json.Object;
            if (this.isRecursiveDescent) {
                this.scope = membersOf(object);
                return getResultFromList(this.visitProperty(node));
            }
            for (const json of membersOf(object)) {
                if (is(json, "Member") && keyOf(json as Json.Member) === node.name) {
                    return json;
                }
            }
        } else if (is(scope, "Array")) {
            return getResultFromList(this.mapScope(valuesOf(scope), () => this.visitProperty(node)));
        } else if (Array.isArray(scope)) {
            return getResultFromList(flatten(this.mapScope(scope, () => this.visitProperty(node))));
        }
        return undefined;
    }

    private visitWildcard(node: WildcardNode): any {
        const scope = this.scope;
        if (is(scope, "Member")) {
            const value = (scope as Json.Member).value;
            if (is(value, "Object")) {
                return getResultFromList(membersOf(value as Json.Object));
            } else if (is(value, "Array")) {
                return getResultFromList(valuesOf(value as Json.Array));
            }
            return undefined;
        } else if (Array.isArray(scope)) {
            return getResultFromList(flatten(this.mapScope(scope, () => this.visitWildcard(node))));
        } else if (is(scope, "Object")) {
            return membersOf(scope);
        } else if (is(scope, "Array")) {
            return this.mapScope(valuesOf(scope), () => this.visitWildcard(node));
        }
        return undefined;
    }

    private visitUnaryExpression(node: UnaryNode): any {
        if (node.at) {
            const scope = this.scope;
            if (is(scope, "Literal")) {
                if (node.name === undefined) {
                    return scope;
                }
            } else if (is(scope, "Member")) {
                const member = scope as Json.Member;
                if (node.name !== undefined && keyOf(member) === node.name) {
                    return member;
                }
                this.scope = member.value;
                return getResultFromList(this.visitUnaryExpression(node));
            } else if (is(scope, "Object")) {
                for (const json of membersOf(scope)) {
                    if (is(json, "Member") && node.name !== undefined && keyOf(json as Json.Member) === node.name) {
                        return scope;
                    }
                }
            } else if (is(scope, "Array")) {
                this.scope = valuesOf(scope);
                return this.visitUnaryExpression(node);
            } else if (Array.isArray(scope)) {
                return getResultFromList(flatten(this.mapScope(scope, () => this.visitUnaryExpression(node))));
            }
        } else if (node.jsonPath) {
            return this.getResultByKey(this.visit(node.jsonPath), node.lastToken);
        }
        return undefined;
    }

    private visitRegexExpression(node: RegexNode): any {
        if (this.scope == null || Array.isArray(this.scope) && this.scope.length === 0) {
            return undefined;
        }
        const lhs = this.visitUnaryExpression(node.unary);
        if (Array.isArray(lhs)) {
            return lhs.filter(match => this.getOperatorResult(match, "=~", node.pattern) != null);
        }
        return this.getOperatorResult(lhs, "=~", node.pattern);
    }

    private visitContainsExpression(node: ContainsNode): any {
        const originalScope = this.scope;
        if (node.unaryFirst) {
            let lhs = this.visitUnaryExpression(node.unary);
            const rhs = node.literal.value;
            if (is(lhs, "Object") && rhs != null) {
                lhs = this.getResultByKey(lhs, node.unary.thirdToken);
                if (is(lhs, "Member")) {
                    const value = (lhs as Json.Member).value;
                    if (is(value, "Array")) {
                        if (valuesOf(value as Json.Array).some(o => is(o, "Literal") &&
                            String(literalValue(o as Json.Literal)).includes(String(rhs)))) {
                            return originalScope;
                        }
                    } else if (is(value, "Literal") && String(literalValue(value as Json.Literal)).includes(String(rhs))) {
                        return originalScope;
                    }
                }
            }
        } else {
            const lhs = node.literal.value;
            let rhs = this.visitUnaryExpression(node.unary);
            if (is(rhs, "Object") && lhs != null) {
                rhs = this.getResultByKey(rhs, node.unary.thirdToken);
                if (is(rhs, "Member")) {
                    const value = (rhs as Json.Member).value;
                    if (is(value, "Literal") && String(literalValue(value as Json.Literal)).includes(String(lhs))) {
                        return originalScope;
                    }
                }
            }
        }
        return undefined;
    }

    private visitNegationExpression(node: NegationNode): any {
        const originalScope = this.scope;
        const inner = () => node.filterExpression ?
            this.visit(node.filterExpression) :
            this.visitUnaryExpression(node.unary!);

        let elements: any[] | undefined;
        if (Array.isArray(originalScope)) {
            elements = originalScope;
        } else if (is(originalScope, "Array")) {
            elements = valuesOf(originalScope);
        } else if (is(originalScope, "Member") && is(originalScope.value, "Array")) {
            elements = valuesOf(originalScope.value);
        }

        if (elements) {
            const results: any[] = [];
            for (const element of elements) {
                this.scope = element;
                if (!notEmpty(inner())) {
                    results.push(element);
                }
            }
            this.scope = originalScope;
            return getResultFromList(results);
        }

        const matched = notEmpty(inner());
        this.scope = originalScope;
        return matched ? undefined : originalScope;
    }

    private visitBinaryExpression(node: BinaryNode): any {
        if (node.operator === "&&" || node.operator === "||") {
            const scopeOfLogicalOp = this.scope;
            const lhs = this.visit(node.left);
            this.scope = scopeOfLogicalOp;
            const rhs = this.visit(node.right);
            if (node.operator === "&&" && notEmpty(lhs) && notEmpty(rhs)) {
                if (!Array.isArray(lhs) && isTree(lhs)) {
                    return rhs;
                } else if (!Array.isArray(rhs) && isTree(rhs)) {
                    return lhs;
                }
                if (Array.isArray(lhs) && Array.isArray(rhs) && lhs.length !== rhs.length) {
                    return lhs.length < rhs.length ? lhs : rhs;
                }
                return scopeOfLogicalOp;
            } else if (node.operator === "||" && (notEmpty(lhs) || notEmpty(rhs))) {
                return scopeOfLogicalOp;
            }
            return undefined;
        }

        const originalScope = this.scope;
        const lhs = this.visit(node.left);
        const rhs = this.visit(node.right);
        if (Array.isArray(lhs)) {
            return lhs.filter(match => this.getOperatorResult(match, node.operator, rhs) != null);
        } else if (is(originalScope, "Member")) {
            if (this.getOperatorResult(lhs, node.operator, rhs) != null) {
                return originalScope;
            }
            return undefined;
        }
        return this.getOperatorResult(lhs, node.operator, rhs);
    }

    private getOperatorResult(lhs: any, operator: string, rhs: any): Json | undefined {
        if (is(lhs, "Member")) {
            const value = (lhs as Json.Member).value;
            if (is(value, "Literal") && this.checkObjectEquality(literalValue(value as Json.Literal), operator, rhs)) {
                return lhs;
            }
        } else if (is(lhs, "Object")) {
            for (const json of membersOf(lhs)) {
                if (is(json, "Member")) {
                    const value = (json as Json.Member).value;
                    if (is(value, "Literal") && this.checkObjectEquality(literalValue(value as Json.Literal), operator, rhs)) {
                        return lhs;
                    }
                }
            }
        } else if (is(lhs, "Literal")) {
            if (this.checkObjectEquality(literalValue(lhs), operator, rhs)) {
                return lhs;
            }
        }
        return undefined;
    }

    private checkObjectEquality(lhs: unknown, operator: string, rhs: any): boolean {
        switch (operator) {
            case "==":
                return valueEquals(lhs, rhs);
            case "!=":
                return !valueEquals(lhs, rhs);
            case "=~":
                return lhs != null && (rhs as RegExp).test(String(lhs));
        }
        return false;
    }

    private getResultByKey(result: any, key: string | undefined): any {
        if (is(result, "Object")) {
            for (const json of membersOf(result)) {
                if (is(json, "Member") && is((json as Json.Member).key.element, "Literal") &&
                    keyOf(json as Json.Member) === key) {
                    return json;
                }
            }
        } else if (is(result, "Member")) {
            const member = result as Json.Member;
            if (is(member.value, "Literal")) {
                return keyOf(member) === key ? member : undefined;
            }
        } else if (Array.isArray(result)) {
            for (const o of result) {
                const r = this.getResultByKey(o, key);
                if (r != null) {
                    return r;
                }
            }
        }
        return undefined;
    }

}

function valueEquals(lhs: unknown, rhs: unknown): boolean {
    if (lhs instanceof JsonPathNumber && rhs instanceof JsonPathNumber) {
        return lhs.equals(rhs);
    }
    return lhs === rhs;
}

type TokenType =
    "$" | "." | ".." | "*" | "[" | "]" | "(" | ")" | "@" | "?" | ":" | "!" |
    "logical" | "equality" | "regex" | "string" | "positive" | "negative" | "numeric" | "identifier" |
    "contains" | "true" | "false" | "null" | "eof";

interface Token {
    type: TokenType;
    text: string;
    offset: number;
}

const KEYWORDS = new Map<string, TokenType>([["contains", "contains"], ["true", "true"], ["false", "false"], ["null", "null"]]);

function tokenize(input: string): Token[] {
    const tokens: Token[] = [];
    const error = (offset: number) =>
        new Error(`Syntax error at offset ${offset} in JsonPath '${input}'`);
    let i = 0;
    while (i < input.length) {
        const c = input[i];
        const start = i;
        if (/[ \t\n\r\f﻿,]/.test(c)) {
            i++;
            continue;
        }
        const rest = input.substring(i);
        let match: RegExpExecArray | null;
        if ((match = /^=~[ \t]?'/.exec(rest))) {
            const close = input.indexOf("'", i + match[0].length);
            if (close < 0) {
                throw error(i);
            }
            tokens.push({type: "regex", text: input.substring(i + match[0].length, close).replace(/^[ \t]+/, ""), offset: start});
            i = close + 1;
        } else if (rest.startsWith("&&") || rest.startsWith("||")) {
            tokens.push({type: "logical", text: rest.substring(0, 2), offset: start});
            i += 2;
        } else if (rest.startsWith("==") || rest.startsWith("!=")) {
            tokens.push({type: "equality", text: rest.substring(0, 2), offset: start});
            i += 2;
        } else if (rest.startsWith("..")) {
            tokens.push({type: "..", text: "..", offset: start});
            i += 2;
        } else if ("$.*[]()@?:!".includes(c)) {
            tokens.push({type: c as TokenType, text: c, offset: start});
            i++;
        } else if (c === "'" || c === '"') {
            let j = i + 1;
            while (j < input.length && input[j] !== c) {
                j += input[j] === "\\" ? 2 : 1;
            }
            if (j >= input.length) {
                throw error(i);
            }
            tokens.push({type: "string", text: input.substring(i, j + 1), offset: start});
            i = j + 1;
        } else if ((match = /^(-?)\d+([eE][+-]?\d+)?/.exec(rest))) {
            const type: TokenType = match[2] ? "numeric" : match[1] ? "negative" : "positive";
            tokens.push({type, text: match[0], offset: start});
            i += match[0].length;
        } else if ((match = /^[_A-Za-z][_A-Za-z0-9-]*/.exec(rest))) {
            tokens.push({type: KEYWORDS.get(match[0]) ?? "identifier", text: match[0], offset: start});
            i += match[0].length;
        } else {
            throw error(i);
        }
    }
    tokens.push({type: "eof", text: "", offset: input.length});
    return tokens;
}

class JsonPathParser {
    private readonly tokens: Token[];
    private pos = 0;

    constructor(private readonly input: string) {
        this.tokens = tokenize(input);
    }

    parse(): JsonPathNode {
        const jsonPath = this.jsonPath();
        if (this.peek().type !== "eof") {
            throw this.error();
        }
        return jsonPath;
    }

    private peek(ahead = 0): Token {
        return this.tokens[Math.min(this.pos + ahead, this.tokens.length - 1)];
    }

    private next(): Token {
        return this.tokens[this.pos++];
    }

    private expect(type: TokenType): Token {
        if (this.peek().type !== type) {
            throw this.error();
        }
        return this.next();
    }

    private error(): Error {
        const token = this.peek();
        return new Error(token.type === "eof" ?
            `Syntax error: unexpected end of JsonPath '${this.input}'` :
            `Syntax error: unexpected '${token.text}' at offset ${token.offset} in JsonPath '${this.input}'`);
    }

    private jsonPath(): JsonPathNode {
        const root = this.peek().type === "$";
        if (root) {
            this.next();
        }
        const node: JsonPathNode = {
            kind: "jsonPath",
            root,
            startsWithBracket: !root && this.peek().type === "[",
            expressions: []
        };
        while (["." as TokenType, "..", "["].includes(this.peek().type)) {
            node.expressions.push(this.expression(node.expressions.length === 0));
        }
        if (!root && node.expressions.length === 0) {
            throw this.error();
        }
        return node;
    }

    private expression(first: boolean): ExpressionNode {
        const token = this.next();
        if (token.type === ".") {
            return {kind: "expression", child: this.dotOperator()};
        } else if (token.type === "..") {
            return {kind: "expression", child: {kind: "recursiveDescent", first, child: this.dotOperator()}};
        }
        this.pos--;
        return {kind: "expression", child: this.bracketOperator()};
    }

    private dotOperator(): DotOperatorNode {
        const token = this.peek();
        if (token.type === "[") {
            return {kind: "dotOperator", child: this.bracketOperator()};
        } else if (token.type === "*") {
            this.next();
            return {kind: "dotOperator", child: {kind: "wildcard"}};
        }
        return {kind: "dotOperator", child: this.property()};
    }

    private property(): PropertyNode {
        const token = this.next();
        if (token.type === "string") {
            return {kind: "property", name: decode(token.text)};
        } else if (token.type === "identifier" || KEYWORDS.has(token.text)) {
            return {kind: "property", name: token.text};
        }
        this.pos--;
        throw this.error();
    }

    private bracketOperator(): BracketNode {
        this.expect("[");
        const node: BracketNode = {kind: "bracket", properties: []};
        const token = this.peek();
        if (token.type === "?") {
            this.next();
            this.expect("(");
            const expressions: FilterExpressionNode[] = [];
            do {
                expressions.push(this.filterExpression());
            } while (this.peek().type !== ")");
            this.expect(")");
            node.filter = {kind: "filter", expressions};
        } else if (token.type === ":") {
            this.next();
            node.slice = {kind: "slice", first: Number(this.expect("positive").text)};
        } else if (token.type === "negative" && this.peek(1).type === ":") {
            this.pos += 2;
            node.slice = {kind: "slice", last: -Number(token.text)};
        } else if (token.type === "positive" && this.peek(1).type === ":") {
            this.pos += 2;
            const end = this.peek().type === "positive" ? Number(this.next().text) : undefined;
            node.slice = {kind: "slice", start: Number(token.text), end};
        } else if (token.type === "positive") {
            node.indexes = [];
            while (this.peek().type === "positive") {
                node.indexes.push(this.next().text);
            }
        } else if (token.type === "*") {
            this.next();
            node.slice = {kind: "slice"};
        } else {
            do {
                node.properties.push(this.property());
            } while (this.peek().type !== "]");
        }
        this.expect("]");
        return node;
    }

    private filterExpression(): FilterExpressionNode {
        let operand = this.operand();
        if (this.peek().type === "logical") {
            if (operand.kind === "unary") {
                throw this.error();
            }
            while (this.peek().type === "logical") {
                const operator = this.next().text as "&&" | "||";
                const right = this.operand();
                if (right.kind === "unary") {
                    throw this.error();
                }
                operand = {kind: "binary", operator, left: operand, right};
            }
        }
        return {kind: "filterExpression", child: operand};
    }

    private operand(): BinaryNode | RegexNode | ContainsNode | NegationNode | UnaryNode {
        const token = this.peek();
        if (token.type === "!") {
            this.next();
            if (this.peek().type === "(") {
                this.next();
                const filterExpression = this.filterExpression();
                this.expect(")");
                return {kind: "negation", filterExpression};
            }
            return {kind: "negation", unary: this.unary()};
        } else if (["string" as TokenType, "positive", "negative", "numeric", "true", "false", "null"].includes(token.type)) {
            const literal = this.literal();
            const operator = this.next();
            if (operator.type === "equality") {
                return {kind: "binary", operator: operator.text as "==" | "!=", left: literal, right: this.unary()};
            } else if (operator.type === "contains") {
                return {kind: "contains", unaryFirst: false, unary: this.unary(), literal};
            }
            this.pos--;
            throw this.error();
        }
        const unary = this.unary();
        const operator = this.peek();
        if (operator.type === "equality") {
            this.next();
            return {kind: "binary", operator: operator.text as "==" | "!=", left: unary, right: this.literal()};
        } else if (operator.type === "contains") {
            this.next();
            return {kind: "contains", unaryFirst: true, unary, literal: this.literal()};
        } else if (operator.type === "regex") {
            this.next();
            return {kind: "regex", unary, pattern: new RegExp(`^(?:${operator.text})$`)};
        }
        return unary;
    }

    private unary(): UnaryNode {
        const start = this.pos;
        let node: UnaryNode;
        if (this.peek().type === "@") {
            this.next();
            node = {kind: "unary", at: true, lastToken: ""};
            if (this.peek().type === "." && this.peek(1).type !== "[") {
                this.next();
                node.name = this.property().name;
            } else if (this.peek().type === "[" || this.peek().type === "." && this.peek(1).type === "[") {
                if (this.peek().type === ".") {
                    this.next();
                }
                this.expect("[");
                node.name = decode(this.expect("string").text);
                this.expect("]");
            }
            node.thirdToken = this.pos - start > 2 ? this.tokens[start + 2].text : undefined;
        } else {
            node = {kind: "unary", at: false, jsonPath: this.jsonPath(), lastToken: ""};
        }
        node.lastToken = this.tokens[this.pos - 1].text;
        return node;
    }

    private literal(): LiteralNode {
        const token = this.next();
        switch (token.type) {
            case "string":
                return {kind: "literal", value: decode(token.text)};
            case "positive":
            case "negative":
            case "numeric":
                return {kind: "literal", value: new JsonPathNumber(token.text)};
            case "true":
            case "false":
                return {kind: "literal", value: token.type === "true"};
            case "null":
                return {kind: "literal", value: null};
        }
        this.pos--;
        throw this.error();
    }
}

/**
 * The string that a single- or double-quoted JSON5 literal stands for. Mirrors
 * `org.openrewrite.json.internal.JsonString.decode`.
 */
function decode(literal: string): string {
    const body = literal.substring(1, literal.length - 1);
    if (!body.includes("\\")) {
        return body;
    }
    let out = "";
    for (let i = 0; i < body.length; i++) {
        const c = body[i];
        if (c !== "\\" || i + 1 === body.length) {
            out += c;
            continue;
        }
        const escaped = body[++i];
        switch (escaped) {
            case "b":
                out += "\b";
                break;
            case "f":
                out += "\f";
                break;
            case "n":
                out += "\n";
                break;
            case "r":
                out += "\r";
                break;
            case "t":
                out += "\t";
                break;
            case "v":
                out += "\v";
                break;
            case "0":
                out += "\0";
                break;
            case "x":
            case "u": {
                const digits = escaped === "x" ? 2 : 4;
                const hex = body.substring(i + 1, i + 1 + digits);
                if (hex.length === digits && /^[0-9a-fA-F]+$/.test(hex)) {
                    out += String.fromCharCode(parseInt(hex, 16));
                    i += digits;
                } else {
                    out += "\\" + escaped;
                }
                break;
            }
            case "\r":
                if (body[i + 1] === "\n") {
                    i++;
                }
                break;
            case "\n":
            case " ":
            case " ":
                break;
            default:
                out += escaped;
        }
    }
    return escapeUnpairedSurrogates(out);
}

/**
 * Keeps a surrogate with no partner as a lower-case `\uXXXX` escape, as `JsonString.decode` and the parser's
 * literal values do. `getMemberKeyName` decodes keys from source and so yields the bare surrogate.
 */
function escapeUnpairedSurrogates(value: string): string {
    return value.replace(/[\ud800-\udfff]/gu, surrogate => `\\u${surrogate.charCodeAt(0).toString(16)}`);
}
