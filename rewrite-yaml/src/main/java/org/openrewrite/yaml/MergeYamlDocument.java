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
import lombok.NoArgsConstructor;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.yaml.MergeYaml.InsertMode;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor(force = true) // TODO: remove with @deprecated constructor
public class MergeYamlDocument extends Recipe {
    @Option(displayName = "JsonPath matcher",
            description = "A [JsonPath](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) expression used to find matching keys.",
            example = "$.metadata")
    String matcher;

    @Option(displayName = "Key path",
            description = "A [JsonPath](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) expression used to find matching keys.",
            example = "$.metadata")
    String key;

    @Option(displayName = "YAML snippet",
            description = "The YAML snippet to insert. The snippet will be indented to match the style of its surroundings.",
            example = "labels:\n  label-one: \"value-one\"")
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

    @Option(displayName = "Insert mode",
            description = "Choose an insertion point when multiple mappings exist. Default is `Last`.",
            valid = {"Before", "After", "Last"},
            required = false)
    @Nullable
    InsertMode insertMode;

    @Option(displayName = "Insert property",
            description = "Define the key for the insertion mode. Takes the `key` JsonPath into account. Only useful when `insert mode` is either `Before` or `After`.",
            required = false,
            example = "some-key")
    @Nullable
    String insertProperty;

    @Option(displayName = "Create new keys",
            description = "When the key path does _not_ match any keys, create new keys on the spot. Default is `true`.",
            required = false)
    @Nullable
    Boolean createNewKeys;

    public MergeYamlDocument(String matcher, String key, @Language("yml") String yaml, @Nullable Boolean acceptTheirs, @Nullable String objectIdentifyingProperty, @Nullable InsertMode insertMode, @Nullable String insertProperty, @Nullable Boolean createNewKeys) {
        this.matcher = matcher;
        this.key = key;
        this.yaml = yaml;
        this.acceptTheirs = acceptTheirs;
        this.objectIdentifyingProperty = objectIdentifyingProperty;
        this.insertMode = insertMode;
        this.insertProperty = insertProperty;
        this.createNewKeys = createNewKeys;
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
    final static String REMOVE_PREFIX = "REMOVE_PREFIX";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new YamlIsoVisitor<ExecutionContext>() {
            private final JsonPathMatcher matcher = new JsonPathMatcher(MergeYamlDocument.this.matcher);

            @Override
            public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                Yaml.Document doc = super.visitDocument(document, ctx);
                if (getCursor().getMessage(FOUND_MATCHING_ELEMENT, false)) {
                    doc = (Yaml.Document) new MergeYaml(key, yaml, acceptTheirs, objectIdentifyingProperty, null, insertMode, insertProperty, createNewKeys).getVisitor().visitNonNull(doc, ctx, getCursor().getParentOrThrow());
                }
                return doc;
            }

            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, ExecutionContext ctx) {
                if (matcher.matches(getCursor())) {
                    getCursor().putMessageOnFirstEnclosing(Yaml.Document.class, FOUND_MATCHING_ELEMENT, true);
                }
                return super.visitMapping(mapping, ctx);
            }

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                if (matcher.matches(getCursor())) {
                    getCursor().putMessageOnFirstEnclosing(Yaml.Document.class, FOUND_MATCHING_ELEMENT, true);
                }
                return super.visitMappingEntry(entry, ctx);
            }

            @Override
            public Yaml.Sequence visitSequence(Yaml.Sequence sequence, ExecutionContext ctx) {
                if (matcher.matches(getCursor().getParentOrThrow())) {
                    getCursor().putMessageOnFirstEnclosing(Yaml.Document.class, FOUND_MATCHING_ELEMENT, true);
                }
                return super.visitSequence(sequence, ctx);
            }
        };
    }
}
