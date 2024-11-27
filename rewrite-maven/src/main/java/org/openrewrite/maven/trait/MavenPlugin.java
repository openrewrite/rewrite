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
package org.openrewrite.maven.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.trait.Trait;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

@Value
public class MavenPlugin implements Trait<Xml.Tag> {
    static final XPathMatcher PLUGIN_MATCHER = new XPathMatcher("//plugins/plugin");

    Cursor cursor;

    String groupId;

    @Nullable
    String artifactId;

    public static class Matcher extends MavenTraitMatcher<MavenPlugin> {

        @Override
        protected @Nullable MavenPlugin test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof Xml.Tag) {
                Xml.Tag tag = (Xml.Tag) value;

                // `XPathMatcher` is still a bit expensive
                if (!"plugin".equals(tag.getName()) ||
                    (!PLUGIN_MATCHER.matches(cursor))) {
                    return null;
                }

                return new MavenPlugin(
                        cursor,
                        getProperty(cursor, "groupId").orElse("org.apache.maven.plugins"),
                        getProperty(cursor, "artifactId").orElse(null)
                );
            }
            return null;
        }

        private Optional<String> getProperty(Cursor cursor, String property) {
            Xml.Tag tag = cursor.getValue();
            MavenResolutionResult resolutionResult = getResolutionResult(cursor);
            if (resolutionResult != null && resolutionResult.getPom().getProperties() != null) {
                if (tag.getChildValue(property).isPresent() && tag.getChildValue(property).get().trim().startsWith("${")) {
                    String propertyKey = tag.getChildValue(property).get().trim();
                    return Optional.ofNullable(resolutionResult.getPom().getValue(propertyKey));
                }
            }
            return tag.getChildValue(property);
        }
    }
}
