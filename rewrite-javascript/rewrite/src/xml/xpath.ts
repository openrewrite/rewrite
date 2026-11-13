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
import {Cursor} from "../tree";
import {getValue, isAttribute, isDocument, isTag, isXml, Xml} from "./tree";
import {localName} from "./namespaces";
import {ComparisonOp, CompiledExpr, CompiledFilterExpr, CompiledStep, CompiledXPath, compileXPath} from "./xpath-compiler";

type Value = string | number | boolean | undefined;

export class XPathMatcher {
    private compiled?: CompiledXPath;

    constructor(readonly expression: string) {
    }

    matches(cursor: Cursor): boolean {
        const compiled = this.compiled ??= compileXPath(this.expression);
        switch (compiled.type) {
            case "path":
                return compiled.steps.length > 0 && new PathMatch(compiled.steps, compiled.absolute).matches(cursor);
            case "boolean":
                return matchBooleanExpr(compiled.expr, cursor);
            case "filter":
                return matchFilterExpr(compiled.filter, cursor);
        }
    }
}

function isTagCursor(cursor: Cursor | undefined): cursor is Cursor & { value: Xml.Tag } {
    return cursor !== undefined && isXml(cursor.value) && isTag(cursor.value);
}

function parentTagCursor(cursor: Cursor | undefined): Cursor | undefined {
    let parent = cursor?.parent;
    while (parent && !isTagCursor(parent)) {
        if (isXml(parent.value) && isDocument(parent.value)) {
            return undefined;
        }
        parent = parent.parent;
    }
    return parent;
}

function matchesElementName(pattern: string | undefined, name: string): boolean {
    return pattern === undefined || pattern === "*" || pattern === "node" || name === pattern;
}

function matchesName(pattern: string | undefined, name: string): boolean {
    return pattern === undefined || pattern === "*" || name === pattern;
}

class PathMatch {
    constructor(private readonly steps: CompiledStep[], private readonly absolute: boolean) {
    }

    matches(cursor: Cursor): boolean {
        const steps = this.steps;
        const value = isXml(cursor.value) ? cursor.value : undefined;
        const last = steps[steps.length - 1];

        if (last.type === "attribute") {
            if (!value || !isAttribute(value) || !matchesName(last.name, value.key.name)) {
                return false;
            }
            if (!last.predicates.every(p => evaluateExpr(p, undefined, value, cursor, 1, 1))) {
                return false;
            }
            const parent = cursor.parent;
            return isTagCursor(parent) && this.matchRemaining(parent, steps.length - 2);
        }

        if (last.type === "nodeType") {
            return this.matchNodeTypeTest(last, cursor, steps.length - 2);
        }

        if (last.type === "dotdot" || (last.type === "axis" && last.axis === "parent")) {
            return this.matchParentStepAsLast(last, cursor, steps.length - 2);
        }

        if (last.type === "dot" || (last.type === "axis" && last.axis === "self")) {
            if (!value || !isTag(value) || (last.type === "axis" && !matchesElementName(last.name, value.name))) {
                return false;
            }
            return this.matchRemaining(cursor, steps.length - 2);
        }

        if (!value || !isTag(value) || !matchStepAgainstTag(last, value, cursor)) {
            return false;
        }
        return this.matchRemaining(parentTagCursor(cursor), steps.length - 2);
    }

    private matchParentStepAsLast(step: CompiledStep, cursor: Cursor, previous: number): boolean {
        if (!isTagCursor(cursor)) {
            return false;
        }
        const tag = cursor.value;
        if (step.type === "axis" && !matchesElementName(step.name, tag.name)) {
            return false;
        }
        if (previous < 0) {
            return true;
        }
        const previousStep = this.steps[previous];
        if (previousStep.type === "nodeTest" && previousStep.name !== undefined &&
            !findChildTag(tag, previousStep.name)) {
            return false;
        }
        return this.matchRemaining(cursor, previous - 1);
    }

    private matchParentStepInMiddle(cursor: Cursor | undefined, previous: number): boolean {
        if (!isTagCursor(cursor)) {
            return false;
        }
        if (previous < 0) {
            if (this.absolute && !this.steps[0].isDescendant) {
                return !isTagCursor(parentTagCursor(cursor));
            }
            return true;
        }
        const previousStep = this.steps[previous];
        if (previousStep.type === "nodeTest" && previousStep.name !== undefined &&
            !findChildTag(cursor.value, previousStep.name)) {
            return false;
        }
        return this.matchRemaining(cursor, previous - 1);
    }

    private matchNodeTypeTest(step: CompiledStep, cursor: Cursor, next: number): boolean {
        const value = cursor.value;
        switch (step.nodeType) {
            case "text":
                if (isXml(value) && value.kind === Xml.Kind.CharData) {
                    return this.matchRemaining(parentTagCursor(cursor), next);
                }
                if (isXml(value) && isTag(value) && getValue(value) !== undefined) {
                    return this.matchRemaining(cursor, next);
                }
                return false;
            case "comment":
                return isXml(value) && value.kind === Xml.Kind.Comment &&
                    this.matchRemaining(parentTagCursor(cursor), next);
            case "node":
                return this.matchRemaining(parentTagCursor(cursor), next);
            case "processing-instruction":
                return isXml(value) && value.kind === Xml.Kind.ProcessingInstruction &&
                    this.matchRemaining(parentTagCursor(cursor), next);
            default:
                return false;
        }
    }

    private matchRemaining(cursor: Cursor | undefined, index: number): boolean {
        if (index < 0) {
            return !this.absolute || !isTagCursor(cursor);
        }

        const step = this.steps[index];
        switch (step.type) {
            case "dot":
                return this.matchRemaining(cursor, index - 1);
            case "dotdot":
                return this.matchParentStepInMiddle(cursor, index - 1);
            case "axis":
                return this.matchAxisStep(step, cursor, index);
            case "nodeType":
                return step.nodeType === "node" && this.matchRemaining(cursor, index - 1);
        }

        if (this.steps[index + 1].isDescendant) {
            for (let pos = cursor; isTagCursor(pos); pos = parentTagCursor(pos)) {
                if (matchStepAgainstTag(step, pos.value, pos) && this.matchRemaining(parentTagCursor(pos), index - 1)) {
                    return true;
                }
            }
            return false;
        }
        return isTagCursor(cursor) && matchStepAgainstTag(step, cursor.value, cursor) &&
            this.matchRemaining(parentTagCursor(cursor), index - 1);
    }

    private matchAxisStep(step: CompiledStep, cursor: Cursor | undefined, index: number): boolean {
        switch (step.axis) {
            case "parent":
                if (isTagCursor(cursor) && !matchesElementName(step.name, cursor.value.name)) {
                    return false;
                }
                return this.matchParentStepInMiddle(cursor, index - 1);
            case "self":
                if (isTagCursor(cursor) && !matchesElementName(step.name, cursor.value.name)) {
                    return false;
                }
                return this.matchRemaining(cursor, index - 1);
            case "child":
                return isTagCursor(cursor) && matchStepAgainstTag(step, cursor.value, cursor) &&
                    this.matchRemaining(parentTagCursor(cursor), index - 1);
            default:
                return false;
        }
    }
}

function matchStepAgainstTag(step: CompiledStep, tag: Xml.Tag, cursor: Cursor): boolean {
    switch (step.type) {
        case "nodeTest":
            return matchesName(step.name, tag.name) &&
                (step.predicates.length === 0 || evaluatePredicates(step.predicates, tag, cursor));
        case "dot":
            return true;
        case "nodeType":
            return step.nodeType === "node";
        default:
            return false;
    }
}

function evaluatePredicates(predicates: CompiledExpr[], tag: Xml.Tag, cursor: Cursor): boolean {
    let position = 1;
    let size = 1;
    const parent = cursor.parent;
    if (isTagCursor(parent) && parent.value.content) {
        let count = 0;
        for (const c of parent.value.content) {
            if (isTag(c) && c.name === tag.name) {
                count++;
                if (c === tag) {
                    position = count;
                }
            }
        }
        size = count > 0 ? count : 1;
    }
    return predicates.every(p => evaluateExpr(p, tag, undefined, cursor, position, size));
}

function evaluateExpr(expr: CompiledExpr, tag: Xml.Tag | undefined, attr: Xml.Attribute | undefined,
                      cursor: Cursor, position: number, size: number): boolean {
    switch (expr.type) {
        case "numeric":
            return position === expr.value;
        case "and":
            return evaluateExpr(expr.left, tag, attr, cursor, position, size) &&
                evaluateExpr(expr.right, tag, attr, cursor, position, size);
        case "or":
            return evaluateExpr(expr.left, tag, attr, cursor, position, size) ||
                evaluateExpr(expr.right, tag, attr, cursor, position, size);
        case "comparison": {
            const left = resolveValue(expr.left, tag, attr, cursor, position, size);
            const right = resolveValue(expr.right, tag, attr, cursor, position, size);
            return left !== undefined && right !== undefined && compareValues(left, right, expr.op);
        }
        case "function":
            return evaluateFunction(expr, tag, attr, cursor, position, size);
        case "child":
            return tag !== undefined && findChildTag(tag, expr.name) !== undefined;
        case "path":
            return tag !== undefined && pathExists(tag, expr.steps);
        case "attribute":
            return tag !== undefined && tag.attributes.some(a => matchesName(expr.name, a.key.name));
        default:
            return false;
    }
}

function resolveValue(expr: CompiledExpr, tag: Xml.Tag | undefined, attr: Xml.Attribute | undefined,
                      cursor: Cursor, position: number, size: number): Value {
    switch (expr.type) {
        case "string":
        case "numeric":
            return expr.value;
        case "child": {
            const child = tag && findChildTag(tag, expr.name);
            return child && (getValue(child) ?? "");
        }
        case "attribute":
            if (tag) {
                return attributeValue(tag, expr.name);
            }
            if (attr) {
                if (expr.name === "*") {
                    return attr.value.value;
                }
                const parent = cursor.parent;
                if (isTagCursor(parent)) {
                    return attributeValue(parent.value, expr.name);
                }
            }
            return undefined;
        case "path":
            return tag && resolvePathValue(expr, tag);
        case "absolutePath": {
            const root = rootTag(cursor);
            const first = root && [...findTagsByPath(root, expr.path).values()][0];
            return first ? getValue(first) ?? "" : "";
        }
        case "function":
            return resolveFunctionValue(expr, tag, attr, cursor, position, size);
        default:
            return undefined;
    }
}

function evaluateFunction(expr: CompiledExpr & { type: "function" }, tag: Xml.Tag | undefined,
                          attr: Xml.Attribute | undefined, cursor: Cursor, position: number, size: number): boolean {
    switch (expr.functionType) {
        case "position":
            return position > 0;
        case "last":
            return position === size;
        case "not":
            return expr.args.length > 0 && !evaluateExpr(expr.args[0], tag, attr, cursor, position, size);
        case "contains":
        case "starts-with":
        case "ends-with":
            return resolveFunctionValue(expr, tag, attr, cursor, position, size) === true;
        case "string-length":
        case "count": {
            const result = resolveFunctionValue(expr, tag, attr, cursor, position, size);
            return typeof result === "number" && result > 0;
        }
        case "text": {
            const value = tag && getValue(tag);
            return value !== undefined && javaTrim(value) !== "";
        }
        case "local-name":
        case "namespace-uri":
            return true;
        default:
            return false;
    }
}

function resolveFunctionValue(expr: CompiledExpr & { type: "function" }, tag: Xml.Tag | undefined,
                              attr: Xml.Attribute | undefined, cursor: Cursor, position: number, size: number): Value {
    const args = expr.args;
    const stringArg = (i: number): string | undefined => {
        const value = resolveValue(args[i], tag, attr, cursor, position, size);
        return value === undefined ? undefined : String(value);
    };
    switch (expr.functionType) {
        case "position":
            return position;
        case "last":
            return size;
        case "local-name": {
            const target = args.length > 0 && args[0].type === "parent" ? parentTagCursor(cursor)?.value as Xml.Tag | undefined : tag;
            if (target) {
                return localName(target.name);
            }
            return attr && args.length === 0 ? localName(attr.key.name) : undefined;
        }
        case "namespace-uri": {
            let target = tag;
            let targetCursor = cursor;
            if (args.length > 0 && args[0].type === "parent") {
                const parent = parentTagCursor(cursor);
                if (!isTagCursor(parent)) {
                    return undefined;
                }
                target = parent.value;
                targetCursor = parent;
            }
            if (target) {
                return findNamespaceUri(namespacePrefixOf(target.name), targetCursor);
            }
            if (attr && args.length === 0) {
                const name = attr.key.name;
                return name.includes(":") ? findNamespaceUri(namespacePrefixOf(name), cursor) : "";
            }
            return undefined;
        }
        case "text":
            return tag && (getValue(tag) ?? "");
        case "string-length":
            return args.length > 0 ? stringArg(0)?.length ?? 0 : 0;
        case "substring-before":
        case "substring-after": {
            if (args.length < 2) {
                return "";
            }
            const str = stringArg(0);
            const delimiter = stringArg(1);
            if (str === undefined || delimiter === undefined) {
                return "";
            }
            const index = str.indexOf(delimiter);
            if (index < 0) {
                return "";
            }
            return expr.functionType === "substring-before" ? str.substring(0, index) : str.substring(index + delimiter.length);
        }
        case "count": {
            const path = args[0];
            if (path?.type === "absolutePath") {
                const root = rootTag(cursor);
                if (root) {
                    return findTagsByPath(root, path.path).size;
                }
            }
            return 0;
        }
        case "contains":
        case "starts-with":
        case "ends-with": {
            if (args.length < 2) {
                return false;
            }
            const str = stringArg(0);
            const other = stringArg(1);
            if (str === undefined || other === undefined) {
                return false;
            }
            return expr.functionType === "contains" ? str.includes(other) :
                expr.functionType === "starts-with" ? str.startsWith(other) : str.endsWith(other);
        }
        default:
            return undefined;
    }
}

function namespacePrefixOf(name: string): string {
    const colon = name.indexOf(":");
    return colon >= 0 ? name.substring(0, colon) : "";
}

function findNamespaceUri(prefix: string, cursor: Cursor): string | undefined {
    const declaration = prefix === "" ? "xmlns" : `xmlns:${prefix}`;
    for (let c: Cursor | undefined = cursor; c; c = c.parent) {
        if (isTagCursor(c)) {
            const attribute = c.value.attributes.find((a: Xml.Attribute) => a.key.name === declaration);
            if (attribute) {
                return attribute.value.value;
            }
        }
    }
    return prefix === "" ? "" : undefined;
}

function attributeValue(tag: Xml.Tag, name: string): string | undefined {
    return tag.attributes.find(a => matchesName(name, a.key.name))?.value.value;
}

function findChildTag(parent: Xml.Tag, name: string): Xml.Tag | undefined {
    return parent.content?.find((c): c is Xml.Tag => isTag(c) && matchesName(name, c.name));
}

function pathExists(tag: Xml.Tag, steps: CompiledExpr[]): boolean {
    let current: Xml.Tag | undefined = tag;
    for (const step of steps) {
        current = step.type === "child" ? findChildTag(current, step.name) : undefined;
        if (!current) {
            return false;
        }
    }
    return true;
}

function resolvePathValue(expr: CompiledExpr & { type: "path" }, tag: Xml.Tag): string | undefined {
    if (expr.steps.length === 0 && expr.terminalFunction !== "text") {
        return undefined;
    }
    let current: Xml.Tag | undefined = tag;
    for (const step of expr.steps) {
        current = step.type === "child" ? findChildTag(current, step.name) : undefined;
        if (!current) {
            return undefined;
        }
    }
    return getValue(current) ?? "";
}

function compareValues(left: string | number | boolean, right: string | number | boolean, op: ComparisonOp): boolean {
    if (typeof left === "number" && typeof right === "number") {
        return compareOrdered(left, right, op);
    }
    if (typeof left === "boolean" && typeof right === "boolean") {
        return op === "=" ? left === right : op === "!=" ? left !== right : false;
    }
    const l = String(left);
    const r = String(right);
    if (op === "=") {
        return l === r;
    } else if (op === "!=") {
        return l !== r;
    }
    const ln = parseJavaDouble(l);
    const rn = parseJavaDouble(r);
    if (ln !== undefined && rn !== undefined) {
        return compareOrdered(ln, rn, op);
    }
    return compareOrdered(l < r ? -1 : l > r ? 1 : 0, 0, op);
}

function compareOrdered(left: number, right: number, op: ComparisonOp): boolean {
    switch (op) {
        case "=":
            return left === right;
        case "!=":
            return left !== right;
        case "<":
            return left < right;
        case "<=":
            return left <= right;
        case ">":
            return left > right;
        case ">=":
            return left >= right;
    }
}

const JAVA_DOUBLE = /^[+-]?(NaN|Infinity|(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?[fFdD]?)$/;
const JAVA_HEX_DOUBLE = /^([+-]?)0[xX]([\da-fA-F]+(?:\.[\da-fA-F]*)?|\.[\da-fA-F]+)[pP]([+-]?\d+)[fFdD]?$/;

function parseJavaDouble(s: string): number | undefined {
    const trimmed = javaTrim(s);
    const hex = JAVA_HEX_DOUBLE.exec(trimmed);
    if (hex) {
        const sign = hex[1] === "-" ? -1 : 1;
        const [whole, fraction = ""] = hex[2].split(".");
        const bits = (whole + fraction).split("")
            .map(digit => parseInt(digit, 16).toString(2).padStart(4, "0")).join("").replace(/^0+/, "");
        if (bits.length === 0) {
            return sign * 0;
        }
        const exponent = Number(hex[3]) - 4 * fraction.length + bits.length - 1;
        if (exponent > 1023) {
            return sign * Infinity;
        }
        if (exponent < -1075) {
            return sign * 0;
        }
        const precision = Math.min(53, exponent + 1075);
        let significand = parseInt(bits.substring(0, precision).padEnd(precision, "0"), 2) || 0;
        if (bits[precision] === "1" && (significand % 2 !== 0 || bits.substring(precision + 1).includes("1"))) {
            significand++;
        }
        return sign * significand * Math.pow(2, exponent - precision + 1);
    }
    if (!JAVA_DOUBLE.test(trimmed)) {
        return undefined;
    }
    return Number(trimmed.replace(/[fFdD]$/, "").replace(/^\+/, ""));
}

function javaTrim(s: string): string {
    let start = 0;
    let end = s.length;
    while (start < end && s.charCodeAt(start) <= 0x20) {
        start++;
    }
    while (end > start && s.charCodeAt(end - 1) <= 0x20) {
        end--;
    }
    return s.substring(start, end);
}

function matchBooleanExpr(expr: CompiledExpr, cursor: Cursor): boolean {
    if (!isTagCursor(cursor)) {
        return false;
    }
    if (hasRelativePath(expr) || !hasPureAbsolutePath(expr)) {
        return evaluateExpr(expr, cursor.value, undefined, cursor, 1, 1);
    }
    let parent = cursor.parent;
    while (parent && !isTagCursor(parent) && !(isXml(parent.value) && isDocument(parent.value))) {
        parent = parent.parent;
    }
    if (!parent || !isXml(parent.value) || !isDocument(parent.value)) {
        return false;
    }
    return evaluateExpr(expr, cursor.value, undefined, cursor, 1, 1);
}

function hasRelativePath(expr: CompiledExpr): boolean {
    switch (expr.type) {
        case "child":
        case "path":
            return true;
        case "comparison":
        case "and":
        case "or":
            return hasRelativePath(expr.left) || hasRelativePath(expr.right);
        case "function":
            return expr.args.some(hasRelativePath);
        default:
            return false;
    }
}

function hasPureAbsolutePath(expr: CompiledExpr): boolean {
    switch (expr.type) {
        case "absolutePath":
            return expr.path.startsWith("/") && !expr.path.startsWith("//");
        case "comparison":
        case "and":
        case "or": {
            const leftPure = !hasAnyAbsolutePath(expr.left) || hasPureAbsolutePath(expr.left);
            const rightPure = !hasAnyAbsolutePath(expr.right) || hasPureAbsolutePath(expr.right);
            return (hasAnyAbsolutePath(expr.left) || hasAnyAbsolutePath(expr.right)) && leftPure && rightPure;
        }
        case "function": {
            const absolute = expr.args.filter(hasAnyAbsolutePath);
            return absolute.length > 0 && absolute.every(hasPureAbsolutePath);
        }
        default:
            return false;
    }
}

function hasAnyAbsolutePath(expr: CompiledExpr): boolean {
    switch (expr.type) {
        case "absolutePath":
            return true;
        case "comparison":
        case "and":
        case "or":
            return hasAnyAbsolutePath(expr.left) || hasAnyAbsolutePath(expr.right);
        case "function":
            return expr.args.some(hasAnyAbsolutePath);
        default:
            return false;
    }
}

function matchFilterExpr(filter: CompiledFilterExpr, cursor: Cursor): boolean {
    const root = rootTag(cursor);
    if (!root) {
        return false;
    }
    const all = [...findTagsByPath(root, filter.path).values()];
    const filtered = all.filter((tag, i) => filter.predicates.every(p => evaluateFilterPredicate(p, tag, i + 1, all.length)));
    if (filtered.length === 0 || !isTagCursor(cursor)) {
        return false;
    }
    const id = cursor.value.id;
    if (filter.trailingPath === undefined) {
        return filtered.some(t => t.id === id);
    }
    return filtered.some(t => (filter.trailingIsDescendant ?
        findDescendants(t, filter.trailingPath!) : findChildrenByPath(t, filter.trailingPath!)).has(id));
}

function evaluateFilterPredicate(expr: CompiledExpr, tag: Xml.Tag, position: number, size: number): boolean {
    switch (expr.type) {
        case "numeric":
            return position === expr.value;
        case "function":
            return expr.functionType === "last" ? position === size : expr.functionType === "position" && position > 0;
        case "comparison": {
            const left = resolveFilterValue(expr.left, tag, position, size);
            const right = resolveFilterValue(expr.right, tag, position, size);
            return left !== undefined && right !== undefined && compareValues(left, right, expr.op);
        }
        default:
            return true;
    }
}

function resolveFilterValue(expr: CompiledExpr, tag: Xml.Tag, position: number, size: number): Value {
    switch (expr.type) {
        case "string":
        case "numeric":
            return expr.value;
        case "function":
            return expr.functionType === "position" ? position :
                expr.functionType === "last" ? size :
                    expr.functionType === "local-name" ? localName(tag.name) : undefined;
        default:
            return undefined;
    }
}

function rootTag(cursor: Cursor): Xml.Tag | undefined {
    let c = cursor;
    while (c.parent && !(isXml(c.parent.value) && isDocument(c.parent.value))) {
        c = c.parent;
    }
    return isTagCursor(c) ? c.value : undefined;
}

type Tags = Map<string, Xml.Tag>;

function findTagsByPath(start: Xml.Tag, path: string): Tags {
    if (path.startsWith("//")) {
        let name = path.substring(2);
        if (name.includes("/")) {
            name = name.substring(0, name.indexOf("/"));
        }
        return findDescendants(start, name);
    }
    const absolute = path.startsWith("/");
    const steps = javaSplit(absolute ? path.substring(1) : path);
    if (steps.length === 0) {
        return new Map();
    }
    if (!absolute) {
        return followChildren(new Map([[start.id, start]]), steps);
    }
    if (steps[0] !== "*" && start.name !== steps[0]) {
        return new Map();
    }
    return followChildren(new Map([[start.id, start]]), steps.slice(1));
}

function findChildrenByPath(parent: Xml.Tag, path: string): Tags {
    return followChildren(new Map([[parent.id, parent]]), javaSplit(path));
}

function followChildren(start: Tags, steps: string[]): Tags {
    let current = start;
    for (const step of steps) {
        const next: Tags = new Map();
        for (const tag of current.values()) {
            for (const child of tag.content ?? []) {
                if (isTag(child) && (step === "*" || child.name === step)) {
                    next.set(child.id, child);
                }
            }
        }
        current = next;
    }
    return current;
}

function findDescendants(tag: Xml.Tag, name: string, result: Tags = new Map()): Tags {
    if (name === "*" || tag.name === name) {
        result.set(tag.id, tag);
    }
    for (const child of tag.content ?? []) {
        if (isTag(child)) {
            findDescendants(child, name, result);
        }
    }
    return result;
}

function javaSplit(s: string): string[] {
    if (!s.includes("/")) {
        return [s];
    }
    const parts = s.split("/");
    while (parts.length > 0 && parts[parts.length - 1] === "") {
        parts.pop();
    }
    return parts;
}
