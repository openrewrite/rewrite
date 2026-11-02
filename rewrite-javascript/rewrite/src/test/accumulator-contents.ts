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
import {types} from "util";
import {AccumulatorListener} from "../run";
import {Recipe} from "../recipe";
import {isSourceFile} from "../tree";
import {heavyweightType} from "../heavyweight";
import {Type} from "../java";
import {typeName} from "./probes";

/** Beyond this many objects in one accumulator the walk stops descending. */
const MAX_OBJECTS = 100_000;

const MAX_PATHS = 5;

interface Finding {
    readonly path: string
    readonly what: string
    readonly phase: string
}

interface Node {
    readonly value: object
    readonly parent?: Node
    readonly step: string
}

/**
 * Finds the heavyweight objects that ScanningRecipe accumulators keep, which are trees and the types
 * registerHeavyweight registers. An accumulator lives from the first source file scanned until the cycle ends. The
 * walk reads data properties and built-in collections, and runs no code of the recipe's own.
 */
export class AccumulatorContents {
    /** What each recipe's accumulator keeps, by recipe name and then by the shape of the path to it. */
    private readonly findings = new Map<string, Map<string, Finding>>();

    readonly listener: AccumulatorListener = (accumulators, phase) => {
        const walked = new Set<unknown>();
        for (const [recipe, acc] of accumulators) {
            if (acc !== null && typeof acc === "object" && !walked.has(acc)) {
                walked.add(acc);
                try {
                    this.walk(recipe.name, acc, phase);
                } catch {
                    // an accumulator the walk can't read is left alone
                }
            }
        }
    };

    assertNoneFound(): void {
        if (this.findings.size === 0) {
            return;
        }
        const lines = ["A ScanningRecipe's accumulator should keep the values the recipe needs, such as package " +
        "names, versions, paths and ids, and no heavyweight objects, such as trees, cursors, visitors, execution " +
        "contexts and dependency models:"];
        for (const [recipe, found] of this.findings) {
            lines.push(`  ${recipe} keeps`);
            const shown = [...found.values()].slice(0, MAX_PATHS);
            lines.push(...shown.map(f => `    ${f.path} -> ${f.what}, ${f.phase}`));
            if (found.size > MAX_PATHS) {
                lines.push(`    and ${found.size - MAX_PATHS} more`);
            }
        }
        lines.push("An accumulator stays in memory from the first source file scanned until the cycle ends, so " +
            "what it keeps for each source file adds up over a large repository. A NodeResolutionResult can reach " +
            "the model of a whole dependency tree, while small markers such as NodeRuntime are fine to keep. A " +
            "recipe that must keep a heavyweight object opts out with spec.checkLightweightAccumulators = false.");
        throw new Error(lines.join("\n"));
    }

    /** Breadth first, so that the path to what the accumulator keeps is a short one. */
    private walk(recipe: string, acc: object, phase: string): void {
        const visited = new Set<object>();
        const queue: Node[] = [];
        let budget = MAX_OBJECTS;
        const visit = (value: unknown, parent: Node | undefined, step: string) => {
            if (!walks(value)) {
                return;
            }
            const what = heavyweight(value);
            if (what) {
                const path = pathOf({value, parent, step});
                let found = this.findings.get(recipe);
                if (!found) {
                    this.findings.set(recipe, found = new Map());
                }
                const shape = `${path.replace(/\[[^\]]*]/g, "[]")} -> ${what}`;
                if (!found.has(shape)) {
                    found.set(shape, {path, what, phase});
                }
            } else if (budget > 0 && !visited.has(value)) {
                budget--;
                visited.add(value);
                queue.push({value, parent, step});
            }
        };

        visit(acc, undefined, "acc");
        for (let i = 0; i < queue.length; i++) {
            const node = queue[i];
            const value = node.value;
            if (types.isMap(value)) {
                Map.prototype.forEach.call(value, (v: unknown, k: unknown) => {
                    if (walks(k) || walks(v)) {
                        visit(k, node, `[${key(k)}]`);
                        visit(v, node, `[${key(k)}]`);
                    }
                });
            } else if (types.isSet(value)) {
                let index = 0;
                Set.prototype.forEach.call(value, (v: unknown) => visit(v, node, `[${index++}]`));
            } else {
                // data properties only, since a getter is code of the recipe's own
                const array = Array.isArray(value);
                for (const name of Reflect.ownKeys(value)) {
                    const descriptor = Reflect.getOwnPropertyDescriptor(value, name);
                    if (descriptor && "value" in descriptor && !(array && name === "length")) {
                        visit(descriptor.value, node, step(name, array));
                    }
                }
            }
        }
    }
}

/** Whether the walk looks at a value at all, which it doesn't for primitives and for what refers to no tree. */
function walks(value: unknown): value is object {
    return value !== null && typeof value === "object" && !types.isProxy(value) && !types.isPromise(value) &&
        !types.isWeakMap(value) && !types.isWeakSet(value) && !types.isAnyArrayBuffer(value) &&
        !types.isArrayBufferView(value) && !types.isDate(value) && !types.isRegExp(value) &&
        !types.isBoxedPrimitive(value) &&
        // a recipe lives for the whole run anyway, and type attribution refers to no tree
        !(value instanceof Recipe) && !Type.isType(value);
}

/** How to name what the accumulator keeps, when it is a heavyweight object. */
function heavyweight(value: object): string | undefined {
    const type = heavyweightType(value);
    if (type === undefined) {
        return undefined;
    } else if (typeof type === "function") {
        return type.name || "an object of an anonymous class";
    }
    return isSourceFile(value) ? `${typeName(type)} ${value.sourcePath}` : typeName(type);
}

function pathOf(node: Node): string {
    const steps: string[] = [];
    for (let n: Node | undefined = node; n; n = n.parent) {
        steps.push(n.step);
    }
    return steps.reverse().join("");
}

function step(name: string | symbol, array: boolean): string {
    if (typeof name === "symbol") {
        return `[${name.toString()}]`;
    }
    return array && /^\d+$/.test(name) ? `[${name}]` :
        /^[A-Za-z_$][\w$]*$/.test(name) ? `.${name}` : `[${JSON.stringify(name)}]`;
}

/** A map key as the path shows it, without calling code of the recipe's own. */
function key(k: unknown): string {
    let shown: string;
    if (k === null || typeof k !== "object") {
        shown = typeof k === "symbol" ? k.toString() : String(k);
    } else if (isSourceFile(k)) {
        shown = k.sourcePath;
    } else if (k instanceof Recipe) {
        shown = k.name;
    } else if (typeof (k as { kind?: unknown }).kind === "string") {
        shown = typeName((k as { kind: string }).kind);
    } else {
        shown = Object.getPrototypeOf(k)?.constructor?.name || "Object";
    }
    return shown.length > 60 ? `${shown.substring(0, 60)}...` : shown;
}

