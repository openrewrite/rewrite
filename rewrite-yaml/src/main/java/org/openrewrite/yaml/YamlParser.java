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
package org.openrewrite.yaml;

import lombok.*;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.tree.ParseError;
import org.openrewrite.tree.ParsingEventListener;
import org.openrewrite.tree.ParsingExecutionContextView;
import org.openrewrite.yaml.tree.Yaml;
import org.openrewrite.yaml.tree.YamlKey;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.events.*;
import org.yaml.snakeyaml.parser.Parser;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.scanner.Scanner;
import org.yaml.snakeyaml.scanner.ScannerImpl;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.internal.StringUtils.indexOfNextNonWhitespaceHashComments;

public class YamlParser implements org.openrewrite.Parser {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(":\\s*(@[^\n\r@]+@)");

    @Override
    public Stream<SourceFile> parse(@Language("yml") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return acceptedInputs(sourceFiles)
                .map(sourceFile -> {
                    Path path = sourceFile.getRelativePath(relativeTo);
                    try (EncodingDetectingInputStream is = sourceFile.getSource(ctx)) {
                        Yaml.Documents yaml = parseFromInput(path, is);
                        parsingListener.parsed(sourceFile, yaml);
                        return yaml.withFileAttributes(sourceFile.getFileAttributes());
                    } catch (Throwable t) {
                        ctx.getOnError().accept(t);
                        return ParseError.build(this, sourceFile, relativeTo, ctx, t);
                    }
                })
                .map(sourceFile -> {
                    if (sourceFile instanceof Yaml.Documents) {
                        Yaml.Documents docs = (Yaml.Documents) sourceFile;
                        // ensure there is always at least one Document, even in an empty yaml file
                        if (docs.getDocuments().isEmpty()) {
                            return docs.withDocuments(singletonList(new Yaml.Document(randomId(), "", Markers.EMPTY,
                                    false, new Yaml.Mapping(randomId(), Markers.EMPTY, null, emptyList(), null, null), null)));
                        }
                        return docs;
                    }
                    return sourceFile;
                });
    }

    private int cursor = 0;
    private String source = "";
    private String whitespace() {
        int lastIndex = source.length() -1;
        int start = Math.min(lastIndex, cursor);
        int i =  Math.min(lastIndex, indexOfNextNonWhitespaceHashComments(start, source));
        String whitespace;
        if(i == -1) {
            whitespace = source.substring(cursor);
            cursor = lastIndex;
        } else {
            whitespace = source.substring(start, i);
            cursor += whitespace.length();
        }
        return whitespace;
    }
    private boolean isNext(char c) {
        int lastIndex = source.length() - 1;
        boolean result = source.charAt(Math.min(lastIndex, cursor)) == c;
        if(result) {
            cursor++;
        }
        return result;
    }
    private Yaml.Documents parseFromInput(Path sourceFile, EncodingDetectingInputStream is) {
        cursor = 0;
        source = is.readFully();
        Map<String, String> variableByUuid = new HashMap<>();

        StringBuilder yamlSourceWithVariablePlaceholders = new StringBuilder();
        Matcher variableMatcher = VARIABLE_PATTERN.matcher(source);
        int pos = 0;
        while (pos < source.length() && variableMatcher.find(pos)) {
            yamlSourceWithVariablePlaceholders.append(source, pos, variableMatcher.start(1));
            String uuid = UUID.randomUUID().toString();
            variableByUuid.put(uuid, variableMatcher.group(1));
            yamlSourceWithVariablePlaceholders.append(uuid);
            pos = variableMatcher.end(1);
        }

        if (pos < source.length() - 1) {
            yamlSourceWithVariablePlaceholders.append(source, pos, source.length());
        }

        try (StringReader stringReader = new StringReader(yamlSourceWithVariablePlaceholders.toString())) {
            StreamReader streamReader = new StreamReader(stringReader);
            Scanner scanner = new ScannerImpl(streamReader, new LoaderOptions());
            Parser parser = new ParserImpl(scanner);

            List<Yaml.Document> documents = new ArrayList<>();
            // https://yaml.org/spec/1.2.2/#3222-anchors-and-aliases, section: 3.2.2.2. Anchors and Aliases.
            // An anchor key should always replace the previous value, since an alias refers to the most recent anchor key.
            Map<String, Yaml.Anchor> anchors = new HashMap<>();
            Yaml.Document document = null;
            Stack<BlockBuilder> blockStack = new Stack<>();

            for (Event event = parser.getEvent(); event != null; event = parser.getEvent()) {
                System.out.println(event.getEventId().name());
                switch (event.getEventId()) {
                    case DocumentEnd: {
                        assert document != null;
                        documents.add(document.withEnd(new Yaml.Document.End(
                                randomId(),
                                whitespace(),
                                Markers.EMPTY,
                                ((DocumentEndEvent) event).getExplicit()
                        )));
                        break;
                    }
                    case DocumentStart: {
                        document = new Yaml.Document(
                                randomId(),
                                whitespace(),
                                Markers.EMPTY,
                                ((DocumentStartEvent) event).getExplicit(),
                                new Yaml.Mapping(randomId(), Markers.EMPTY, null, emptyList(), null, null),
                                null
                        );
                        break;
                    }
                    case MappingStart: {
                        String fmt = whitespace();
                        boolean dash = isNext('-'); // mapping may be within a sequence
                        MappingStartEvent mappingStartEvent = (MappingStartEvent) event;
                        Yaml.Anchor anchor = null;
                        if (mappingStartEvent.getAnchor() != null) {
                            //TODO
                            throw new RuntimeException("not implemented");
//                            String fmt = whitespace();
//                            anchor = buildYamlAnchor(source, cursor, fmt, mappingStartEvent.getAnchor(), event.getEndMark().getIndex(), false);
//                            anchors.put(mappingStartEvent.getAnchor(), anchor);
//
//                            cursor += mappingStartEvent.getAnchor().length();
//                            fmt = source.substring(cursor, event.getEndMark().getIndex());
//                            int dashPrefixIndex = commentAwareIndexOf('-', fmt);
//                            if (dashPrefixIndex > -1) {
//                                fmt = fmt.substring(0, dashPrefixIndex);
//                            }
                        }

                        String startBracePrefix = null;
                        if(isNext('{')) {
                            startBracePrefix = fmt;
                            fmt = "";
                        }

                        blockStack.push(new MappingBuilder(dash, fmt, startBracePrefix, anchor));
                        break;
                    }
                    case Scalar: {
                        String fmt = whitespace();
                        String dashPrefix = null;
                        if(isNext('-')) {
                            dashPrefix = fmt;
                            fmt = whitespace();
                        }

                        ScalarEvent scalar = (ScalarEvent) event;
                        String scalarValue = scalar.getValue();
                        if (variableByUuid.containsKey(scalarValue)) {
                            scalarValue = variableByUuid.get(scalarValue);
                        }

                        Yaml.Anchor anchor = null;
                        if (scalar.getAnchor() != null) {
                            int saveCursor = cursor;
                            cursor += scalar.getAnchor().length() + 1;
                            anchor = new Yaml.Anchor(randomId(), "", whitespace(), Markers.EMPTY, scalar.getAnchor());
                            anchors.put(scalar.getAnchor(), anchor);
                            cursor = saveCursor;
                        }
                        // Use event length rather than scalarValue.length() to account for escape characters and quotes
                        cursor += event.getEndMark().getIndex() - event.getStartMark().getIndex();

                        Yaml.Scalar.Style style;
                        switch (scalar.getScalarStyle()) {
                            case DOUBLE_QUOTED:
                                style = Yaml.Scalar.Style.DOUBLE_QUOTED;
                                break;
                            case SINGLE_QUOTED:
                                style = Yaml.Scalar.Style.SINGLE_QUOTED;
                                break;
                            case LITERAL:
                                //TODO
                                style = Yaml.Scalar.Style.LITERAL;
                                scalarValue = source.substring(event.getStartMark().getIndex() + 1, event.getEndMark().getIndex() - 1);
                                if (scalarValue.endsWith("\n")) {
//                                    newLine = "\n";
                                    scalarValue = scalarValue.substring(0, scalarValue.length() - 1);
                                }
                                break;
                            case FOLDED:
                                style = Yaml.Scalar.Style.FOLDED;
                                scalarValue = source.substring(event.getStartMark().getIndex() + 1, event.getEndMark().getIndex() - 1);
                                break;
                            case PLAIN:
                            default:
                                style = Yaml.Scalar.Style.PLAIN;
                                break;
                        }

                        BlockBuilder builder = blockStack.isEmpty() ? null : blockStack.peek();
                        if (builder != null) {
                            int saveCursor = cursor;
                            String commaPrefix = whitespace();
                            if(!isNext(',')) {
                                cursor = saveCursor;
                                commaPrefix = null;
                            }
                            builder.push(dashPrefix, new Yaml.Scalar(randomId(), fmt, Markers.EMPTY, style, anchor, scalarValue), commaPrefix);
                        }
                        break;
                    }
                    case SequenceEnd:
                    case MappingEnd: {
                        Yaml.Block mappingOrSequence = blockStack.pop().build();
                        if (mappingOrSequence instanceof Yaml.Sequence) {
                            Yaml.Sequence seq = (Yaml.Sequence) mappingOrSequence;
                            if (seq.getOpeningBracketPrefix() != null) {
                                mappingOrSequence = seq.withClosingBracketPrefix(whitespace());
                                cursor++;
                            }
                        }
                        if (mappingOrSequence instanceof Yaml.Mapping) {
                            Yaml.Mapping map = (Yaml.Mapping) mappingOrSequence;
                            if (map.getOpeningBracePrefix() != null) {
                                mappingOrSequence = map.withClosingBracePrefix(whitespace());
                                cursor++;
                            }
                        }
                        if (blockStack.isEmpty()) {
                            assert document != null;
                            document = document.withBlock(mappingOrSequence);
                        } else {
                            blockStack.peek().push(null, mappingOrSequence, null);
                        }
                        break;
                    }
                    case SequenceStart: {
                        String fmt = whitespace();
                        boolean dash = isNext('-');
                        SequenceStartEvent sse = (SequenceStartEvent) event;
                        Yaml.Anchor anchor = null;
                        if (sse.getAnchor() != null) {
                            throw new RuntimeException("TODO");
//                            anchor = buildYamlAnchor(source, cursor, fmt, sse.getAnchor(), event.getEndMark().getIndex(), false);
//                            anchors.put(sse.getAnchor(), anchor);
//
//                            cursor = cursor + sse.getAnchor().length() + fmt.length() + 1;
//                            fmt = source.substring(cursor, event.getEndMark().getIndex());
//                            int dashPrefixIndex = commentAwareIndexOf('-', fmt);
//                            if (dashPrefixIndex > -1) {
//                                fmt = fmt.substring(0, dashPrefixIndex);
//                            }
                        }

                        String startBracketPrefix = null;
                        if(isNext('[')) {
                            startBracketPrefix = fmt;
                            fmt = "";
                        }

                        blockStack.push(new SequenceBuilder(dash, fmt, startBracketPrefix, anchor));
                        break;
                    }
                    case Alias: {
                        String fmt = whitespace();

                        AliasEvent alias = (AliasEvent) event;
                        Yaml.Anchor anchor = anchors.get(alias.getAnchor());
                        if (anchor == null) {
                            throw new UnsupportedOperationException("Unknown anchor: " + alias.getAnchor());
                        }
                        BlockBuilder builder = blockStack.peek();
                        cursor += alias.getAnchor().length() + 1;
                        builder.push(null, new Yaml.Alias(randomId(), fmt, Markers.EMPTY, anchor), null);
                        break;
                    }
                    case StreamEnd: {
                        String fmt = whitespace();
                        if (document == null && !fmt.isEmpty()) {
                            documents.add(
                                    new Yaml.Document(
                                            randomId(), fmt, Markers.EMPTY, false,
                                            new Yaml.Mapping(randomId(), Markers.EMPTY, null, emptyList(), null, null),
                                            new Yaml.Document.End(randomId(), "", Markers.EMPTY, false)
                                    ));
                        }
                        break;
                    }
                    case StreamStart:
                        break;
                }
            }

            return new Yaml.Documents(randomId(), Markers.EMPTY, sourceFile, FileAttributes.fromPath(sourceFile),
                    is.getCharset().name(), is.isCharsetBomMarked(), null, documents);
        }
    }

    @Override
    public boolean accept(Path path) {
        String fileName = path.toString();
        return fileName.endsWith(".yml") || fileName.endsWith(".yaml");
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("file.yaml");
    }

    private interface BlockBuilder {
        Yaml.Block build();
        void push(@Nullable String dashPrefix, Yaml.Block block, @Nullable String commaPrefix);
    }

    private class MappingBuilder implements BlockBuilder {
        boolean dash;
        private final String prefix;

        @Nullable
        private final String startBracePrefix;

        @Nullable
        private final Yaml.Anchor anchor;

        private final List<Yaml.Mapping.Entry> entries = new ArrayList<>();

        @Nullable
        private YamlKey key;

        private String keyValueSeparatorPrefix = "";

        private MappingBuilder(boolean dash, String prefix, @Nullable String startBracePrefix, @Nullable Yaml.Anchor anchor) {
            this.dash = dash;
            this.prefix = prefix;
            this.startBracePrefix = startBracePrefix;
            this.anchor = anchor;
        }

        @Override
        public void push(@Nullable String dashPrefix, Yaml.Block block, @Nullable String commaPrefix) {
            if (key == null && (block instanceof Yaml.Scalar || block instanceof Yaml.Alias)) {
                key = (YamlKey) block;
                keyValueSeparatorPrefix = whitespace();
                cursor += 1; // pass over ':'
            } else {
                if(key == null) {
                    throw new IllegalStateException("Expected key to be set");
                }
                // Follow the convention of placing whitespace on the outermost possible element
                String entryPrefix;
                if(entries.isEmpty()) {
                    // By convention Yaml.Mapping have no prefix of their own, donating it to their first entry
                    entryPrefix = prefix;
                    if(dash) {
                        // If this mapping is part of a sequence, put the dash _back_ into its prefix for now.
                        // SequenceBuilder will remove it later and put it in the appropriate place.
                        entryPrefix += '-';
                    }
                    entryPrefix += key.getPrefix();
                } else {
                    entryPrefix = key.getPrefix();
                }
                key = key.withPrefix("");
                Yaml.Mapping.Entry entry = new Yaml.Mapping.Entry(randomId(), entryPrefix, Markers.EMPTY, key, keyValueSeparatorPrefix, block);
                entries.add(entry);
                key = null;
            }
        }

        @Override
        public Yaml.Mapping build() {
            return new Yaml.Mapping(randomId(), Markers.EMPTY, startBracePrefix, entries, null, anchor);
        }
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    private class SequenceBuilder implements BlockBuilder {

        private final boolean dash;

        private final String prefix;
        @Nullable
        private final String startBracketPrefix;

        @Nullable
        private final Yaml.Anchor anchor;

        private final List<Yaml.Sequence.Entry> entries = new ArrayList<>();

        private SequenceBuilder(@Nullable boolean dash, String prefix, @Nullable String startBracketPrefix, @Nullable Yaml.Anchor anchor) {
            this.dash = dash;
            this.prefix = prefix;
            this.startBracketPrefix = startBracketPrefix;
            this.anchor = anchor;
        }

        @Override
        public void push(@Nullable String dashPrefix, Yaml.Block block, @Nullable String commaPrefix) {
            String entryPrefix = "";
            boolean thisDash = dash;
            if(dashPrefix == null) {
                // If the dash has ended up inside the entryPrefix of the element, bring it back out
                if(block instanceof Yaml.Mapping) {
                    Yaml.Mapping mapping = (Yaml.Mapping) block;
                    Yaml.Mapping.Entry entry = mapping.getEntries().get(0);
                    String mappingEntryPrefix = entry.getPrefix();
                    int maybeDashIndex = StringUtils.indexOfNextNonWhitespaceHashComments(0, mappingEntryPrefix);
                    if(maybeDashIndex >= 0 && mappingEntryPrefix.charAt(maybeDashIndex) == '-') {
                        thisDash = true;
                        entryPrefix = mappingEntryPrefix.substring(0, maybeDashIndex);
                        String afterDash = mappingEntryPrefix.substring(maybeDashIndex + 1);
                        block = mapping.withEntries(ListUtils.concat(entry.withPrefix(afterDash), mapping.getEntries().subList(1, mapping.getEntries().size())));
                    }
                }
            } else {
                entryPrefix = dashPrefix;
            }
            if(entries.isEmpty()) {
                entryPrefix = prefix + entryPrefix;
            }
            entries.add(new Yaml.Sequence.Entry(randomId(), entryPrefix, Markers.EMPTY, block, thisDash, commaPrefix));
        }

        @Override
        public Yaml.Sequence build() {
            return new Yaml.Sequence(randomId(), Markers.EMPTY, startBracketPrefix, entries, null, anchor);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends org.openrewrite.Parser.Builder {

        public Builder() {
            super(Yaml.Documents.class);
        }

        @Override
        public YamlParser build() {
            return new YamlParser();
        }

        @Override
        public String getDslName() {
            return "yaml";
        }
    }
}
