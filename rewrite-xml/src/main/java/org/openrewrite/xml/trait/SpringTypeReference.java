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
package org.openrewrite.xml.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.TypeReference;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.regex.Pattern;

@Value
class SpringTypeReference implements TypeReference {
    Cursor cursor;

    @Override
    public Tree getTree() {
        return TypeReference.super.getTree();
    }

    @Override
    public @Nullable String getName() {
        if (getTree() instanceof Xml.Attribute) {
            Xml.Attribute attribute = (Xml.Attribute) getTree();
            return attribute.getValueAsString();
        } else if (getTree() instanceof Xml.Tag) {
            Xml.Tag tag = (Xml.Tag) getTree();
            if (tag.getValue().isPresent()) {
                return tag.getValue().get();
            }
        }
        return null;
    }

    public static class Matcher extends SimpleTraitMatcher<SpringTypeReference> {
        private final Pattern typeReference = Pattern.compile("(?:[a-zA-Z_][a-zA-Z0-9_]*\\.)+[A-Z*][a-zA-Z0-9_]*(?:<[a-zA-Z0-9_,?<> ]*>)?");
        private final XPathMatcher classXPath = new XPathMatcher("//@class");
        private final XPathMatcher typeXPath = new XPathMatcher("//@type");
        private final XPathMatcher tags = new XPathMatcher("//value");

        @Override
        protected @Nullable SpringTypeReference test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof Xml.Attribute) {
                Xml.Attribute attrib = (Xml.Attribute) value;
                if (classXPath.matches(cursor) || typeXPath.matches(cursor)) {
                    if (typeReference.matcher(attrib.getValueAsString()).matches()) {
                        return new SpringTypeReference(cursor);
                    }
                }
            } else if (value instanceof Xml.Tag) {
                Xml.Tag tag = (Xml.Tag) value;
                if (tags.matches(cursor)) {
                    if (tag.getValue().isPresent() && typeReference.matcher(tag.getValue().get()).matches()) {
                        return new SpringTypeReference(cursor);
                    }
                }
            }
            return null;
        }
    }
}
