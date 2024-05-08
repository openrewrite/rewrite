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

import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.openrewrite.Tree.randomId;

public class PropertiesParser implements Parser {
    @Override
    public Stream<SourceFile> parse(@Language("properties") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return acceptedInputs(sourceFiles).map(input -> {
            parsingListener.startedParsing(input);
            Path path = input.getRelativePath(relativeTo);
            try (EncodingDetectingInputStream is = input.getSource(ctx)) {
                Properties.File file = parseFromInput(path, is)
                        .withFileAttributes(input.getFileAttributes());
                parsingListener.parsed(input, file);
                return requirePrintEqualsInput(file, input, relativeTo, ctx);
            } catch (Throwable t) {
                ctx.getOnError().accept(t);
                return ParseError.build(this, input, relativeTo, ctx, t);
            }
        });
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
                    buff.append(c);
                    continue;
                } else {
                    isEscapedNewLine = false;
                }
            }

            if (c == '\n') {
                if (prev == '\\') {
                    isEscapedNewLine = true;
                    buff.append(c);
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
                Collections.unmodifiableList(contents),
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
        return line.length() >= 3 && !Character.isWhitespace(line.charAt(0)) && !Character.isWhitespace(line.length() - 1) && line.contains(" ");
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

    static enum State {
        WHITESPACE_BEFORE_KEY,
        KEY,
        KEY_OR_WHITESPACE,
        WHITESPACE_OR_DELIMITER,
        WHITESPACE_OR_VALUE,
        VALUE,
        VALUE_OR_TRAILING
    }

    private Properties.Entry entryFromLine(String line, String prefix, StringBuilder trailingWhitespaceBuffer) {
        StringBuilder prefixBuilder = new StringBuilder(prefix),
                key = new StringBuilder(),
                equalsPrefix = new StringBuilder(),
                valuePrefix = new StringBuilder(),
                value = new StringBuilder();
        
        Properties.Entry.Delimiter delimiter = Properties.Entry.Delimiter.NONE;
        char prev = '$';
        State state = State.WHITESPACE_BEFORE_KEY;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            switch (state) {
                case WHITESPACE_BEFORE_KEY:
                    if (Character.isWhitespace(c)) {
                        prefixBuilder.append(c);
                        break;
                    }
                    state = State.KEY;
                case KEY:
                    if (c == '=' || c == ':') {
                        if (prev == '\\') {
                            key.append(c);
                            break;
                        } else {
                            delimiter = Properties.Entry.Delimiter.getDelimiter(String.valueOf(c));
                            state = State.WHITESPACE_OR_VALUE;
                            break;
                        }
                    } else if (c == '\\') {
                        key.append(c);
                        state = State.KEY_OR_WHITESPACE;
                        break;
                    } else if (!Character.isWhitespace(c)) {
                        key.append(c);
                        break;
                    } else {
                        equalsPrefix.append(c);
                        state = State.WHITESPACE_OR_DELIMITER;
                        break;
                    }
                case KEY_OR_WHITESPACE:
                    if (Character.isWhitespace(c)) {
                        trailingWhitespaceBuffer.append(c);
                        break;
                    } else {
                        // multi-word or continuation line value
                        key.append(trailingWhitespaceBuffer);
                        trailingWhitespaceBuffer.setLength(0);
                        key.append(c);
                        state = State.KEY;
                        break;
                    }
                case WHITESPACE_OR_DELIMITER:
                    if (Character.isWhitespace(c)) {
                        equalsPrefix.append(c);
                        break;
                    } else if (c == '=' || c == ':') {
                        delimiter = Properties.Entry.Delimiter.getDelimiter(String.valueOf(c));
                        state = State.WHITESPACE_OR_VALUE;
                        break;
                    }
                case WHITESPACE_OR_VALUE:
                    if (Character.isWhitespace(c)) {
                        valuePrefix.append(c);
                        break;
                    }
                    else {
                        value.append(c);
                        state = State.VALUE;
                        break;
                    }
                case VALUE:
                    if (!Character.isWhitespace(c)) {
                        value.append(c);
                        break;
                    }
                    state = State.VALUE_OR_TRAILING;
                case VALUE_OR_TRAILING:
                    if (Character.isWhitespace(c)) {
                        trailingWhitespaceBuffer.append(c);
                    } else {
                        // multi-word or continuation line value
                        value.append(trailingWhitespaceBuffer);
                        trailingWhitespaceBuffer.setLength(0);
                        value.append(c);
                        state = State.VALUE;
                        break;
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
