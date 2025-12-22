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
 * Helper function to create a new object only if any properties have changed.
 * Compares each property in updates with the original object.
 * Returns the original object if nothing changed, or a new object with updates applied.
 */
export function updateIfChanged<O extends object>(original: O, updates: Partial<O>): O {
    for (const key in updates) {
        if (updates[key] !== original[key]) {
            return { ...original, ...updates };
        }
    }
    return original;
}
