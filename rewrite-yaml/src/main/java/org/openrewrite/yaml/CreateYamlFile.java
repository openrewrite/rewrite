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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.CreateFileVisitor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
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

    @Option(displayName = "Overwrite existing file",
            description = "If there is an existing file, should it be overwritten.",
            required = false)
    @Nullable
    Boolean overwriteExisting;

    @JsonCreator
    public CreateYamlFile(
            @JsonProperty("relativeFileName") String relativeFileName,
            @JsonProperty("overwriteExisting") @Nullable Boolean overwriteExisting
    ) {
        this.relativeFileName = relativeFileName;
        this.overwriteExisting = overwriteExisting;
    }

    @Override
    public @NonNull String getDisplayName() {
        return "Create properties file";
    }

    @Override
    public @NonNull String getDescription() {
        return "Create a new YAML file.";
    }

    @Override
    public @NonNull AtomicBoolean getInitialValue(@NonNull ExecutionContext ctx) {
        return new AtomicBoolean(true);
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getScanner(@NonNull AtomicBoolean shouldCreate) {
        return new CreateFileVisitor(relativeFileName, shouldCreate);
    }

    @Override
    public @NonNull Collection<SourceFile> generate(AtomicBoolean shouldCreate, @NonNull ExecutionContext ctx) {
        if (shouldCreate.get()) {
            return YamlParser.builder().build().parse("")
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
            public Yaml visitDocuments(Yaml.Documents documents, ExecutionContext executionContext) {
                if ((created.get() || Boolean.TRUE.equals(overwriteExisting)) && path.toString().equals(documents.getSourcePath().toString())) {
                    return documents.withDocuments(emptyList());
                }
                return documents;
            }
        };
    }
}
