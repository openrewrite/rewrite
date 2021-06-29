/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.yaml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.format.AutoFormatVisitor;
import org.openrewrite.yaml.search.ContainsTree;
import org.openrewrite.yaml.search.YamlSearchResult;
import org.openrewrite.yaml.tree.Yaml;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.openrewrite.yaml.search.ContainsTree.doesNotMatch;

@Value
@EqualsAndHashCode(callSuper = true)
public class MergeYaml extends Recipe {

    @Option(displayName = "Key path",
            description = "XPath expression used to find matching keys.",
            example = "/metadata")
    String key;
    @Option(displayName = "YAML snippet",
            description = "The YAML snippet to insert. The snippet will be indented to match the style of its surroundings.",
            example = "labels: \n\tlabel-one: \"value-one\"")
    @Language("yml")
    String yaml;
    @Option(displayName = "Filter YAML snippet",
            description = "The YAML snippet to to use as a basis for filtering which files are applicable to this recipe.",
            example = "labels: \n\tlabel-two: \"value-two\"",
            required = false)
    @Language("yml")
    @Nullable
    String filterYaml;

    @Override
    public Validated validate() {
        Validated v = super.validate();
        if (!StringUtils.isNullOrEmpty(filterYaml)) {
            v = v.and(Validated.test("filterYaml", "Filter expression must be valid YAML",
                    yaml, MergeYaml::isYamlMapping));
        }
        return v.and(Validated.test("yaml", "Must be valid YAML",
                yaml, MergeYaml::isYamlMapping));
    }

    @Override
    public String getDisplayName() {
        return "Merge YAML snippet";
    }

    @Override
    public String getDescription() {
        return "Merge a YAML snippet with an existing YAML document.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (StringUtils.isNullOrEmpty(filterYaml)) {
            return null;
        }

        Yaml.Documents filter = new YamlParser().parse(filterYaml).get(0);
        Set<YamlSearchResult> searchResults = new HashSet<>();

        return new YamlVisitor<ExecutionContext>() {
            @Override
            public Yaml visitSequence(Yaml.Sequence sequence, ExecutionContext ctx) {
                new ContainsTree<>(sequence, filter, key).visit(sequence, searchResults, getCursor());
                if (doesNotMatch(searchResults)) {
                    return sequence;
                } else {
                    return sequence.withMarkers(sequence.getMarkers().addIfAbsent(ContainsTree.MATCHES));
                }
            }

            @Override
            public Yaml visitDocuments(Yaml.Documents documents, ExecutionContext ctx) {
                new ContainsTree<>(documents, filter, key).visit(documents, searchResults, getCursor());
                if (doesNotMatch(searchResults)) {
                    return documents;
                } else {
                    return documents.withMarkers(documents.getMarkers().addIfAbsent(ContainsTree.MATCHES));
                }
            }
        };
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        Yaml.Documents incoming = new YamlParser().parse(yaml).get(0);

        if ("/".equals(key)) {
            return new YamlVisitor<ExecutionContext>() {
                @Override
                public Yaml visitDocuments(Yaml.Documents documents, ExecutionContext ctx) {
                    Yaml.Documents docs = (Yaml.Documents) new MergeYamlVisitor(documents, incoming).visit(documents, ctx, getCursor());
                    doAfterVisit(new AutoFormatVisitor<>(null));
                    return docs;
                }
            };
        }

        XPathMatcher matcher = new XPathMatcher(key);
        XPathMatcher parentMatcher = new XPathMatcher(key.substring(0, key.lastIndexOf('/')));

        return new YamlVisitor<ExecutionContext>() {
            @Override
            public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = (Yaml.Mapping.Entry) super.visitMappingEntry(entry, ctx);

                boolean needsFormat = false;
                if (matcher.matches(getCursor())) {
                    Yaml parent = getCursor().getParentOrThrow().getValue();
                    doAfterVisit(new MergeYamlVisitor(parent, incoming.getDocuments().get(0).getBlock()));
                    needsFormat = true;
                } else if (parentMatcher.matches(getCursor())
                        && entry.getValue() instanceof Yaml.Mapping
                        && !hasChildWithKey(e.getKey().getValue(), (Yaml.Mapping) e.getValue())) {
                    doAfterVisit(new MergeYamlVisitor(entry.getValue(), incoming.getDocuments().get(0).getBlock()));
                    needsFormat = true;
                }

                if (needsFormat) {
                    doAfterVisit(new AutoFormatVisitor<>(entry));
                }

                return e;
            }
        };
    }

    private static boolean isYamlMapping(String yaml) {
        List<Yaml.Documents> docs = new YamlParser().parse(yaml);
        if (docs.size() == 0) {
            return false;
        }
        Yaml.Documents doc = docs.get(0);
        if (doc.getDocuments().size() == 0) {
            return false;
        }
        Yaml.Block block = doc.getDocuments().get(0).getBlock();
        return block instanceof Yaml.Mapping;
    }

    private static boolean hasChildWithKey(String key, Yaml.Mapping mapping) {
        return mapping.getEntries().stream()
                .anyMatch(childEntry -> key.equals(childEntry.getKey().getValue()));
    }

}
