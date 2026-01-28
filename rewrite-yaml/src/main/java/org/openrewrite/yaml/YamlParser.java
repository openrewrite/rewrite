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

import lombok.Getter;
import lombok.Value;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
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

import org.openrewrite.yaml.marker.OmitColon;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static org.openrewrite.Tree.randomId;

public class YamlParser implements org.openrewrite.Parser {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(":\\s+(@[^\n\r@]+@)");
    // Only match single-line Helm templates that don't span multiple lines
    private static final Pattern HELM_TEMPLATE_PATTERN = Pattern.compile("\\{\\{[^{}\\n\\r]*}}");
    // Match single-brace placeholder templates like {C App} that contain at least one space
    // These are invalid YAML but used by some tools as placeholders
    private static final Pattern SINGLE_BRACE_TEMPLATE_PATTERN = Pattern.compile("\\{[A-Za-z][^{}\\n\\r]*\\s[^{}\\n\\r]*}");
    // Prefix used to convert standalone Helm template lines (control flow directives) to YAML comments
    private static final String HELM_STANDALONE_COMMENT_PREFIX = "#__helm_standalone__";

    @Override
    public Stream<SourceFile> parse(@Language("yml") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sourceFiles, @Nullable Path relativeTo, ExecutionContext ctx) {
        ParsingEventListener parsingListener = ParsingExecutionContextView.view(ctx).getParsingListener();
        return acceptedInputs(sourceFiles)
                .map(input -> {
                    parsingListener.startedParsing(input);
                    Path path = input.getRelativePath(relativeTo);
                    try (EncodingDetectingInputStream is = input.getSource(ctx)) {
                        Yaml.Documents yaml = parseFromInput(path, is);
                        parsingListener.parsed(input, yaml);
                        yaml = yaml.withFileAttributes(input.getFileAttributes());
                        yaml = unwrapPrefixedMappings(yaml);
                        return requirePrintEqualsInput(yaml, input, relativeTo, ctx);
                    } catch (Throwable t) {
                        ctx.getOnError().accept(t);
                        return ParseError.build(this, input, relativeTo, ctx, t);
                    }
                })
                .map(sourceFile -> {
                    if (sourceFile instanceof Yaml.Documents) {
                        Yaml.Documents docs = (Yaml.Documents) sourceFile;
                        // ensure there is always at least one Document, even in an empty yaml file
                        if (docs.getDocuments().isEmpty()) {
                            Yaml.Document.End end = new Yaml.Document.End(randomId(), "", Markers.EMPTY, false);
                            Yaml.Mapping mapping = new Yaml.Mapping(randomId(), Markers.EMPTY, null, emptyList(), null, null, null);
                            return docs.withDocuments(singletonList(new Yaml.Document(randomId(), "", Markers.EMPTY, emptyList(), false, mapping, end)));
                        }
                        return docs;
                    }
                    return sourceFile;
                });
    }

    private Yaml.Documents parseFromInput(Path sourceFile, EncodingDetectingInputStream source) {
        String yamlSource = source.readFully();
        Map<String, String> variableByUuid = new HashMap<>();
        Map<String, String> helmTemplateByUuid = new HashMap<>();
        Map<String, String> singleBraceTemplateByUuid = new HashMap<>();
        Map<String, String> helmStandaloneByMarker = new HashMap<>();

        // First, replace all Helm templates with UUIDs
        String processedSource = yamlSource;
        Matcher helmMatcher = HELM_TEMPLATE_PATTERN.matcher(processedSource);
        StringBuffer helmBuffer = new StringBuffer();
        while (helmMatcher.find()) {
            String uuid = UUID.randomUUID().toString();
            helmTemplateByUuid.put(uuid, helmMatcher.group());
            helmMatcher.appendReplacement(helmBuffer, uuid);
        }
        helmMatcher.appendTail(helmBuffer);
        processedSource = helmBuffer.toString();

        // Convert standalone Helm template lines (lines where a UUID is the only content)
        // to YAML comments so they don't create structurally invalid YAML
        processedSource = convertStandaloneHelmLinesToComments(
                processedSource, helmTemplateByUuid, helmStandaloneByMarker);

        // Then, replace single-brace templates like {C App} with UUIDs
        Matcher singleBraceMatcher = SINGLE_BRACE_TEMPLATE_PATTERN.matcher(processedSource);
        StringBuffer singleBraceBuffer = new StringBuffer();
        while (singleBraceMatcher.find()) {
            String uuid = UUID.randomUUID().toString();
            singleBraceTemplateByUuid.put(uuid, singleBraceMatcher.group());
            singleBraceMatcher.appendReplacement(singleBraceBuffer, uuid);
        }
        singleBraceMatcher.appendTail(singleBraceBuffer);
        processedSource = singleBraceBuffer.toString();

        // Then, replace @variable@ patterns with UUIDs
        StringBuilder yamlSourceWithPlaceholders = new StringBuilder();
        Matcher variableMatcher = VARIABLE_PATTERN.matcher(processedSource);
        int pos = 0;
        while (pos < processedSource.length() && variableMatcher.find(pos)) {
            yamlSourceWithPlaceholders.append(processedSource, pos, variableMatcher.start(1));
            String uuid = UUID.randomUUID().toString();
            variableByUuid.put(uuid, variableMatcher.group(1));
            yamlSourceWithPlaceholders.append(uuid);
            pos = variableMatcher.end(1);
        }

        if (pos < processedSource.length()) {
            yamlSourceWithPlaceholders.append(processedSource, pos, processedSource.length());
        }

        // Pre-scan the source to extract directives, since SnakeYAML doesn't preserve exact directive text
        String sourceText = yamlSourceWithPlaceholders.toString();
        Queue<List<Yaml.Directive>> documentDirectives = preScanDirectives(sourceText);

        try (FormatPreservingReader reader = new FormatPreservingReader(sourceText)) {
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
            String suffix = "";

            for (Event event = parser.getEvent(); event != null; event = parser.getEvent()) {
                switch (event.getEventId()) {
                    case DocumentEnd: {
                        assert document != null;
                        if (blockStack.size() == 1 && blockStack.peek() instanceof ScalarBuilder) {
                            // The yaml document consists of a single scalar value not in a mapping or sequence
                            ScalarBuilder builder = (ScalarBuilder) blockStack.pop();
                            lastEnd = builder.getLastEnd();
                            Yaml.Scalar scalar = builder.getScalar();
                            document = document.withBlock(scalar);
                        }
                        String fmt = newLine + reader.prefix(lastEnd, event);

                        newLine = "";
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
                        DocumentStartEvent docStartEvent = (DocumentStartEvent) event;

                        // Use pre-scanned directives if available, otherwise fall back to event-based extraction
                        List<Yaml.Directive> directives = documentDirectives.poll();
                        String documentPrefix;
                        if (directives != null && !directives.isEmpty()) {
                            // Directives were pre-scanned, document prefix is whatever remains after them
                            documentPrefix = "";
                        } else {
                            directives = new ArrayList<>();
                            documentPrefix = parseDirectives(fmt, directives, docStartEvent);
                        }

                        document = new Yaml.Document(
                                randomId(),
                                documentPrefix,
                                Markers.EMPTY,
                                directives != null ? directives : emptyList(),
                                docStartEvent.getExplicit(),
                                new Yaml.Mapping(randomId(), Markers.EMPTY, null, emptyList(), null, null, null),
                                new Yaml.Document.End(randomId(), "", Markers.EMPTY, false)
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

                            // dashPrefixIndex could be 0 (if anchoring a sequence item) or greater than 0 (if anchoring entire list)
                            int dashPrefixIndex = commentAwareIndexOf('-', fmt);
                            lastEnd = lastEnd + mappingStartEvent.getAnchor().length() + fmt.length() + 1;

                            if (dashPrefixIndex > 0) {
                                fmt = fmt.substring(0, dashPrefixIndex);
                            }
                        }

                        String fullPrefix = reader.readStringFromBuffer(lastEnd, event.getEndMark().getIndex() - 1);
                        int startIndex = commentAwareIndexOf(Arrays.asList(':', '-'), fullPrefix) + 1;
                        Yaml.Tag tag = null;
                        if (mappingStartEvent.getTag() != null) {
                            String prefixAfterColon = fullPrefix.substring(startIndex);
                            final int tagStartIndex = prefixAfterColon.indexOf('!');
                            String tagPrefix = prefixAfterColon.substring(0, tagStartIndex);
                            int i = tagStartIndex;
                            while (i < prefixAfterColon.length() && !Character.isWhitespace(prefixAfterColon.charAt(i))) {
                                i++;
                            }
                            // Cannot use sse.getTag() here, because it is sometimes expanded, e.g. `!!seq` becomes `tag:yaml.org,2002:seq`
                            String tagName = prefixAfterColon.substring(tagStartIndex, i);
                            String tagSuffix = prefixAfterColon.substring(i, prefixAfterColon.length() - 2);
                            tag = createTag(tagPrefix, Markers.EMPTY, tagName, tagSuffix);
                            lastEnd = lastEnd + startIndex + i + 1;
                        }

                        String startBracePrefix = null;
                        int openingBraceIndex = commentAwareIndexOf('{', fullPrefix);
                        if (openingBraceIndex != -1) {
                            startBracePrefix = fullPrefix.substring(startIndex, openingBraceIndex);
                            lastEnd = event.getEndMark().getIndex();
                        }
                        blockStack.push(new MappingBuilder(fmt, startBracePrefix, anchor, tag));
                        break;
                    }
                    case Scalar: {
                        String fmt = newLine + reader.prefix(lastEnd, event);

                        ScalarEvent scalar = (ScalarEvent) event;

                        Yaml.Anchor anchor = null;
                        int valueStart;
                        if (scalar.getAnchor() != null) {
                            anchor = buildYamlAnchor(reader, lastEnd, fmt, scalar.getAnchor(), event.getEndMark().getIndex(), true);
                            anchors.put(scalar.getAnchor(), anchor);
                            valueStart = lastEnd + fmt.length() + scalar.getAnchor().length() + 1 + anchor.getPostfix().length();
                        } else {
                            valueStart = lastEnd + fmt.length();
                        }
                        valueStart -= newLine.length();
                        newLine = "";

                        Yaml.Tag tag = null;
                        if (scalar.getTag() != null) {
                            String potentialScalarValue = reader.readStringFromBuffer(valueStart, event.getEndMark().getIndex() - 1);
                            assert (potentialScalarValue.contains("!"));
                            final int tagStartIndex = potentialScalarValue.indexOf('!');
                            String tagPrefix = potentialScalarValue.substring(0, tagStartIndex);
                            int indexOfTagName = tagStartIndex;
                            while (indexOfTagName < potentialScalarValue.length() && !Character.isWhitespace(potentialScalarValue.charAt(indexOfTagName))) {
                                indexOfTagName++;
                            }
                            String tagName = potentialScalarValue.substring(tagStartIndex, indexOfTagName);
                            int indexOfSpaceAfterTag = indexOfTagName;
                            while (indexOfSpaceAfterTag < potentialScalarValue.length() && Character.isWhitespace(potentialScalarValue.charAt(indexOfSpaceAfterTag))) {
                                indexOfSpaceAfterTag++;
                            }
                            String tagSuffix = potentialScalarValue.substring(indexOfTagName, indexOfSpaceAfterTag);
                            valueStart += indexOfSpaceAfterTag;
                            tag = createTag(tagPrefix, Markers.EMPTY, tagName, tagSuffix);
                        }

                        // Adjust `valueStart` by subtracting the count of supplementary Unicode characters in `fmt`.
                        valueStart -= fmt.codePoints().map(c -> Character.isSupplementaryCodePoint(c) ? 1 : 0).sum();

                        String scalarValue;
                        switch (scalar.getScalarStyle()) {
                            case DOUBLE_QUOTED:
                            case SINGLE_QUOTED:
                                scalarValue = reader.readStringFromBuffer(valueStart + 1, event.getEndMark().getIndex() - 2);
                                break;
                            case PLAIN:
                                scalarValue = reader.readStringFromBuffer(valueStart, event.getEndMark().getIndex() - 1);
                                break;
                            case FOLDED:
                            case LITERAL:
                                scalarValue = reader.readStringFromBuffer(valueStart + 1, event.getEndMark().getIndex() - 1);
                                break;
                            default:
                                scalarValue = reader.readStringFromBuffer(valueStart + 1, event.getEndMark().getIndex() - 1);
                                break;
                        }
                        // First restore any Helm template UUIDs
                        for (Map.Entry<String, String> entry : helmTemplateByUuid.entrySet()) {
                            if (scalarValue.contains(entry.getKey())) {
                                scalarValue = scalarValue.replace(entry.getKey(), entry.getValue());
                            }
                        }
                        // Then restore any single-brace template UUIDs
                        for (Map.Entry<String, String> entry : singleBraceTemplateByUuid.entrySet()) {
                            if (scalarValue.contains(entry.getKey())) {
                                scalarValue = scalarValue.replace(entry.getKey(), entry.getValue());
                            }
                        }
                        // Then check for variable UUIDs (these are exact matches)
                        if (variableByUuid.containsKey(scalarValue)) {
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
                                break;
                            case FOLDED:
                                style = Yaml.Scalar.Style.FOLDED;
                                break;
                            case PLAIN:
                            default:
                                style = Yaml.Scalar.Style.PLAIN;
                                break;
                        }
                        Yaml.Scalar finalScalar = new Yaml.Scalar(randomId(), fmt, Markers.EMPTY, style, anchor, tag, scalarValue);
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
                            sequenceBuilder.push(finalScalar, commaPrefix);
                        } else if (builder == null) {
                            if (!"".equals(finalScalar.getValue())) {
                                // If the "scalar" is just a comment, allow it to accrue to the Document.End rather than create a phantom scalar
                                blockStack.push(new ScalarBuilder(finalScalar, event.getEndMark().getIndex()));
                            }
                        } else {
                            builder.push(finalScalar);
                            lastEnd = event.getEndMark().getIndex();
                        }
                        break;
                    }
                    case SequenceEnd:
                    case MappingEnd: {
                        Yaml.Block mappingOrSequence = blockStack.pop().build();
                        if (mappingOrSequence instanceof SequenceWithPrefix) {
                            SequenceWithPrefix seq = (SequenceWithPrefix) mappingOrSequence;
                            if (seq.getOpeningBracketPrefix() != null) {
                                String s = reader.readStringFromBuffer(lastEnd, event.getStartMark().getIndex());
                                int closingBracketIndex = commentAwareIndexOf(']', s);
                                lastEnd = lastEnd + closingBracketIndex + 1;
                                mappingOrSequence = seq.withClosingBracketPrefix(s.substring(0, closingBracketIndex));
                            }
                        } else if (mappingOrSequence instanceof Yaml.Mapping) {
                            Yaml.Mapping map = (Yaml.Mapping) mappingOrSequence;
                            if (map.getOpeningBracePrefix() != null) {
                                String s = reader.readStringFromBuffer(lastEnd, event.getStartMark().getIndex());
                                int closingBraceIndex = commentAwareIndexOf('}', s);
                                lastEnd = lastEnd + closingBraceIndex + 1;
                                mappingOrSequence = map.withClosingBracePrefix(s.substring(0, closingBraceIndex));
                            }
                        } else {
                            throw new IllegalStateException("Unsupported element type: " + mappingOrSequence.getClass());
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
                        int nextLastEnd = event.getEndMark().getIndex();
                        if (shouldUseYamlParserBugWorkaround(sse)) {
                            nextLastEnd--;
                        }

                        Yaml.Anchor anchor = null;
                        if (sse.getAnchor() != null) {
                            anchor = buildYamlAnchor(reader, lastEnd, fmt, sse.getAnchor(), nextLastEnd, false);
                            anchors.put(sse.getAnchor(), anchor);

                            lastEnd = lastEnd + sse.getAnchor().length() + fmt.length() + 1;
                            fmt = reader.readStringFromBuffer(lastEnd, nextLastEnd);
                            int dashPrefixIndex = commentAwareIndexOf('-', fmt);
                            if (dashPrefixIndex > -1) {
                                fmt = fmt.substring(0, dashPrefixIndex);
                            }
                        }
                        String fullPrefix = reader.readStringFromBuffer(lastEnd, nextLastEnd);
                        String startBracketPrefix = null;
                        int openingBracketIndex = commentAwareIndexOf('[', fullPrefix);
                        int startIndex = commentAwareIndexOf(Arrays.asList(':', '-'), fullPrefix) + 1;
                        if (openingBracketIndex != -1) {
                            startBracketPrefix = fullPrefix.substring(startIndex, openingBracketIndex);
                        }
                        Yaml.Tag tag = null;
                        if (sse.getTag() != null && startBracketPrefix == null) {
                            String prefixAfterColon = fullPrefix.substring(startIndex);
                            final int tagStartIndex = prefixAfterColon.indexOf('!');
                            String tagPrefix = prefixAfterColon.substring(0, tagStartIndex);
                            int i = tagStartIndex;
                            while (i < prefixAfterColon.length() && !Character.isWhitespace(prefixAfterColon.charAt(i))) {
                                i++;
                            }
                            // Cannot use sse.getTag() here, because it is sometimes expanded, e.g. `!!seq` becomes `tag:yaml.org,2002:seq`
                            String tagName = prefixAfterColon.substring(tagStartIndex, i);
                            String tagSuffix = prefixAfterColon.substring(i, prefixAfterColon.length() - 2);
                            tag = createTag(tagPrefix, Markers.EMPTY, tagName, tagSuffix);
                        }
                        lastEnd = nextLastEnd;
                        blockStack.push(new SequenceBuilder(fmt, startBracketPrefix, anchor, tag));
                        break;
                    }
                    case Alias: {
                        String fmt = newLine + reader.prefix(lastEnd, event);
                        newLine = "";

                        AliasEvent alias = (AliasEvent) event;
                        Yaml.Anchor anchor = anchors.get(alias.getAnchor());
                        if (anchor == null) {
                            throw new UnsupportedOperationException("Unknown anchor: " + alias.getAnchor());
                        }
                        BlockBuilder builder = blockStack.peek();
                        builder.push(new Yaml.Alias(randomId(), fmt, Markers.EMPTY, anchor));
                        lastEnd = event.getEndMark().getIndex();
                        break;
                    }
                    case StreamEnd: {
                        if (document == null) {
                            String fmt = newLine + reader.prefix(lastEnd, event);
                            if (!fmt.isEmpty()) {
                                documents.add(
                                        new Yaml.Document(
                                                randomId(), fmt, Markers.EMPTY, emptyList(), false,
                                                new Yaml.Mapping(randomId(), Markers.EMPTY, null, emptyList(), null, null, null),
                                                new Yaml.Document.End(randomId(), "", Markers.EMPTY, false)
                                        ));
                            }
                        } else {
                            suffix = reader.prefix(lastEnd, event);
                        }
                        break;
                    }
                    case StreamStart:
                        break;
                }
            }

            Yaml.Documents result = new Yaml.Documents(randomId(), Markers.EMPTY, sourceFile, FileAttributes.fromPath(sourceFile), source.getCharset().name(), source.isCharsetBomMarked(), null, suffix, documents);
            if (helmTemplateByUuid.isEmpty() && variableByUuid.isEmpty() && singleBraceTemplateByUuid.isEmpty() && helmStandaloneByMarker.isEmpty()) {
                return result;
            }

            // Restore UUID placeholders in all prefixes and suffixes
            return (Yaml.Documents) new YamlIsoVisitor<Integer>() {
                private String restoreUuidPlaceholders(String text) {
                    if (StringUtils.isBlank(text)) {
                        return text;
                    }
                    String result = text;
                    // First restore standalone Helm comment markers (these appear in prefix text as comments)
                    for (Map.Entry<String, String> entry : helmStandaloneByMarker.entrySet()) {
                        if (result.contains(entry.getKey())) {
                            result = result.replace(entry.getKey(), entry.getValue());
                        }
                    }
                    for (Map.Entry<String, String> entry : helmTemplateByUuid.entrySet()) {
                        if (result.contains(entry.getKey())) {
                            result = result.replace(entry.getKey(), entry.getValue());
                        }
                    }
                    for (Map.Entry<String, String> entry : singleBraceTemplateByUuid.entrySet()) {
                        if (result.contains(entry.getKey())) {
                            result = result.replace(entry.getKey(), entry.getValue());
                        }
                    }
                    for (Map.Entry<String, String> entry : variableByUuid.entrySet()) {
                        if (result.contains(entry.getKey())) {
                            result = result.replace(entry.getKey(), entry.getValue());
                        }
                    }
                    return result;
                }

                @Override
                public @Nullable Yaml visit(@Nullable Tree tree, Integer integer) {
                    Yaml visit = super.visit(tree, integer);
                    if (visit == null || StringUtils.isBlank(visit.getPrefix())) {
                        return visit;
                    }
                    return visit.withPrefix(restoreUuidPlaceholders(visit.getPrefix()));
                }

                @Override
                public Yaml.Documents visitDocuments(Yaml.Documents documents, Integer integer) {
                    return unwrapPrefixedMappings(super.visitDocuments(documents, integer));
                }

                @Override
                public Yaml.Mapping visitMapping(Yaml.Mapping mapping, Integer p) {
                    Yaml.Mapping m = super.visitMapping(mapping, p);
                    String opening = m.getOpeningBracePrefix();
                    if (!StringUtils.isBlank(opening)) {
                        m = m.withOpeningBracePrefix((restoreUuidPlaceholders(opening)));
                    }
                    String closing = m.getClosingBracePrefix();
                    if (!StringUtils.isBlank(closing)) {
                        m = m.withClosingBracePrefix((restoreUuidPlaceholders(closing)));
                    }
                    return m;
                }

                @Override
                public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, Integer p) {
                    Yaml.Mapping.Entry e = super.visitMappingEntry(entry, p);
                    return e.withBeforeMappingValueIndicator(restoreUuidPlaceholders(e.getBeforeMappingValueIndicator()));
                }

                @Override
                public Yaml.Sequence visitSequence(Yaml.Sequence sequence, Integer p) {
                    Yaml.Sequence s = super.visitSequence(sequence, p);
                    String opening = s.getOpeningBracketPrefix();
                    if (!StringUtils.isBlank(opening)) {
                        s = s.withOpeningBracketPrefix((restoreUuidPlaceholders(opening)));
                    }
                    String closing = s.getClosingBracketPrefix();
                    if (!StringUtils.isBlank(closing)) {
                        s = s.withClosingBracketPrefix((restoreUuidPlaceholders(closing)));
                    }
                    return s;
                }

                @Override
                public Yaml.Document.End visitDocumentEnd(Yaml.Document.End end, Integer integer) {
                    return end.withPrefix(restoreUuidPlaceholders(end.getPrefix()));
                }

            }.visitNonNull(result, 0);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /*
    The SnakeYAML parser library unfortunately returns inconsistent marks.
    If the dashes of the sequence have an indentation, the end mark and the start mark point to the dash.
    If the dashes of the sequence do not have an indentation, the end mark will point to the character AFTER the dash.
    */
    private boolean shouldUseYamlParserBugWorkaround(SequenceStartEvent event) {
        int startCharIndex = event.getStartMark().getPointer();
        int endCharIndex = event.getEndMark().getPointer();
        if (endCharIndex >= event.getEndMark().getBuffer().length) {
            return false;
        }
        int startChar = event.getStartMark().getBuffer()[startCharIndex];
        int endChar = event.getEndMark().getBuffer()[endCharIndex];
        if (startChar == '&') { // anchor
            return event.getEndMark().getBuffer()[endCharIndex - 1] == '-' && endChar != '-';
        }
        return startChar == '-' && endChar != '-';
    }

    private Yaml.Anchor buildYamlAnchor(FormatPreservingReader reader, int lastEnd, String eventPrefix, String anchorKey, int eventEndIndex, boolean isForScalar) {
        int anchorLength = isForScalar ? anchorKey.length() + 1 : anchorKey.length();
        String whitespaceAndScalar = reader.prefix(
                lastEnd + eventPrefix.length() + anchorLength, eventEndIndex);

        StringBuilder postFix = new StringBuilder();
        for (int i = 0; i < whitespaceAndScalar.length(); i++) {
            char c = whitespaceAndScalar.charAt(i);
            if (c != ' ' && c != '\t') {
                break;
            }
            postFix.append(c);
        }

        String prefix = "";
        if (!isForScalar) {
            int prefixStart = commentAwareIndexOf(':', eventPrefix);
            if (prefixStart == -1) {
                prefixStart = commentAwareIndexOf('-', eventPrefix);
            }
            prefix = (prefixStart > -1 && eventPrefix.length() > prefixStart + 1) ? eventPrefix.substring(prefixStart + 1) : "";
        }
        return new Yaml.Anchor(randomId(), prefix, postFix.toString(), Markers.EMPTY, anchorKey);
    }

    private static int commentAwareIndexOf(char target, String s) {
        return commentAwareIndexOf(singleton(target), s);
    }

    private static int commentAwareIndexOf(Collection<Character> anyOf, String s) {
        boolean inComment = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inComment) {
                if (c == '\n') {
                    inComment = false;
                }
            } else {
                if (anyOf.contains(c)) {
                    return i;
                } else if (c == '#') {
                    inComment = true;
                }
            }
        }
        return -1;
    }


    /**
     * After Helm templates have been replaced with UUIDs, some lines may consist
     * entirely of a UUID (with optional surrounding whitespace). These "standalone"
     * Helm lines represent control flow directives (if/else/end/range/with/include)
     * that don't produce YAML values. A bare UUID on its own line creates invalid
     * YAML structure, so we convert them to YAML comments that SnakeYAML will
     * preserve in prefix text.
     */
    private static String convertStandaloneHelmLinesToComments(
            String source,
            Map<String, String> helmTemplateByUuid,
            Map<String, String> helmStandaloneByMarker) {
        if (helmTemplateByUuid.isEmpty()) {
            return source;
        }

        Set<String> uuids = new HashSet<>(helmTemplateByUuid.keySet());
        StringBuilder result = new StringBuilder(source.length());

        boolean inBlockScalar = false;
        int blockScalarBaseIndent = -1;

        int i = 0;
        while (i < source.length()) {
            // Find end of current line (before newline characters)
            int lineEnd = i;
            while (lineEnd < source.length() && source.charAt(lineEnd) != '\n' && source.charAt(lineEnd) != '\r') {
                lineEnd++;
            }

            String lineContent = source.substring(i, lineEnd);
            String trimmed = lineContent.trim();
            int indent = leadingSpaceCount(lineContent);

            // Track block scalar context
            if (inBlockScalar) {
                if (!trimmed.isEmpty() && indent <= blockScalarBaseIndent) {
                    inBlockScalar = false;
                } else {
                    // Inside block scalar: append line as-is, don't convert to comment
                    result.append(lineContent);
                    i = appendNewline(source, lineEnd, result);
                    continue;
                }
            }

            if (uuids.contains(trimmed)) {
                // Standalone Helm line: convert UUID to comment marker
                String uuid = trimmed;
                String original = helmTemplateByUuid.remove(uuid);
                uuids.remove(uuid);
                String marker = HELM_STANDALONE_COMMENT_PREFIX + uuid;
                helmStandaloneByMarker.put(marker, original);

                // Preserve leading whitespace, replace UUID with comment marker
                result.append(lineContent, 0, indent);
                result.append(marker);
            } else {
                result.append(lineContent);

                // Check if this line starts a block scalar
                if (isBlockScalarIndicator(trimmed)) {
                    inBlockScalar = true;
                    blockScalarBaseIndent = indent;
                }
            }

            i = appendNewline(source, lineEnd, result);
        }

        return result.toString();
    }

    private static int appendNewline(String source, int lineEnd, StringBuilder result) {
        if (lineEnd < source.length()) {
            result.append(source.charAt(lineEnd));
            lineEnd++;
            if (lineEnd < source.length() && source.charAt(lineEnd - 1) == '\r' && source.charAt(lineEnd) == '\n') {
                result.append(source.charAt(lineEnd));
                lineEnd++;
            }
        }
        return lineEnd;
    }

    private static boolean isBlockScalarIndicator(String trimmedLine) {
        // A block scalar indicator line ends with | or > (optionally followed by
        // chomp/indent modifiers like +, -, or a digit) after a colon
        int colonIndex = trimmedLine.indexOf(':');
        if (colonIndex == -1) {
            return false;
        }
        String afterColon = trimmedLine.substring(colonIndex + 1).trim();
        // Strip trailing comment
        int commentIndex = afterColon.indexOf(" #");
        if (commentIndex != -1) {
            afterColon = afterColon.substring(0, commentIndex).trim();
        }
        if (afterColon.isEmpty()) {
            return false;
        }
        char first = afterColon.charAt(0);
        if (first != '|' && first != '>') {
            return false;
        }
        // Check that remaining chars (if any) are valid block scalar modifiers
        for (int j = 1; j < afterColon.length(); j++) {
            char c = afterColon.charAt(j);
            if (c != '+' && c != '-' && !Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private static int leadingSpaceCount(String line) {
        int count = 0;
        for (int j = 0; j < line.length(); j++) {
            if (line.charAt(j) == ' ') {
                count++;
            } else {
                break;
            }
        }
        return count;
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


        private final Yaml.@Nullable Anchor anchor;
        private final Yaml.@Nullable Tag tag;

        private final List<Yaml.Mapping.Entry> entries = new ArrayList<>();

        @Nullable
        private YamlKey key;

        private MappingBuilder(String prefix, @Nullable String startBracePrefix, Yaml.@Nullable Anchor anchor, Yaml.@Nullable Tag tag) {
            this.prefix = prefix;
            this.startBracePrefix = startBracePrefix;
            this.anchor = anchor;
            this.tag = tag;
        }

        @Override
        public void push(Yaml.Block block) {
            if (key == null && block instanceof Yaml.Scalar) {
                key = (Yaml.Scalar) block;
            } else if (key == null && block instanceof Yaml.Alias) {
                key = (Yaml.Alias) block;
            } else {
                String keySuffix = block.getPrefix();
                int colonIndex = commentAwareIndexOf(':', keySuffix);
                // In flow mappings like { "MV7", "7J04" }, entries may lack explicit colons.
                // Only omit colon when we're in a flow mapping (has opening brace)
                // AND there's no colon in the keySuffix. For block mappings or when anchors
                // consume the colon, we assume the colon was present.
                boolean omitColon = colonIndex == -1 && startBracePrefix != null;
                block = block.withPrefix(keySuffix.substring(colonIndex + 1));

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
                        Math.max(colonIndex, 0));
                Markers markers = omitColon ? Markers.EMPTY.addIfAbsent(new OmitColon(randomId())) : Markers.EMPTY;
                entries.add(new Yaml.Mapping.Entry(randomId(), entryPrefix, markers, key, beforeMappingValueIndicator, block));
                key = null;
            }
        }

        @Override
        public MappingWithPrefix build() {
            return new MappingWithPrefix(prefix, startBracePrefix, entries, null, anchor, tag);
        }
    }

    private static class SequenceBuilder implements BlockBuilder {
        private final String prefix;

        @Nullable
        private final String startBracketPrefix;


        private final Yaml.@Nullable Anchor anchor;
        private final Yaml.@Nullable Tag tag;

        private final List<Yaml.Sequence.Entry> entries = new ArrayList<>();

        private SequenceBuilder(String prefix, @Nullable String startBracketPrefix, Yaml.@Nullable Anchor anchor, Yaml.@Nullable Tag tag) {
            this.prefix = prefix;
            this.startBracketPrefix = startBracketPrefix;
            this.anchor = anchor;
            this.tag = tag;
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
            return new SequenceWithPrefix(prefix, startBracketPrefix, entries, null, anchor, tag);
        }
    }

    @Value
    private static class ScalarBuilder implements BlockBuilder {
        Yaml.Scalar scalar;
        int lastEnd;

        @Override
        public void push(Yaml.Block block) {
            throw new IllegalStateException("Unable to push on top of a scalar.");
        }

        @Override
        public Yaml.Block build() {
            return scalar;
        }
    }

    @Getter
    private static class MappingWithPrefix extends Yaml.Mapping {
        private String prefix;

        public MappingWithPrefix(String prefix, @Nullable String startBracePrefix, List<Yaml.Mapping.Entry> entries, @Nullable String endBracePrefix, @Nullable Anchor anchor, @Nullable Tag tag) {
            super(randomId(), Markers.EMPTY, startBracePrefix, entries, endBracePrefix, anchor, tag);
            this.prefix = prefix;
        }

        public MappingWithPrefix(UUID id, Markers markers, @Nullable String openingBracePrefix, List<Entry> entries, @Nullable String closingBracePrefix, @Nullable Anchor anchor, @Nullable Tag tag, String prefix) {
            super(id, markers, openingBracePrefix, entries, closingBracePrefix, anchor, tag);
            this.prefix = prefix;
        }

        @Override
        public Mapping withPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        @Override
        public MappingWithPrefix withClosingBracePrefix(@Nullable String closingBracePrefix) {
            return new MappingWithPrefix(
                    getId(),
                    getMarkers(),
                    getOpeningBracePrefix(),
                    getEntries(),
                    closingBracePrefix,
                    getAnchor(),
                    getTag(),
                    this.prefix
            );
        }
    }

    @Getter
    private static class SequenceWithPrefix extends Yaml.Sequence {
        private String prefix;

        public SequenceWithPrefix(String prefix, @Nullable String startBracketPrefix, List<Yaml.Sequence.Entry> entries, @Nullable String endBracketPrefix, @Nullable Anchor anchor, @Nullable Tag tag) {
            super(randomId(), Markers.EMPTY, startBracketPrefix, entries, endBracketPrefix, anchor, tag);
            this.prefix = prefix;
        }

        public SequenceWithPrefix(UUID id, Markers markers, @Nullable String openingBracketPrefix, List<Entry> entries, @Nullable String closingBracketPrefix, @Nullable Anchor anchor, @Nullable Tag tag, String prefix) {
            super(id, markers, openingBracketPrefix, entries, closingBracketPrefix, anchor, tag);
            this.prefix = prefix;
        }

        @Override
        public Sequence withPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        @Override
        public SequenceWithPrefix withClosingBracketPrefix(@Nullable String closingBracketPrefix) {
            // Cannot use super as this returns Yaml.Sequence
            return new SequenceWithPrefix(getId(), getMarkers(), getOpeningBracketPrefix(), getEntries(), closingBracketPrefix, getAnchor(), getTag(), prefix);
        }

        @Override
        public SequenceWithPrefix withOpeningBracketPrefix(@Nullable String openingBracketPrefix) {
            // Cannot use super as this returns Yaml.Sequence
            return new SequenceWithPrefix(getId(), getMarkers(), openingBracketPrefix, getEntries(), getClosingBracketPrefix(), getAnchor(), getTag(), prefix);
        }

        @Override
        public Sequence withTag(@Nullable Tag tag) {
            // Cannot use super as this returns Yaml.Sequence
            return new SequenceWithPrefix(getId(), getMarkers(), getOpeningBracketPrefix(), getEntries(), getClosingBracketPrefix(), getAnchor(), tag, prefix);
        }

        @Override
        public Sequence withEntries(List<Entry> entries) {
            // Cannot use super as this returns Yaml.Sequence
            return new SequenceWithPrefix(getId(), getMarkers(), getOpeningBracketPrefix(), entries, getClosingBracketPrefix(), getAnchor(), getTag(), prefix);
        }

        @Override
        public Sequence withMarkers(Markers markers) {
            // Cannot use super as this returns Yaml.Sequence
            return new SequenceWithPrefix(getId(), markers, getOpeningBracketPrefix(), getEntries(), getClosingBracketPrefix(), getAnchor(), getTag(), prefix);
        }

        public Sequence toSequence() {
            return new Yaml.Sequence(getId(), getMarkers(), getOpeningBracketPrefix(), getEntries(), getClosingBracketPrefix(), getAnchor(), getTag());
        }
    }

    private Yaml.Documents unwrapPrefixedMappings(Yaml.Documents y) {
        //noinspection ConstantConditions
        return (Yaml.Documents) new YamlIsoVisitor<Integer>() {
            @Override
            public Yaml.Sequence visitSequence(Yaml.Sequence sequence, Integer p) {
                if (sequence instanceof SequenceWithPrefix) {
                    SequenceWithPrefix sequenceWithPrefix = (SequenceWithPrefix) sequence;
                    if (sequenceWithPrefix.getOpeningBracketPrefix() != null) {
                        // For inline sequence, the prefix got already transferred to the left-hand neighbor
                        return super.visitSequence(sequenceWithPrefix.toSequence(), p);
                    } else {
                        // For normal sequence with dashes, the prefix of the sequence gets transferred to the first entry
                        return super.visitSequence(
                                new Yaml.Sequence(
                                        sequenceWithPrefix.getId(),
                                        sequenceWithPrefix.getMarkers(),
                                        sequenceWithPrefix.getOpeningBracketPrefix(),
                                        ListUtils.mapFirst(sequenceWithPrefix.getEntries(), e -> e.withPrefix(sequenceWithPrefix.getPrefix())),
                                        sequenceWithPrefix.getClosingBracketPrefix(),
                                        sequenceWithPrefix.getAnchor(),
                                        sequenceWithPrefix.getTag()
                                ), p);
                    }
                }
                return super.visitSequence(sequence, p);
            }

            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, Integer p) {
                if (mapping instanceof MappingWithPrefix) {
                    MappingWithPrefix mappingWithPrefix = (MappingWithPrefix) mapping;
                    return super.visitMapping(new Yaml.Mapping(mappingWithPrefix.getId(),
                            mappingWithPrefix.getMarkers(), mappingWithPrefix.getOpeningBracePrefix(), mappingWithPrefix.getEntries(), mappingWithPrefix.getClosingBracePrefix(), mappingWithPrefix.getAnchor(), mappingWithPrefix.getTag()), p);
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

    private String parseDirectives(String input, List<Yaml.Directive> directives, DocumentStartEvent docStartEvent) {
        // First try to parse directives from the input prefix
        String remainingPrefix = parseDirectivesFromPrefix(input, directives);

        // If no directives found in prefix but the event has version/tags, reconstruct them
        if (directives.isEmpty()) {
            String prefix = remainingPrefix;
            if (docStartEvent.getVersion() != null) {
                directives.add(new Yaml.Directive(
                        randomId(),
                        prefix,
                        Markers.EMPTY,
                        "YAML " + docStartEvent.getVersion().major() + "." + docStartEvent.getVersion().minor(),
                        "\n"
                ));
                prefix = "";
            }
            Map<String, String> tags = docStartEvent.getTags();
            if (tags != null && !tags.isEmpty()) {
                for (Map.Entry<String, String> tag : tags.entrySet()) {
                    directives.add(new Yaml.Directive(
                            randomId(),
                            prefix,
                            Markers.EMPTY,
                            "TAG " + tag.getKey() + " " + tag.getValue(),
                            "\n"
                    ));
                    prefix = "";
                }
            }
            if (!directives.isEmpty()) {
                return "";
            }
        }

        return remainingPrefix;
    }

    private String parseDirectivesFromPrefix(String input, List<Yaml.Directive> directives) {
        if (input.isEmpty()) {
            return input;
        }

        StringBuilder currentPrefix = new StringBuilder();
        StringBuilder currentValue = new StringBuilder();
        boolean inDirective = false;
        int i = 0;

        while (i < input.length()) {
            char c = input.charAt(i);

            if (!inDirective) {
                if (c == '%') {
                    inDirective = true;
                    i++;
                } else {
                    currentPrefix.append(c);
                    i++;
                }
            } else {
                if (c == '\n' || c == '\r') {
                    StringBuilder suffix = new StringBuilder();
                    suffix.append(c);
                    i++;
                    if (c == '\r' && i < input.length() && input.charAt(i) == '\n') {
                        suffix.append('\n');
                        i++;
                    }
                    directives.add(new Yaml.Directive(
                            randomId(),
                            currentPrefix.toString(),
                            Markers.EMPTY,
                            currentValue.toString(),
                            suffix.toString()
                    ));
                    currentPrefix.setLength(0);
                    currentValue.setLength(0);
                    inDirective = false;
                } else {
                    currentValue.append(c);
                    i++;
                }
            }
        }

        if (inDirective && currentValue.length() > 0) {
            directives.add(new Yaml.Directive(
                    randomId(),
                    currentPrefix.toString(),
                    Markers.EMPTY,
                    currentValue.toString(),
                    ""
            ));
            return "";
        }

        return currentPrefix.toString();
    }

    private Queue<List<Yaml.Directive>> preScanDirectives(String source) {
        Queue<List<Yaml.Directive>> result = new LinkedList<>();
        List<Yaml.Directive> currentDocDirectives = new ArrayList<>();

        int i = 0;
        StringBuilder prefix = new StringBuilder();
        StringBuilder value = new StringBuilder();
        boolean inDirective = false;

        while (i < source.length()) {
            char c = source.charAt(i);

            if (c == '%' && (i == 0 || source.charAt(i - 1) == '\n' || source.charAt(i - 1) == '\r')) {
                // Start of a directive
                inDirective = true;
                i++;
            } else if (inDirective) {
                if (c == '\n' || c == '\r') {
                    // End of directive line
                    StringBuilder suffix = new StringBuilder();
                    suffix.append(c);
                    i++;
                    if (c == '\r' && i < source.length() && source.charAt(i) == '\n') {
                        suffix.append('\n');
                        i++;
                    }
                    currentDocDirectives.add(new Yaml.Directive(
                            randomId(),
                            prefix.toString(),
                            Markers.EMPTY,
                            value.toString(),
                            suffix.toString()
                    ));
                    prefix.setLength(0);
                    value.setLength(0);
                    inDirective = false;
                } else {
                    value.append(c);
                    i++;
                }
            } else if (c == '-' && i + 2 < source.length() &&
                    source.charAt(i + 1) == '-' && source.charAt(i + 2) == '-') {
                // Found document start marker, commit current directives
                if (!currentDocDirectives.isEmpty()) {
                    result.add(new ArrayList<>(currentDocDirectives));
                    currentDocDirectives.clear();
                } else {
                    result.add(emptyList());
                }
                prefix.setLength(0);
                // Skip past ---
                i += 3;
                // Skip to end of line
                while (i < source.length() && source.charAt(i) != '\n') {
                    i++;
                }
                if (i < source.length()) {
                    i++; // skip newline
                }
            } else if (c == '\n' || c == '\r') {
                // Accumulate newlines as potential prefix for next directive
                prefix.append(c);
                i++;
                if (c == '\r' && i < source.length() && source.charAt(i) == '\n') {
                    prefix.append('\n');
                    i++;
                }
            } else {
                // Not a directive or document marker, just content
                // If we have pending directives and hit content, they belong to an implicit document start
                if (!currentDocDirectives.isEmpty()) {
                    result.add(new ArrayList<>(currentDocDirectives));
                    currentDocDirectives.clear();
                }
                prefix.setLength(0);
                // Skip to end of document or file
                while (i < source.length()) {
                    if (source.charAt(i) == '\n') {
                        i++;
                        // Check for document start/end markers on new line
                        if (i + 2 < source.length() &&
                                ((source.charAt(i) == '-' && source.charAt(i + 1) == '-' && source.charAt(i + 2) == '-') ||
                                        (source.charAt(i) == '.' && source.charAt(i + 1) == '.' && source.charAt(i + 2) == '.'))) {
                            break;
                        }
                    } else {
                        i++;
                    }
                }
            }
        }

        // Handle any remaining directives (for document without explicit start)
        if (!currentDocDirectives.isEmpty()) {
            result.add(currentDocDirectives);
        }

        return result;
    }

    private Yaml.Tag createTag(String prefix, Markers markers, String text, String suffix) {
        final String name;
        final Yaml.Tag.Kind kind;
        if (text.startsWith("!<") && text.endsWith(">")) {
            name = text.substring(2, text.length() - 1);
            kind = Yaml.Tag.Kind.EXPLICIT_GLOBAL;
        } else if (text.startsWith("!!")) {
            name = text.substring(2);
            kind = Yaml.Tag.Kind.IMPLICIT_GLOBAL;
        } else if (text.startsWith("!")) {
            name = text.substring(1);
            kind = Yaml.Tag.Kind.LOCAL;
        } else {
            throw new IllegalArgumentException("Invalid tag format: " + text);
        }
        return new Yaml.Tag(randomId(), prefix, markers, name, suffix, kind);
    }


}
