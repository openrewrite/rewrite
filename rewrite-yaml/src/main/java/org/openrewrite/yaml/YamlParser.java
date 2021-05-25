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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.MetricsHelper;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.tree.Yaml;
import org.yaml.snakeyaml.events.DocumentEndEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.parser.Parser;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.scanner.Scanner;
import org.yaml.snakeyaml.scanner.ScannerImpl;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

public class YamlParser implements org.openrewrite.Parser<Yaml.Documents> {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(":\\s*(@[^\n\r@]+@)");

    @Override
    public List<Yaml.Documents> parse(@Language("yml") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public List<Yaml.Documents> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        return acceptedInputs(sourceFiles).stream()
                .map(sourceFile -> {
                    Timer.Builder timer = Timer.builder("rewrite.parse")
                            .description("The time spent parsing a YAML file")
                            .tag("file.type", "YAML");
                    Timer.Sample sample = Timer.start();
                    try (InputStream is = sourceFile.getSource()) {
                        Yaml.Documents yaml = parseFromInput(sourceFile.getRelativePath(relativeTo), is);
                        sample.stop(MetricsHelper.successTags(timer).register(Metrics.globalRegistry));
                        return yaml;
                    } catch (Throwable t) {
                        sample.stop(MetricsHelper.errorTags(timer, t).register(Metrics.globalRegistry));
                        ctx.getOnError().accept(t);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(this::unwrapPrefixedMappings)
                .map(docs -> {
                    // ensure there is always at least one Document, even in an empty yaml file
                    if (docs.getDocuments().isEmpty()) {
                        return docs.withDocuments(singletonList(new Yaml.Document(randomId(), "", Markers.EMPTY,
                                false, new Yaml.Mapping(randomId(), Markers.EMPTY, emptyList()), null)));
                    }
                    return docs;
                })
                .collect(toList());
    }

    private Yaml.Documents parseFromInput(Path sourceFile, InputStream source) {
        String yamlSource = StringUtils.readFully(source);
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

        if(pos < yamlSource.length() - 1) {
            yamlSourceWithVariablePlaceholders.append(yamlSource, pos, yamlSource.length());
        }

        try (FormatPreservingReader reader = new FormatPreservingReader(
                new InputStreamReader(new ByteArrayInputStream(yamlSourceWithVariablePlaceholders.toString()
                        .getBytes(StandardCharsets.UTF_8))))) {
            StreamReader streamReader = new StreamReader(reader);
            Scanner scanner = new ScannerImpl(streamReader);
            Parser parser = new ParserImpl(scanner);

            int lastEnd = 0;

            List<Yaml.Document> documents = new ArrayList<>();
            Yaml.Document document = null;
            Stack<BlockBuilder> blockStack = new Stack<>();

            for (Event event = parser.getEvent(); event != null; event = parser.getEvent()) {
                String fmt = reader.prefix(lastEnd, event);

                switch (event.getEventId()) {
                    case DocumentEnd:
                        assert document != null;
                        documents.add(document.withEnd(new Yaml.Document.End(
                                randomId(),
                                fmt,
                                Markers.EMPTY,
                                ((DocumentEndEvent) event).getExplicit()
                        )));
                        lastEnd = event.getEndMark().getIndex();
                        break;
                    case DocumentStart:
                        document = new Yaml.Document(
                                randomId(),
                                fmt,
                                Markers.EMPTY,
                                event.getEndMark().getIndex() - event.getStartMark().getIndex() > 0,
                                new Yaml.Mapping(randomId(), Markers.EMPTY, emptyList()),
                                null
                        );
                        lastEnd = event.getEndMark().getIndex();
                        break;
                    case MappingStart:
                        blockStack.push(new MappingBuilder(fmt));
                        break;
                    case Scalar:
                        ScalarEvent scalar = (ScalarEvent) event;
                        String scalarValue = scalar.getValue();
                        if(variableByUuid.containsKey(scalarValue)) {
                            scalarValue = variableByUuid.get(scalarValue);
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

                        blockStack.peek().push(new Yaml.Scalar(randomId(), fmt, Markers.EMPTY, style, scalarValue));
                        lastEnd = event.getEndMark().getIndex();
                        break;
                    case SequenceEnd:
                    case MappingEnd:
                        Yaml.Block mappingOrSequence = blockStack.pop().build();
                        if(mappingOrSequence instanceof Yaml.Sequence) {
                            Yaml.Sequence seq = (Yaml.Sequence) mappingOrSequence;
                            if(seq.getOpeningBracketPrefix() != null) {
                                String s = reader.readStringFromBuffer(lastEnd, event.getStartMark().getIndex());
                                int closingBracketIndex = commentAwareIndexOf(']', s);
                                lastEnd = lastEnd + closingBracketIndex + 1;
                                mappingOrSequence = seq.withClosingBracketPrefix(s.substring(0, closingBracketIndex));
                            }
                        }
                        if (blockStack.isEmpty()) {
                            assert document != null;
                            document = document.withBlock(mappingOrSequence);
                        } else {
                            blockStack.peek().push(mappingOrSequence);
                        }
                        break;
                    case SequenceStart:
                        String fullPrefix = reader.readStringFromBuffer(lastEnd, event.getEndMark().getIndex() - 1);
                        String startBracketPrefix = null;
                        int openingBracketIndex = commentAwareIndexOf('[', fullPrefix);
                        if(openingBracketIndex != -1) {
                            int startIndex = commentAwareIndexOf(':', fullPrefix) + 1;
                            startBracketPrefix = fullPrefix.substring(startIndex, openingBracketIndex);
                            lastEnd = event.getEndMark().getIndex();
                        }
                        blockStack.push(new SequenceBuilder(fmt, startBracketPrefix));
                        break;
                    case Alias:
                    case StreamEnd:
                    case StreamStart:
                        break;
                }
            }

            return new Yaml.Documents(randomId(), Markers.EMPTY, sourceFile, documents);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Return the index of the target character if it appears in a non-comment portion of the String, or -1 if it does not appear.
     */
    private static int commentAwareIndexOf(char target, String s) {
        boolean inComment = false;
        for(int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inComment) {
                if (c == '\n') {
                    inComment = false;
                }
            } else {
                if(c == target) {
                    return i;
                } else if(c == '#') {
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

    private interface BlockBuilder {
        Yaml.Block build();

        void push(Yaml.Block block);
    }

    private static class MappingBuilder implements BlockBuilder {
        private final String prefix;

        private final List<Yaml.Mapping.Entry> entries = new ArrayList<>();

        @Nullable
        private Yaml.Scalar key;

        private MappingBuilder(String prefix) {
            this.prefix = prefix;
        }

        public void push(Yaml.Block block) {
            if (key == null && block instanceof Yaml.Scalar) {
                key = (Yaml.Scalar) block;
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

        public MappingWithPrefix build() {
            return new MappingWithPrefix(prefix, entries);
        }
    }

    private static class SequenceBuilder implements BlockBuilder {
        private final String prefix;
        @Nullable private final String startBracketPrefix;

        private final List<Yaml.Sequence.Entry> entries = new ArrayList<>();

        private SequenceBuilder(String prefix, @Nullable String startBracketPrefix) {
            this.prefix = prefix;
            this.startBracketPrefix = startBracketPrefix;
        }

        @Override
        public void push(Yaml.Block block) {
            String rawPrefix = block.getPrefix();
            int dashIndex = commentAwareIndexOf('-', rawPrefix);
            String entryPrefix;
            String blockPrefix;
            boolean hasDash = dashIndex != -1;
            if(hasDash) {
                entryPrefix = rawPrefix.substring(0, dashIndex);
                blockPrefix = rawPrefix.substring(dashIndex + 1);
            } else {
                entryPrefix = "";
                blockPrefix = rawPrefix;
            }
            entries.add(new Yaml.Sequence.Entry(randomId(), entryPrefix, Markers.EMPTY, block.withPrefix(blockPrefix), hasDash));
        }

        public SequenceWithPrefix build() {
            return new SequenceWithPrefix(prefix, startBracketPrefix, entries, null);
        }
    }

    @Getter
    private static class MappingWithPrefix extends Yaml.Mapping {
        private String prefix;

        public MappingWithPrefix(String prefix, List<Yaml.Mapping.Entry> entries) {
            super(randomId(), Markers.EMPTY, entries);
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

        public SequenceWithPrefix(String prefix, @Nullable String startBracketPrefix, List<Yaml.Sequence.Entry> entries, @Nullable String endBracketPrefix) {
            super(randomId(), Markers.EMPTY, startBracketPrefix, entries, endBracketPrefix);
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
                                    sequenceWithPrefix.getClosingBracketPrefix()
                            ), p);
                }
                return super.visitSequence(sequence, p);
            }

            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, Integer p) {
                if (mapping instanceof MappingWithPrefix) {
                    MappingWithPrefix mappingWithPrefix = (MappingWithPrefix) mapping;
                    return super.visitMapping(new Yaml.Mapping(mappingWithPrefix.getId(),
                            mappingWithPrefix.getMarkers(), mappingWithPrefix.getEntries()), p);
                }
                return super.visitMapping(mapping, p);
            }
        }.visit(y, 0);
    }
}
