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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.openrewrite.Tree.randomId;

public class QuarkParser implements Parser<Quark> {

    public static List<Quark> parseAllOtherFiles(Path rootDir, List<SourceFile> sourceFiles) throws IOException {
        Stack<List<PathMatcher>> gitignores = new Stack<>();
        parseGitignore(new File(System.getProperty("user.home") + "/.gitignore"), gitignores);

        Set<Path> sourceFilePaths = new HashSet<>();
        for (SourceFile sourceFile : sourceFiles) {
            sourceFilePaths.add(sourceFile.getSourcePath());
        }

        List<Path> quarks = new ArrayList<>();
        Files.walkFileTree(rootDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (isIgnored(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                parseGitignore(dir.resolve(".gitignore").toFile(), gitignores);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!attrs.isSymbolicLink() && !attrs.isOther() &&
                    !sourceFilePaths.contains(rootDir.relativize(file)) &&
                    !isIgnored(file)) {
                    quarks.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                if (dir.resolve(".gitignore").toFile().exists()) {
                    gitignores.pop();
                }
                return FileVisitResult.CONTINUE;
            }

            private boolean isIgnored(Path path) {
                for (List<PathMatcher> gitignore : gitignores) {
                    for (PathMatcher gitignoreLine : gitignore) {
                        if (gitignoreLine.matches(path)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        return new QuarkParser().parse(quarks, rootDir, new InMemoryExecutionContext())
                .collect(Collectors.toList());
    }

    private static void parseGitignore(File gitignore, Stack<List<PathMatcher>> gitignores) throws IOException {
        if (gitignore.exists()) {
            try (FileInputStream fis = new FileInputStream(gitignore);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
                List<PathMatcher> gitignorePaths = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().startsWith("#") && !StringUtils.isBlank(line)) {
                        gitignorePaths.add(gitignore.toPath().getFileSystem().getPathMatcher("glob:**/" + line.trim() +
                                                                                             (line.trim().endsWith("/") ? "**" : "")));
                    }
                }
                gitignores.add(gitignorePaths);
            }
        }
    }

    @Override
    public Stream<Quark> parseInputs(Iterable<Parser.Input> sources, @Nullable Path relativeTo,
                                     ExecutionContext ctx) {
        return StreamSupport.stream(sources.spliterator(), false).map(source ->
                new Quark(randomId(),
                        source.getRelativePath(relativeTo),
                        Markers.EMPTY,
                        null,
                        source.getFileAttributes())
        );
    }

    @Override
    public boolean accept(Path path) {
        return true;
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {
        public Builder() {
            super(Quark.class);
        }

        @Override
        public Parser<Quark> build() {
            return new QuarkParser();
        }

        @Override
        public String getDslName() {
            return "other";
        }

    }
}
