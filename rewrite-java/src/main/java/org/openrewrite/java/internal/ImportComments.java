/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.internal;

import lombok.Getter;
import lombok.Value;
import lombok.With;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Marker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.StringUtils.hasLineBreak;

/**
 * Keeps the import-section header in place and temporarily attaches end-of-line comments to their import.
 * The markers travel with imports through sorting and unused-import removal, and are removed by {@link #restore}.
 */
public final class ImportComments {
    private final Space header;
    @Getter
    private final J.CompilationUnit prepared;

    public ImportComments(J.CompilationUnit cu) {
        header = Space.firstPrefix(cu.getImports());
        List<J.Import> imports = new ArrayList<>(cu.getImports());
        imports.set(0, imports.get(0).withPrefix(header.withComments(emptyList())));
        Space following = following(cu);
        for (int i = 0; i < imports.size(); i++) {
            Space next = i + 1 < imports.size() ? imports.get(i + 1).getPrefix() : following;
            int count = 0;
            String whitespace = next.getWhitespace();
            while (count < next.getComments().size() && !hasLineBreak(whitespace)) {
                whitespace = next.getComments().get(count++).getSuffix();
            }
            if (count == 0) {
                continue;
            }
            J.Import anImport = imports.get(i);
            Space trailing = Space.build(next.getWhitespace(), next.getComments().subList(0, count));
            imports.set(i, anImport.withMarkers(anImport.getMarkers().add(new Trailing(randomId(), trailing))));
            Space remaining = Space.build(whitespace, next.getComments().subList(count, next.getComments().size()));
            if (i + 1 < imports.size()) {
                imports.set(i + 1, imports.get(i + 1).withPrefix(remaining));
            } else {
                following = remaining;
            }
        }
        prepared = withFollowing(cu.withImports(imports), following);
    }

    public J.CompilationUnit restore(J.CompilationUnit cu) {
        List<J.Import> imports = new ArrayList<>(cu.getImports());
        Space following = following(cu);
        Set<UUID> restored = new HashSet<>();
        // Unfolding copies the marker to each replacement. Only the last replacement gets the trailing comment.
        for (int i = imports.size() - 1; i >= 0; i--) {
            J.Import anImport = imports.get(i);
            Trailing trailing = anImport.getMarkers().findFirst(Trailing.class).orElse(null);
            if (trailing == null) {
                continue;
            }
            imports.set(i, anImport.withMarkers(anImport.getMarkers().removeByType(Trailing.class)));
            if (!restored.add(trailing.getId())) {
                continue;
            }
            Space next = i + 1 < imports.size() ? imports.get(i + 1).getPrefix() : following;
            List<Comment> comments = ListUtils.mapLast(trailing.getSpace().getComments(),
                    comment -> comment.withSuffix(next.getWhitespace()));
            Space prefix = trailing.getSpace().withComments(ListUtils.concatAll(comments, next.getComments()));
            if (i + 1 < imports.size()) {
                imports.set(i + 1, imports.get(i + 1).withPrefix(prefix));
            } else {
                following = prefix;
            }
        }
        if (!imports.isEmpty()) {
            J.Import first = imports.get(0);
            imports.set(0, first.withPrefix(header.withComments(
                    ListUtils.concatAll(header.getComments(), first.getComments()))));
        } else if (!header.getComments().isEmpty()) {
            following = header.withComments(ListUtils.concatAll(header.getComments(), following.getComments()));
        }
        return withFollowing(cu.withImports(imports), following);
    }

    /** Move comments from imports being folded above the surviving wildcard. */
    public static J.Import foldComments(List<JRightPadded<J.Import>> imports) {
        J.Import first = imports.get(0).getElement();
        List<Comment> comments = first.getComments();
        for (int i = 1; i < imports.size(); i++) {
            JRightPadded<J.Import> padded = imports.get(i);
            J.Import anImport = padded.getElement();
            List<Comment> moved = ListUtils.concatAll(anImport.getComments(), padded.getAfter().getComments());
            Trailing trailing = anImport.getMarkers().findFirst(Trailing.class).orElse(null);
            if (trailing != null) {
                moved = ListUtils.concatAll(moved, trailing.getSpace().getComments());
            }
            String newline = first.getPrefix().getWhitespace().contains("\r\n") ||
                    anImport.getPrefix().getWhitespace().contains("\r\n") ? "\r\n" : "\n";
            comments = ListUtils.concatAll(comments, ListUtils.map(moved,
                    comment -> hasLineBreak(comment.getSuffix()) ? comment : comment.withSuffix(newline)));
        }
        return first.withPrefix(first.getPrefix().withComments(comments));
    }

    public static boolean hasTrailingComments(J.Import anImport) {
        return anImport.getMarkers().findFirst(Trailing.class).isPresent();
    }

    private static Space following(J.CompilationUnit cu) {
        return cu.getClasses().isEmpty() ? cu.getEof() : cu.getClasses().get(0).getPrefix();
    }

    private static J.CompilationUnit withFollowing(J.CompilationUnit cu, Space space) {
        if (following(cu).equals(space)) {
            return cu;
        }
        return cu.getClasses().isEmpty() ? cu.withEof(space) :
                cu.withClasses(ListUtils.mapFirst(cu.getClasses(), cd -> cd.withPrefix(space)));
    }

    @Value
    @With
    private static class Trailing implements Marker {
        UUID id;
        Space space;
    }
}
