/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.trait.Comments.Placement;

import java.util.List;

/**
 * A language-agnostic contract for reading and writing the comments associated with an LST element.
 * Each language module provides its own implementation (knowing how that language models comments)
 * and registers it via {@link SourceFile#service(Class)}.
 * <p>
 * Operations take a {@link Cursor} rather than a bare {@link Tree} because, in some languages, the
 * comments "on" an element are not stored on the element itself. In XML, for example, a comment
 * preceding an element is a sibling within the parent's content, so the implementation must walk up
 * the cursor to find and edit it. Consequently a mutation returns the modified <em>owner</em> of the
 * comment: the element itself for prefix-based languages (Java, JSON, YAML), or the parent for XML.
 * Callers should apply the returned tree at the appropriate level (the trait does this implicitly by
 * being used from the corresponding visit method).
 * <p>
 * <strong>Referential equality contract:</strong> when a mutation makes no semantic change (adding a
 * comment that already exists, or removing/editing one that does not), the implementation
 * <em>must</em> return the exact same owner instance it would otherwise modify, so that recipes using
 * this service always stabilize within a single cycle.
 * <p>
 * Recipes should not generally use this service directly; prefer the
 * {@code org.openrewrite.trait.Comments} trait, which resolves the correct implementation for the
 * source file under the cursor.
 */
@Incubating(since = "8.86.0")
public abstract class CommentService {

    /**
     * @param cursor A cursor whose value is the element whose associated comments to read.
     * @return The textual content of each associated comment, in source order. Comment kinds without
     * simple textual content (e.g. structured Javadoc) may be omitted by an implementation.
     */
    public abstract List<String> getComments(Cursor cursor);

    /**
     * @return {@code true} if a comment with exactly this text is already associated with the element.
     */
    public boolean hasComment(Cursor cursor, String text) {
        return getComments(cursor).contains(text);
    }

    /**
     * @return {@code true} if the element already has a comment <em>equivalent</em> to {@code text},
     * ignoring surrounding whitespace, line-ending differences, and the line/block comment style. This
     * is the check the {@code addComment} methods use to stay idempotent, so that (for example) an
     * existing {@code // note} is treated as already satisfying a request to add {@code /* note *​/}.
     */
    public boolean hasEquivalentComment(Cursor cursor, String text) {
        String normalized = normalizeForEquivalence(text);
        for (String existing : getComments(cursor)) {
            if (normalizeForEquivalence(existing).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@code true} if the two comment bodies are equivalent up to surrounding whitespace and
     * line-ending differences.
     */
    protected static boolean equivalent(String a, String b) {
        return normalizeForEquivalence(a).equals(normalizeForEquivalence(b));
    }

    private static String normalizeForEquivalence(String text) {
        return text.trim().replaceAll("\\R", "\n");
    }

    /**
     * Add a comment immediately preceding the element at the cursor ({@link Placement#BEFORE}).
     *
     * @see #addComment(Cursor, String, boolean, Placement)
     */
    public Tree addComment(Cursor cursor, String text, boolean multiline) {
        return addComment(cursor, text, multiline, Placement.BEFORE);
    }

    /**
     * Add a comment relative to the element at the cursor, at the given {@link Placement}. Idempotent:
     * adding a comment {@link #hasEquivalentComment equivalent} to one that already exists (at that
     * placement) returns the unchanged owner (same instance). Implementations that have no notion of
     * children for the element treat {@link Placement#FIRST_CHILD} as {@link Placement#BEFORE}.
     *
     * @param cursor    A cursor whose value is the element to comment.
     * @param text      The comment body (without language-specific comment delimiters).
     * @param multiline Whether to render a block rather than a line comment, where the distinction is
     *                  meaningful for the language.
     * @param placement Where to place the comment relative to the element.
     * @return The comment's owner with the comment added, or the unchanged owner (same instance) if it
     * already had an equivalent comment or does not support comments.
     */
    public abstract Tree addComment(Cursor cursor, String text, boolean multiline, Placement placement);

    /**
     * Add a comment as {@link #addComment(Cursor, String, boolean, Placement)}, but control the
     * whitespace placed between the inserted comment and the element it precedes.
     * <p>
     * This only has an effect for languages that store comments as prefix metadata of the element
     * (Java, Kotlin, Groovy, JSON), where the suffix determines whether the comment lands on the line
     * above (a suffix containing a newline) or inline (e.g. a single space). It is the caller's
     * responsibility to supply a suffix that produces well-formed output; passing {@code null} keeps
     * the default of reusing the element's own leading indentation. Languages whose comments are
     * sibling nodes (XML) or embedded in prefix strings (YAML) ignore the suffix and behave as
     * {@link #addComment(Cursor, String, boolean, Placement)}.
     *
     * @param suffix The whitespace to place after the comment and before the element, or {@code null}
     *               to use the element's own leading indentation.
     */
    public Tree addComment(Cursor cursor, String text, boolean multiline, Placement placement, @Nullable String suffix) {
        return addComment(cursor, text, multiline, placement);
    }

    /**
     * Remove every comment associated with the element whose text exactly matches {@code text}.
     *
     * @return The owner with matching comments removed, or the unchanged owner (same instance) if there
     * were none.
     */
    public abstract Tree removeComment(Cursor cursor, String text);

    /**
     * Replace the text of every associated comment matching {@code existingText} with {@code newText},
     * preserving the comment's position and style.
     *
     * @return The owner with matching comments edited, or the unchanged owner (same instance) if there
     * were none.
     */
    public abstract Tree replaceComment(Cursor cursor, String existingText, String newText);
}
