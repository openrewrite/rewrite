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
import {noopVisitor, TreeVisitor} from './visitor';
import {Cursor, isSourceFile, SourceFile, Tree} from './tree';
import {Recipe} from "./recipe";
import {ExecutionContext} from "./execution";

/**
 * Recipe-identity placeholder for use as a precondition.
 *
 * Captures a Java recipe class name + options without instantiating the
 * recipe or firing an RPC. The framework introspects a
 * ``check(RecipeRef, editor)`` wrapper at PrepareRecipe time and emits
 * the recipe identity directly in
 * ``PrepareRecipeResponse.editPreconditions``. The Java host's
 * ``PreparedRecipeCache.instantiateVisitor`` constructs the recipe via
 * Jackson and uses its visitor.
 *
 * This avoids requiring the recipe author to do an RPC at ``editor()``
 * construction time, which would otherwise block in-process unit tests
 * that don't have an active RPC connection.
 *
 * Helpers in ``@openrewrite/rewrite/javascript/preconditions``
 * (``usesMethod``, ``usesType``, ``hasSourcePath``, ``findMethods``,
 * ``findTypes``) return ``RecipeRef`` instances.
 */
export class RecipeRef {
    constructor(
        readonly recipeName: string,
        readonly options: Readonly<Record<string, any>> = {}
    ) {
    }
}

/**
 * Composite of nested precondition operands joined by an operator.
 *
 * Mirrors Java's ``Preconditions.or``/``and``/``not``: a gate that
 * short-circuits over its operands. ``op`` is one of ``"or"`` / ``"and"`` /
 * ``"not"``. Operands may be ``TreeVisitor``, ``Recipe``, ``RecipeRef``,
 * or another ``CompositePrecondition``.
 *
 * The framework promotes the composite to a structured wire entry at
 * PrepareRecipe time; the Java host's ``RewriteRpc.matchAll`` rebuilds
 * the visitor via the matching ``Preconditions`` factory so the gate runs
 * locally and the visit RPC is skipped for files the gate rejects.
 */
export class CompositePrecondition {
    constructor(
        readonly op: "or" | "and" | "not",
        readonly operands: ReadonlyArray<CheckArg>
    ) {
    }
}

export type CheckArg = Recipe | TreeVisitor<any, ExecutionContext> | RecipeRef | CompositePrecondition;

export async function check<T extends Tree>(
    checkCondition: CheckArg | Promise<CheckArg> | boolean,
    v: TreeVisitor<T, ExecutionContext> | Promise<TreeVisitor<T, ExecutionContext>>
): Promise<TreeVisitor<T, ExecutionContext>> {
    const resolvedCheck = await checkCondition;
    const resolvedV = await v;

    if (typeof resolvedCheck === 'boolean') {
        return resolvedCheck ? resolvedV : noopVisitor<T, ExecutionContext>();
    }
    return new Check(resolvedCheck, resolvedV);
}

/**
 * OR-compose precondition checks. Mirrors Java's
 * ``Preconditions.or(visitor...)``: the gate matches if any operand
 * matches. Requires at least two operands; a single-operand OR has no
 * value over a bare ``check``.
 */
export function or(...operands: CheckArg[]): CompositePrecondition {
    if (operands.length < 2) {
        throw new Error("Preconditions.or requires at least two operands");
    }
    return new CompositePrecondition("or", operands);
}

/**
 * AND-compose precondition checks. The outer ``editPreconditions`` list
 * is already AND-composed by the host, so this is mainly useful as an
 * operand of ``or``/``not``.
 */
export function and(...operands: CheckArg[]): CompositePrecondition {
    if (operands.length < 2) {
        throw new Error("Preconditions.and requires at least two operands");
    }
    return new CompositePrecondition("and", operands);
}

/**
 * Negate a precondition check. Mirrors Java's ``Preconditions.not(visitor)``:
 * the gate matches iff the operand does not.
 */
export function not(operand: CheckArg): CompositePrecondition {
    return new CompositePrecondition("not", [operand]);
}

export class Check<T extends Tree> extends TreeVisitor<T, ExecutionContext> {
    constructor(
        readonly check: CheckArg,
        readonly v: TreeVisitor<T, ExecutionContext>
    ) {
        super();
    }

    async isAcceptable(sourceFile: SourceFile, ctx: ExecutionContext): Promise<boolean> {
        if (this.check instanceof RecipeRef || this.check instanceof CompositePrecondition) {
            // RecipeRef / Composite have no in-process is_acceptable — defer to the wrapped editor.
            return this.v.isAcceptable(sourceFile, ctx);
        }
        return await (await this.checkVisitor()).isAcceptable(sourceFile, ctx) &&
            await this.v.isAcceptable(sourceFile, ctx);
    }

    async visit<R extends T>(tree: Tree, ctx: ExecutionContext, parent?: Cursor): Promise<R | undefined> {
        // if tree isn't an instanceof of SourceFile, then a precondition visitor may
        // not be able to do its work because it may assume we are starting from the root level
        if (!isSourceFile(tree)) {
            return parent !== undefined
                ? this.v.visit<R>(tree, ctx, parent)
                : this.v.visit<R>(tree, ctx);
        }

        // In-process fallback: a RecipeRef is a wire-only placeholder with no
        // ``visit`` of its own — treat as "always matches" so the wrapped
        // editor still runs in unit tests and direct in-process calls. The
        // wire-side optimization (skip the visit RPC when the precondition
        // rejects the file) lives in optimizePreconditions.
        if (this.check instanceof RecipeRef) {
            return parent !== undefined
                ? this.v.visit<R>(tree, ctx, parent)
                : this.v.visit<R>(tree, ctx);
        }

        const matched = this.check instanceof CompositePrecondition
            ? await evaluateComposite(this.check, tree, ctx, parent)
            : await this.runLeafCheck(tree, ctx, parent);

        if (matched) {
            return parent !== undefined
                ? this.v.visit<R>(tree, ctx, parent)
                : this.v.visit<R>(tree, ctx);
        }

        return tree as unknown as R;
    }

    private async runLeafCheck(tree: Tree, ctx: ExecutionContext, parent?: Cursor): Promise<boolean> {
        const checkResult = parent !== undefined
            ? await (await this.checkVisitor()).visit(tree, ctx, parent)
            : await (await this.checkVisitor()).visit(tree, ctx);
        return checkResult !== (tree as unknown as T);
    }

    private async checkVisitor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return this.check instanceof Recipe ? this.check.editor() : (this.check as TreeVisitor<any, ExecutionContext>);
    }
}

/**
 * Evaluate a {@link CompositePrecondition} in-process for unit tests and
 * direct callers that don't have a live RPC. Mirrors Java's
 * ``Preconditions.or``/``and``/``not`` semantics. Returns ``true`` iff the
 * gate would let the wrapped visitor run.
 */
async function evaluateComposite(
    composite: CompositePrecondition,
    tree: Tree,
    ctx: ExecutionContext,
    parent?: Cursor
): Promise<boolean> {
    const operands = composite.operands;
    switch (composite.op) {
        case "or":
            for (const operand of operands) {
                if (await operandMatches(operand, tree, ctx, parent)) return true;
            }
            return false;
        case "and":
            for (const operand of operands) {
                if (!(await operandMatches(operand, tree, ctx, parent))) return false;
            }
            return true;
        case "not":
            if (operands.length !== 1) {
                throw new Error("CompositePrecondition op=not requires exactly one operand");
            }
            return !(await operandMatches(operands[0], tree, ctx, parent));
    }
}

async function operandMatches(
    operand: CheckArg,
    tree: Tree,
    ctx: ExecutionContext,
    parent?: Cursor
): Promise<boolean> {
    if (operand instanceof RecipeRef) {
        // Wire-only — treat as "always matches" in-process so the wrapped
        // editor still runs in unit tests. The host evaluates the gate
        // for real once the response goes over the wire.
        return true;
    }
    if (operand instanceof CompositePrecondition) {
        return evaluateComposite(operand, tree, ctx, parent);
    }
    const visitor = operand instanceof Recipe ? await operand.editor() : operand;
    const result = parent !== undefined
        ? await visitor.visit(tree, ctx, parent)
        : await visitor.visit(tree, ctx);
    return result !== tree;
}
