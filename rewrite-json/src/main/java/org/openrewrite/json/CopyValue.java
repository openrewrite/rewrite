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

@SuppressWarnings("LanguageMismatch")
@Value
@EqualsAndHashCode(callSuper = false)
public class CopyValue extends ScanningRecipe<CopyValue.Accumulator> {
    @Option(displayName = "Source key path",
            description = "A [JSONPath](https://www.rfc-editor.org/rfc/rfc9535.html) expression to locate a JSON value to copy.",
            example = "$.source.kind")
    String sourceKeyPath;

    @Option(displayName = "Source file path",
            description = "The file path to the JSON file to copy the value from. " +
                          "If `null` then the value will be copied from any JSON file it appears within.",
            example = "src/main/resources/application.json",
            required = false)
    @Nullable
    String sourceFilePath;

    @Option(displayName = "Destination key path",
            description = "A [JSONPath](https://www.rfc-editor.org/rfc/rfc9535.html) expression to locate the *parent* JSON entry.",
            example = "'$.subjects.*' or '$.' or '$.x[1].y.*' etc.")
    String destinationKeyPath;

    @Option(displayName = "Destination key",
            description = "The key to create.",
            example = "myKey")
    String destinationKey;

    @Option(displayName = "Destination file path",
            description = "The file path to the JSON file to copy the value to. " +
                          "If `null` then the value will be copied only into the same file it was found in.",
            example = "src/main/resources/application.json",
            required = false)
    @Nullable
    String destinationFilePath;

    @Override
    public String getDisplayName() {
        return "Copy JSON value";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("%s`%s` to %s`%s`",
                (sourceFilePath == null) ? "" : sourceFilePath + ":",
                sourceKeyPath,
                (destinationFilePath == null) ? "" : destinationFilePath + ":",
                destinationKeyPath);
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
            final JsonPathMatcher sourcePathMatcher = new JsonPathMatcher(sourceKeyPath);

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
                if (sourcePathMatcher.matches(getCursor())) {
                    acc.snippet = member.getValue().print(getCursor());
                    acc.path = getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath();
                }
                return source;
            }
        };
        if (sourceFilePath != null) {
            visitor = Preconditions.check(new FindSourceFiles(sourceFilePath), visitor);
        }

        return visitor;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        if (acc.snippet == null) {
            return TreeVisitor.noop();
        }
        return Preconditions.check(new FindSourceFiles(destinationFilePath == null ? acc.path.toString() : destinationFilePath),
                new AddKeyValue(destinationKeyPath, destinationKey, acc.snippet, false).getVisitor());
    }
}
