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
import {produce} from "immer";

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
export function applyRemovedElementPrefix<T extends J>(removedElement: J, nextElement: T, preserveRemovedComments: boolean = false): T {
    if (!removedElement.prefix || !nextElement.prefix) {
        return nextElement;
    }

    const removedPrefix = removedElement.prefix;
    const currentPrefix = nextElement.prefix;
    const currentComments = currentPrefix.comments || [];

    // If the next element has no comments, apply appropriate formatting
    if (currentComments.length === 0) {
        if (currentPrefix === removedPrefix) {
            return nextElement;
        }

        // If preserving comments, transfer file header comments and use removed element's whitespace
        if (preserveRemovedComments) {
            const removedComments = removedPrefix.comments || [];

            // Check if removed element has leading comments (not inline trailing comments)
            // These are likely file headers and should be preserved
            const removedWhitespace = removedPrefix.whitespace || '';
            const hasLeadingNewlineBeforeComments = removedComments.length === 0 || /[\r\n]/.test(removedWhitespace);

            // Transfer file header comments from removed element (non-inline comments only)
            const commentsToTransfer = hasLeadingNewlineBeforeComments ? removedComments : [];

            return produce(nextElement, draft => {
                draft.prefix = {
                    ...removedPrefix,
                    comments: commentsToTransfer.length > 0 ? commentsToTransfer : []
                };
            });
        }

        // When not preserving comments, preserve blank lines from the current prefix
        const removedWhitespace = removedPrefix.whitespace || '';
        const currentWhitespace = currentPrefix.whitespace || '';
        const removedNewlines = (removedWhitespace.match(/\r?\n/g) || []).length;
        const currentNewlines = (currentWhitespace.match(/\r?\n/g) || []).length;

        // If the current element has more newlines (i.e., blank lines), preserve them
        if (currentNewlines > removedNewlines) {
            return nextElement;
        }

        // Use removed element's whitespace but don't transfer comments from removed middle elements
        return produce(nextElement, draft => {
            draft.prefix = {
                kind: removedPrefix.kind,
                whitespace: removedWhitespace,
                comments: []
            };
        });
    }

    // The next element has comments - check if we need to remove inline/trailing line comments
    const currentWhitespace = currentPrefix.whitespace || '';
    const hasLeadingNewline = /[\r\n]/.test(currentWhitespace);

    let commentsToKeep = currentComments;

    // Check for truly inline trailing line comments that should be removed
    if (currentComments.length > 0) {
        const firstComment: any = currentComments[0];
        const commentText = firstComment.text || firstComment.message || '';
        const isLineComment = commentText.includes('//') || firstComment.multiline === false;

        if (isLineComment && !hasLeadingNewline) {
            // Only remove comments that are truly inline (no newline before them)
            // Comments on their own line (with leading newline) are preserved as they're
            // likely leading comments for the next element, not trailing comments from removed element
            commentsToKeep = currentComments.slice(1);
        }
    }

    // If preserving comments, preserve leading comments from removed element
    if (preserveRemovedComments) {
        const removedComments = removedPrefix.comments || [];

        // Only transfer leading comments (not inline trailing comments)
        const removedWhitespace = removedPrefix.whitespace || '';
        const hasLeadingComments = removedComments.length === 0 || /[\r\n]/.test(removedWhitespace);

        // Combine leading comments from removed element with filtered comments from current element
        const allComments = hasLeadingComments ? [...removedComments, ...commentsToKeep] : commentsToKeep;

        // When preserving comments, always use the removed element's whitespace
        return produce(nextElement, draft => {
            draft.prefix = {
                ...removedPrefix,
                comments: allComments.length > 0 ? allComments : []
            };
        });
    }

    // If we still have comments after filtering, use removed element's whitespace with filtered comments
    if (commentsToKeep.length > 0) {
        return produce(nextElement, draft => {
            draft.prefix = {
                ...removedPrefix,
                comments: commentsToKeep
            };
        });
    }

    // No comments left after filtering, use removed element's prefix entirely
    return produce(nextElement, draft => {
        draft.prefix = removedPrefix;
    });
}
