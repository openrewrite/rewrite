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
package org.openrewrite.properties;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.MetricsHelper;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.openrewrite.Tree.randomId;

public class PropertiesParser implements Parser<Properties.File> {
    @Override
    public Stream<Properties.File> parse(@Language("properties") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public Stream<Properties.File> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return acceptedInputs(sourceFiles).stream()
                .map(sourceFile -> {
                    Path path = sourceFile.getRelativePath(relativeTo);
                    Timer.Builder timer = Timer.builder("rewrite.parse")
                            .description("The time spent parsing a properties file")
                            .tag("file.type", "Properties");
                    Timer.Sample sample = Timer.start();
                    try (EncodingDetectingInputStream is = sourceFile.getSource(ctx)) {
                        Properties.File file = parseFromInput(path, is)
                                .withFileAttributes(sourceFile.getFileAttributes());
                        sample.stop(MetricsHelper.successTags(timer).register(Metrics.globalRegistry));
                        parsingListener.parsed(sourceFile, file);
                        return file;
                    } catch (Throwable t) {
                        sample.stop(MetricsHelper.errorTags(timer, t).register(Metrics.globalRegistry));
                        ParsingExecutionContextView.view(ctx).parseFailure(sourceFile, relativeTo, this, t);
                        ctx.getOnError().accept(new IllegalStateException(sourceFile.getPath() + " " + t.getMessage(), t));
                        return null;
                    }
                })
                .filter(Objects::nonNull);
    }

    private Properties.File parseFromInput(Path sourceFile, EncodingDetectingInputStream source) {
        List<Properties.Content> contents = new ArrayList<>();

        StringBuilder prefix = new StringBuilder();
        StringBuilder buff = new StringBuilder();
        String s = source.readFully();

        char prev = '$';
        boolean isEscapedNewLine = false;
        for (char c : s.toCharArray()) {
            if (isEscapedNewLine) {
                if (Character.isWhitespace(c)) {
                    continue;
                } else {
                    isEscapedNewLine = false;
                }
            }

            if (c == '\n') {
                if (prev == '\\') {
                    isEscapedNewLine = true;
                    buff.deleteCharAt(buff.length() - 1);
                } else {
                    Properties.Content content = extractContent(buff.toString(), prefix);
                    if (content != null) {
                        contents.add(content);
                    }
                    buff = new StringBuilder();
                    prefix.append(c);
                }
            } else {
                buff.append(c);
            }
            prev = c;
        }

        Properties.Content content = extractContent(buff.toString(), prefix);
        if (content != null) {
            contents.add(content);
        }

        return new Properties.File(
                randomId(),
                "",
                Markers.EMPTY,
                sourceFile,
                contents,
                prefix.toString(),
                source.getCharset().name(),
                source.isCharsetBomMarked(),
                FileAttributes.fromPath(sourceFile),
                null
                );
    }

    @Nullable
    private Properties.Content extractContent(String line, StringBuilder prefix) {
        Properties.Content content = null;
        if (line.trim().startsWith("#") || line.trim().startsWith("!")) {
            Properties.Comment.Delimiter delimiter = line.trim().startsWith("#") ?
                    Properties.Comment.Delimiter.HASH_TAG : Properties.Comment.Delimiter.EXCLAMATION_MARK;
            content = commentFromLine(line, prefix.toString(), delimiter);
            prefix.delete(0, prefix.length());
        } else if (line.contains("=") || line.contains(":") || isDelimitedByWhitespace(line)) {
            StringBuilder trailingWhitespaceBuffer = new StringBuilder();
            content = entryFromLine(line, prefix.toString(), trailingWhitespaceBuffer);
            prefix.delete(0, prefix.length());
            prefix.append(trailingWhitespaceBuffer);
        } else {
            prefix.append(line);
        }
        return content;
    }

    private boolean isDelimitedByWhitespace(String line) {
        return line.length() >=3 && !Character.isWhitespace(line.charAt(0)) && !Character.isWhitespace(line.length() - 1) && line.contains(" ");
    }

    private Properties.Comment commentFromLine(String line, String prefix, Properties.Comment.Delimiter delimiter) {
        StringBuilder prefixBuilder = new StringBuilder(prefix);
        StringBuilder message = new StringBuilder();

        boolean inComment = false;
        int state = 0;
        for (char c : line.toCharArray()) {
            switch (state) {
                case 0:
                    if (Character.isWhitespace(c)) {
                        prefixBuilder.append(c);
                        break;
                    }
                    state++;
                case 1:
                    if ((c == '#' || c == '!') && !inComment) {
                        inComment = true;
                        continue;
                    } else if (!Character.isWhitespace(c)) {
                        message.append(c);
                        break;
                    }
                    state++;
                case 2:
                    if (!Character.isWhitespace(c)) {
                        // multi-word comment
                        message.append(c);
                        state--;
                        break;
                    } else {
                        message.append(c);
                    }
            }
        }

        return new Properties.Comment(
                randomId(),
                prefixBuilder.toString(),
                Markers.EMPTY,
                delimiter,
                message.toString()
        );
    }

    private Properties.Entry entryFromLine(String line, String prefix, StringBuilder trailingWhitespaceBuffer) {
        StringBuilder prefixBuilder = new StringBuilder(prefix),
                key = new StringBuilder(),
                equalsPrefix = new StringBuilder(),
                valuePrefix = new StringBuilder(),
                value = new StringBuilder();

        Properties.Entry.Delimiter delimiter = Properties.Entry.Delimiter.NONE;
        char prev = '$';
        int state = 0;
        for (char c : line.toCharArray()) {
            switch (state) {
                case 0:
                    if (Character.isWhitespace(c)) {
                        prefixBuilder.append(c);
                        break;
                    }
                    state++;
                case 1:
                    if (c == '=' || c == ':') {
                        if (prev == '\\') {
                            key.append(c);
                            break;
                        } else {
                            delimiter = Properties.Entry.Delimiter.getDelimiter(String.valueOf(c));
                            state += 2;
                        }
                    } else if (!Character.isWhitespace(c)) {
                        key.append(c);
                        break;
                    } else {
                        state++;
                    }
                case 2:
                    if (Character.isWhitespace(c)) {
                        equalsPrefix.append(c);
                        break;
                    } else if (c == '=' || c == ':') {
                        delimiter = Properties.Entry.Delimiter.getDelimiter(String.valueOf(c));
                    }
                    state++;
                case 3:
                    if (c == '=' || c == ':') {
                        continue;
                    } else if (Character.isWhitespace(c)) {
                        valuePrefix.append(c);
                        break;
                    }
                    state++;
                case 4:
                    if (!Character.isWhitespace(c)) {
                        value.append(c);
                        break;
                    }
                    state++;
                case 5:
                    if (!Character.isWhitespace(c)) {
                        // multi-word value
                        value.append(trailingWhitespaceBuffer);
                        trailingWhitespaceBuffer.delete(0, trailingWhitespaceBuffer.length());
                        value.append(c);
                        state--;
                        break;
                    } else {
                        trailingWhitespaceBuffer.append(c);
                    }
            }
            prev = c;
        }

        return new Properties.Entry(
                randomId(),
                prefixBuilder.toString(),
                Markers.EMPTY,
                key.toString(),
                equalsPrefix.toString(),
                delimiter,
                new Properties.Value(randomId(), valuePrefix.toString(), Markers.EMPTY, value.toString())
        );
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().endsWith(".properties");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.properties");
    }

    public static Builder builder() {
        return new Builder();
    }
    public static class Builder extends org.openrewrite.Parser.Builder {

        public Builder() {
            super(Properties.File.class);
        }

        @Override
        public PropertiesParser build() {
            return new PropertiesParser();
        }

        @Override
        public String getDslName() {
            return "properties";
        }
    }
}
