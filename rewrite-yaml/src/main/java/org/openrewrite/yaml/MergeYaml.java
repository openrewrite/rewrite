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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = false)
public class MergeYaml extends Recipe {
    @Option(displayName = "Key path",
            description = "A [JsonPath](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) expression used to find matching keys.",
            example = "$.metadata")
    String key;

    @Option(displayName = "YAML snippet",
            description = "The YAML snippet to insert. The snippet will be indented to match the style of its surroundings.",
            example = "labels: \n\tlabel-one: \"value-one\"")
    @Language("yml")
    String yaml;

    @Option(displayName = "Accept theirs",
            description = "When the YAML snippet to insert conflicts with an existing key value pair and an existing key has a different value, prefer the original value.",
            required = false)
    @Nullable
    Boolean acceptTheirs;

    @Incubating(since = "7.30.0")
    @Option(displayName = "Object identifying property",
            description = "Name of a property which will be used to identify objects (mapping). This serves as the key to match on when merging entries of a sequence.",
            required = false,
            example = "name")
    @Nullable
    String objectIdentifyingProperty;

    @Option(displayName = "File pattern",
            description = "A glob expression representing a file path to search for (relative to the project root). Blank/null matches all.",
            required = false,
            example = ".github/workflows/*.yml")
    @Nullable
    String filePattern;

    @Override
    public Validated<Object> validate() {
        return super.validate()
                .and(Validated.test("yaml", "Must be valid YAML",
                        yaml, y -> new YamlParser().parse(yaml)
                                .findFirst()
                                .map(doc -> !((Yaml.Documents) doc).getDocuments().isEmpty())
                                .orElse(false)));
    }

    @Override
    public String getDisplayName() {
        return "Merge YAML snippet";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("at `%s`", key);
    }

    @Override
    public String getDescription() {
        return "Merge a YAML snippet with an existing YAML document.";
    }

    final static String FOUND_MATCHING_ELEMENT = "FOUND_MATCHING_ELEMENT";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles(filePattern), new YamlIsoVisitor<ExecutionContext>() {
            final JsonPathMatcher matcher = new JsonPathMatcher(key);

            final Yaml incoming = new YamlParser().parse(yaml)
                    .findFirst()
                    .map(Yaml.Documents.class::cast)
                    .map(docs -> {
                        // Any comments will have been put on the parent Document node, preserve by copying to the mapping
                        Yaml.Document doc = docs.getDocuments().get(0);
                        if (doc.getBlock() instanceof Yaml.Mapping) {
                            Yaml.Mapping m = (Yaml.Mapping) doc.getBlock();
                            return m.withEntries(ListUtils.mapFirst(m.getEntries(), entry -> entry.withPrefix(doc.getPrefix())));
                        } else if (doc.getBlock() instanceof Yaml.Sequence) {
                            Yaml.Sequence s = (Yaml.Sequence) doc.getBlock();
                            return s.withEntries(ListUtils.mapFirst(s.getEntries(), entry -> entry.withPrefix(doc.getPrefix())));
                        }
                        return doc.getBlock().withPrefix(doc.getPrefix());
                    })
                    .orElseThrow(() -> new IllegalArgumentException("Could not parse as YAML"));

            @Override
            public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                if ("$".equals(key)) {
                    Yaml.Document d = document.withBlock((Yaml.Block)
                            new MergeYamlVisitor<>(document.getBlock(), yaml, Boolean.TRUE.equals(acceptTheirs), objectIdentifyingProperty)
                                    .visitNonNull(document.getBlock(), ctx, getCursor())
                    );

                    if (getCursor().getMessage("RemovePrefix", false)) {
                        return d.withEnd(d.getEnd().withPrefix(""));
                    }

                    return d;
                }
                Yaml.Document d = super.visitDocument(document, ctx);
                if (d == document && !getCursor().getMessage(FOUND_MATCHING_ELEMENT, false)) {
                    // No matching element already exists, attempt to construct one
                    String valueKey = maybeKeyFromJsonPath(key);
                    if (valueKey == null) {
                        return d;
                    }
                    // If there is no space between the colon and the value it will not be interpreted as a mapping
                    String snippet;
                    if (incoming instanceof Yaml.Mapping) {
                        snippet = valueKey + ":\n" + indent(yaml);
                    } else {
                        snippet = valueKey + ":" + (yaml.startsWith(" ") ? yaml : " " + yaml);
                    }
                    // No matching element already exists, so it must be constructed
                    //noinspection LanguageMismatch
                    return d.withBlock((Yaml.Block) new MergeYamlVisitor<>(d.getBlock(), snippet,
                            Boolean.TRUE.equals(acceptTheirs), objectIdentifyingProperty).visitNonNull(d.getBlock(),
                            ctx, getCursor()));
                }
                return d;
            }

            public String indent(String text) {
                int index = text.indexOf('\n');
                if (index == -1 || index == text.length() - 1) {
                    return text;
                }
                StringBuilder padding = new StringBuilder();
                for (int i = index + 1; i < text.length(); i++) {
                    if (!Character.isWhitespace(text.charAt(i))) {
                        break;
                    }
                    padding.append(text.charAt(i));
                }
                if (padding.length() == 0) {
                    padding.append("  ");
                }
                return text.replaceAll("(?m)^", padding.toString());
            }

            private @Nullable String maybeKeyFromJsonPath(String jsonPath) {
                if (!jsonPath.startsWith("$.")) {
                    return null;
                }
                // if the key contains a jsonpath filter we cannot infer a valid key
                if (jsonPath.matches(".*\\[\\s?\\?\\s?\\(\\s?@\\..*\\)\\s?].*")) {
                    return null;
                }
                // remove keys that contain wildcard or deep search
                if (jsonPath.matches(".*\\*.*") || jsonPath.matches(".*\\.\\..*")) {
                    return null;
                }
                return jsonPath.substring(2);
            }

            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, ExecutionContext ctx) {
                Yaml.Mapping m = super.visitMapping(mapping, ctx);
                if (matcher.matches(getCursor())) {
                    getCursor().putMessageOnFirstEnclosing(Yaml.Document.class, FOUND_MATCHING_ELEMENT, true);
                    m = (Yaml.Mapping) new MergeYamlVisitor<>(mapping, incoming, Boolean.TRUE.equals(acceptTheirs),
                            objectIdentifyingProperty).visitNonNull(mapping, ctx, getCursor().getParentOrThrow());
                }
                return m;
            }

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                if (matcher.matches(getCursor())) {
                    getCursor().putMessageOnFirstEnclosing(Yaml.Document.class, FOUND_MATCHING_ELEMENT, true);
                    Yaml.Block value = (Yaml.Block) new MergeYamlVisitor<>(entry.getValue(), incoming,
                            Boolean.TRUE.equals(acceptTheirs), objectIdentifyingProperty).visitNonNull(entry.getValue(),
                            ctx, getCursor());
                    if (value instanceof Yaml.Scalar && value.getPrefix().isEmpty()) {
                        value = value.withPrefix(" ");
                    }
                    return entry.withValue(value);
                }
                return super.visitMappingEntry(entry, ctx);
            }

            @Override
            public Yaml.Sequence visitSequence(Yaml.Sequence sequence, ExecutionContext ctx) {
                if (matcher.matches(getCursor().getParentOrThrow())) {
                    getCursor().putMessageOnFirstEnclosing(Yaml.Document.class, FOUND_MATCHING_ELEMENT, true);
                    return sequence.withEntries(ListUtils.map(sequence.getEntries(),
                            entry -> entry.withBlock((Yaml.Block) new MergeYamlVisitor<>(entry.getBlock(), incoming,
                                    Boolean.TRUE.equals(acceptTheirs), objectIdentifyingProperty)
                                    .visitNonNull(entry.getBlock(), ctx, new Cursor(getCursor(), entry)))));
                }
                return super.visitSequence(sequence, ctx);
            }
        });
    }
}
