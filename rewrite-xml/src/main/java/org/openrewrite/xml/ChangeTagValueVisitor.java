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
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.tree.Xml;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

public class ChangeTagValueVisitor<P> extends XmlVisitor<P> {

    private final Xml.@Nullable Tag scope;
    private final @Nullable String value;

    public ChangeTagValueVisitor(Xml.@Nullable Tag scope, @Nullable String value) {
        this.scope = scope;
        this.value = value;
    }

    @Override
    public Xml visitTag(Xml.Tag tag, P p) {
        Xml.Tag t = (Xml.Tag) super.visitTag(tag, p);
        if (scope != null && scope.isScope(t)) {
            if (value == null) {
                doAfterVisit(new RemoveContentVisitor<>(t, false, true));
                return tag;
            }
            String prefix = "";
            String afterText = "";
            if (t.getContent() != null && t.getContent().size() == 1 && t.getContent().get(0) instanceof Xml.CharData) {
                Xml.CharData existingValue = (Xml.CharData) t.getContent().get(0);

                if (existingValue.getText().equals(value)) {
                    return tag;
                }

                // if the previous content was also character data, preserve its prefix and afterText
                prefix = existingValue.getPrefix();
                afterText = existingValue.getAfterText();
            }
            t = t.withContent(singletonList(new Xml.CharData(randomId(),
                    prefix, Markers.EMPTY, false, value, afterText)));
        }

        return t;
    }
}
