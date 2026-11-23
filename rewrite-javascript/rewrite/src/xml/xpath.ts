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
import {getValue, isAttribute, isCharData, isDocument, isTag, isXml, Xml} from "./tree";
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

function matchesElementName(step: CompiledStep, name: string): boolean {
    return step.nodeType === "node" || step.nodeType === "unknown" && matchesName(step.name, name);
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
            if (!value || !isTag(value) || (last.type === "axis" && !matchesElementName(last, value.name))) {
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
        if (step.type === "axis" && !matchesElementName(step, tag.name)) {
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
                if (isTagCursor(cursor) && !matchesElementName(step, cursor.value.name)) {
                    return false;
                }
                return this.matchParentStepInMiddle(cursor, index - 1);
            case "self":
                if (isTagCursor(cursor) && !matchesElementName(step, cursor.value.name)) {
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
                (step.predicates.length === 0 || evaluatePredicates(step, tag, cursor));
        case "dot":
            return true;
        case "nodeType":
            return step.nodeType === "node";
        default:
            return false;
    }
}

function needsPosition(expr: CompiledExpr): boolean {
    switch (expr.type) {
        case "numeric":
            return true;
        case "function":
            return expr.functionType === "position" || expr.functionType === "last" || expr.args.some(needsPosition);
        case "comparison":
        case "and":
        case "or":
            return needsPosition(expr.left) || needsPosition(expr.right);
        default:
            return false;
    }
}

function evaluatePredicates(step: CompiledStep, tag: Xml.Tag, cursor: Cursor): boolean {
    if (!step.predicates.some(needsPosition)) {
        return step.predicates.every(p => evaluateExpr(p, tag, undefined, cursor, 1, 1));
    }
    const parent = cursor.parent;
    const candidates = isTagCursor(parent) ? (parent.value.content ?? [])
        .filter((c: Xml.Content): c is Xml.Tag => isTag(c) && matchesName(step.name, c.name))
        .map((c: Xml.Tag) => new Cursor(c, parent)) : [cursor];
    return filterCandidates(candidates, step.predicates).some(c => c.value === tag);
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
            return tag !== undefined && (expr.pathSteps ? selectSteps([cursor], expr.pathSteps).length > 0 : pathExists(tag, expr.steps));
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
        case "self":
            return tag ? stringValue(tag) : attr?.value.value;
        case "path": {
            if (expr.pathSteps) {
                const first = selectSteps([cursor], expr.pathSteps)[0];
                return first && stringValue(first.value);
            }
            return tag && resolvePathValue(expr.steps, tag);
        }
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
        return value === undefined ? undefined : functionStringValue(value);
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

function resolvePathValue(steps: CompiledExpr[], tag: Xml.Tag): string | undefined {
    if (steps.length === 0) {
        return undefined;
    }
    let current: Xml.Tag | undefined = tag;
    for (const step of steps) {
        current = step.type === "child" ? findChildTag(current, step.name) : undefined;
        if (!current) {
            return undefined;
        }
    }
    return getValue(current) ?? "";
}

function compareValues(left: string | number | boolean, right: string | number | boolean, op: ComparisonOp): boolean {
    if (typeof left === "number" || typeof right === "number" || op !== "=" && op !== "!=") {
        return compareOrdered(numberValue(left), numberValue(right), op);
    }
    return op === "=" ? String(left) === String(right) : String(left) !== String(right);
}

function numberValue(value: string | number | boolean): number {
    if (typeof value === "number") {
        return value;
    }
    const text = javaTrim(String(value));
    return /^-?(?:[0-9]+(?:\.[0-9]*)?|\.[0-9]+)$/.test(text) ? Number(text) : NaN;
}

function functionStringValue(value: string | number | boolean): string {
    const text = String(value);
    if (typeof value !== "number" || !Number.isFinite(value) || !text.includes("e")) {
        return text;
    }
    const [coefficient, exponent] = text.split("e");
    const sign = coefficient.startsWith("-") ? "-" : "";
    const unsigned = coefficient.substring(sign.length);
    const digits = unsigned.replace(".", "");
    const point = (unsigned.includes(".") ? unsigned.indexOf(".") : unsigned.length) + Number(exponent);
    if (point <= 0) {
        return `${sign}0.${"0".repeat(-point)}${digits}`;
    }
    if (point >= digits.length) {
        return sign + digits + "0".repeat(point - digits.length);
    }
    return `${sign}${digits.substring(0, point)}.${digits.substring(point)}`;
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
    let nodes = filterCandidates(findNodesByPath(root, filter.path), filter.predicates);
    if (filter.trailingPath) {
        if (filter.trailingIsDescendant) {
            nodes = nodes.flatMap(c => collectDescendants(c));
        }
        nodes = selectSteps(nodes, filter.trailingPath);
    }
    return nodes.some(c => c.value === cursor.value);
}

function rootTag(cursor: Cursor): Xml.Tag | undefined {
    let c = cursor;
    while (c.parent && !(isXml(c.parent.value) && isDocument(c.parent.value))) {
        c = c.parent;
    }
    return isTagCursor(c) ? c.value : undefined;
}

type Tags = Map<string, Xml.Tag>;

function findTagsByPath(root: Xml.Tag, path: string): Tags {
    return new Map(findNodesByPath(root, path).filter(isTagCursor).map(c => [c.value.id, c.value]));
}

function findNodesByPath(root: Xml.Tag, path: string): Cursor[] {
    if (compileXPath(path).type !== "path") {
        throw new Error(`Unsupported XPath expression: parenthesized expression must be a location path: ${path}`);
    }
    const matcher = new XPathMatcher(path);
    const matches: Cursor[] = [];
    for (const candidate of collectDescendants(new Cursor(root))) {
        if (matcher.matches(candidate)) {
            matches.push(candidate);
        }
        if (isTagCursor(candidate)) {
            for (const attr of candidate.value.attributes) {
                const attrCursor = new Cursor(attr, candidate);
                if (matcher.matches(attrCursor)) {
                    matches.push(attrCursor);
                }
            }
        }
    }
    return matches;
}

function collectDescendants(cursor: Cursor, result: Cursor[] = []): Cursor[] {
    result.push(cursor);
    if (isTagCursor(cursor)) {
        for (const child of cursor.value.content ?? []) {
            if (isTag(child)) {
                collectDescendants(new Cursor(child, cursor), result);
            }
        }
    }
    return result;
}

function selectSteps(contexts: Cursor[], steps: CompiledStep[]): Cursor[] {
    for (const step of steps) {
        const next: Cursor[] = [];
        for (const context of contexts) {
            const origins = step.isDescendant ? collectDescendants(context) : [context];
            for (const origin of origins) {
                const candidates: Cursor[] = [];
                const value = origin.value;
                if (step.type === "dot" || step.type === "axis" && step.axis === "self") {
                    if (step.type === "dot" || matchesNodeTest(step, value)) {
                        candidates.push(origin);
                    }
                } else if (step.type === "dotdot" || step.type === "axis" && step.axis === "parent") {
                    const parent = parentTagCursor(origin);
                    if (isTagCursor(parent) && (step.type === "dotdot" || matchesElementName(step, parent.value.name))) {
                        candidates.push(parent);
                    }
                } else if (isTagCursor(origin)) {
                    if (step.type === "attribute") {
                        for (const attr of origin.value.attributes) {
                            if (matchesName(step.name, attr.key.name)) {
                                candidates.push(new Cursor(attr, origin));
                            }
                        }
                    } else if (step.type === "nodeType") {
                        for (const content of origin.value.content ?? []) {
                            if (matchesNodeTest(step, content)) {
                                candidates.push(new Cursor(content, origin));
                            }
                        }
                    } else {
                        for (const child of origin.value.content ?? []) {
                            if (isTag(child) && (step.type === "axis" ? matchesElementName(step, child.name) :
                                matchesName(step.name, child.name))) {
                                candidates.push(new Cursor(child, origin));
                            }
                        }
                    }
                }
                next.push(...filterCandidates(candidates, step.predicates));
            }
        }
        contexts = next;
    }
    return contexts;
}

function filterCandidates(candidates: Cursor[], predicates: CompiledExpr[]): Cursor[] {
    for (const predicate of predicates) {
        candidates = candidates.filter((candidate, i) => {
            const value = candidate.value;
            return evaluateExpr(predicate, isTagCursor(candidate) ? value : undefined,
                isXml(value) && isAttribute(value) ? value : undefined, candidate, i + 1, candidates.length);
        });
    }
    return candidates;
}

function stringValue(node: Xml): string {
    if (isAttribute(node)) {
        return node.value.value;
    }
    if (isCharData(node)) {
        return node.text;
    }
    return isTag(node) ? (node.content ?? []).filter(c => isTag(c) || c.kind === Xml.Kind.CharData)
        .map(stringValue).join("") : "";
}

function matchesNodeTest(step: CompiledStep, value: Xml): boolean {
    switch (step.nodeType) {
        case "node":
            return true;
        case "text":
            return value.kind === Xml.Kind.CharData;
        case "comment":
            return value.kind === Xml.Kind.Comment;
        case "processing-instruction":
            return value.kind === Xml.Kind.ProcessingInstruction;
        default:
            return isTag(value) && matchesName(step.name, value.name);
    }
}
