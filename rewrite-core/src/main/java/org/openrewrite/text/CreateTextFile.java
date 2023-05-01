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
package org.openrewrite.text;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class CreateTextFile extends ScanningRecipe<AtomicBoolean> {

    @Option(displayName = "File contents",
            description = "Multiline text content for the file.",
            example = "Some text.")
    String fileContents;

    @Option(displayName = "Relative file path",
            description = "File path of new file.",
            example = "foo/bar/baz.txt")
    String relativeFileName;

    @Option(displayName = "Overwrite existing file",
            description = "If there is an existing file, should it be overwritten.",
            required = false)
    @Nullable
    Boolean overwriteExisting;

    @Override
    public String getDisplayName() {
        return "Create text file";
    }

    @Override
    public String getDescription() {
        return "Creates a new plain text file.";
    }

    @Override
    public AtomicBoolean getInitialValue() {
        return new AtomicBoolean(true);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean shouldCreate) {
        Path path = Paths.get(relativeFileName);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if (path.toString().equals(sourceFile.getSourcePath().toString())) {
                    shouldCreate.set(false);
                }
                return sourceFile;
            }
        };
    }

    @Override
    public Collection<SourceFile> generate(AtomicBoolean shouldCreate, ExecutionContext ctx) {
        if(shouldCreate.get()) {
            return new PlainTextParser().parse(fileContents)
                    .map(brandNewFile -> brandNewFile.withSourcePath(Paths.get(relativeFileName)))
                    .collect(Collectors.toList());
        }
        return emptyList();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean created) {
        Path path = Paths.get(relativeFileName);
        return new PlainTextVisitor<ExecutionContext>() {

            @Override
            public @Nullable PlainText visit(@Nullable Tree tree, ExecutionContext executionContext) {
                if(created.get()) {
                    return (PlainText) tree;
                }
                return super.visit(tree, executionContext);
            }

            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                if (path.toString().equals(text.getSourcePath().toString()) && Boolean.TRUE.equals(overwriteExisting)) {
                    return text.withText(fileContents);
                }
                return text;
            }
        };
    }
}
