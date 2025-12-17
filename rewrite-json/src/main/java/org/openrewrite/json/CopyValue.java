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
package org.openrewrite.json;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.json.tree.Json;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("LanguageMismatch")
@Value
@EqualsAndHashCode(callSuper = false)
public class CopyValue extends ScanningRecipe<CopyValue.Accumulator> {
    @Option(displayName = "Old key path",
            description = "A [JsonPath](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) expression to locate a JSON key/value pair to copy.",
            example = "$.source.kind")
    String oldKeyPath;

    @Option(displayName = "Old file path",
            description = "The file path to the JSON file to copy the value from. " +
                          "If `null` then the value will be copied from any JSON file it appears within.",
            example = "src/main/resources/application.json",
            required = false)
    @Nullable
    String oldFilePath;

    @Option(displayName = "New key path",
            description = "A [JsonPath](https://docs.openrewrite.org/reference/jsonpath-and-jsonpathmatcher-reference) expression defining where the value should be written.",
            example = "$.dest.kind")
    String newKey;

    @Option(displayName = "New file path",
            description = "The file path to the JSON file to copy the value to. " +
                          "If `null` then the value will be copied only into the same file it was found in.",
            example = "src/main/resources/application.json",
            required = false)
    @Nullable
    String newFilePath;

    @Override
    public String getDisplayName() {
        return "Copy JSON value";
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
        return "Copies a JSON value from one key to another. " +
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
        TreeVisitor<?, ExecutionContext> visitor = new JsonIsoVisitor<ExecutionContext>() {
            final JsonPathMatcher oldPathMatcher = new JsonPathMatcher(oldKeyPath);

            @Override
            public Json.Document visitDocument(Json.Document document, ExecutionContext ctx) {
                if (acc.snippet == null) {
                    return super.visitDocument(document, ctx);
                }
                return document;
            }

            @Override
            public Json.Member visitMember(Json.Member member, ExecutionContext ctx) {
                Json.Member source = super.visitMember(member, ctx);
                if (oldPathMatcher.matches(getCursor())) {
                    acc.snippet = member.getValue().print(getCursor());
                    acc.path = getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath();
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
        if (acc.snippet == null) {
            return TreeVisitor.noop();
        }
        return Preconditions.check(new FindSourceFiles(newFilePath == null ? acc.path.toString() : newFilePath),
                new AddKeyValue(extractPath(oldKeyPath), extractKey(newKey), acc.snippet, false).getVisitor());
    }

    // Extract the last key from the JsonPath expression
    // For example, "$.dest.kind" should return "kind"
    // For "$.dest" should return "dest"
    // Regex also works for bracket notation like "$['dest']['kind']"
    private static final Pattern lastKey = Pattern.compile("\\.(\\w+)$|\\['(\\w+)'\\]$");

    private String extractKey(String keyPath) {
        Matcher matcher = lastKey.matcher(keyPath);
        if (matcher.find() && matcher.groupCount() == 2) {
            return matcher.group(1);
        }
        return keyPath;
    }

    private static final Pattern arrayIndex = Pattern.compile("\\[\\d+\\]$");

    private String extractPath(String keyPath) {
        // Extract the path from the JsonPath expression
        // For example, "$.dest.kind" should return "$.dest"
        // For "$.destination" should return "$"
        // For bracket notation like "$['dest']['kind']" should return "$['dest']"
        int index = keyPath.lastIndexOf(".");
        if (index == -1) {
            index = keyPath.lastIndexOf("[");
        }
        keyPath = index > 0 ? keyPath.substring(0, index) : keyPath;

        // strip json array index, e.g. "$.arr[0]" should be "$.arr"
        Matcher matcher = arrayIndex.matcher(keyPath);
        if (matcher.find()) {
            keyPath = keyPath.substring(0, matcher.start());
        }
        return "$".equals(keyPath) ? "$." : keyPath;
    }
}
