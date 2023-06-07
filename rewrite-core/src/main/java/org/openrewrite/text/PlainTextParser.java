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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.openrewrite.Tree.randomId;

public class PlainTextParser implements Parser<PlainText> {

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
                .parse(sourceFile.printAll())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Failed to parse as plain text"))
                .withSourcePath(sourceFile.getSourcePath())
                .withFileAttributes(sourceFile.getFileAttributes())
                .withCharsetBomMarked(sourceFile.isCharsetBomMarked())
                .withId(sourceFile.getId());
        if (sourceFile.getCharset() != null) {
            text = (PlainText) text.withCharset(sourceFile.getCharset());
        }
        return text;
    }

    @Override
    public Stream<PlainText> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo,
                                         ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return StreamSupport.stream(sources.spliterator(), false)
                .map(source -> {
                    Path path = source.getRelativePath(relativeTo);
                    try {
                        EncodingDetectingInputStream is = source.getSource(ctx);
                        String sourceStr = is.readFully();
                        PlainText plainText = new PlainText(randomId(),
                                path,
                                Markers.EMPTY,
                                is.getCharset().name(),
                                is.isCharsetBomMarked(),
                                source.getFileAttributes(),
                                null,
                                sourceStr,
                                null);
                        parsingListener.parsed(source, plainText);
                        return plainText;
                    } catch (Throwable t) {
                        ParsingExecutionContextView.view(ctx).parseFailure(source, relativeTo, this, t);
                        ctx.getOnError().accept(t);
                    }
                    return null;
                })
                .filter(Objects::nonNull);
    }

    @Override
    public boolean accept(Path path) {
        return true;
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.txt");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {
        public Builder() {
            super(PlainText.class);
        }

        @Override
        public Parser<?> build() {
            return new PlainTextParser();
        }

        @Override
        public String getDslName() {
            return "text";
        }
    }
}
