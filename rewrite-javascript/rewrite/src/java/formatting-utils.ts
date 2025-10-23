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
 * - If the next element has no comments (or only an inline trailing comment to remove),
 *   uses the removed element's prefix whitespace
 * - If the next element has comments to preserve, keeps the next element's whitespace
 *   to maintain proper spacing (e.g., blank lines before comments)
 * - Removes inline line comments (//...) that were on the removed element's line
 *
 * @param removedElement The element that was removed
 * @param nextElement The element that follows the removed one
 * @returns The next element with adjusted prefix, or the original if no changes needed
 */
export function applyRemovedElementPrefix<T extends J>(removedElement: J, nextElement: T): T {
    if (!removedElement.prefix || !nextElement.prefix) {
        return nextElement;
    }

    const removedPrefix = removedElement.prefix;
    const currentPrefix = nextElement.prefix;
    const currentComments = currentPrefix.comments || [];

    // If the next element has no comments, just use the removed element's prefix entirely
    if (currentComments.length === 0) {
        if (currentPrefix === removedPrefix) {
            return nextElement;
        }
        return produce(nextElement, draft => {
            draft.prefix = removedPrefix;
        });
    }

    // The next element has comments - check if we need to remove inline line comments
    const currentWhitespace = currentPrefix.whitespace || '';
    const hasLeadingNewline = /[\r\n]/.test(currentWhitespace);

    let commentsToKeep = currentComments;
    if (!hasLeadingNewline && currentComments.length > 0) {
        // First comment has no leading newline - check if it's an inline line comment
        const firstComment: any = currentComments[0];
        const commentText = firstComment.text || firstComment.message || '';
        const isLineComment = commentText.includes('//') || firstComment.multiline === false;

        if (isLineComment) {
            // Remove the inline line comment
            commentsToKeep = currentComments.slice(1);
        }
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
