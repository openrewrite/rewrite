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
import {Cursor, isTree, Tree} from '../../tree';
import {J} from '../../java';
import {MarkerPrinter, PrintOutputCapture} from '../../print';
import {JS} from '../tree';
import {JavaScriptVisitor} from '../visitor';
import {JavaScriptPrinter} from '../print';
import {afterChildren} from '../after-children';

/** A tree on the way from a replaced slot to the root, and the tree it holds on that way. */
export interface Enclosing {
    readonly tree: J;
    readonly child: J;
    /** The cursor of the visit that `tree` is in, or of the nearest tree around it that has one. */
    readonly cursor?: Cursor;
}

/**
 * Keeps `replacement` from fusing with the code around the slot it fills, as `return!!x` rewritten to
 * `returnBoolean(x)` or `foo["bar"]in x` to `foo.barin x` would. A separating space before it goes in its own
 * prefix. One after it belongs to the padding of a tree around it, so it goes in once the visitor is done
 * with that tree. `path` lists the trees around the slot, innermost first.
 */
export async function separateFromNeighbours(replacement: J, path: Enclosing[]): Promise<J> {
    separateWhatFollows(path);
    return isEmpty(replacement.prefix) && isWord((await printed(replacement))[0]) &&
    isWord(await lastCharBefore(path)) ? {...replacement, prefix: singleSpace(replacement.prefix)} : replacement;
}

function isWord(char: string | undefined): boolean {
    return char !== undefined && /[\p{ID_Continue}$\u200C\u200D]/u.test(char);
}

function isEmpty(space: J.Space): boolean {
    return space.whitespace === '' && space.comments.length === 0;
}

async function printed(tree: J): Promise<string> {
    const out = new PrintOutputCapture(MarkerPrinter.SANITIZED);
    await new JavaScriptPrinter().visit(tree, out);
    return out.out;
}

/** The last character printed before the slot, or `undefined` where nothing is or it cannot tell. */
async function lastCharBefore(path: Enclosing[]): Promise<string | undefined> {
    for (const {tree, child} of path) {
        if (tree.kind === J.Kind.Block || tree.kind === JS.Kind.CompilationUnit) {
            // A statement follows `{`, `;` or `}`, and printing the ones before it would cost more than it tells
            return undefined;
        }
        const before = await printedBefore(tree, child);
        if (before === undefined) {
            return undefined;
        } else if (before !== '') {
            return before[before.length - 1];
        }
    }
    return undefined;
}

async function printedBefore(tree: J, child: J): Promise<string | undefined> {
    const printer = new class extends JavaScriptPrinter {
        reached?: number;

        override async visit<R extends J>(t: Tree, p: PrintOutputCapture, parent?: Cursor): Promise<R | undefined> {
            if (this.reached !== undefined) {
                return t as R;
            } else if (t === child) {
                this.reached = p.out.length;
                return t as R;
            }
            return super.visit(t, p, parent);
        }
    }();
    const out = new PrintOutputCapture(MarkerPrinter.SANITIZED);
    await printer.visit(tree, out);
    return printer.reached === undefined ? undefined : out.out.substring(0, printer.reached);
}

/**
 * Where a keyword follows the slot, separates it from what then fills the slot once the visitor is done with the
 * tree printing the keyword. The slot can sit at the end of other trees on the way to that one.
 */
function separateWhatFollows(path: Enclosing[]): void {
    for (const {tree, child, cursor} of path) {
        if (keywordFollows(tree, child)) {
            if (cursor) {
                const id = tree.id;
                afterChildren(cursor, visited => withTreeAt(visited, id, separateFollowingKeyword));
            }
            return;
        } else if (!endsWith(tree, child)) {
            return;
        }
    }
}

function keywordFollows(tree: J, child: J): boolean {
    switch (tree.kind) {
        case JS.Kind.Binary: {
            const binary = tree as JS.Binary;
            return binary.left === child && isWordOperator(binary.operator.element);
        }
        case J.Kind.InstanceOf:
            return (tree as J.InstanceOf).expression.element === child;
        case JS.Kind.As:
            return (tree as JS.As).left.element === child;
        case JS.Kind.SatisfiesExpression:
            return (tree as JS.SatisfiesExpression).expression === child;
        case J.Kind.ForEachLoopControl:
            return (tree as J.ForEachLoop.Control).variable.element === child;
        default:
            return false;
    }
}

function isWordOperator(operator: JS.Binary.Type): boolean {
    return operator === JS.Binary.Type.In;
}

/** A single space before the keyword of {@link keywordFollows}, where what precedes it ends in a word. */
async function separateFollowingKeyword(tree: J): Promise<J> {
    switch (tree.kind) {
        case JS.Kind.Binary: {
            const binary = tree as JS.Binary;
            return isWordOperator(binary.operator.element) && await fuses(binary.left, binary.operator.before) ?
                {...binary, operator: {...binary.operator, before: singleSpace(binary.operator.before)}} as JS.Binary :
                tree;
        }
        case J.Kind.InstanceOf: {
            const instanceOf = tree as J.InstanceOf;
            return await fuses(instanceOf.expression.element, instanceOf.expression.after) ?
                {...instanceOf, expression: separatedAfter(instanceOf.expression)} as J.InstanceOf : tree;
        }
        case JS.Kind.As: {
            const as = tree as JS.As;
            return await fuses(as.left.element, as.left.after) ? {...as, left: separatedAfter(as.left)} as JS.As : tree;
        }
        case JS.Kind.SatisfiesExpression: {
            const satisfies = tree as JS.SatisfiesExpression;
            return await fuses(satisfies.expression, satisfies.satisfiesType.before) ? {
                ...satisfies,
                satisfiesType: {...satisfies.satisfiesType, before: singleSpace(satisfies.satisfiesType.before)}
            } as JS.SatisfiesExpression : tree;
        }
        case J.Kind.ForEachLoopControl: {
            const control = tree as J.ForEachLoop.Control;
            return await fuses(control.variable.element, control.variable.after) ?
                {...control, variable: separatedAfter(control.variable)} as J.ForEachLoop.Control : tree;
        }
        default:
            return tree;
    }
}

async function fuses(preceding: J, space: J.Space): Promise<boolean> {
    if (!isEmpty(space)) {
        return false;
    }
    const text = await printed(preceding);
    return isWord(text[text.length - 1]);
}

function singleSpace(space: J.Space): J.Space {
    return {...space, whitespace: ' '};
}

function separatedAfter<T extends J>(padded: J.RightPadded<T>): J.RightPadded<T> {
    return {...padded, after: singleSpace(padded.after)};
}

function endsWith(tree: J, child: J): boolean {
    switch (tree.kind) {
        case J.Kind.Binary:
            return (tree as J.Binary).right === child;
        case JS.Kind.Binary:
            return (tree as JS.Binary).right === child;
        case J.Kind.Unary: {
            const unary = tree as J.Unary;
            return unary.expression === child && unary.operator.element !== J.Unary.Type.PostIncrement &&
                unary.operator.element !== J.Unary.Type.PostDecrement;
        }
        case J.Kind.Ternary:
            return (tree as J.Ternary).falsePart.element === child;
        case J.Kind.Assignment:
            return (tree as J.Assignment).assignment.element === child;
        case J.Kind.AssignmentOperation:
            return (tree as J.AssignmentOperation).assignment === child;
        case JS.Kind.AssignmentOperation:
            return (tree as JS.AssignmentOperation).assignment === child;
        case J.Kind.InstanceOf:
            return (tree as J.InstanceOf).class === child;
        case JS.Kind.As:
            return (tree as JS.As).right === child;
        case JS.Kind.SatisfiesExpression:
            return (tree as JS.SatisfiesExpression).satisfiesType.element === child;
        case J.Kind.TypeCast:
            return (tree as J.TypeCast).expression === child;
        case JS.Kind.Await:
        case JS.Kind.Delete:
        case JS.Kind.TypeOf:
        case JS.Kind.Void:
            return (tree as JS.Await).expression === child;
        case JS.Kind.Spread:
            return (tree as JS.Spread).expression === child;
        case JS.Kind.ExpressionStatement:
            return (tree as JS.ExpressionStatement).expression === child;
        default:
            return false;
    }
}

async function withTreeAt(tree: J, id: string, change: (t: J) => Promise<J>): Promise<J> {
    if (tree.id === id) {
        return change(tree);
    }
    return (await new class extends JavaScriptVisitor<undefined> {
        override async visit<R extends J>(t: Tree, p: undefined, parent?: Cursor): Promise<R | undefined> {
            return (isTree(t) && t.id === id ? await change(t as J) : await super.visit(t, p, parent)) as R;
        }
    }().visit<J>(tree, undefined))!;
}
