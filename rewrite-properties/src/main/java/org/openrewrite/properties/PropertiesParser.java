/*
 * Copyright 2020 the original author or authors.
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

import org.openrewrite.Formatting;
import org.openrewrite.marker.Markers;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.tree.Properties;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.Formatting.format;
import static org.openrewrite.Tree.randomId;

public class PropertiesParser implements Parser<Properties.File> {

    @Override
    public List<Properties.File> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo) {
        return acceptedInputs(sourceFiles).stream()
                .map(sourceFile -> {
                    try (InputStream is = sourceFile.getSource()) {
                        return parseFromInput(sourceFile.getRelativePath(relativeTo), is);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }).collect(toList());
    }

    private Properties.File parseFromInput(Path sourceFile, InputStream source) {
        List<Properties.Content> contents = new ArrayList<>();

        StringBuilder prefix = new StringBuilder();
        Scanner scanner = new Scanner(source);
        scanner.useDelimiter(Pattern.compile("\n"));

        while (scanner.hasNext()) {
            String line = scanner.next();
            Properties.Content content = null;

            if (line.trim().startsWith("#")) {
                content = commentFromLine(line);
            } else if (line.contains("=")) {
                content = entryFromLine(line);
            } else {
                prefix.append(line).append("\n");
            }

            if (content != null) {
                content = content.withFormatting(format(prefix.toString()));
                // FIXME content.getSuffix() + "\n")
                prefix = new StringBuilder();
                contents.add(content);
            }
        }

        String suffix = "";

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = source.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            String line = buffer.toString();

            if (line.trim().startsWith("#")) {
                contents.add(commentFromLine(line));
            } else if (line.contains("=")) {
                contents.add(entryFromLine(line));
            } else {
                suffix = line;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return new Properties.File(
                randomId(),
                sourceFile.toString(),
                contents,
                format(suffix),
                Formatting.EMPTY,
                Markers.EMPTY
        );
    }

    private Properties.Comment commentFromLine(String line) {
        StringBuilder prefix = new StringBuilder();
        StringBuilder message = new StringBuilder();

        int state = 0;
        for (char c : line.toCharArray()) {
            switch (state) {
                case 0:
                    if (Character.isWhitespace(c)) {
                        prefix.append(c);
                        break;
                    }
                    state++;
                case 1:
                    if (c == '#') {
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
                message.toString(),
                format(prefix.toString()),
                Markers.EMPTY
        );
    }

    private Properties.Entry entryFromLine(String line) {
        StringBuilder prefix = new StringBuilder(),
                key = new StringBuilder(),
                equalsPrefix = new StringBuilder(),
                valuePrefix = new StringBuilder(),
                value = new StringBuilder(),
                afterValue = new StringBuilder();

        int state = 0;
        for (char c : line.toCharArray()) {
            switch (state) {
                case 0:
                    if (Character.isWhitespace(c)) {
                        prefix.append(c);
                        break;
                    }
                    state++;
                case 1:
                    if (c == '=') {
                        state += 2;
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
                    }
                    state++;
                case 3:
                    if (c == '=') {
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
                        value.append(afterValue);
                        value.append(c);
                        afterValue = new StringBuilder();
                        state--;
                        break;
                    } else {
                        afterValue.append(c);
                    }
            }
        }

        return new Properties.Entry(
                randomId(),
                key.toString(),
                format(equalsPrefix.toString()),
                new Properties.Value(randomId(), value.toString(), format(valuePrefix.toString())),
                format(afterValue.toString()),
                format(prefix.toString()),
                Markers.EMPTY
        );
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().endsWith(".properties");
    }
}
