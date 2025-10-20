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

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.jgit.diff.RawText;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markup;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

public class PlainTextParser implements Parser {

    /**
     * Downcast a {@link SourceFile} to a {@link PlainText} if it isn't already one.
     *
     * @param sourceFile A source file which may be a {@link PlainText} or not.
     * @return The same {@link PlainText} reference if the source file is already a {@link PlainText}.
     * Otherwise, a new {@link PlainText} instance with the same contents as the source file.
     */
    public static PlainText convert(SourceFile sourceFile) {
        if (sourceFile instanceof PlainText) {
            return (PlainText) sourceFile;
        }
        PlainText text = PlainTextParser.builder().build()
                .parse(sourceFile.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Failed to parse as plain text"))
                .withSourcePath(sourceFile.getSourcePath())
                .withFileAttributes(sourceFile.getFileAttributes())
                .withCharsetBomMarked(sourceFile.isCharsetBomMarked())
                .withId(sourceFile.getId())
                .withMarkers(sourceFile.getMarkers().withMarkers(gatherMarkers(sourceFile)));
        if (sourceFile.getCharset() != null) {
            text = (PlainText) text.withCharset(sourceFile.getCharset());
        }
        return text;
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo,
                                          ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return StreamSupport.stream(sources.spliterator(), false).map(input -> {
            Path path = input.getRelativePath(relativeTo);
            parsingListener.startedParsing(input);
            try {
                EncodingDetectingInputStream is = input.getSource(ctx);
                String sourceStr = is.readFully();
                PlainText plainText = PlainText.builder()
                        .sourcePath(path)
                        .charsetName(is.getCharset().name())
                        .charsetBomMarked(is.isCharsetBomMarked())
                        .fileAttributes(input.getFileAttributes())
                        .text(sourceStr)
                        .build();
                parsingListener.parsed(input, plainText);
                return plainText;
            } catch (Throwable t) {
                ctx.getOnError().accept(t);
                return ParseError.build(this, input, relativeTo, ctx, t);
            }
        });
    }

    @Override
    public boolean accept(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            return !RawText.isBinary(is);
        } catch (IOException ignored) {
            return false;
        }
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.txt");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {
        @Nullable
        private Collection<PathMatcher> plainTextMasks;

        public Builder() {
            super(PlainText.class);
        }

        public Builder plainTextMasks(Collection<PathMatcher> plainTextMasks) {
            this.plainTextMasks = plainTextMasks;
            return this;
        }

        public Builder plainTextMasks(Path basePath, Iterable<String> plainTextMaskGlobs) {
            return plainTextMasks(StreamSupport.stream(plainTextMaskGlobs.spliterator(), false)
                    .map((o) -> basePath.getFileSystem().getPathMatcher("glob:" + o))
                    .collect(toList()));
        }

        @Override
        public PlainTextParser build() {
            if (plainTextMasks != null) {
                return new PlainTextParser() {
                    @Override
                    public boolean accept(Path path) {
                        for (PathMatcher matcher : plainTextMasks) {
                            if (matcher.matches(path)) {
                                return true;
                            }
                        }
                        // PathMather will not evaluate the path "README.md" to be matched by the pattern "**/README.md"
                        // This is counter-intuitive for most users and would otherwise require separate exclusions for files at the root and files in subdirectories
                        if (!path.isAbsolute() && !path.startsWith(File.separator)) {
                            return accept(Paths.get("/" + path));
                        }
                        return super.accept(path);
                    }
                };
            }
            return new PlainTextParser();
        }

        @Override
        public String getDslName() {
            return "text";
        }
    }

    private static List<Marker> gatherMarkers(SourceFile sourceFile) {
        return new TreeVisitor<Tree, List<Marker>>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, List<Marker> markers) {
                if (tree != null && tree != sourceFile) {
                    tree.getMarkers().getMarkers().forEach(marker -> {
                        if (marker instanceof SearchResult || marker instanceof Markup) {
                            markers.add(marker);
                        }
                    });
                }
                return super.visit(tree, markers);
            }
        }.reduce(sourceFile, new ArrayList<>(sourceFile.getMarkers().getMarkers()));
    }
}
