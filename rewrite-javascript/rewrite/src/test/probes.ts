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
import * as path from "path";
import {Recipe} from "../recipe";
import {ExecutionContext} from "../execution";
import {SourceFile} from "../tree";
import {ACCUMULATOR_LISTENER, scheduleRun} from "../run";
import {RecipeRunError} from "../visitor";
import {DATA_TABLE_STORE} from "../data-table";
import {Marker} from "../markers";

/** Messages that belong to the test's own run. */
const NOT_COPIED = new Set<string | symbol>([DATA_TABLE_STORE, ACCUMULATOR_LISTENER]);

/** What each recipe class passed a probe of, so that a recipe a suite tests many times is probed once. */
const PASSED = new WeakMap<Function, Set<string>>();

/**
 * Whether the recipe already passed `probe`, a key of what the probe runs it on. A probe that fails is never
 * remembered, so that every test of the recipe fails until it is fixed.
 */
export async function probedBefore(recipe: Recipe, probe: string): Promise<boolean> {
    return PASSED.get(recipe.constructor)?.has(await probeKey(recipe, probe)) ?? false;
}

export async function passed(recipe: Recipe, probe: string): Promise<void> {
    let passed = PASSED.get(recipe.constructor);
    if (!passed) {
        PASSED.set(recipe.constructor, passed = new Set());
    }
    passed.add(await probeKey(recipe, probe));
}

async function probeKey(recipe: Recipe, probe: string): Promise<string> {
    try {
        return `${JSON.stringify(await recipe.descriptor())}\0${probe}`;
    } catch {
        return `${recipe.name}\0${probe}`;
    }
}

/** The error the recipe threw when run on `sources` for up to `cycles` cycles, if it threw. */
export async function thrownBy(recipe: Recipe, sources: SourceFile[], recipeCtx: ExecutionContext,
                               cycles: number): Promise<{ error: unknown } | undefined> {
    const messages: { [key: string | symbol]: any } = {};
    for (const key of Reflect.ownKeys(recipeCtx.messages)) {
        if (!NOT_COPIED.has(key)) {
            messages[key] = recipeCtx.messages[key];
        }
    }
    const ctx = new ExecutionContext(messages);
    let current = sources;
    try {
        for (let cycle = 1; cycle <= cycles; cycle++) {
            const changeset = (await scheduleRun(recipe, current, ctx)).changeset;
            if (changeset.every(r => r.before === r.after)) {
                break;
            }
            current = changeset.filter(r => r.after).map(r => r.after!);
        }
    } catch (error) {
        return {error};
    }
    return undefined;
}

/**
 * The error the recipe threw, with the first line of its message and the first frame of its stack, and when that
 * frame is in a dependency, the first frame that isn't.
 */
export function describeThrown(thrown: unknown, indent: string): string {
    const cause = thrown instanceof RecipeRunError ? thrown.cause : thrown;
    if (!(cause instanceof Error)) {
        return `${indent}${String(cause)}`;
    }
    const firstLine = cause.message.trim().split("\n")[0];
    let description = `${indent}${cause.name}${firstLine ? `: ${firstLine.length > 200 ?
        `${firstLine.substring(0, 200)}...` : firstLine}` : ""}`;
    const frames = (cause.stack ?? "").split("\n").map(line => line.trim())
        .filter(line => line.startsWith("at ") && !/\(node:|^at node:|<anonymous>/.test(line))
        .map(line => line.split(process.cwd() + path.sep).join(""));
    if (frames.length > 0) {
        description += `\n${indent}    ${frames[0]}`;
        const own = frames.find(frame => !frame.includes("node_modules"));
        if (own && own !== frames[0]) {
            description += `\n${indent}    ${own}`;
        }
    }
    return description;
}

/** Reads as " with the markers of the test's source (Autodetect, PrettierStyle)", or as nothing. */
export function withMarkersOf(markers: Marker[]): string {
    const kinds = [...new Set(markers.map(m => typeName(m.kind)))];
    return kinds.length === 0 ? "" : ` with the markers of the test's source (${kinds.join(", ")})`;
}

/** Reads as "PlainText" or "JS.CompilationUnit". */
export function typeName(kind: string): string {
    return kind.replace(/^.*\./, "").replace(/\$/g, ".");
}
