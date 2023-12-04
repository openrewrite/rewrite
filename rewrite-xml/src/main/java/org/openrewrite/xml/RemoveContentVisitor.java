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

import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;

public class RemoveContentVisitor<P> extends XmlVisitor<P> {

    private final Content scope;
    private final boolean removeEmptyAncestors;

    public RemoveContentVisitor(Content tag, boolean removeEmptyAncestors) {
        this.scope = tag;
        this.removeEmptyAncestors = removeEmptyAncestors;
    }

    @Override
    public Xml visitTag(Xml.Tag tag, P p) {
        Xml.Tag t = (Xml.Tag) super.visitTag(tag, p);

        if (t.getContent() != null) {
            for (Content content : t.getContent()) {
                if (scope.isScope(content)) {
                    List<Content> contents = new ArrayList<>(t.getContent());
                    contents.remove(content);

                    if (removeEmptyAncestors && contents.isEmpty() && t.getAttributes().isEmpty()) {
                        if (getCursor().getParentOrThrow().getValue() instanceof Xml.Document) {
                            return t.withContent(null).withClosing(null);
                        } else {
                            doAfterVisit(new RemoveContentVisitor<>(t, true));
                        }
                    } else {
                        return t.withContent(contents);
                    }
                }
            }
        }

        return t;
    }
}
