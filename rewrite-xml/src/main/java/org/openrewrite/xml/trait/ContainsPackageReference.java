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
import org.openrewrite.trait.Trait;
import org.openrewrite.xml.tree.Xml;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Value
public class ContainsPackageReference implements Trait<Tree> {
    Cursor cursor;

    public @Nullable String getValue() {
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


    public static class Matcher extends SimpleTraitMatcher<ContainsPackageReference> {
        private final Pattern PACKAGE_OR_TYPE_REFERENCE = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)(\\.[a-zA-Z_][a-zA-Z0-9_]*)*(\\.[A-Z][a-zA-Z0-9_]*|\\$[A-Z][a-zA-Z0-9_]*)*(\\.\\*)?$");
        private final List<String> ATTRIBUTES_THAT_REFERENCE_PACKAGE_OR_TYPE = Arrays.asList("class", "type");
        private final List<String> ELEMENTS_THAT_REFERENCE_PACKAGE_OR_TYPE = Arrays.asList("value");

        @Override
        protected @Nullable ContainsPackageReference test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof Xml.Attribute) {
                Xml.Attribute attrib = (Xml.Attribute) value;
                if (ATTRIBUTES_THAT_REFERENCE_PACKAGE_OR_TYPE.contains(attrib.getKeyAsString())) {
                    if (PACKAGE_OR_TYPE_REFERENCE.matcher(attrib.getValueAsString()).matches()) {
                        return new ContainsPackageReference(cursor);
                    }
                }
            } else if (value instanceof Xml.Tag) {
                Xml.Tag tag = (Xml.Tag) value;
                if (ELEMENTS_THAT_REFERENCE_PACKAGE_OR_TYPE.contains(tag.getName())) {
                    if (tag.getValue().isPresent()) {
                        if (PACKAGE_OR_TYPE_REFERENCE.matcher(tag.getValue().get()).matches()) {
                            return new ContainsPackageReference(cursor);
                        }
                    }
                }
            }
            return null;
        }
    }
}
