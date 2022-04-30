/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.quark;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.openrewrite.Tree.randomId;

public class QuarkParser implements Parser<Quark> {

    public static List<Quark> parseAllOtherFiles(Path rootDir, List<SourceFile> sourceFiles) throws IOException {
        Set<Path> sourceFilePaths = new HashSet<>();
        for (SourceFile sourceFile : sourceFiles) {
            sourceFilePaths.add(sourceFile.getSourcePath());
        }

        List<Path> quarks = new ArrayList<>();
        Files.walkFileTree(rootDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!attrs.isSymbolicLink() && !attrs.isOther() && !sourceFilePaths.contains(file)) {
                    quarks.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return new QuarkParser().parse(quarks, rootDir, new InMemoryExecutionContext());
    }

    @Override
    public List<Quark> parseInputs(Iterable<Parser.Input> sources, @Nullable Path relativeTo,
                                   ExecutionContext ctx) {
        List<Quark> quarks = new ArrayList<>();
        for (Parser.Input source : sources) {
            quarks.add(new Quark(randomId(),
                    relativeTo == null ?
                            source.getPath() :
                            relativeTo.relativize(source.getPath()).normalize(),
                    Markers.EMPTY));
        }
        return quarks;
    }

    @Override
    public boolean accept(Path path) {
        return true;
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file");
    }
}
