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

import org.openrewrite.marker.Markers;
import org.openrewrite.xml.tree.Xml;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

public class ChangeTagValueProcessor<P> extends XmlProcessor<P> {

    private final Xml.Tag scope;
    private final String value;

    public ChangeTagValueProcessor(Xml.Tag scope, String value) {
        this.scope = scope;
        this.value = value;
        setCursoringOn();
    }

    @Override
    public Xml visitTag(Xml.Tag tag, P p) {

        if (scope.isScope(tag)) {
            String prefix = "";
            String afterText = "";
            if (tag.getContent() != null && tag.getContent().size() == 1 && tag.getContent().get(0) instanceof Xml.CharData) {
                Xml.CharData existingValue = (Xml.CharData) tag.getContent().get(0);

                if (existingValue.getText().equals(value)) {
                    return tag;
                }

                // if the previous content was also character data, preserve its prefix and afterText
                prefix = existingValue.getPrefix();
                afterText = existingValue.getAfterText();
            }
            tag = tag.withContent(singletonList(new Xml.CharData(randomId(), false, value,
                    afterText, prefix, Markers.EMPTY)));
        }

        return super.visitTag(tag, p);
    }
}
