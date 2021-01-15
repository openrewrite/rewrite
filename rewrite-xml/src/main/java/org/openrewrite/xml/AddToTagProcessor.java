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

import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AddToTagProcessor<P> extends XmlProcessor<P> {
    private final Xml.Tag scope;
    private final Xml.Tag tagToAdd;

    @Nullable
    private final Comparator<Xml.Tag> tagComparator;

    public AddToTagProcessor(Xml.Tag scope, Xml.Tag tagToAdd) {
        this(scope, tagToAdd, null);
    }

    public AddToTagProcessor(Xml.Tag scope, Xml.Tag tagToAdd, @Nullable Comparator<Xml.Tag> tagComparator) {
        this.scope = scope;
        this.tagToAdd = tagToAdd;
        this.tagComparator = tagComparator;
        setCursoringOn();
    }

    @Override
    public Xml visitTag(Xml.Tag t, P p) {
        if (scope.isScope(t)) {
            boolean formatRequested = false;
            if (t.getClosing() == null) {
                t = t.withClosing(new Xml.Tag.Closing(Tree.randomId(), "\n",
                        Markers.EMPTY, t.getName(), ""))
                        .withBeforeTagDelimiterPrefix("");
                doAfterVisit(new AutoFormatProcessor<>(t));
                formatRequested = true;
            }

            //noinspection ConstantConditions
            if (!t.getClosing().getPrefix().contains("\n")) {
                t = t.withClosing(t.getClosing().withPrefix("\n"));
            }

            Xml.Tag formattedTagToAdd = tagToAdd;
            if (!formattedTagToAdd.getPrefix().contains("\n")) {
                formattedTagToAdd = formattedTagToAdd.withPrefix("\n");
            }

            List<Xml.Tag> content = t.getContent() == null ? new ArrayList<>() : new ArrayList<>(t.getChildren());
            content.add(formattedTagToAdd);

            if (tagComparator != null) {
                content.sort(tagComparator);
            }

            t = t.withContent(content);

            if (!formatRequested) {
                doAfterVisit(new AutoFormatProcessor<>(formattedTagToAdd));
            }
        }
        return super.visitTag(t, p);
    }
}

