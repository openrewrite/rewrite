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
package org.openrewrite.yaml.service;

import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.CommentService;
import org.openrewrite.trait.Comments.Placement;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.indexOfSubList;

/**
 * {@link CommentService} for the YAML LST model. YAML has no dedicated comment node; comments live
 * as {@code # ...} lines inside a node's {@code prefix} string (conventionally on
 * {@link Yaml.Mapping.Entry}). This service reads and rewrites those lines, placing an added comment
 * immediately above the node, one {@code #} line per line of comment text.
 */
@Incubating(since = "8.86.0")
public class YamlCommentService extends CommentService {

    @Override
    public List<String> getComments(Cursor cursor) {
        Object tree = cursor.getValue();
        if (!(tree instanceof Yaml)) {
            return emptyList();
        }
        List<String> texts = new ArrayList<>();
        for (String line : ((Yaml) tree).getPrefix().split("\n", -1)) {
            String stripped = stripLeadingWhitespace(line);
            if (stripped.startsWith("#")) {
                texts.add(stripped.substring(1));
            }
        }
        return texts;
    }

    @Override
    public Tree addComment(Cursor cursor, String text, boolean multiline, Placement placement) {
        // YAML keeps comments in the prefix string, so FIRST_CHILD behaves as BEFORE.
        Tree tree = cursor.getValue();
        if (!(tree instanceof Yaml) || hasEquivalentComment(cursor, text)) {
            return tree;
        }
        Yaml yaml = (Yaml) tree;
        String prefix = yaml.getPrefix();
        String indent = extractIndent(prefix);
        StringBuilder comments = new StringBuilder(prefix);
        for (String line : text.split("\\R")) {
            comments.append('#').append(line).append('\n').append(indent);
        }
        return yaml.withPrefix(comments.toString());
    }

    @Override
    private boolean hasEquivalentComment(Cursor cursor, String text) {
        return indexOfSubList(trimmed(leadingComments(cursor)), trimmed(asList(text.split("\\R")))) >= 0;
    }

    /**
     * The parser attaches the comments above the first entry of a file to the enclosing document, so
     * gather comments from every ancestor that starts where this element does.
     */
    private List<String> leadingComments(Cursor cursor) {
        List<String> comments = new ArrayList<>(getComments(cursor));
        for (Cursor c = cursor; c.getParent() != null && startsWith(c.getParentTreeCursor().getValue(), c.getValue()); ) {
            c = c.getParentTreeCursor();
            comments.addAll(0, getComments(c));
        }
        return comments;
    }

    private static boolean startsWith(Object parent, Tree child) {
        if (parent instanceof Yaml.Mapping) {
            Yaml.Mapping mapping = (Yaml.Mapping) parent;
            return mapping.getOpeningBracePrefix() == null && !mapping.getEntries().isEmpty() &&
                    mapping.getEntries().get(0).getId().equals(child.getId());
        }
        if (parent instanceof Yaml.Document) {
            Yaml.Document document = (Yaml.Document) parent;
            return !document.isExplicit() && document.getBlock().getId().equals(child.getId());
        }
        return false;
    }

    @Override
    public Tree removeComment(Cursor cursor, String text) {
        Tree tree = cursor.getValue();
        if (!(tree instanceof Yaml) || !hasComment(cursor, text)) {
            return tree;
        }
        Yaml yaml = (Yaml) tree;
        List<String> kept = new ArrayList<>();
        for (String line : yaml.getPrefix().split("\n", -1)) {
            String stripped = stripLeadingWhitespace(line);
            if (stripped.startsWith("#") && text.equals(stripped.substring(1))) {
                continue;
            }
            kept.add(line);
        }
        return yaml.withPrefix(String.join("\n", kept));
    }

    @Override
    public Tree replaceComment(Cursor cursor, String existingText, String newText) {
        Tree tree = cursor.getValue();
        if (!(tree instanceof Yaml) || existingText.equals(newText) || !hasComment(cursor, existingText)) {
            return tree;
        }
        Yaml yaml = (Yaml) tree;
        List<String> lines = new ArrayList<>();
        for (String line : yaml.getPrefix().split("\n", -1)) {
            int hash = line.indexOf('#');
            if (hash >= 0 && isOnlyWhitespace(line.substring(0, hash)) &&
                    existingText.equals(line.substring(hash + 1))) {
                lines.add(line.substring(0, hash + 1) + newText);
            } else {
                lines.add(line);
            }
        }
        return yaml.withPrefix(String.join("\n", lines));
    }

    private static List<String> trimmed(List<String> lines) {
        List<String> trimmed = new ArrayList<>(lines.size());
        for (String line : lines) {
            trimmed.add(line.trim());
        }
        return trimmed;
    }

    private static String extractIndent(String prefix) {
        int lastNewline = prefix.lastIndexOf('\n');
        return lastNewline >= 0 ? prefix.substring(lastNewline + 1) : prefix;
    }

    private static String stripLeadingWhitespace(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return s.substring(i);
    }

    private static boolean isOnlyWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
