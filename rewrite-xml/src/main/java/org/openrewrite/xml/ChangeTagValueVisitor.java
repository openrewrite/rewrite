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
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

public class ChangeTagValueVisitor<P> extends XmlVisitor<P> {

    private final Xml.Tag scope;
    private final String newValue;
    @Nullable
    private final String oldValue;
    private final boolean regexp;

    public ChangeTagValueVisitor(final Xml.Tag scope, @Nullable final String oldValue,
                                 final String newValue, final Boolean regexp) {
        this.scope = scope;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.regexp = Boolean.TRUE.equals(regexp);
    }

    public ChangeTagValueVisitor(final Xml.Tag scope, final String newValue) {
        this.scope = scope;
        this.oldValue = null;
        this.newValue = newValue;
        this.regexp = Boolean.FALSE;
    }

    @Override
    public Xml visitTag(Xml.Tag tag, P p) {
        Xml.Tag t = (Xml.Tag) super.visitTag(tag, p);
        if (Objects.equals(newValue, oldValue)) {
            return tag;
        }

        if (scope.isScope(t)) {
            final Content onlyNode = Optional.ofNullable(t.getContent())
                    .filter(c -> c.size() == 1)
                    .map(c -> c.get(0)).orElse(null);

            final boolean isTextContent = onlyNode instanceof Xml.CharData;
            final String prefix = isTextContent ? onlyNode.getPrefix() : "";
            final String afterText = isTextContent ? ((Xml.CharData) onlyNode).getAfterText() : "";

            if (isTextContent && ((Xml.CharData) onlyNode).getText().equals(newValue)) {
                return tag;
            }

            t =  !regexp ? t.withContent(
                    singletonList(
                            new Xml.CharData(randomId(), prefix, Markers.EMPTY, false, newValue, afterText)
                    )) : isTextContent ? updateUsingRegex(t, (Xml.CharData) onlyNode) : t;

        }
        return t;
    }

    private Xml.Tag updateUsingRegex(final Xml.Tag t, final Xml.CharData content) {
        final String text = content.getText();
        if (oldValue != null && Pattern.compile(oldValue).matcher(text).find()) {
            final boolean notProcessed = content.getMarkers()
                    .findAll(AlreadyReplaced.class)
                    .stream()
                    .noneMatch(m -> m.getFind().equals(oldValue) && newValue.equals(m.getReplace()));

            if (notProcessed) {
                final String newContent = text.replaceAll(oldValue, newValue);
                final Markers markers = Markers.build(
                        singletonList(new AlreadyReplaced(randomId(), oldValue, newValue))
                );
                return t.withContent(singletonList(new Xml.CharData(randomId(),
                        content.getPrefix(), markers, false, newContent, content.getAfterText())));
            }
        }
        return t;
    }
}