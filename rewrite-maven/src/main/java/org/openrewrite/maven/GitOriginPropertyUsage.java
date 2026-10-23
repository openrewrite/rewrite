/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.maven;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static org.openrewrite.maven.tree.ResolvedPom.placeholderHelper;

// Tracks which POM properties are referenced only from the tags a Git origin recipe updates
public class GitOriginPropertyUsage {
    private final Set<String> usedElsewhere = new HashSet<>();
    private final Map<String, Set<Path>> usedInTarget = new HashMap<>();

    TreeVisitor<?, ExecutionContext> scanner(XPathMatcher target) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                Optional<String> value = t.getValue();
                if (value.isPresent()) {
                    boolean inTarget = target.matches(getCursor());
                    for (String property : placeholderHelper.getPlaceholders(value.get())) {
                        if (inTarget) {
                            usedInTarget.computeIfAbsent(property, k -> new HashSet<>())
                                    .add(getCursor().firstEnclosingOrThrow(Xml.Document.class).getSourcePath());
                        } else {
                            usedElsewhere.add(property);
                        }
                    }
                }
                return t;
            }

            @Override
            public Xml.Attribute visitAttribute(Xml.Attribute attribute, ExecutionContext ctx) {
                usedElsewhere.addAll(placeholderHelper.getPlaceholders(attribute.getValueAsString()));
                return super.visitAttribute(attribute, ctx);
            }
        };
    }

    // The property making up the whole value, if declared in this POM and referenced only from its target tags
    @Nullable
    String safeToUpdate(String value, Xml.Tag project, Path sourcePath) {
        List<String> placeholders = placeholderHelper.getPlaceholders(value);
        if (placeholders.size() != 1 || !value.equals("${" + placeholders.get(0) + "}")) {
            return null;
        }
        String property = placeholders.get(0);
        String propertyValue = project.getChild("properties").flatMap(p -> p.getChildValue(property)).orElse(null);
        if (propertyValue == null || placeholderHelper.hasPlaceholders(propertyValue) ||
            usedElsewhere.contains(property) || !Collections.singleton(sourcePath).equals(usedInTarget.get(property))) {
            return null;
        }
        return property;
    }

    // Null when any placeholder remains unresolved
    static @Nullable String resolve(String value, ResolvedPom pom, Function<String, @Nullable String> overrides) {
        String resolved = placeholderHelper.replacePlaceholders(value, key -> {
            String override = overrides.apply(key);
            if (override != null) {
                return override;
            }
            String placeholder = "${" + key + "}";
            String propertyValue = pom.getValue(placeholder);
            return placeholder.equals(propertyValue) ? null : propertyValue;
        });
        return placeholderHelper.hasPlaceholders(resolved) ? null : resolved;
    }
}
