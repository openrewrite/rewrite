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

import {J} from "../java";
import {create as produce} from "mutative";

/**
 * Gets the effective last whitespace from a Space.
 * When there are comments, the last whitespace is the suffix of the last comment.
 * When there are no comments, it's the whitespace property.
 */
export function lastWhitespace(space: J.Space | undefined): string {
    if (!space) {
        return "";
    }
    if (space.comments.length > 0) {
        return space.comments[space.comments.length - 1].suffix;
    }
    return space.whitespace;
}

/**
 * Replaces the effective last whitespace in a Space using a transform function.
 * When there are comments, updates the suffix of the last comment.
 * When there are no comments, updates the whitespace property.
 *
 * @param space The Space to modify (Mutative draft)
 * @param transform Function that receives the current last whitespace and returns the new value
 */
export function replaceLastWhitespace(space: J.Space, transform: (ws: string) => string): J.Space {
    return produce(space, draft => {
        if (draft.comments.length > 0) {
            const lastComment = draft.comments[draft.comments.length - 1];
            lastComment.suffix = transform(lastComment.suffix);
        } else {
            draft.whitespace = transform(draft.whitespace);
        }
    });
}

/**
 * Strips leading spaces and tabs from a string (but not newlines).
 */
export function stripLeadingIndent(s: string): string {
    return s.replace(/^[ \t]*/, '');
}

/**
 * Replaces the indentation after the last newline in a whitespace string.
 * If there's no newline, returns just the new indent.
 */
export function replaceIndentAfterLastNewline(ws: string, newIndent: string): string {
    const lastNewline = ws.lastIndexOf("\n");
    return ws.substring(0, lastNewline + 1) + newIndent;
}

/**
 * Checks if a Space contains any newlines (in whitespace or comment suffixes).
 */
export function spaceContainsNewline(space: J.Space | undefined): boolean {
    if (!space) return false;
    if (space.whitespace.includes("\n")) return true;
    return space.comments.some(c => c.suffix.includes("\n"));
}

/**
 * Normalizes indentation in an entire Space, including both whitespace and comment suffixes.
 * Each newline followed by whitespace gets its indentation normalized to the target indent.
 *
 * @param space The Space to normalize
 * @param targetIndent The indentation to use after newlines
 * @returns The normalized Space, or the original if unchanged
 */
export function normalizeSpaceIndent(space: J.Space, targetIndent: string): J.Space {
    let changed = false;

    // Normalize whitespace
    let newWhitespace = space.whitespace;
    if (space.whitespace.includes("\n")) {
        newWhitespace = replaceIndentAfterLastNewline(space.whitespace, targetIndent);
        changed = changed || newWhitespace !== space.whitespace;
    }

    // Normalize comment suffixes
    const newComments = space.comments.map(comment => {
        if (comment.suffix.includes("\n")) {
            const newSuffix = replaceIndentAfterLastNewline(comment.suffix, targetIndent);
            if (newSuffix !== comment.suffix) {
                changed = true;
                return {...comment, suffix: newSuffix};
            }
        }
        return comment;
    });

    if (!changed) {
        return space;
    }

    return {
        ...space,
        whitespace: newWhitespace,
        comments: newComments
    };
}

/**
 * Handles element removal from lists while preserving LST formatting.
 * Automatically applies prefixes from removed elements to the next kept element,
 * handling whitespace and comment preservation.
 *
 * @example
 * const formatter = new ElementRemovalFormatter<J>();
 *
 * for (const stmt of statements) {
 *     if (shouldRemove(stmt)) {
 *         formatter.markRemoved(stmt.element);
 *         continue;
 *     }
 *     const adjusted = formatter.processKept(stmt.element);
 *     filteredList.push({...stmt, element: adjusted});
 * }
 *
 * if (formatter.hasRemovals) {
 *     // Apply the filtered list
 * }
 */
export class ElementRemovalFormatter<T extends J> {
    private lastRemoved?: T;
    private keptCount = 0;
    private removedCount = 0;

    /**
     * @param preserveFirstElementComments Whether to preserve comments from the first removed element.
     *        Set to true for imports (to preserve file headers). Defaults to false.
     */
    constructor(private readonly preserveFirstElementComments: boolean = false) {}

    /**
     * Returns true if any elements have been marked as removed.
     */
    get hasRemovals(): boolean {
        return this.removedCount > 0;
    }

    /**
     * Mark an element as removed. Only the first consecutive removed element is tracked.
     */
    markRemoved(elem: T): void {
        this.lastRemoved ??= elem;
        this.removedCount++;
    }

    /**
     * Process a kept element, applying prefix from any previously removed element if needed.
     */
    processKept(elem: T): T {
        if (!this.lastRemoved) {
            this.keptCount++;
            return elem;
        }

        const preserveComments = this.preserveFirstElementComments && this.keptCount === 0;
        const adjusted = applyRemovedElementPrefix(this.lastRemoved, elem, preserveComments);
        this.lastRemoved = undefined;
        this.keptCount++;
        return adjusted;
    }
}

/**
 * Applies the prefix from a removed element to the next element.
 *
 * This is used when removing elements from a list to preserve formatting:
 * - Uses the removed element's prefix whitespace for the next element
 * - Optionally preserves leading comments from the removed element (e.g., file headers)
 * - For middle elements, may preserve blank lines in the next element's prefix
 * - Removes inline line comments (//...) that were on the removed element's line
 *
 * @param removedElement The element that was removed
 * @param nextElement The element that follows the removed one
 * @param preserveRemovedComments Whether to preserve leading comments from removed element (default: false)
 * @returns The next element with adjusted prefix, or the original if no changes needed
 */
function applyRemovedElementPrefix<T extends J>(removedElement: J, nextElement: T, preserveRemovedComments: boolean = false): T {
    if (!removedElement.prefix || !nextElement.prefix) {
        return nextElement;
    }

    const removedPrefix = removedElement.prefix;
    const currentPrefix = nextElement.prefix;

    if (currentPrefix === removedPrefix) {
        return nextElement;
    }

    // Helper to count newlines in whitespace
    const countNewlines = (ws: string | undefined) => (ws?.match(/\r?\n/g) || []).length;

    // Helper to check if whitespace has leading newline (comments are on their own line, not inline)
    const hasLeadingNewline = (ws: string | undefined) => /[\r\n]/.test(ws || '');

    const removedWs = removedPrefix.whitespace || '';
    const currentWs = currentPrefix.whitespace || '';
    const removedComments = removedPrefix.comments || [];
    const currentComments = currentPrefix.comments || [];

    // Filter out inline trailing line comments from current element
    let commentsToKeep = currentComments;
    if (currentComments.length > 0 && !hasLeadingNewline(currentWs)) {
        const firstComment: any = currentComments[0];
        const commentText = firstComment.text || firstComment.message || '';
        const isLineComment = commentText.includes('//') || firstComment.multiline === false;
        if (isLineComment) {
            commentsToKeep = currentComments.slice(1);
        }
    }

    // Determine which comments to include in final prefix
    let finalComments: any[];
    if (preserveRemovedComments && hasLeadingNewline(removedWs)) {
        // Transfer leading comments from removed element
        finalComments = [...removedComments, ...commentsToKeep];
    } else {
        finalComments = commentsToKeep;
    }

    // Determine which whitespace to use: preserve current if it has more blank lines
    const shouldPreserveCurrentWhitespace =
        !preserveRemovedComments &&
        countNewlines(currentWs) > countNewlines(removedWs);

    return produce(nextElement, draft => {
        draft.prefix = {
            kind: shouldPreserveCurrentWhitespace ? currentPrefix.kind : removedPrefix.kind,
            whitespace: shouldPreserveCurrentWhitespace ? currentWs : removedWs,
            comments: finalComments.length > 0 ? finalComments : []
        };
    });
}
