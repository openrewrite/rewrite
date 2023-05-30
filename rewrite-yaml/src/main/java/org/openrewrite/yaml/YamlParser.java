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

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FileAttributes;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.MetricsHelper;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

public class YamlParser implements org.openrewrite.Parser<Yaml.Documents> {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(":\\s*(@[^\n\r@]+@)");

    @Override
    public Stream<Yaml.Documents> parse(@Language("yml") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public Stream<Yaml.Documents> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return acceptedInputs(sourceFiles).stream()
                .map(sourceFile -> {
                    Timer.Builder timer = Timer.builder("rewrite.parse")
                            .description("The time spent parsing a YAML file")
                            .tag("file.type", "YAML");
                    Timer.Sample sample = Timer.start();
                    Path path = sourceFile.getRelativePath(relativeTo);
                    try (EncodingDetectingInputStream is = sourceFile.getSource(ctx)) {
                        Yaml.Documents yaml = parseFromInput(path, is);
                        sample.stop(MetricsHelper.successTags(timer).register(Metrics.globalRegistry));
                        parsingListener.parsed(sourceFile, yaml);
                        return yaml.withFileAttributes(sourceFile.getFileAttributes());
                    } catch (Throwable t) {
                        sample.stop(MetricsHelper.errorTags(timer, t).register(Metrics.globalRegistry));
                        ParsingExecutionContextView.view(ctx).parseFailure(sourceFile, relativeTo, this, t);
                        ctx.getOnError().accept(new IllegalStateException(path + " " + t.getMessage(), t));
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(this::unwrapPrefixedMappings)
                .map(docs -> {
                    // ensure there is always at least one Document, even in an empty yaml file
                    if (docs.getDocuments().isEmpty()) {
                        return docs.withDocuments(singletonList(new Yaml.Document(randomId(), "", Markers.EMPTY,
                                false, new Yaml.Mapping(randomId(), Markers.EMPTY, null, emptyList(), null, null), null)));
                    }
                    return docs;
                });
    }

    private Yaml.Documents parseFromInput(Path sourceFile, EncodingDetectingInputStream source) {
        String yamlSource = source.readFully();
        Map<String, String> variableByUuid = new HashMap<>();

        StringBuilder yamlSourceWithVariablePlaceholders = new StringBuilder();
        Matcher variableMatcher = VARIABLE_PATTERN.matcher(yamlSource);
        int pos = 0;
        while (pos < yamlSource.length() && variableMatcher.find(pos)) {
            yamlSourceWithVariablePlaceholders.append(yamlSource, pos, variableMatcher.start(1));
            String uuid = UUID.randomUUID().toString();
            variableByUuid.put(uuid, variableMatcher.group(1));
            yamlSourceWithVariablePlaceholders.append(uuid);
            pos = variableMatcher.end(1);
        }

        if (pos < yamlSource.length() - 1) {
            yamlSourceWithVariablePlaceholders.append(yamlSource, pos, yamlSource.length());
        }

        try (FormatPreservingReader reader = new FormatPreservingReader(
                new InputStreamReader(new ByteArrayInputStream(yamlSourceWithVariablePlaceholders.toString()
                        .getBytes(StandardCharsets.UTF_8))))) {
            StreamReader streamReader = new StreamReader(reader);
            Scanner scanner = new ScannerImpl(streamReader, new LoaderOptions());
            Parser parser = new ParserImpl(scanner);

            int lastEnd = 0;

            List<Yaml.Document> documents = new ArrayList<>();
            // https://yaml.org/spec/1.2.2/#3222-anchors-and-aliases, section: 3.2.2.2. Anchors and Aliases.
            // An anchor key should always replace the previous value, since an alias refers to the most recent anchor key.
            Map<String, Yaml.Anchor> anchors = new HashMap<>();
            Yaml.Document document = null;
            Stack<BlockBuilder> blockStack = new Stack<>();
            String newLine = "";

            for (Event event = parser.getEvent(); event != null; event = parser.getEvent()) {
                switch (event.getEventId()) {
                    case DocumentEnd: {
                        String fmt = newLine + reader.prefix(lastEnd, event);
                        newLine = "";

                        assert document != null;
                        documents.add(document.withEnd(new Yaml.Document.End(
                                randomId(),
                                fmt,
                                Markers.EMPTY,
                                ((DocumentEndEvent) event).getExplicit()
                        )));
                        lastEnd = event.getEndMark().getIndex();
                        break;
                    }
                    case DocumentStart: {
                        String fmt = newLine + reader.prefix(lastEnd, event);
                        newLine = "";

                        document = new Yaml.Document(
                                randomId(),
                                fmt,
                                Markers.EMPTY,
                                ((DocumentStartEvent) event).getExplicit(),
                                new Yaml.Mapping(randomId(), Markers.EMPTY, null, emptyList(), null, null),
                                null
                        );
                        lastEnd = event.getEndMark().getIndex();
                        break;
                    }
                    case MappingStart: {
                        String fmt = newLine + reader.prefix(lastEnd, event);
                        newLine = "";

                        MappingStartEvent mappingStartEvent = (MappingStartEvent) event;
                        Yaml.Anchor anchor = null;
                        if (mappingStartEvent.getAnchor() != null) {
                            anchor = buildYamlAnchor(reader, lastEnd, fmt, mappingStartEvent.getAnchor(), event.getEndMark().getIndex(), false);
                            anchors.put(mappingStartEvent.getAnchor(), anchor);

                            lastEnd = lastEnd + mappingStartEvent.getAnchor().length() + fmt.length() + 1;
                            fmt = reader.readStringFromBuffer(lastEnd, event.getEndMark().getIndex());
                            int dashPrefixIndex = commentAwareIndexOf('-', fmt);
                            if (dashPrefixIndex > -1) {
                                fmt = fmt.substring(0, dashPrefixIndex);
                            }
                        }

                        String fullPrefix = reader.readStringFromBuffer(lastEnd, event.getEndMark().getIndex() - 1);
                        String startBracePrefix = null;
                        int openingBraceIndex = commentAwareIndexOf('{', fullPrefix);
                        if (openingBraceIndex != -1) {
                            int startIndex = commentAwareIndexOf(':', fullPrefix) + 1;
                            startBracePrefix = fullPrefix.substring(startIndex, openingBraceIndex);
                            lastEnd = event.getEndMark().getIndex();
                        }
                        blockStack.push(new MappingBuilder(fmt, startBracePrefix, anchor));
                        break;
                    }
                    case Scalar: {
                        String fmt = newLine + reader.prefix(lastEnd, event);
                        newLine = "";

                        ScalarEvent scalar = (ScalarEvent) event;
                        String scalarValue = scalar.getValue();
                        if (variableByUuid.containsKey(scalarValue)) {
                            scalarValue = variableByUuid.get(scalarValue);
                        }

                        Yaml.Anchor anchor = null;
                        if (scalar.getAnchor() != null) {
                            anchor = buildYamlAnchor(reader, lastEnd, fmt, scalar.getAnchor(), event.getEndMark().getIndex(), true);
                            anchors.put(scalar.getAnchor(), anchor);
                        }

                        Yaml.Scalar.Style style;
                        switch (scalar.getScalarStyle()) {
                            case DOUBLE_QUOTED:
                                style = Yaml.Scalar.Style.DOUBLE_QUOTED;
                                break;
                            case SINGLE_QUOTED:
                                style = Yaml.Scalar.Style.SINGLE_QUOTED;
                                break;
                            case LITERAL:
                                style = Yaml.Scalar.Style.LITERAL;
                                scalarValue = reader.readStringFromBuffer(event.getStartMark().getIndex() + 1, event.getEndMark().getIndex() - 1);
                                if (scalarValue.endsWith("\n")) {
                                    newLine = "\n";
                                    scalarValue = scalarValue.substring(0, scalarValue.length() - 1);
                                }
                                break;
                            case FOLDED:
                                style = Yaml.Scalar.Style.FOLDED;
                                scalarValue = reader.readStringFromBuffer(event.getStartMark().getIndex() + 1, event.getEndMark().getIndex() - 1);
                                break;
                            case PLAIN:
                            default:
                                style = Yaml.Scalar.Style.PLAIN;
                                break;
                        }
                        BlockBuilder builder = blockStack.isEmpty() ? null : blockStack.peek();
                        if (builder instanceof SequenceBuilder) {
                            // Inline sequences like [1, 2] need to keep track of any whitespace between the element
                            // and its trailing comma.
                            SequenceBuilder sequenceBuilder = (SequenceBuilder) builder;
                            String betweenEvents = reader.readStringFromBuffer(event.getEndMark().getIndex(), parser.peekEvent().getStartMark().getIndex() - 1);
                            int commaIndex = commentAwareIndexOf(',', betweenEvents);
                            String commaPrefix = null;
                            if (commaIndex != -1) {
                                commaPrefix = betweenEvents.substring(0, commaIndex);

                            }
                            lastEnd = event.getEndMark().getIndex() + commaIndex + 1;
                            sequenceBuilder.push(new Yaml.Scalar(randomId(), fmt, Markers.EMPTY, style, anchor, scalarValue), commaPrefix);

                        } else if (builder != null) {
                            builder.push(new Yaml.Scalar(randomId(), fmt, Markers.EMPTY, style, anchor, scalarValue));
                            lastEnd = event.getEndMark().getIndex();
                        }
                        break;
                    }
                    case SequenceEnd:
                    case MappingEnd: {
                        Yaml.Block mappingOrSequence = blockStack.pop().build();
                        if (mappingOrSequence instanceof Yaml.Sequence) {
                            Yaml.Sequence seq = (Yaml.Sequence) mappingOrSequence;
                            if (seq.getOpeningBracketPrefix() != null) {
                                String s = reader.readStringFromBuffer(lastEnd, event.getStartMark().getIndex());
                                int closingBracketIndex = commentAwareIndexOf(']', s);
                                lastEnd = lastEnd + closingBracketIndex + 1;
                                mappingOrSequence = seq.withClosingBracketPrefix(s.substring(0, closingBracketIndex));
                            }
                        }
                        if (mappingOrSequence instanceof Yaml.Mapping) {
                            Yaml.Mapping map = (Yaml.Mapping) mappingOrSequence;
                            if (map.getOpeningBracePrefix() != null) {
                                String s = reader.readStringFromBuffer(lastEnd, event.getStartMark().getIndex());
                                int closingBraceIndex = commentAwareIndexOf('}', s);
                                lastEnd = lastEnd + closingBraceIndex + 1;
                                mappingOrSequence = map.withClosingBracePrefix(s.substring(0, closingBraceIndex));
                            }
                        }
                        if (blockStack.isEmpty()) {
                            assert document != null;
                            document = document.withBlock(mappingOrSequence);
                        } else {
                            blockStack.peek().push(mappingOrSequence);
                        }
                        break;
                    }
                    case SequenceStart: {
                        String fmt = newLine + reader.prefix(lastEnd, event);
                        newLine = "";

                        SequenceStartEvent sse = (SequenceStartEvent) event;
                        Yaml.Anchor anchor = null;
                        if (sse.getAnchor() != null) {
                            anchor = buildYamlAnchor(reader, lastEnd, fmt, sse.getAnchor(), event.getEndMark().getIndex(), false);
                            anchors.put(sse.getAnchor(), anchor);

                            lastEnd = lastEnd + sse.getAnchor().length() + fmt.length() + 1;
                            fmt = reader.readStringFromBuffer(lastEnd, event.getEndMark().getIndex());
                            int dashPrefixIndex = commentAwareIndexOf('-', fmt);
                            if (dashPrefixIndex > -1) {
                                fmt = fmt.substring(0, dashPrefixIndex);
                            }
                        }

                        String fullPrefix = reader.readStringFromBuffer(lastEnd, event.getEndMark().getIndex());
                        String startBracketPrefix = null;
                        int openingBracketIndex = commentAwareIndexOf('[', fullPrefix);
                        if (openingBracketIndex != -1) {
                            int startIndex = commentAwareIndexOf(':', fullPrefix) + 1;
                            startBracketPrefix = fullPrefix.substring(startIndex, openingBracketIndex);
                            lastEnd = event.getEndMark().getIndex();
                        }
                        blockStack.push(new SequenceBuilder(fmt, startBracketPrefix, anchor));
                        break;
                    }
                    case Alias: {
                        String fmt = newLine + reader.prefix(lastEnd, event);
                        newLine = "";

                        AliasEvent alias = (AliasEvent) event;
                        Yaml.Anchor anchor = anchors.get(alias.getAnchor());
                        BlockBuilder builder = blockStack.peek();
                        builder.push(new Yaml.Alias(randomId(), fmt, Markers.EMPTY, anchor));
                        lastEnd = event.getEndMark().getIndex();
                        break;
                    }
                    case StreamEnd: {
                        String fmt = newLine + reader.prefix(lastEnd, event);
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

            return new Yaml.Documents(randomId(), Markers.EMPTY, sourceFile, FileAttributes.fromPath(sourceFile), source.getCharset().name(), source.isCharsetBomMarked(), null, documents);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Yaml.Anchor buildYamlAnchor(FormatPreservingReader reader, int lastEnd, String eventPrefix, String anchorKey, int eventEndIndex, boolean isForScalar) {
        int anchorLength = isForScalar ? anchorKey.length() + 1 : anchorKey.length();
        String whitespaceAndScalar = reader.prefix(
                lastEnd + eventPrefix.length() + anchorLength, eventEndIndex);

        StringBuilder postFix = new StringBuilder();
        for (char c : whitespaceAndScalar.toCharArray()) {
            if (c != ' ' && c != '\t') {
                break;
            }
            postFix.append(c);
        }

        int prefixStart = commentAwareIndexOf(':', eventPrefix);
        String prefix = "";
        if (!isForScalar) {
            prefix = (prefixStart > -1 && eventPrefix.length() > prefixStart + 1) ? eventPrefix.substring(prefixStart + 1) : "";
        }
        return new Yaml.Anchor(randomId(), prefix, postFix.toString(), Markers.EMPTY, anchorKey);
    }

    /**
     * Return the index of the target character if it appears in a non-comment portion of the String, or -1 if it does not appear.
     */
    private static int commentAwareIndexOf(char target, String s) {
        boolean inComment = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inComment) {
                if (c == '\n') {
                    inComment = false;
                }
            } else {
                if (c == target) {
                    return i;
                } else if (c == '#') {
                    inComment = true;
                }
            }
        }
        return -1;
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

        void push(Yaml.Block block);
    }

    private static class MappingBuilder implements BlockBuilder {
        private final String prefix;

        @Nullable
        private final String startBracePrefix;

        @Nullable
        private final Yaml.Anchor anchor;

        private final List<Yaml.Mapping.Entry> entries = new ArrayList<>();

        @Nullable
        private YamlKey key;

        private MappingBuilder(String prefix, @Nullable String startBracePrefix, @Nullable Yaml.Anchor anchor) {
            this.prefix = prefix;
            this.startBracePrefix = startBracePrefix;
            this.anchor = anchor;
        }

        @Override
        public void push(Yaml.Block block) {
            if (key == null && block instanceof Yaml.Scalar) {
                key = (Yaml.Scalar) block;
            } else if (key == null && block instanceof Yaml.Alias) {
                key = (Yaml.Alias) block;
            } else {
                String keySuffix = block.getPrefix();
                block = block.withPrefix(keySuffix.substring(commentAwareIndexOf(':', keySuffix) + 1));

                // Begin moving whitespace from the key to the entry that contains the key
                String originalKeyPrefix = key.getPrefix();
                key = key.withPrefix("");

                // When a dash, indicating the beginning of a sequence, is present whitespace before it will be handled by the SequenceEntry
                // Similarly if the prefix includes a ':', it will be owned by the mapping that contains this mapping
                // So this entry's prefix begins after any such delimiter
                int entryPrefixStartIndex = Math.max(
                        commentAwareIndexOf('-', originalKeyPrefix),
                        commentAwareIndexOf(':', originalKeyPrefix)) + 1;
                String entryPrefix = originalKeyPrefix.substring(entryPrefixStartIndex);
                String beforeMappingValueIndicator = keySuffix.substring(0,
                        Math.max(commentAwareIndexOf(':', keySuffix), 0));
                entries.add(new Yaml.Mapping.Entry(randomId(), entryPrefix, Markers.EMPTY, key, beforeMappingValueIndicator, block));
                key = null;
            }
        }

        @Override
        public MappingWithPrefix build() {
            return new MappingWithPrefix(prefix, startBracePrefix, entries, null, anchor);
        }
    }

    private static class SequenceBuilder implements BlockBuilder {
        private final String prefix;
        @Nullable
        private final String startBracketPrefix;

        @Nullable
        private final Yaml.Anchor anchor;

        private final List<Yaml.Sequence.Entry> entries = new ArrayList<>();

        private SequenceBuilder(String prefix, @Nullable String startBracketPrefix, @Nullable Yaml.Anchor anchor) {
            this.prefix = prefix;
            this.startBracketPrefix = startBracketPrefix;
            this.anchor = anchor;
        }

        @Override
        public void push(Yaml.Block block) {
            push(block, null);
        }

        public void push(Yaml.Block block, @Nullable String commaPrefix) {
            String rawPrefix = block.getPrefix();
            int dashIndex = commentAwareIndexOf('-', rawPrefix);
            String entryPrefix;
            String blockPrefix;
            boolean hasDash = dashIndex != -1;
            if (hasDash) {
                entryPrefix = rawPrefix.substring(0, dashIndex);
                blockPrefix = rawPrefix.substring(dashIndex + 1);
            } else {
                entryPrefix = "";
                blockPrefix = rawPrefix;
            }
            entries.add(new Yaml.Sequence.Entry(randomId(), entryPrefix, Markers.EMPTY, block.withPrefix(blockPrefix), hasDash, commaPrefix));
        }

        @Override
        public SequenceWithPrefix build() {
            return new SequenceWithPrefix(prefix, startBracketPrefix, entries, null, anchor);
        }
    }

    @Getter
    private static class MappingWithPrefix extends Yaml.Mapping {
        private String prefix;

        public MappingWithPrefix(String prefix, @Nullable String startBracePrefix, List<Yaml.Mapping.Entry> entries, @Nullable String endBracePrefix, @Nullable Anchor anchor) {
            super(randomId(), Markers.EMPTY, startBracePrefix, entries, endBracePrefix, anchor);
            this.prefix = prefix;
        }

        @Override
        public Mapping withPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }
    }

    @Getter
    private static class SequenceWithPrefix extends Yaml.Sequence {
        private String prefix;

        public SequenceWithPrefix(String prefix, @Nullable String startBracketPrefix, List<Yaml.Sequence.Entry> entries, @Nullable String endBracketPrefix, @Nullable Anchor anchor) {
            super(randomId(), Markers.EMPTY, startBracketPrefix, entries, endBracketPrefix, anchor);
            this.prefix = prefix;
        }

        @Override
        public Sequence withPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }
    }

    private Yaml.Documents unwrapPrefixedMappings(Yaml.Documents y) {
        //noinspection ConstantConditions
        return (Yaml.Documents) new YamlIsoVisitor<Integer>() {
            @Override
            public Yaml.Sequence visitSequence(Yaml.Sequence sequence, Integer p) {
                if (sequence instanceof SequenceWithPrefix) {
                    SequenceWithPrefix sequenceWithPrefix = (SequenceWithPrefix) sequence;
                    return super.visitSequence(
                            new Yaml.Sequence(
                                    sequenceWithPrefix.getId(),
                                    sequenceWithPrefix.getMarkers(),
                                    sequenceWithPrefix.getOpeningBracketPrefix(),
                                    ListUtils.mapFirst(sequenceWithPrefix.getEntries(), e -> e.withPrefix(sequenceWithPrefix.getPrefix())),
                                    sequenceWithPrefix.getClosingBracketPrefix(),
                                    sequenceWithPrefix.getAnchor()
                            ), p);
                }
                return super.visitSequence(sequence, p);
            }

            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, Integer p) {
                if (mapping instanceof MappingWithPrefix) {
                    MappingWithPrefix mappingWithPrefix = (MappingWithPrefix) mapping;
                    return super.visitMapping(new Yaml.Mapping(mappingWithPrefix.getId(),
                            mappingWithPrefix.getMarkers(), mappingWithPrefix.getOpeningBracePrefix(), mappingWithPrefix.getEntries(), null, mappingWithPrefix.getAnchor()), p);
                }
                return super.visitMapping(mapping, p);
            }
        }.visit(y, 0);
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
