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
package org.openrewrite.yaml;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class CopyValue extends ScanningRecipe<CopyValue.Accumulator> {
    @Option(displayName = "Old key path",
            description = "A [JsonPath](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) expression to locate a YAML key/value pair to copy.",
            example = "$.source.kind")
    String oldKeyPath;

    @Option(displayName = "Old file path",
            description = "The file path to the YAML file to copy the value from. " +
                          "If `null` then the value will be copied from any yaml file it appears within.",
            example = "src/main/resources/application.yaml",
            required = false)
    @Nullable
    String oldFilePath;

    @Option(displayName = "New key path",
            description = "A [JsonPath](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) expression defining where the value should be written.",
            example = "$.dest.kind")
    String newKey;

    @Option(displayName = "New file path",
            description = "The file path to the YAML file to copy the value to. " +
                          "If `null` then the value will be copied only into the same file it was found in.",
            example = "src/main/resources/application.yaml",
            required = false)
    @Nullable
    String newFilePath;

    @Option(displayName = "Create new keys",
            description = "When the key path does _not_ match any keys, create new keys on the spot. Default is `true`.",
            required = false)
    @Nullable
    Boolean createNewKeys;

    String displayName = "Copy YAML value";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("%s`%s` to %s`%s`",
                (oldFilePath == null) ? "" : oldFilePath + ":",
                oldKeyPath,
                (newFilePath == null) ? "" : newFilePath + ":",
                newKey);
    }

    String description = "Copies a YAML value from one key to another. " +
               "The existing key/value pair remains unaffected by this change. " +
               "Attempts to merge the copied value into the new key if it already exists. " +
               "By default, attempts to create the new key if it does not exist.";

    @Override
    public Validated<Object> validate() {
        return super.validate()
                .and(JsonPathMatcher.validate("oldKeyPath", oldKeyPath))
                .and(JsonPathMatcher.validate("newKey", newKey));
    }

    @JsonCreator
    public CopyValue(String oldKeyPath, @Nullable String oldFilePath, String newKey, @Nullable String newFilePath, @Nullable Boolean createNewKeys) {
        this.oldKeyPath = oldKeyPath;
        this.oldFilePath = oldFilePath;
        this.newKey = newKey;
        this.newFilePath = newFilePath;
        this.createNewKeys = createNewKeys;
    }

    @Deprecated
    public CopyValue(String oldKeyPath, String oldFilePath, String newKey, String newFilePath) {
        this(oldKeyPath, oldFilePath, newKey, newFilePath, null);
    }

    public static class Accumulator {
        final Map<Path, String> snippetsByPath = new HashMap<>();
    }


    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        TreeVisitor<?, ExecutionContext> visitor = new YamlIsoVisitor<ExecutionContext>() {
            final JsonPathMatcher oldPathMatcher = new JsonPathMatcher(oldKeyPath);

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry source = super.visitMappingEntry(entry, ctx);
                if (oldPathMatcher.matches(getCursor())) {
                    Path path = getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath();
                    acc.snippetsByPath.putIfAbsent(path, entry.getValue().print(getCursor()));
                }
                return source;
            }
        };
        if (oldFilePath != null) {
            visitor = Preconditions.check(new FindSourceFiles(oldFilePath), visitor);
        }

        return visitor;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        if (acc.snippetsByPath.isEmpty()) {
            return TreeVisitor.noop();
        }

        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Documents visitDocuments(Yaml.Documents documents, ExecutionContext ctx) {
                Path currentPath = getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath();
                Yaml.Documents d = documents;
                boolean changed = false;
                for (Map.Entry<Path, String> entry : acc.snippetsByPath.entrySet()) {
                    Path targetPath = newFilePath != null
                            ? Paths.get(newFilePath)
                            : entry.getKey();
                    if (currentPath.equals(targetPath)) {
                        d = (Yaml.Documents) new MergeYaml(newKey, entry.getValue(), false, null, null, null, null, createNewKeys)
                                .getVisitor()
                                .visitNonNull(d, ctx);
                        changed = true;
                    }
                }
                if (changed) {
                    doAfterVisit(new UnfoldProperties(null, singletonList(newKey)).getVisitor());
                }
                return d;
            }
        };
    }
}
