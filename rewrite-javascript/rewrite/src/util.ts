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
 * Removes common leading whitespace from each line, optimized for template strings.
 *
 * Behavior:
 * - Removes ONE leading newline if present (for template string ergonomics)
 * - Removes trailing newline + whitespace (for template string ergonomics)
 * - Preserves additional leading/trailing empty lines beyond the first
 * - For lines with content: removes common indentation
 * - For lines with only whitespace: removes common indentation, preserving remaining spaces
 *
 * Examples:
 * - `\n  code` → `code` (single leading newline removed)
 * - `\n\n  code` → `\ncode` (first newline removed, second preserved)
 * - `  code\n` → `code` (trailing newline removed)
 * - `  code\n\n` → `code\n` (first trailing newline removed, second preserved)
 */
export function dedent(s: string): string {
    if (!s) return s;

    // Remove single leading newline for ergonomics
    let start = s.charCodeAt(0) === 10 ? 1 : 0;  // 10 = '\n'

    // Remove trailing newline + any trailing whitespace
    let end = s.length;
    for (let i = s.length - 1; i >= start; i--) {
        const ch = s.charCodeAt(i);
        if (ch === 10) {  // '\n'
            end = i;
            break;
        }
        if (ch !== 32 && ch !== 9) break;  // not ' ' or '\t'
    }

    if (start >= end) return '';

    const str = start > 0 || end < s.length ? s.slice(start, end) : s;
    const lines = str.split('\n');

    // If we removed a leading newline, consider all lines for minIndent
    // Otherwise, skip the first line (it's on the same line as the opening quote)
    const startLine = start > 0 ? 0 : 1;

    // Find minimum indentation
    let minIndent = Infinity;
    for (let i = startLine; i < lines.length; i++) {
        const line = lines[i];
        let indent = 0;
        for (let j = 0; j < line.length; j++) {
            const ch = line.charCodeAt(j);
            if (ch === 32 || ch === 9) {  // ' ' or '\t'
                indent++;
            } else {
                // Found non-whitespace, update minIndent
                if (indent < minIndent) minIndent = indent;
                break;
            }
        }
    }

    // If all lines are empty or no indentation
    if (minIndent === Infinity || minIndent === 0) {
        return lines.join('\n');
    }

    // Remove common indentation from lines (skip first line only if we didn't remove leading newline)
    return lines.map((line, i) =>
        (i === 0 && startLine === 1) ? line : (line.length >= minIndent ? line.slice(minIndent) : '')
    ).join('\n');
}

/**
 * Prefixes each line of a multi-line string with the given prefix.
 * Optionally uses a different prefix for the first line.
 *
 * @param str The string to prefix
 * @param prefix The prefix to add to each line (or all lines if firstPrefix not provided)
 * @param firstPrefix Optional different prefix for the first line
 * @returns The prefixed string
 */
export function prefixLines(str: string, prefix: string, firstPrefix?: string): string {
    if (!str) return str;
    const lines = str.split('\n');
    if (lines.length === 0) return str;

    if (firstPrefix !== undefined) {
        return lines.map((line, i) => i === 0 ? firstPrefix + line : prefix + line).join('\n');
    } else {
        return lines.map(line => prefix + line).join('\n');
    }
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
