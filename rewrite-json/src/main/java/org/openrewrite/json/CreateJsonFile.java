/*
 * Copyright 2025 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.json.tree.Json;
import org.openrewrite.remote.Remote;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Value
@EqualsAndHashCode(callSuper = false)
public class CreateJsonFile extends ScanningRecipe<AtomicBoolean> {
    @Option(displayName = "Relative file path",
            description = "File path of new file.",
            example = "foo/bar/baz.json")
    String relativeFileName;

    @Language("json")
    @Option(displayName = "File contents",
            description = "Multiline text content for the file.",
            example = "{\"a\": {\"property\": \"value\"}, \"another\": {\"property\": \"value\"}}",
            required = false)
    @Nullable
    String fileContents;

    @Option(displayName = "File contents URL",
            description = "URL to file containing text content for the file. Use either `fileContents` or `fileContentsUrl` option.",
            example = "http://foo.bar/baz.json",
            required = false)
    @Nullable
    String fileContentsUrl;

    @Option(displayName = "Overwrite existing file",
            description = "If there is an existing file, should it be overwritten.",
            required = false)
    @Nullable
    Boolean overwriteExisting;

    @Override
    public String getDisplayName() {
        return "Create JSON file";
    }

    @Override
    public String getDescription() {
        return "Create a new JSON file.";
    }

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(true);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean shouldCreate) {
        return new CreateFileVisitor(Paths.get(relativeFileName), shouldCreate);
    }

    @Override
    public Collection<SourceFile> generate(AtomicBoolean shouldCreate, ExecutionContext ctx) {
        if (shouldCreate.get()) {
            return JsonParser.builder().build().parse(getJsonContents(ctx))
                    .map(brandNewFile -> (SourceFile) brandNewFile.withSourcePath(Paths.get(relativeFileName)))
                    .collect(toList());
        }
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean created) {
        Path path = Paths.get(relativeFileName);
        return new JsonVisitor<ExecutionContext>() {
            @Override
            public Json visitDocument(Json.Document document, ExecutionContext ctx) {
                if (Boolean.TRUE.equals(overwriteExisting) && path.equals(document.getSourcePath())) {
                    @Language("json")
                    String jsonContents = getJsonContents(ctx);
                    if (StringUtils.isBlank(jsonContents)) {
                        return document.withValue(null);
                    }
                    if (document.printAll().equals(jsonContents)) {
                        return document;
                    }
                    Optional<SourceFile> sourceFiles = JsonParser.builder().build()
                            .parse(jsonContents)
                            .findFirst();
                    if (sourceFiles.isPresent()) {
                        SourceFile sourceFile = sourceFiles.get();
                        if (sourceFile instanceof Json.Document) {
                            return document.withValue(((Json.Document) sourceFile).getValue());
                        }
                    }
                }
                return document;
            }
        };
    }

    @Language("json")
    private String getJsonContents(ExecutionContext ctx) {
        @Language("json") String jsonContents = fileContents;
        if (jsonContents == null && fileContentsUrl != null) {
            jsonContents = Remote.builder(Paths.get(relativeFileName))
                    .build(URI.create(fileContentsUrl))
                    .printAll(ctx);
        }
        return jsonContents == null ? "" : jsonContents;
    }
}
