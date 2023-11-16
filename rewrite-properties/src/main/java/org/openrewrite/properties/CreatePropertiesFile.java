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
package org.openrewrite.properties;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.junit.platform.commons.util.StringUtils;
import org.openrewrite.CreateFileVisitor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.tree.Properties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class CreatePropertiesFile extends ScanningRecipe<AtomicBoolean> {

    @Option(displayName = "Relative file path",
            description = "File path of new file.",
            example = "foo/bar/baz.properties")
    String relativeFileName;

    @Language("properties")
    @Option(displayName = "File contents",
            description = "Multiline text content for the file.",
            example = "a.property=value\nanother.property=value",
            required = false)
    @Nullable
    String fileContents;

    @Option(displayName = "Overwrite existing file",
            description = "If there is an existing file, should it be overwritten.",
            required = false)
    @Nullable
    Boolean overwriteExisting;

    @Override
    public @NonNull String getDisplayName() {
        return "Create Properties file";
    }

    @Override
    public @NonNull String getDescription() {
        return "Create a new Properties file.";
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
            return PropertiesParser.builder().build().parse(StringUtils.isNotBlank(fileContents) ? fileContents : "")
                    .map(brandNewFile -> (SourceFile) brandNewFile.withSourcePath(Paths.get(relativeFileName)))
                    .collect(Collectors.toList());
        }
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean created) {
        Path path = Paths.get(relativeFileName);
        return new PropertiesVisitor<ExecutionContext>() {
            @Override
            public Properties visitFile(Properties.File file, ExecutionContext executionContext) {
                if ((created.get() || Boolean.TRUE.equals(overwriteExisting)) && path.toString().equals(file.getSourcePath().toString())) {
                    if (StringUtils.isBlank(fileContents)) {
                        return file.withContent(emptyList());
                    }
                    Optional<SourceFile> sourceFiles = PropertiesParser.builder()
                            .build()
                            .parse(fileContents)
                            .map(brandNewFile -> (SourceFile) brandNewFile.withSourcePath(Paths.get(relativeFileName)))
                            .findFirst();

                    if (sourceFiles.isPresent() && sourceFiles.get() instanceof Properties.File) {
                        return (Properties.File) sourceFiles.get();
                    }
                }
                return file;
            }
        };
    }
}
