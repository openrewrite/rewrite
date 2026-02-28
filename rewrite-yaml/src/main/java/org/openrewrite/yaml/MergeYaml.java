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
import lombok.experimental.NonFinal;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.yaml.tree.Yaml;

import static org.openrewrite.internal.StringUtils.isBlank;
import static org.openrewrite.yaml.MergeYaml.InsertMode.*;

@Value
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor(force = true) // TODO: remove with @deprecated constructor
public class MergeYaml extends Recipe {
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

    @Option(displayName = "File pattern",
            description = "A glob expression representing a file path to search for (relative to the project root). Blank/null matches all.",
            required = false,
            example = ".github/workflows/*.yml")
    @Nullable
    String filePattern;

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

    public MergeYaml(String key, @Language("yml") String yaml, @Nullable Boolean acceptTheirs, @Nullable String objectIdentifyingProperty, @Nullable String filePattern, @Nullable InsertMode insertMode, @Nullable String insertProperty, @Nullable Boolean createNewKeys) {
        this.key = key;
        this.yaml = yaml;
        this.acceptTheirs = acceptTheirs;
        this.objectIdentifyingProperty = objectIdentifyingProperty;
        this.filePattern = filePattern;
        this.insertMode = insertMode;
        this.insertProperty = insertProperty;
        this.createNewKeys = createNewKeys;
    }

    public enum InsertMode { Before, After, Last }

    @Nullable
    @NonFinal
    transient Yaml incoming = null;

    @Override
    public Validated<Object> validate() {
        return super.validate()
                .and(Validated.test("yaml", "Must be valid YAML",
                        yaml, y -> {
                            if (yaml == null) {
                                return false;
                            }
                            try {
                                incoming = MergeYaml.parse(yaml);
                                return true;
                            } catch (IllegalArgumentException e) {
                                return false;
                            }
                        }))
                .and(Validated.required("key", key))
                .and(Validated.test("insertProperty", "Insert property must be filed when `insert mode` is either `BeforeProperty` or `AfterProperty`.", insertProperty,
                        s -> insertMode == null || insertMode == Last || !isBlank(s)));
    }
    String displayName = "Merge YAML snippet";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("at `%s`", key);
    }

    String description = "Merge a YAML snippet with an existing YAML document.";

    final static String FOUND_MATCHING_ELEMENT = "FOUND_MATCHING_ELEMENT";
    final static String REMOVE_PREFIX = "REMOVE_PREFIX";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // When `new MergeYaml(..).getVisitor() is used directly in another recipe, the `validate` function will not be called, thus `incoming` is null
        if (incoming == null) {
            incoming = MergeYaml.parse(yaml);
        }
        return Preconditions.check(new FindSourceFiles(filePattern), new YamlIsoVisitor<ExecutionContext>() {
            private final boolean accptTheirs = Boolean.TRUE.equals(acceptTheirs);
            private final JsonPathMatcher matcher = new JsonPathMatcher(key);

            @Override
            public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                if ("$".equals(key) || "$.".equals(key)) {
                    Yaml.Document d = document.withBlock((Yaml.Block)
                            new MergeYamlVisitor<>(document.getBlock(), incoming, accptTheirs, objectIdentifyingProperty, insertMode, insertProperty)
                                    .visitNonNull(document.getBlock(), ctx, getCursor())
                    );
                    if (getCursor().getMessage(REMOVE_PREFIX, false)) {
                        if (insertMode == Before) {
                            d = d.withPrefix("");
                        } else {
                            // Preserve newline before document separator in multi-document YAML
                            d = d.withEnd(d.getEnd().withPrefix(preserveDocumentSeparator(d)));
                        }
                    }
                    return d;
                }
                Yaml.Document d = super.visitDocument(document, ctx);
                if ((createNewKeys == null || Boolean.TRUE.equals(createNewKeys)) && d == document && !getCursor().getMessage(FOUND_MATCHING_ELEMENT, false)) {
                    // No matching element found, but check if the key maybe exists in the json path.
                    String valueKey = maybeKeyFromJsonPath(key);
                    if (valueKey == null) {
                        return d;
                    }
                    // No matching element already exists, so it must be constructed.
                    @Language("yml") String snippet;
                    if (incoming instanceof Yaml.Mapping) {
                        // Use two spaces as indent, the `MergeYamlVisitor` recipe will take care for proper indenting by calling `autoformat`,
                        snippet = valueKey + ":\n  " + yaml.replaceAll("\n", "\n  ");
                    } else {
                        // If there is no space between the colon and the value it will not be interpreted as a mapping
                        snippet = valueKey + ":" + (yaml.startsWith(" ") ? yaml : " " + yaml);
                    }
                    return d.withBlock((Yaml.Block)
                            new MergeYamlVisitor<>(d.getBlock(), MergeYaml.parse(snippet), accptTheirs, objectIdentifyingProperty, insertMode, insertProperty)
                                    .visitNonNull(d.getBlock(), ctx, getCursor()));
                }
                if (getCursor().getMessage(REMOVE_PREFIX, false)) {
                    // Preserve newline before document separator in multi-document YAML
                    d = d.withEnd(d.getEnd().withPrefix(preserveDocumentSeparator(d)));
                }
                return d;
            }

            private String preserveDocumentSeparator(Yaml.Document document) {
                // Check if this is a multi-document YAML and if there's a following explicit document
                Yaml.Documents documents = getCursor().firstEnclosing(Yaml.Documents.class);
                if (documents != null) {
                    int currentIndex = documents.getDocuments().indexOf(document);
                    // Preserve a newline before the next document separator
                    if (0 <= currentIndex && currentIndex < documents.getDocuments().size() - 1 &&
                            documents.getDocuments().get(currentIndex + 1).isExplicit()) {
                        return "\n";
                    }
                    // Or if this is the last document and it is explicit
                    if (currentIndex == documents.getDocuments().size() - 1 && document.getEnd().isExplicit()) {
                        return "\n";
                    }
                }
                return "";
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
                    m = (Yaml.Mapping) new MergeYamlVisitor<>(mapping, incoming, accptTheirs,
                            objectIdentifyingProperty, insertMode, insertProperty).visitNonNull(mapping, ctx, getCursor().getParentOrThrow());
                }
                return m;
            }

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                if (matcher.matches(getCursor())) {
                    getCursor().putMessageOnFirstEnclosing(Yaml.Document.class, FOUND_MATCHING_ELEMENT, true);
                    Yaml.Block value = (Yaml.Block) new MergeYamlVisitor<>(entry.getValue(), incoming,
                            accptTheirs, objectIdentifyingProperty, insertMode, insertProperty).visitNonNull(entry.getValue(),
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
                                    accptTheirs, objectIdentifyingProperty, insertMode, insertProperty)
                                    .visitNonNull(entry.getBlock(), ctx, new Cursor(getCursor(), entry)))));
                }
                return super.visitSequence(sequence, ctx);
            }
        });
    }

    static Yaml parse(@Language("yml") String yaml) {
        SourceFile sourceFile = new YamlParser().parse(yaml)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Could not parse as YAML:\n" + yaml));
        if (sourceFile instanceof Yaml.Documents) {
            // Any comments will have been put on the parent Document node, preserve by copying to the mapping
            Yaml.Document doc = ((Yaml.Documents) sourceFile).getDocuments().get(0);
            if (doc.getBlock() instanceof Yaml.Mapping) {
                Yaml.Mapping m = (Yaml.Mapping) doc.getBlock();
                return m.withEntries(ListUtils.mapFirst(m.getEntries(), entry -> entry.withPrefix(doc.getPrefix())));
            } else if (doc.getBlock() instanceof Yaml.Sequence) {
                Yaml.Sequence s = (Yaml.Sequence) doc.getBlock();
                return s.withEntries(ListUtils.mapFirst(s.getEntries(), entry -> entry.withPrefix(doc.getPrefix())));
            }
            return doc.getBlock().withPrefix(doc.getPrefix());
        }
        String message = sourceFile.getMarkers()
                .findFirst(ParseExceptionResult.class)
                .map(ParseExceptionResult::getMessage)
                .orElse("unknown error");
        throw new IllegalArgumentException("Could not parse as YAML: " + message + "\n" + yaml);
    }
}
