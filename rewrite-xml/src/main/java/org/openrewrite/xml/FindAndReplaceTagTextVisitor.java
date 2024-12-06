/*
 * Copyright 2024 the original author or authors.
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


import org.openrewrite.marker.AlreadyReplaced;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

/**
 * This visitor finds and replaces text within a specified XML tag.
 * It supports both literal and regular expression replacements.
 * Use {@link ChangeTagValueVisitor} if you only wish change the value, irrespective of current data.
 * @param <P>
 */
public class FindAndReplaceTagTextVisitor<P> extends XmlVisitor<P> {

    private final Xml.Tag scope;
    private final String newValue;
    private final String oldValue;
    private final boolean regexp;

    public FindAndReplaceTagTextVisitor(final Xml.Tag tag, final String oldValue,
                                 final String newValue, final boolean regexp) {
        this.scope = tag;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.regexp = regexp;
    }

    @Override
    public Xml visitTag(Xml.Tag tag, P p) {
        Xml.Tag t = (Xml.Tag) super.visitTag(tag, p);

        if (scope.isScope(t)) {
            if (Objects.equals(newValue, oldValue)) {
                return tag;
            }

            final Content onlyNode = Optional.ofNullable(t.getContent())
                    .filter(c -> c.size() == 1)
                    .map(c -> c.get(0)).orElse(null);

            final boolean isTextContent = onlyNode instanceof Xml.CharData;
            final String prefix = isTextContent ? onlyNode.getPrefix() : "";
            final String afterText = isTextContent ? ((Xml.CharData) onlyNode).getAfterText() : "";

            if (isTextContent) {
                final String text = ((Xml.CharData) onlyNode).getText();
                if(text.equals(newValue)) {
                    return tag;
                }
                t =  regexp ? updateUsingRegex(t, (Xml.CharData) onlyNode)
                        : text.contains(oldValue) ?
                            t.withContent(singletonList(
                                    new Xml.CharData(randomId(),
                                            prefix,
                                            Markers.EMPTY,
                                            false,
                                            text.replace(oldValue, newValue),
                                            afterText)
                            )) : t;
            }
        }
        return t;
    }

    private Xml.Tag updateUsingRegex(final Xml.Tag t, final Xml.CharData content) {
        final String text = content.getText();
        if (Pattern.compile(oldValue).matcher(text).find()) {
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
