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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

public class YamlParser implements org.openrewrite.Parser<Yaml.Documents> {
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
        try (FormatPreservingReader reader = new FormatPreservingReader(new InputStreamReader(source))) {
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
                        if (((DocumentEndEvent) event).getExplicit()) {
                            assert document != null;
                            documents.add(document.withEnd(new Yaml.Document.End(
                                    randomId(),
                                    fmt,
                                    Markers.EMPTY
                            )));
                            lastEnd = event.getEndMark().getIndex();
                        } else {
                            documents.add(document);
                        }
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
                        if (blockStack.isEmpty()) {
                            document = document.withBlock(mappingOrSequence);
                        } else {
                            blockStack.peek().push(mappingOrSequence);
                        }
                        break;
                    case SequenceStart:
                        blockStack.push(new SequenceBuilder(fmt));
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
                block = block.withPrefix(keySuffix.substring(keySuffix.lastIndexOf(':') + 1));

                String keyPrefix = key.getPrefix();
                key = key.withPrefix("");

                String beforeMappingValueIndicator = keySuffix.substring(0,
                        Math.max(keySuffix.lastIndexOf(':'), 0));
                String entryPrefix = keyPrefix.substring(keyPrefix.lastIndexOf(':') + 1);
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

        private final List<Yaml.Sequence.Entry> entries = new ArrayList<>();

        private SequenceBuilder(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void push(Yaml.Block block) {
            String entryPrefix = block.getPrefix();
            if (entryPrefix.indexOf(':') != -1) {
                block = block.withPrefix(entryPrefix.substring(entryPrefix.lastIndexOf(':') + 1));
                entryPrefix = entryPrefix.substring(0, entryPrefix.lastIndexOf(':'));
            }
            if (entryPrefix.indexOf('-') != -1) {
                block = block.withPrefix(entryPrefix.substring(entryPrefix.lastIndexOf('-') + 1));
                entryPrefix = entryPrefix.substring(0, entryPrefix.lastIndexOf('-'));
            }
            if (block instanceof Yaml.Mapping) {
                Yaml.Mapping mapping = (Yaml.Mapping) block;
                mapping = mapping.withEntries(ListUtils.map(mapping.getEntries(),
                        e -> e.withPrefix(e.getPrefix().substring(e.getPrefix().lastIndexOf('-') + 1))));
                entries.add(new Yaml.Sequence.Entry(randomId(), entryPrefix, Markers.EMPTY, mapping));
            } else {
                entries.add(new Yaml.Sequence.Entry(randomId(), entryPrefix, Markers.EMPTY, block));
            }
        }

        public SequenceWithPrefix build() {
            return new SequenceWithPrefix(prefix, entries);
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

        public SequenceWithPrefix(String prefix, List<Yaml.Sequence.Entry> entries) {
            super(randomId(), Markers.EMPTY, entries);
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
                                    ListUtils.mapFirst(sequenceWithPrefix.getEntries(), e -> e.withPrefix(sequenceWithPrefix.getPrefix()))
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
