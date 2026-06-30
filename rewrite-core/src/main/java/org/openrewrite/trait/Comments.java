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
package org.openrewrite.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.internal.CommentService;

import java.util.List;

/**
 * A language-agnostic trait for reading and writing the comments attached to any LST element.
 * <p>
 * This is the production-friendly counterpart to {@code SearchResult.found(tree, "...")}: where a
 * search result renders as a transient {@code /*~~>*​/} marker that is stripped from committed
 * source, this trait inserts (or edits) a <em>real</em> comment node that survives printing and
 * re-parsing and can itself be edited or removed later.
 * <p>
 * The trait resolves the language-specific {@link CommentService} from the {@link SourceFile} under
 * the cursor, so the same call site works for Java, Kotlin, Groovy, and any other language that
 * registers a {@code CommentService}.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * new JavaIsoVisitor<ExecutionContext>() {
 *     @Override
 *     public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration m, ExecutionContext ctx) {
 *         return Comments.of(getCursor()).addComment(" TODO: revisit");
 *     }
 * }
 * }</pre>
 */
@Incubating(since = "8.86.0")
@Value
public class Comments implements Trait<Tree> {
    Cursor cursor;

    /**
     * @return The text of each comment associated with this element, in source order.
     */
    public List<String> getComments() {
        return service().getComments(cursor);
    }

    public boolean hasComment(String text) {
        return service().hasComment(cursor, text);
    }

    /**
     * Add a line comment immediately preceding this element ({@link Placement#BEFORE}). Idempotent.
     *
     * @return The comment's owner with the comment added, typed as the caller expects. This is the
     * element itself for prefix-based languages and the parent for languages (such as XML) where a
     * preceding comment is a sibling.
     */
    public <T extends Tree> T comment(String text) {
        return comment(text, Placement.BEFORE);
    }

    /**
     * Add a line comment relative to this element at the given {@link Placement}. Idempotent.
     *
     * @param placement Where to place the comment relative to this element.
     * @return The comment's owner with the comment added, typed as the caller expects. The owner
     * depends on the placement and language: e.g. {@link Placement#FIRST_CHILD} returns the element
     * itself, while {@link Placement#BEFORE} returns the parent for languages where a preceding
     * comment is a sibling.
     */
    public <T extends Tree> T comment(String text, Placement placement) {
        return comment(text, placement, null);
    }

    /**
     * Add a line comment relative to this element, controlling the whitespace placed between the
     * comment and the element it precedes. Idempotent.
     *
     * @param placement Where to place the comment relative to this element.
     * @param suffix    The whitespace separating the comment from the element (e.g. a newline plus
     *                  indentation to put it on the line above, or a space to keep it inline), or
     *                  {@code null} to use the element's own leading indentation. Honored only by
     *                  languages that store comments as element prefix metadata; see
     *                  {@link CommentService#addComment(Cursor, String, boolean, Placement, String)}.
     * @return The comment's owner with the comment added, typed as the caller expects.
     */
    public <T extends Tree> T comment(String text, Placement placement, @Nullable String suffix) {
        return add(text, false, placement, suffix);
    }

    /**
     * Add a block ({@code /* ... *​/}) comment immediately preceding this element
     * ({@link Placement#BEFORE}). Idempotent.
     *
     * @return The comment's owner with the comment added, typed as the caller expects.
     */
    public <T extends Tree> T multilineComment(String text) {
        return multilineComment(text, Placement.BEFORE);
    }

    /**
     * Add a block comment relative to this element at the given {@link Placement}. Idempotent.
     *
     * @param placement Where to place the comment relative to this element.
     * @return The comment's owner with the comment added, typed as the caller expects.
     */
    public <T extends Tree> T multilineComment(String text, Placement placement) {
        return multilineComment(text, placement, null);
    }

    /**
     * Add a block comment relative to this element, controlling the whitespace placed between the
     * comment and the element it precedes. Idempotent.
     *
     * @param placement Where to place the comment relative to this element.
     * @param suffix    The whitespace separating the comment from the element, or {@code null} to use
     *                  the element's own leading indentation. Honored only by languages that store
     *                  comments as element prefix metadata; see
     *                  {@link CommentService#addComment(Cursor, String, boolean, Placement, String)}.
     * @return The comment's owner with the comment added, typed as the caller expects.
     */
    public <T extends Tree> T multilineComment(String text, Placement placement, @Nullable String suffix) {
        return add(text, true, placement, suffix);
    }

    private <T extends Tree> T add(String text, boolean multiline, Placement placement, @Nullable String suffix) {
        //noinspection unchecked
        return (T) service().addComment(cursor, text, multiline, placement, suffix);
    }

    /**
     * Remove every comment associated with this element whose text exactly matches {@code text}.
     */
    public <T extends Tree> T removeComment(String text) {
        //noinspection unchecked
        return (T) service().removeComment(cursor, text);
    }

    /**
     * Edit every comment associated with this element matching {@code existingText}, preserving its
     * position.
     */
    public <T extends Tree> T replaceComment(String existingText, String newText) {
        //noinspection unchecked
        return (T) service().replaceComment(cursor, existingText, newText);
    }

    private CommentService service() {
        SourceFile sourceFile = cursor.firstEnclosing(SourceFile.class);
        if (sourceFile == null) {
            throw new IllegalStateException("The Comments trait requires a cursor rooted at a " +
                    "SourceFile in order to resolve the language-specific CommentService.");
        }
        return sourceFile.service(CommentService.class);
    }

    /**
     * Ergonomic entry point mirroring {@code SearchResult.found(...)}: obtain the comments trait
     * for the tree at this cursor.
     */
    public static Comments of(Cursor cursor) {
        return new Comments(cursor);
    }

    /**
     * Where to place an added comment relative to the element it is associated with.
     */
    public enum Placement {
        /**
         * Immediately before the element — a preceding sibling, or (for prefix-based languages) on
         * the line above. Supported by every language; the default.
         */
        BEFORE,

        /**
         * As the first child of the element. Meaningful for container elements (e.g. an XML tag).
         * Languages that have no notion of children for the element fall back to {@link #BEFORE}.
         */
        FIRST_CHILD
    }

    /**
     * Matches every {@link Tree}, exposing its comments. Useful with
     * {@link TraitMatcher#asVisitor} when a recipe wants to inspect or rewrite comments uniformly.
     */
    public static class Matcher extends SimpleTraitMatcher<Comments> {
        @Override
        protected @Nullable Comments test(Cursor cursor) {
            return cursor.getValue() instanceof Tree ? new Comments(cursor) : null;
        }
    }
}
