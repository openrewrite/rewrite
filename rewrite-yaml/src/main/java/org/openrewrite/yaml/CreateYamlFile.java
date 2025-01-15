/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.remote.Remote;
import org.openrewrite.yaml.tree.Yaml;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class CreateYamlFile extends ScanningRecipe<AtomicBoolean> {

    @Option(displayName = "Relative file path",
            description = "File path of new file.",
            example = "foo/bar/baz.yaml")
    String relativeFileName;

    @Language("yml")
    @Option(displayName = "File contents",
            description = "Multiline text content for the file.",
            example = "a:\n" +
                      "  property: value\n" +
                      "another:\n" +
                      "  property: value",
            required = false)
    @Nullable
    String fileContents;

    @Option(displayName = "File contents URL",
            description = "URL to file containing text content for the file. Use either `fileContents` or `fileContentsUrl` option.",
            example = "http://foo.bar/baz.yaml",
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
        return "Create YAML file";
    }

    @Override
    public String getDescription() {
        return "Create a new YAML file.";
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
            return YamlParser.builder().build().parse(getYamlContents(ctx))
                    .map(brandNewFile -> (SourceFile) brandNewFile.withSourcePath(Paths.get(relativeFileName)))
                    .collect(Collectors.toList());
        }
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean created) {
        Path path = Paths.get(relativeFileName);
        return new YamlVisitor<ExecutionContext>() {

            @Override
            public Yaml visitDocuments(Yaml.Documents documents, ExecutionContext ctx) {
                if (Boolean.TRUE.equals(overwriteExisting) && path.equals(documents.getSourcePath())) {
                    @Language("yml")
                    String yamlContents = getYamlContents(ctx);
                    if (StringUtils.isBlank(yamlContents)) {
                        return documents.withDocuments(emptyList());
                    }
                    if (documents.printAll().equals(yamlContents)) {
                        return documents;
                    }
                    Optional<SourceFile> sourceFiles = YamlParser.builder().build()
                            .parse(yamlContents)
                            .findFirst();
                    if (sourceFiles.isPresent()) {
                        SourceFile sourceFile = sourceFiles.get();
                        if (sourceFile instanceof Yaml.Documents) {
                            return documents.withDocuments(((Yaml.Documents) sourceFile).getDocuments());
                        }
                    }
                }
                return documents;
            }
        };
    }

    @Language("yml")
    private String getYamlContents(ExecutionContext ctx) {
        @Language("yml") String yamlContents = fileContents;
        if (yamlContents == null && fileContentsUrl != null) {
            yamlContents = Remote.builder(Paths.get(relativeFileName), URI.create(fileContentsUrl)).build().printAll(ctx);
        }
        return yamlContents == null ? "" : yamlContents;
    }
}
