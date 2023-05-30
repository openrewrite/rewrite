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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = true)
public class MergeYaml extends Recipe {
    @Option(displayName = "Key path",
            description = "A JsonPath expression used to find matching keys.",
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

    @Override
    public Validated validate() {
        return super.validate()
                .and(Validated.test("yaml", "Must be valid YAML",
                        yaml, y -> new YamlParser().parse(yaml)
                                .findFirst()
                                .map(doc -> !doc.getDocuments().isEmpty())
                                .orElse(false)));
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
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(key);
        Yaml incoming = new YamlParser().parse(yaml)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not parse as YAML"))
                .getDocuments().get(0).getBlock();

        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                if ("$".equals(key)) {
                    return document.withBlock((Yaml.Block) new MergeYamlVisitor<>(document.getBlock(), yaml,
                            Boolean.TRUE.equals(acceptTheirs), objectIdentifyingProperty).visit(document.getBlock(),
                            ctx, getCursor()));
                }
                return super.visitDocument(document, ctx);
            }

            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, ExecutionContext ctx) {
                Yaml.Mapping m = super.visitMapping(mapping, ctx);
                if (matcher.matches(getCursor())) {
                    m = (Yaml.Mapping) new MergeYamlVisitor<>(mapping, incoming, Boolean.TRUE.equals(acceptTheirs),
                            objectIdentifyingProperty).visitNonNull(mapping, ctx, getCursor().getParentOrThrow());
                }
                return m;
            }

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                if (matcher.matches(getCursor())) {
                    // this tests for an awkward case that will be better handled by JsonPathMatcher.
                    // if it is a sequence, we want to insert into every sequence entry for now.
                    if (!(entry.getValue() instanceof Yaml.Sequence)) {
                        return entry.withValue((Yaml.Block) new MergeYamlVisitor<>(entry.getValue(), incoming,
                                Boolean.TRUE.equals(acceptTheirs), objectIdentifyingProperty).visit(entry.getValue(),
                                ctx, getCursor()));
                    }
                }
                return super.visitMappingEntry(entry, ctx);
            }

            @Override
            public Yaml.Sequence visitSequence(Yaml.Sequence sequence, ExecutionContext ctx) {
                if (matcher.matches(getCursor().getParentOrThrow())) {
                    return sequence.withEntries(ListUtils.map(sequence.getEntries(),
                            entry -> entry.withBlock((Yaml.Block) new MergeYamlVisitor<>(entry.getBlock(), incoming,
                                    Boolean.TRUE.equals(acceptTheirs), objectIdentifyingProperty)
                                    .visit(entry.getBlock(), ctx, new Cursor(getCursor(), entry)))));
                }
                return super.visitSequence(sequence, ctx);
            }
        };
    }
}
