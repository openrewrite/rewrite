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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;

@SuppressWarnings("LanguageMismatch")
@Value
@EqualsAndHashCode(callSuper = false)
public class CopyValue extends ScanningRecipe<CopyValue.Accumulator> {
    @Option(displayName = "Old key path",
            description = "A [JsonPath](https://github.com/json-path/JsonPath) expression to locate a YAML key/value pair to copy.",
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
            description = "A [JsonPath](https://github.com/json-path/JsonPath) expression defining where the value should be written.",
            example = "$.dest.kind")
    String newKey;

    @Option(displayName = "New file path",
            description = "The file path to the YAML file to copy the value to. " +
                          "If `null` then the value will be copied only into the same file it was found in.",
            example = "src/main/resources/application.yaml",
            required = false)
    @Nullable
    String newFilePath;

    @Override
    public String getDisplayName() {
        return "Copy YAML value";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("%s`%s` to %s`%s`",
                (oldFilePath == null) ? "" : oldFilePath + ":",
                oldKeyPath,
                (newFilePath == null) ? "" : newFilePath + ":",
                newKey);
    }

    @Override
    public String getDescription() {
        return "Copies a YAML value from one key to another. " +
               "The existing key/value pair remains unaffected by this change. " +
               "Attempts to merge the copied value into the new key if it already exists. " +
               "Attempts to create the new key if it does not exist.";
    }

    @Data
    public static class Accumulator {
        @Nullable
        String snippet;
        Path path;
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
            public Yaml.Documents visitDocuments(Yaml.Documents documents, ExecutionContext executionContext) {
                if(acc.snippet == null) {
                    return super.visitDocuments(documents, executionContext);
                }
                return documents;
            }

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry source = super.visitMappingEntry(entry, ctx);
                if (oldPathMatcher.matches(getCursor())) {
                    acc.snippet = entry.getValue().print(getCursor());
                    acc.path = getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath();
                }
                return source;
            }
        };
        if(oldFilePath != null) {
            visitor = Preconditions.check(new FindSourceFiles(oldFilePath).getVisitor(), visitor);
        }

        return visitor;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        if(acc.snippet == null) {
            return TreeVisitor.noop();
        }
        TreeVisitor<?, ExecutionContext> visitor = new MergeYaml(newKey, acc.snippet, false, null).getVisitor();
        if(newFilePath == null) {
            visitor = Preconditions.check(new FindSourceFiles(acc.path.toString()).getVisitor(), visitor);
        } else {
            visitor = Preconditions.check(new FindSourceFiles(newFilePath).getVisitor(), visitor);
        }

        return visitor;
    }
}
