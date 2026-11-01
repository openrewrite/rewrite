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

/**
 * Probes of recipes that resolve dependencies take seconds each, so a recipe stops being probed in this process once
 * its probes have taken this long.
 */
const TIME_CAP_MS = 20_000;

/** How long each recipe's probes have taken in this process, by {@link timeCapKey}. */
const PROBE_MS = new Map<string, number>();

/** The probes each recipe class ran, which a process runs once whether they pass or fail. */
const PROBED = new WeakMap<Function, Set<string>>();

/**
 * Runs `run`, a probe of the recipe on what `probe` names, unless this process ran that probe before or the recipe's
 * probes have taken the time cap, and charges the recipe the time it takes.
 */
export async function probeOnce(recipe: Recipe, probe: string, run: () => Promise<void>): Promise<void> {
    if ((PROBE_MS.get(await timeCapKey(recipe)) ?? 0) > TIME_CAP_MS) {
        return;
    }
    let probed = PROBED.get(recipe.constructor);
    if (!probed) {
        PROBED.set(recipe.constructor, probed = new Set());
    }
    const key = await probeKey(recipe, probe);
    if (probed.has(key)) {
        return;
    }
    probed.add(key);
    const start = performance.now();
    try {
        await run();
    } finally {
        await charge(recipe, performance.now() - start);
    }
}

/** Adds `ms` to the time the recipe's probes have taken in this process. */
export async function charge(recipe: Recipe, ms: number): Promise<void> {
    const key = await timeCapKey(recipe);
    PROBE_MS.set(key, (PROBE_MS.get(key) ?? 0) + ms);
}

/**
 * What a recipe's probes share a time cap by: its name, and for a recipe with a recipe list its children's keys too,
 * since a composite a test declares has one name whatever it runs.
 */
async function timeCapKey(recipe: Recipe): Promise<string> {
    let children: Recipe[];
    try {
        children = await recipe.recipeList();
    } catch {
        return recipe.name;
    }
    const keys: string[] = [];
    for (const child of children) {
        keys.push(await timeCapKey(child));
    }
    return keys.length === 0 ? recipe.name : `${recipe.name}[${keys.join(",")}]`;
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
