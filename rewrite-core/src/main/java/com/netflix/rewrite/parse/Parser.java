/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.parse;

import com.netflix.rewrite.internal.lang.NonNullApi;
import com.netflix.rewrite.internal.lang.Nullable;
import com.netflix.rewrite.tree.Tr;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@NonNullApi
public interface Parser {
    /**
     * Clear any in-memory parser caches that may prevent reparsing of classes with the same fully qualified name in
     * different rounds
     */
    void reset();

    List<Tr.CompilationUnit> parse(List<Path> sourceFiles, @Nullable Path relativeTo);

    default List<Tr.CompilationUnit> parse(List<Path> sourceFiles) {
        return parse(sourceFiles, null);
    }

    default Tr.CompilationUnit parse(String source, String whichDependsOn) {
        return parse(source, singletonList(whichDependsOn));
    }

    default Tr.CompilationUnit parse(String source, List<String> whichDependOn) {
        return parse(source, whichDependOn.toArray(String[]::new));
    }

    default Tr.CompilationUnit parse(String source, String... whichDependOn) {
        try {
            Path temp = Files.createTempDirectory("sources");

            var classPattern = Pattern.compile("(class|interface|enum)\\s*(<[^>]*>)?\\s+(\\w+)");

            Function<String, String> simpleName = sourceStr -> {
                var classMatcher = classPattern.matcher(sourceStr);
                return classMatcher.find() ? classMatcher.group(3) : null;
            };

            Function<String, Path> sourceFile = sourceText -> {
                var file = temp.resolve(simpleName.apply(sourceText) + ".java");
                try {
                    Files.writeString(file, sourceText);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                return file;
            };

            try {
                List<Tr.CompilationUnit> cus = parse(Stream.concat(
                        Arrays.stream(whichDependOn).map(sourceFile),
                        Stream.of(sourceFile.apply(source))
                ).collect(toList()));

                return cus.get(cus.size() - 1);
            } finally {
                // delete temp recursively
                //noinspection ResultOfMethodCallIgnored
                Files.walk(temp)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
