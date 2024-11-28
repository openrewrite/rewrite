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
import org.openrewrite.marker.AlreadyReplaced;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.tree.Xml;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

public class ChangeTagValueVisitor<P> extends XmlVisitor<P> {

    private final Xml.Tag scope;
    private final String newValue;
    @Nullable
    private final String oldValue;
    private final boolean regexp;

    public ChangeTagValueVisitor(Xml.Tag scope, @javax.annotation.Nullable String oldValue, String newValue, Boolean regexp) {
        this.scope = scope;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.regexp = Boolean.TRUE.equals(regexp);
    }

    public ChangeTagValueVisitor(Xml.Tag scope, String newValue) {
        this.scope = scope;
        this.oldValue = null;
        this.newValue = newValue;
        this.regexp = Boolean.FALSE;
    }

    @Override
    public Xml visitTag(Xml.Tag tag, P p) {
        Xml.Tag t = (Xml.Tag) super.visitTag(tag, p);

        if (scope.isScope(t)) {
            if(Objects.equals(newValue, oldValue)) {
                return tag;
            }

            String prefix = "";
            String afterText = "";
            if (t.getContent() != null && t.getContent().size() == 1 && t.getContent().get(0) instanceof Xml.CharData) {
                Xml.CharData existingValue = (Xml.CharData) t.getContent().get(0);
                String text = existingValue.getText();

                boolean alreadyProcessed = this.regexp && existingValue.getMarkers()
                        .findAll(AlreadyReplaced.class)
                        .stream()
                        .anyMatch(m -> m.getFind().equals(oldValue) && newValue.equals(m.getReplace()));

                if (alreadyProcessed || (!regexp && text.equals(newValue))) {
                    return tag;
                }

                // if the previous content was also character data, preserve its prefix and afterText
                prefix = existingValue.getPrefix();
                afterText = existingValue.getAfterText();

                if(regexp && oldValue != null && Pattern.compile(oldValue).matcher(text).find()) {
                    String newContent = text.replaceAll(oldValue, Matcher.quoteReplacement(newValue));
                    Markers markers = Markers.build(singletonList( new AlreadyReplaced(randomId(),oldValue, newValue)));
                    t = t.withContent(singletonList(new Xml.CharData(randomId(),
                            prefix, markers, false, newContent, afterText)));
                }

            }
            if(!regexp) {
                t = t.withContent(singletonList(new Xml.CharData(randomId(),
                        prefix, Markers.EMPTY, false, newValue, afterText)));
            }
        }

        return t;
    }
}