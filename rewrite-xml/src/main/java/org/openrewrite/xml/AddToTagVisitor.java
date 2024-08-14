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
import org.openrewrite.marker.Markers;
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

    public AddToTagVisitor(Xml.Tag scope, Xml.Tag tagToAdd) {
        this(scope, tagToAdd, null);
    }

    public AddToTagVisitor(Xml.Tag scope, Xml.Tag tagToAdd, @Nullable Comparator<Content> tagComparator) {
        this.scope = scope;
        this.tagToAdd = tagToAdd;
        this.tagComparator = tagComparator;
    }

    @Override
    public Xml visitTag(Xml.Tag t, P p) {
        if (scope.isScope(t)) {
            assert getCursor().getParent() != null;
            if (t.getClosing() == null) {
                t = t.withClosing(autoFormat(new Xml.Tag.Closing(Tree.randomId(), "\n",
                                Markers.EMPTY, t.getName(), ""), null, p, getCursor()))
                        .withBeforeTagDelimiterPrefix("");
            }

            //noinspection ConstantConditions
            if (!t.getClosing().getPrefix().contains("\n")) {
                t = t.withClosing(t.getClosing().withPrefix("\n"));
            }

            Xml.Tag formattedTagToAdd = tagToAdd;
            if (!formattedTagToAdd.getPrefix().contains("\n")) {
                formattedTagToAdd = formattedTagToAdd.withPrefix("\n");
            }
            formattedTagToAdd = autoFormat(formattedTagToAdd, null, p, getCursor());

            List<Content> content = t.getContent() == null ? new ArrayList<>() : new ArrayList<>(t.getContent());
            if (tagComparator != null) {
                int i = 0;
                for (; i < content.size(); i++) {
                    if (tagComparator.compare(content.get(i), formattedTagToAdd) > 0) {
                        content.add(i, formattedTagToAdd);
                        break;
                    }
                }
                if (i == content.size()) {
                    content.add(formattedTagToAdd);
                }
            } else {
                content.add(formattedTagToAdd);
            }

            t = t.withContent(content);
        }

        return super.visitTag(t, p);
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
        return (Xml.Tag) new AddToTagVisitor<Void>(parent, newChild)
                .visitNonNull(parentScope, null, parentCursor);
    }
}
