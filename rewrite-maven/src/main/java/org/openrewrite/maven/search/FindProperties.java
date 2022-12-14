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
package org.openrewrite.maven.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.openrewrite.Tree.randomId;

@EqualsAndHashCode(callSuper = true)
@Value
public class FindProperties extends Recipe {

    @Option(displayName = "Property pattern",
            description = "Regular expression pattern used to match property tag names.",
            example = "guava*")
    String propertyPattern;

    UUID searchId = randomId();

    @Override
    public String getDisplayName() {
        return "Find Maven project properties";
    }

    @Override
    public String getDescription() {
        return "Finds the specified Maven project properties within a pom.xml.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern propertyMatcher = Pattern.compile(propertyPattern.replace(".", "\\.")
                .replace("*", ".*"));
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext context) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, context);
                if (isPropertyTag() && propertyMatcher.matcher(tag.getName()).matches()) {
                    t = SearchResult.found(t);
                }

                Optional<String> value = tag.getValue();
                if (t.getContent() != null && value.isPresent() && value.get().contains("${")) {
                    //noinspection unchecked
                    t = t.withContent(ListUtils.mapFirst((List<Content>) t.getContent(), v ->
                            SearchResult.found(v, getResolutionResult().getPom().getValue(value.get()))));
                }
                return t;
            }
        };
    }

    public static Set<Xml.Tag> find(Xml.Document xml, String propertyPattern) {
        Pattern propertyMatcher = Pattern.compile(propertyPattern.replace(".", "\\.")
                .replace("*", ".*"));
        Set<Xml.Tag> found = new HashSet<>();
        new MavenVisitor<Set<Xml.Tag>>(){
            @Override
            public Xml visitTag(Xml.Tag tag, Set<Xml.Tag> tags) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, tags);
                if (isPropertyTag() && propertyMatcher.matcher(tag.getName()).matches()) {
                    tags.add(t);
                }
                return t;
            }
        }.visit(xml, found);
        return found;
    }
}
