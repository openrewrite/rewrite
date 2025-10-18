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
package org.openrewrite.toml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.remote.Remote;
import org.openrewrite.toml.tree.Toml;

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
public class CreateTomlFile extends ScanningRecipe<AtomicBoolean> {

    @Option(displayName = "Relative file path",
            description = "File path of the new file.",
            example = "pyproject.toml")
    String relativeFileName;

    @Language("toml")
    @Option(displayName = "File contents",
            description = "Multiline text content for the file.",
            example = "[tool.poetry]\nname = \"my-project\"\nversion = \"0.1.0\"",
            required = false)
    @Nullable
    String fileContents;

    @Option(displayName = "File contents URL",
            description = "URL to fetch the TOML file contents from a remote source.",
            example = "https://raw.githubusercontent.com/example/repo/main/pyproject.toml",
            required = false)
    @Nullable
    String fileContentsUrl;

    @Option(displayName = "Overwrite existing file",
            description = "If there is an existing file, should it be overwritten?",
            required = false)
    @Nullable
    Boolean overwriteExisting;

    @Override
    public String getDisplayName() {
        return "Create TOML file";
    }

    @Override
    public String getDescription() {
        return "Create a new TOML file.";
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
            String tomlContents = getTomlContents(ctx);
            // Don't create empty files
            if (StringUtils.isBlank(tomlContents)) {
                return emptyList();
            }
            return TomlParser.builder().build().parse(tomlContents)
                    .map(brandNewFile -> (SourceFile) brandNewFile.withSourcePath(Paths.get(relativeFileName)))
                    .collect(toList());
        }
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean created) {
        Path path = Paths.get(relativeFileName);
        return new TomlVisitor<ExecutionContext>() {
            @Override
            public Toml.Document visitDocument(Toml.Document document, ExecutionContext ctx) {
                if (Boolean.TRUE.equals(overwriteExisting) && path.equals(document.getSourcePath())) {
                    @Language("toml")
                    String tomlContents = getTomlContents(ctx);
                    if (StringUtils.isBlank(tomlContents)) {
                        return document.withValues(emptyList());
                    }
                    if (document.printAll().equals(tomlContents.trim())) {
                        return document;
                    }
                    Optional<SourceFile> sourceFiles = TomlParser.builder().build()
                            .parse(tomlContents)
                            .findFirst();
                    if (sourceFiles.isPresent()) {
                        SourceFile sourceFile = sourceFiles.get();
                        if (sourceFile instanceof Toml.Document) {
                            return document.withValues(((Toml.Document) sourceFile).getValues());
                        }
                    }
                }
                return document;
            }
        };
    }

    private @Language("toml") String getTomlContents(ExecutionContext ctx) {
        @Language("toml") String tomlContents = fileContents;
        if (tomlContents == null && fileContentsUrl != null) {
            return Remote.builder(Paths.get(relativeFileName))
                    .build(URI.create(fileContentsUrl))
                    .printAll(ctx);
        }
        return tomlContents == null ? "" : tomlContents;
    }
}
