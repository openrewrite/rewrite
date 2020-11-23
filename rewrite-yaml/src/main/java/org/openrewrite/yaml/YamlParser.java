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

import org.openrewrite.Formatting;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.tree.Yaml;
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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Formatting.format;
import static org.openrewrite.Tree.randomId;

public class YamlParser implements org.openrewrite.Parser<Yaml.Documents> {

    @Override
    public List<Yaml.Documents> parseInputs(Iterable<Input> sourceFiles, @Nullable URI relativeTo) {
        return acceptedInputs(sourceFiles).stream()
                .map(sourceFile -> {
                    try (InputStream is = sourceFile.getSource()) {
                        return parseFromInput(sourceFile.getRelativePath(relativeTo), is);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }).collect(toList());
    }

    private Yaml.Documents parseFromInput(URI sourceFile, InputStream source) {
        try (FormatPreservingReader reader = new FormatPreservingReader(new InputStreamReader(source))) {
            StreamReader streamReader = new StreamReader(reader);
            Scanner scanner = new ScannerImpl(streamReader);
            Parser parser = new ParserImpl(scanner);

            int lastEnd = 0;

            List<Yaml.Document> documents = new ArrayList<>();
            Yaml.Document document = null;
            Stack<BlockBuilder> blockStack = new Stack<>();

            for (Event event = parser.getEvent(); event != null; event = parser.getEvent()) {
                Formatting fmt = reader.prefix(lastEnd, event);

                switch (event.getEventId()) {
                    case Alias:
                        break;
                    case DocumentEnd:
                        documents.add(document);
                        break;
                    case DocumentStart:
                        document = new Yaml.Document(
                                randomId(),
                                event.getEndMark().getIndex() - event.getStartMark().getIndex() > 0,
                                emptyList(),
                                null,
                                fmt
                        );
                        lastEnd = event.getEndMark().getIndex();
                        break;
                    case MappingStart:
                        blockStack.push(new MappingBuilder(fmt));
                        break;
                    case Scalar:
                        ScalarEvent scalar = (ScalarEvent) event;
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
                                break;
                            case FOLDED:
                                style = Yaml.Scalar.Style.FOLDED;
                                break;
                            case PLAIN:
                            default:
                                style = Yaml.Scalar.Style.PLAIN;
                                break;
                        }

                        blockStack.peek().push(new Yaml.Scalar(randomId(), style, scalar.getValue(), fmt));
                        lastEnd = event.getEndMark().getIndex();
                        break;
                    case SequenceEnd:
                    case MappingEnd:
                        Yaml.Block mappingOrSequence = blockStack.pop().build();
                        if (blockStack.isEmpty()) {
                            //noinspection ConstantConditions
                            document = document.withBlocks(Stream.concat(
                                    document.getBlocks().stream(),
                                    Stream.of(mappingOrSequence)
                            ).collect(toList()));
                        } else {
                            blockStack.peek().push(mappingOrSequence);
                        }
                        break;
                    case SequenceStart:
                        blockStack.push(new SequenceBuilder(fmt));
                        break;
                    case StreamEnd:
                    case StreamStart:
                        break;
                }
            }

            return new Yaml.Documents(randomId(), sourceFile, emptyList(),
                    documents, Formatting.EMPTY);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean accept(URI path) {
        String fileName = path.toString();
        return fileName.endsWith(".yml") || fileName.endsWith(".yaml");
    }

    private interface BlockBuilder {
        Yaml.Block build();

        void push(Yaml.Block block);
    }

    private static class MappingBuilder implements BlockBuilder {
        private final Formatting formatting;

        private final List<Yaml.Mapping.Entry> entries = new ArrayList<>();

        @Nullable
        private Yaml.Scalar key;

        private MappingBuilder(Formatting formatting) {
            this.formatting = formatting;
        }

        public void push(Yaml.Block block) {
            if (key == null && block instanceof Yaml.Scalar) {
                key = (Yaml.Scalar) block;
            } else {
                String keySuffix = block.getPrefix();
                block = block.withPrefix(keySuffix.substring(keySuffix.lastIndexOf(':') + 1));

                String keyPrefix = key.getPrefix();
                key = key.withFormatting(format(
                        "",
                        keySuffix.substring(0, keySuffix.lastIndexOf(':'))
                ));
                entries.add(new Yaml.Mapping.Entry(randomId(), key, block, format(keyPrefix.substring(keyPrefix.lastIndexOf(':') + 1))));
                key = null;
            }
        }

        public Yaml.Mapping build() {
            return new Yaml.Mapping(randomId(), entries, formatting);
        }
    }

    private static class SequenceBuilder implements BlockBuilder {
        private final Formatting formatting;

        private final List<Yaml.Sequence.Entry> entries = new ArrayList<>();

        private SequenceBuilder(Formatting formatting) {
            this.formatting = formatting;
        }

        @Override
        public void push(Yaml.Block block) {
            String entryPrefix = block.getPrefix();
            block = block.withPrefix(entryPrefix.substring(entryPrefix.lastIndexOf('-') + 1));
            entryPrefix = entryPrefix.substring(0, entryPrefix.lastIndexOf('-'));
            entries.add(new Yaml.Sequence.Entry(randomId(), block, format(entryPrefix)));
        }

        public Yaml.Sequence build() {
            return new Yaml.Sequence(randomId(), entries, formatting);
        }
    }
}
