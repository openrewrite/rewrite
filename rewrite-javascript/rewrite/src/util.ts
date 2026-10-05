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
export async function mapAsync<T, U>(arr: T[], fn: (t: T, i: number) => Promise<U | undefined>): Promise<U[]> {
    let results: U[] | undefined = undefined;

    for (let i = 0; i < arr.length; i++) {
        const result = await fn(arr[i], i);

        if (result !== arr[i]) {
            if (results === undefined) {
                results = arr.slice(0, i) as unknown[] as U[];
            }

            if (result !== undefined) {
                results.push(result);
            }
        } else if (results !== undefined && result !== undefined) {
            results.push(result);
        }
    }

    return results === undefined ? arr as unknown[] as U[] : results;
}

export function trimIndent(str: string | null | undefined): string {
    if (!str) {
        return "";
    }
    const lines = str.split("\n");
    const nonEmpty = lines.filter(l => l.trim().length > 0);
    const minIndent = Math.min(
        ...nonEmpty.map(l => l.match(/^(\s*)/)![0].length)
    );
    return lines
        .map(l => l.slice(minIndent))
        .join("\n")
        .trim();
}

/**
 * A compiled `(original, updates) => merged` builder that names every resulting property in an
 * object literal. V8 sizes a literal's in-object storage to the properties it names, so the merged
 * node keeps its fields inline; the `{...original, ...updates}` spread of a shared, megamorphic
 * function instead lands in an out-of-object `system / PropertyArray` (millions of them across an
 * RPC-received LST forest). Builders are keyed and cached by the pair of property-name lists so a
 * given shape compiles once.
 */
type MergeBuilder = (original: any, updates: any) => any;

const mergeBuilders = new Map<string, MergeBuilder>();

function mergeBuilderFor(originalKeys: string[], updateKeys: string[]): MergeBuilder | undefined {
    const cacheKey = JSON.stringify(originalKeys) + JSON.stringify(updateKeys);
    let builder = mergeBuilders.get(cacheKey);
    if (builder === undefined) {
        const fromUpdates = new Set(updateKeys);
        const names = originalKeys.slice();
        for (const key of updateKeys) {
            if (!names.includes(key)) {
                names.push(key);
            }
        }
        const body = "return {" + names
            .map(name => `${JSON.stringify(name)}:(${fromUpdates.has(name) ? "u" : "o"})[${JSON.stringify(name)}]`)
            .join(",") + "};";
        try {
            builder = new Function("o", "u", body) as MergeBuilder;
        } catch {
            builder = (o, u) => ({...o, ...u});
        }
        mergeBuilders.set(cacheKey, builder);
    }
    return builder;
}

/**
 * Helper function to create a new object only if any properties have changed.
 * Compares each property in updates with the original object.
 * Returns the original object if nothing changed, or a new object with updates applied.
 */
export function updateIfChanged<O extends object>(original: O, updates: Partial<O>): O {
    let changed = false;
    for (const key in updates) {
        if (updates[key] !== original[key]) {
            changed = true;
            break;
        }
    }
    if (!changed) {
        return original;
    }
    // A merged node built through a shared spread overflows into a PropertyArray; a compiled
    // literal builder keeps its fields inline. Symbol-keyed originals fall back to the spread,
    // since only string keys survive the JSON-serialized builder body.
    if (typeof original === "object" && Object.getOwnPropertySymbols(original).length === 0) {
        const builder = mergeBuilderFor(Object.keys(original), Object.keys(updates as object));
        if (builder !== undefined) {
            return builder(original, updates);
        }
    }
    return {...original, ...updates};
}
