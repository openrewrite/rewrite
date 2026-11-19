/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.xml;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.style.Style;
import org.openrewrite.xml.style.Autodetect;
import org.openrewrite.xml.style.TabsAndIndentsStyle;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AddToTagVisitor<P> extends XmlVisitor<P> {
    private final Xml.Tag scope;
    private final Xml.Tag tagToAdd;

    @Nullable
    private final Comparator<Content> tagComparator;

    private final boolean allowDuplicates;

    public AddToTagVisitor(Xml.Tag scope, Xml.Tag tagToAdd) {
        this(scope, tagToAdd, null, false);
    }

    public AddToTagVisitor(Xml.Tag scope, Xml.Tag tagToAdd, boolean allowDuplicates) {
        this(scope, tagToAdd, null, allowDuplicates);
    }

    public AddToTagVisitor(Xml.Tag scope, Xml.Tag tagToAdd, @Nullable Comparator<Content> tagComparator) {
        this(scope, tagToAdd, tagComparator, false);
    }

    public AddToTagVisitor(Xml.Tag scope, Xml.Tag tagToAdd, @Nullable Comparator<Content> tagComparator, boolean allowDuplicates) {
        this.scope = scope;
        this.tagToAdd = tagToAdd;
        this.tagComparator = tagComparator;
        this.allowDuplicates = allowDuplicates;
    }

    @Override
    public Xml visitTag(Xml.Tag t, P p) {
        if (scope.isScope(t)) {
            if (!allowDuplicates && t.getContent() != null) {
                for (Content existing : t.getContent()) {
                    if (existing instanceof Xml.Tag && SemanticallyEqual.areEqual(existing, tagToAdd)) {
                        return super.visitTag(t, p);
                    }
                }
            }
            assert getCursor().getParent() != null;
            boolean expandClosing = t.getClosing() == null || !t.getClosing().getPrefix().contains("\n");
            if (t.getClosing() == null) {
                t = t.withClosing(autoFormat(new Xml.Tag.Closing(Tree.randomId(), "\n",
                                Markers.EMPTY, t.getName(), ""), null, p, getCursor()))
                        .withBeforeTagDelimiterPrefix("");
            }

            //noinspection ConstantConditions
            if (!t.getClosing().getPrefix().contains("\n")) {
                t = t.withClosing(t.getClosing().withPrefix("\n"));
            }
            String parentIndent = indentation(t.getPrefix());
            if (expandClosing && parentIndent != null) {
                String prefix = t.getClosing().getPrefix();
                t = t.withClosing(t.getClosing().withPrefix(prefix.substring(0, prefix.lastIndexOf('\n') + 1) + parentIndent));
            }

            Xml.Tag formattedTagToAdd = tagToAdd;
            if (!formattedTagToAdd.getPrefix().contains("\n")) {
                formattedTagToAdd = formattedTagToAdd.withPrefix("\n");
            }
            formattedTagToAdd = autoFormat(formattedTagToAdd, null, p, getCursor());

            List<Content> content = t.getContent() == null ? new ArrayList<>() : new ArrayList<>(t.getContent());
            int insertionIndex = content.size();
            if (tagComparator != null) {
                for (int i = 0; i < content.size(); i++) {
                    if (tagComparator.compare(content.get(i), formattedTagToAdd) > 0) {
                        insertionIndex = i;
                        break;
                    }
                }
            }
            // A detected style describes the document as a whole. In mixed-style files the
            // adjacent children are a better guide, and none of them need to be reformatted.
            Xml.Tag indentationScope = content.isEmpty() ? scope : t;
            List<? extends Content> neighbours = indentationScope.getContent();
            boolean matched = false;
            if (neighbours != null && !neighbours.isEmpty()) {
                int nearest = Math.max(0, Math.min(insertionIndex - 1, neighbours.size() - 1));
                for (int pass = 0; pass < 2 && !matched; pass++) {
                    for (int distance = 0; distance < neighbours.size(); distance++) {
                        int index = nearest - distance;
                        if (index >= 0 && isIndentationReference(neighbours.get(index), pass == 0, parentIndent)) {
                            formattedTagToAdd = matchIndentation(formattedTagToAdd, indentationScope, neighbours.get(index));
                            matched = true;
                            break;
                        }
                        index = nearest + distance;
                        if (index < neighbours.size() && isIndentationReference(neighbours.get(index), pass == 0, parentIndent)) {
                            formattedTagToAdd = matchIndentation(formattedTagToAdd, indentationScope, neighbours.get(index));
                            matched = true;
                            break;
                        }
                    }
                }
            }
            if (!matched && parentIndent != null) {
                Xml.Document doc = getCursor().firstEnclosingOrThrow(Xml.Document.class);
                TabsAndIndentsStyle style = Style.from(TabsAndIndentsStyle.class, doc,
                        () -> Autodetect.detector().sample(doc).build().getStyle(TabsAndIndentsStyle.class));
                String step = style.getUseTabCharacter() ? "\t" : StringUtils.repeat(" ", style.getIndentSize());
                formattedTagToAdd = matchIndentation(formattedTagToAdd, t,
                        formattedTagToAdd.withPrefix("\n" + parentIndent + step));
            }
            content.add(insertionIndex, formattedTagToAdd);

            t = t.withContent(content);
        }

        return super.visitTag(t, p);
    }

    private static boolean isIndentationReference(Content content, boolean tagsOnly, @Nullable String parentIndent) {
        String indent = indentation(content.getPrefix());
        return indent != null && (content instanceof Xml.Tag || !tagsOnly && parentIndent != null &&
                indent.startsWith(parentIndent) && indent.length() > parentIndent.length());
    }

    static Xml.Tag matchIndentation(Xml.Tag formatted, Xml.Tag parent, Content neighbour) {
        String oldIndent = indentation(formatted.getPrefix());
        String newIndent = indentation(neighbour.getPrefix());
        if (oldIndent == null || newIndent == null) {
            return formatted;
        }
        String oldStep = childIndent(formatted, oldIndent);
        String parentIndent = indentation(parent.getPrefix());
        if (parentIndent == null && !parent.getPrefix().contains("\n")) {
            parentIndent = "";
        }
        String newStep = parentIndent != null && newIndent.startsWith(parentIndent) && newIndent.length() > parentIndent.length() ?
                newIndent.substring(parentIndent.length()) :
                neighbour instanceof Xml.Tag ? childIndent((Xml.Tag) neighbour, newIndent) : null;
        String localStep = newStep;
        return (Xml.Tag) new XmlVisitor<Integer>() {
            @Override
            public Xml preVisit(Xml tree, Integer unused) {
                String indent = indentation(tree.getPrefix());
                if (indent == null || !indent.startsWith(oldIndent)) {
                    return tree;
                }
                String relative = indent.substring(oldIndent.length());
                StringBuilder shifted = new StringBuilder(newIndent);
                int offset = 0;
                if (oldStep != null && localStep != null) {
                    while (relative.startsWith(oldStep, offset)) {
                        shifted.append(localStep);
                        offset += oldStep.length();
                    }
                }
                shifted.append(relative.substring(offset));
                return tree.withPrefix(tree.getPrefix().substring(0, tree.getPrefix().lastIndexOf('\n') + 1) + shifted);
            }
        }.visitNonNull(formatted, 0);
    }

    private static @Nullable String childIndent(Xml.Tag tag, String parentIndent) {
        for (Xml.Tag child : tag.getChildren()) {
            String indent = indentation(child.getPrefix());
            if (indent != null && indent.startsWith(parentIndent) && indent.length() > parentIndent.length()) {
                return indent.substring(parentIndent.length());
            }
        }
        return null;
    }

    private static @Nullable String indentation(String prefix) {
        int newline = prefix.lastIndexOf('\n');
        if (newline < 0) {
            return null;
        }
        String indent = prefix.substring(newline + 1);
        return indent.chars().allMatch(c -> c == ' ' || c == '\t') ? indent : null;
    }

    /**
     * Add a tag to the children of another tag
     *
     * @param parent the tag that will have 'newChild' added to its children
     * @param newChild the tag to add as a child of 'parent'
     * @param parentCursor A cursor pointing one level above 'parent'. Determines the final indentation of 'newChild'.
     *
     * @return 'parent' with 'newChild' amongst its child elements
     */
    public static Xml.Tag addToTag(Xml.Tag parent, Xml.Tag newChild, Cursor parentCursor) {
        return addToTag(parent, parent, newChild, parentCursor);
    }

    /**
     * Add a tag to the children of another tag
     *
     * @param parentScope a tag which contains 'parent' as a direct or transitive child element.
     * @param parent the tag that will have 'newChild' added to its children
     * @param newChild the tag to add as a child of 'parent'
     * @param parentCursor A cursor pointing one level above 'parentScope'. Determines the final indentation of 'newChild'.
     *
     * @return 'parentScope' which somewhere contains 'parent' with 'newChild' amongst its child elements
     */
    public static Xml.Tag addToTag(Xml.Tag parentScope, Xml.Tag parent, Xml.Tag newChild, Cursor parentCursor) {
        //noinspection ConstantConditions
        return (Xml.Tag) new AddToTagVisitor<Integer>(parent, newChild)
                .visitNonNull(parentScope, 0, parentCursor);
    }
}
