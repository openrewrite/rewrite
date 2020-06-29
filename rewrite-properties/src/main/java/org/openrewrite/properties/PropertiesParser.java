package org.openrewrite.properties;

import org.openrewrite.Formatting;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.tree.Properties;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Formatting.format;
import static org.openrewrite.Tree.randomId;

public class PropertiesParser {
    public Properties.File parse(String source) {
        return parseFromInput(Paths.get("unknown.properties"), new ByteArrayInputStream(source.getBytes()));
    }

    public List<Properties.File> parse(List<Path> sourceFiles, @Nullable Path relativeTo) {
        return sourceFiles.stream().map(source -> parse(source, relativeTo)).collect(toList());
    }

    public Properties.File parse(Path sourceFile, @Nullable Path relativeTo) {
        try (FileInputStream fis = new FileInputStream(sourceFile.toFile())) {
            return parseFromInput(relativeTo == null ? sourceFile : relativeTo.relativize(sourceFile), fis);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Properties.File parseFromInput(Path sourceFile, InputStream source) {
        List<Properties.Content> contents = new ArrayList<>();

        StringBuilder prefix = new StringBuilder();
        Scanner scanner = new Scanner(source);
        scanner.useDelimiter(Pattern.compile("\n"));

        while (scanner.hasNext()) {
            String line = scanner.next();
            Properties.Content content = null;

            if(line.trim().startsWith("#")) {
                content = commentFromLine(line);
            }
            else if(line.contains("=")) {
                content = entryFromLine(line);
            }
            else {
                prefix.append(line).append("\n");
            }

            if(content != null) {
                content = content.withFormatting(Formatting.format(prefix.toString(),
                        content.getFormatting().getSuffix() + "\n"));
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
            String line = new String(buffer.toByteArray());

            if(line.trim().startsWith("#")) {
                contents.add(commentFromLine(line));
            }
            else if(line.contains("=")) {
                contents.add(entryFromLine(line));
            }
            else {
                suffix = line;
            }
        } catch(IOException e) {
            throw new UncheckedIOException(e);
        }

        return new Properties.File(randomId(), sourceFile.toFile().getPath(),
                emptyMap(), contents, Formatting.format("", suffix));
    }

    private Properties.Comment commentFromLine(String line) {
        StringBuilder prefix = new StringBuilder(),
                message = new StringBuilder(),
                suffix = new StringBuilder();

        int state = 0;
        for(char c: line.toCharArray()) {
            switch(state) {
                case 0:
                    if (Character.isWhitespace(c)) {
                        prefix.append(c);
                        break;
                    }
                    state++;
                case 1:
                    if(c == '#') {
                        continue;
                    }
                    else if(!Character.isWhitespace(c)) {
                        message.append(c);
                        break;
                    }
                    state++;
                case 2:
                    if(!Character.isWhitespace(c)) {
                        // multi-word comment
                        message.append(suffix);
                        message.append(c);
                        suffix = new StringBuilder();
                        state--;
                        break;
                    }
                    else {
                        suffix.append(c);
                    }
            }
        }

        return new Properties.Comment(randomId(), message.toString(), format(prefix.toString(), suffix.toString()));
    }

    private Properties.Entry entryFromLine(String line) {
        StringBuilder prefix = new StringBuilder(),
                key = new StringBuilder(),
                equalsPrefix = new StringBuilder(),
                equalsSuffix = new StringBuilder(),
                value = new StringBuilder(),
                suffix = new StringBuilder();

        int state = 0;
        for(char c: line.toCharArray()) {
            switch(state) {
                case 0:
                    if (Character.isWhitespace(c)) {
                        prefix.append(c);
                        break;
                    }
                    state++;
                case 1:
                    if(c == '=') {
                        state += 2;
                    }
                    else if (!Character.isWhitespace(c)) {
                        key.append(c);
                        break;
                    }
                    else {
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
                    }
                    else if (Character.isWhitespace(c)) {
                        equalsSuffix.append(c);
                        break;
                    }
                    state++;
                case 4:
                    if(!Character.isWhitespace(c)) {
                        value.append(c);
                        break;
                    }
                    state++;
                case 5:
                    if(!Character.isWhitespace(c)) {
                        // multi-word value
                        value.append(suffix);
                        value.append(c);
                        suffix = new StringBuilder();
                        state--;
                        break;
                    }
                    else {
                        suffix.append(c);
                    }
            }
        }

        return new Properties.Entry(randomId(), key.toString(), value.toString(),
                format(equalsPrefix.toString(), equalsSuffix.toString()),
                format(prefix.toString(), suffix.toString()));
    }
}
