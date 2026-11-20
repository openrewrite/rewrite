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
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.scanner.Scanner;
import org.yaml.snakeyaml.scanner.ScannerException;
import org.yaml.snakeyaml.scanner.ScannerImpl;
import org.yaml.snakeyaml.tokens.Token;

import org.openrewrite.yaml.marker.OmitColon;
import org.openrewrite.yaml.marker.TagBeforeAnchor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static org.openrewrite.Tree.randomId;

public class YamlParser implements org.openrewrite.Parser {
    private static final Path GITHUB_ACTIONS_LOCKFILE = Paths.get(".github", "workflows", "actions.lock");

    private static final Pattern VARIABLE_PATTERN = Pattern.compile(":\\s+(@[^\n\r@]+@)");
    // Match single-brace placeholder templates like {C App} that contain at least one space
    // These are invalid YAML but used by some tools as placeholders
    private static final Pattern SINGLE_BRACE_TEMPLATE_PATTERN = Pattern.compile("\\{[A-Za-z][^{}\\n\\r]*\\s[^{}\\n\\r]*}");
    // Match an anchor or alias token, i.e. a `&` or `*` opening a node rather than sitting inside a scalar
    private static final Pattern ANCHOR_OR_ALIAS_PATTERN = Pattern.compile("[\\[{,:\\s][&*][^\\s\\[\\]{},]");
    // Match placeholder values starting with multiple asterisks like "*** REMOVED ***"
    // These are invalid YAML aliases but used as credential placeholders
    private static final Pattern ASTERISK_PLACEHOLDER_PATTERN = Pattern.compile(":\\s+(\\*{2,}[^\n\r]*)");

    private static final int UUID_LENGTH = 36;

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

    private static boolean isInsideQuotedScalar(String source, int index) {
        int lineStart = Math.max(source.lastIndexOf('\n', index - 1), source.lastIndexOf('\r', index - 1)) + 1;
        char quote = 0;
        for (int i = lineStart; i < index; i++) {
            char c = source.charAt(i);
            if (quote == 0) {
                char prev = i == lineStart ? ' ' : source.charAt(i - 1);
                if ((c == '"' || c == '\'') &&
                    (Character.isWhitespace(prev) || prev == '[' || prev == '{' || prev == ',')) {
                    quote = c;
                }
            } else if (quote == '"' && c == '\\') {
                i++;
            } else if (c == quote) {
                quote = 0;
            }
        }
        return quote != 0;
    }

    /**
     * A brace group carrying an anchor or alias and standing alone as a value or element is flow
     * syntax rather than a placeholder. SnakeYAML has to see it, or the anchor never registers and
     * an alias to it resolves to nothing.
     */
    private static boolean isFlowMappingWithAnchor(String source, int start, int end) {
        if (!ANCHOR_OR_ALIAS_PATTERN.matcher(source.substring(start, end)).find()) {
            return false;
        }
        char before = start == 0 ? '\n' : source.charAt(start - 1);
        char after = end == source.length() ? '\n' : source.charAt(end);
        return (Character.isWhitespace(before) || before == '[' || before == '{' || before == ',') &&
               (Character.isWhitespace(after) || after == ']' || after == '}' || after == ',' || after == '#');
    }

    private Yaml.Documents parseFromInput(Path sourceFile, EncodingDetectingInputStream source) {
        String yamlSource = source.readFully();
        Map<String, String> variableByUuid = new HashMap<>();
        Map<String, String> helmTemplateByUuid = new HashMap<>();
        Map<String, String> singleBraceTemplateByUuid = new HashMap<>();

        // First, replace all Helm templates, including those spanning lines, with UUIDs
        StringBuilder helmBuffer = new StringBuilder();
        int helmPos = 0;
        for (HelmTemplateScanner.Span span : HelmTemplateScanner.scan(yamlSource)) {
            String uuid = UUID.randomUUID().toString();
            helmTemplateByUuid.put(uuid, yamlSource.substring(span.getStart(), span.getEnd()));
            helmBuffer.append(yamlSource, helmPos, span.getStart()).append(uuid);
            helmPos = span.getEnd();
        }
        helmBuffer.append(yamlSource, helmPos, yamlSource.length());
        String processedSource = helmBuffer.toString();

        // Convert standalone Helm template lines (lines where a UUID is the only content)
        // to YAML comments so they don't create structurally invalid YAML
        Set<String> commentedHelmUuids = new HashSet<>();
        processedSource = convertStandaloneHelmLinesToComments(
                processedSource, helmTemplateByUuid.keySet(), commentedHelmUuids);

        // Then, replace single-brace templates like {C App} with UUIDs
        Matcher singleBraceMatcher = SINGLE_BRACE_TEMPLATE_PATTERN.matcher(processedSource);
        StringBuffer singleBraceBuffer = new StringBuffer();
        while (singleBraceMatcher.find()) {
            if (isFlowMappingWithAnchor(processedSource, singleBraceMatcher.start(), singleBraceMatcher.end())) {
                continue;
            }
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

        // Then, replace asterisk placeholder patterns like "*** REMOVED ***" with UUIDs
        processedSource = yamlSourceWithPlaceholders.toString();
        yamlSourceWithPlaceholders = new StringBuilder();
        Matcher asteriskMatcher = ASTERISK_PLACEHOLDER_PATTERN.matcher(processedSource);
        pos = 0;
        while (pos < processedSource.length() && asteriskMatcher.find(pos)) {
            if (isInsideQuotedScalar(processedSource, asteriskMatcher.start())) {
                yamlSourceWithPlaceholders.append(processedSource, pos, asteriskMatcher.end());
                pos = asteriskMatcher.end();
                continue;
            }
            yamlSourceWithPlaceholders.append(processedSource, pos, asteriskMatcher.start(1));
            String uuid = UUID.randomUUID().toString();
            variableByUuid.put(uuid, asteriskMatcher.group(1));
            yamlSourceWithPlaceholders.append(uuid);
            pos = asteriskMatcher.end(1);
        }

        if (pos < processedSource.length()) {
            yamlSourceWithPlaceholders.append(processedSource, pos, processedSource.length());
        }

        // Pre-scan the source to extract directives, since SnakeYAML doesn't preserve exact directive text
        String sourceText = yamlSourceWithPlaceholders.toString();
        Queue<List<Yaml.Directive>> documentDirectives = preScanDirectives(sourceText);

        // SnakeYAML reads a copy with tab separators swapped for spaces, while the reader keeps the
        // original text, from which every prefix and scalar value is taken
        String scannableText = replaceTabSeparators(sourceText);

        try (FormatPreservingReader reader = new FormatPreservingReader(sourceText, scannableText)) {
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
                        Yaml.Tag tag = null;
                        Markers markers = Markers.EMPTY;
                        if (isTagBeforeAnchor(reader, event, mappingStartEvent.getAnchor(), mappingStartEvent.getTag())) {
                            TagBeforeAnchorProperties properties = readTagBeforeAnchor(reader, event, mappingStartEvent.getAnchor(),
                                    fmt.substring(anchorPrefixStart(fmt) + 1), false);
                            anchor = properties.getAnchor();
                            tag = properties.getTag();
                            markers = properties.getMarkers();
                            anchors.put(mappingStartEvent.getAnchor(), anchor);
                            lastEnd = properties.getEnd();
                        } else if (mappingStartEvent.getAnchor() != null) {
                            anchor = buildYamlAnchor(reader, lastEnd, fmt, mappingStartEvent.getAnchor(), event.getEndMark().getIndex(), false);
                            anchors.put(mappingStartEvent.getAnchor(), anchor);
                            lastEnd = lastEnd + mappingStartEvent.getAnchor().length() + fmt.length() + 1;
                        }

                        String fullPrefix = reader.readStringFromBuffer(lastEnd, event.getEndMark().getIndex() - 1);
                        int openingBraceIndex = commentAwareIndexOf('{', fullPrefix);
                        int tagStartIndex = tag == null && mappingStartEvent.getTag() != null ? commentAwareIndexOf('!', fullPrefix) : -1;
                        int propertiesStart = tagStartIndex != -1 ? tagStartIndex : openingBraceIndex != -1 ? openingBraceIndex : fullPrefix.length();
                        int startIndex = commentAwareIndexOf(Arrays.asList(':', '-'), fullPrefix.substring(0, propertiesStart)) + 1;
                        if (tagStartIndex != -1) {
                            int i = tagStartIndex;
                            while (i < fullPrefix.length() && !Character.isWhitespace(fullPrefix.charAt(i)) && i != openingBraceIndex) {
                                i++;
                            }
                            // Cannot use sse.getTag() here, because it is sometimes expanded, e.g. `!!seq` becomes `tag:yaml.org,2002:seq`
                            String tagName = fullPrefix.substring(tagStartIndex, i);
                            String afterTag = fullPrefix.substring(i, openingBraceIndex == -1 ? fullPrefix.length() : openingBraceIndex);
                            // The indentation of a block mapping's first key belongs to its entry
                            String tagSuffix = openingBraceIndex == -1 ? afterTag.substring(0, afterTag.lastIndexOf('\n') + 1) : afterTag;
                            tag = createTag(fullPrefix.substring(startIndex, tagStartIndex), Markers.EMPTY, tagName, tagSuffix);
                            startIndex = i + tagSuffix.length();
                            lastEnd += fullPrefix.codePointCount(0, startIndex);
                        }

                        String startBracePrefix = null;
                        if (openingBraceIndex != -1) {
                            startBracePrefix = fullPrefix.substring(startIndex, openingBraceIndex);
                            lastEnd = event.getEndMark().getIndex();
                        }
                        blockStack.push(new MappingBuilder(fmt, markers, startBracePrefix, anchor, tag));
                        break;
                    }
                    case Scalar: {
                        String fmt = newLine + reader.prefix(lastEnd, event);

                        ScalarEvent scalar = (ScalarEvent) event;

                        Yaml.Anchor anchor = null;
                        Yaml.Tag tag = null;
                        Markers markers = Markers.EMPTY;
                        int valueStart;
                        if (isTagBeforeAnchor(reader, event, scalar.getAnchor(), scalar.getTag())) {
                            TagBeforeAnchorProperties properties = readTagBeforeAnchor(reader, event, scalar.getAnchor(), "", true);
                            anchor = properties.getAnchor();
                            tag = properties.getTag();
                            markers = properties.getMarkers();
                            anchors.put(scalar.getAnchor(), anchor);
                            valueStart = lastEnd + fmt.length() + properties.getSource().length();
                        } else if (scalar.getAnchor() != null) {
                            anchor = buildYamlAnchor(reader, lastEnd, fmt, scalar.getAnchor(), event.getEndMark().getIndex(), true);
                            anchors.put(scalar.getAnchor(), anchor);
                            valueStart = lastEnd + fmt.length() + scalar.getAnchor().length() + 1 + anchor.getPostfix().length();
                        } else {
                            valueStart = lastEnd + fmt.length();
                        }
                        valueStart -= newLine.length();
                        newLine = "";

                        if (tag == null && scalar.getTag() != null) {
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
                        // Restore Helm/single-brace/variable placeholder UUIDs. A UUID may be
                        // the entire scalar value (e.g. `key: @var@`) or embedded within a longer
                        // scalar (e.g. markdown bold text inside a block scalar that happened to
                        // match the asterisk placeholder regex). A restored value may itself
                        // embed another UUID (e.g. an asterisk placeholder wrapping a Helm
                        // expression like `**${{ ... }}**`), so this resolves to a fixpoint.
                        scalarValue = restorePlaceholders(scalarValue, helmTemplateByUuid,
                                commentedHelmUuids, singleBraceTemplateByUuid, variableByUuid);

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
                        Yaml.Scalar finalScalar = new Yaml.Scalar(randomId(), fmt, markers, style, anchor, tag, scalarValue);
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
                        if (shouldUseYamlParserBugWorkaround(reader, sse)) {
                            nextLastEnd--;
                        }

                        Yaml.Anchor anchor = null;
                        Yaml.Tag tag = null;
                        Markers markers = Markers.EMPTY;
                        if (isTagBeforeAnchor(reader, event, sse.getAnchor(), sse.getTag())) {
                            int delimiterIndex = anchorPrefixStart(fmt);
                            TagBeforeAnchorProperties properties = readTagBeforeAnchor(reader, event, sse.getAnchor(),
                                    fmt.substring(delimiterIndex + 1), false);
                            anchor = properties.getAnchor();
                            tag = properties.getTag();
                            markers = properties.getMarkers();
                            anchors.put(sse.getAnchor(), anchor);
                            lastEnd = properties.getEnd();
                            fmt = fmt.substring(0, delimiterIndex + 1) + prefixBeforeDash(reader.readStringFromBuffer(lastEnd, nextLastEnd));
                        } else if (sse.getAnchor() != null) {
                            anchor = buildYamlAnchor(reader, lastEnd, fmt, sse.getAnchor(), nextLastEnd, false);
                            anchors.put(sse.getAnchor(), anchor);

                            // The sequence's prefix keeps the delimiter: the enclosing builder locates it there to
                            // split the prefix and to decide whether a sequence entry has a dash.
                            int delimiterIndex = anchorPrefixStart(fmt);
                            String prefixThroughDelimiter = fmt.substring(0, delimiterIndex + 1);
                            lastEnd = lastEnd + sse.getAnchor().length() + fmt.length() + 1;
                            fmt = prefixThroughDelimiter + prefixBeforeDash(reader.readStringFromBuffer(lastEnd, nextLastEnd));
                        }
                        String fullPrefix = reader.readStringFromBuffer(lastEnd, nextLastEnd);
                        String startBracketPrefix = null;
                        int openingBracketIndex = commentAwareIndexOf('[', fullPrefix);
                        int startIndex = commentAwareIndexOf(Arrays.asList(':', '-'),
                                openingBracketIndex == -1 ? fullPrefix : fullPrefix.substring(0, openingBracketIndex)) + 1;
                        if (openingBracketIndex != -1) {
                            startBracketPrefix = fullPrefix.substring(startIndex, openingBracketIndex);
                            int tagStartIndex = tag == null && sse.getTag() != null ? commentAwareIndexOf('!', startBracketPrefix) : -1;
                            if (tagStartIndex != -1) {
                                int i = tagStartIndex;
                                while (i < startBracketPrefix.length() && !Character.isWhitespace(startBracketPrefix.charAt(i))) {
                                    i++;
                                }
                                tag = createTag(startBracketPrefix.substring(0, tagStartIndex), Markers.EMPTY,
                                        startBracketPrefix.substring(tagStartIndex, i), startBracketPrefix.substring(i));
                                startBracketPrefix = "";
                            }
                        }
                        if (tag == null && sse.getTag() != null && startBracketPrefix == null) {
                            // When the sequence is not the value of a mapping entry the delimiter found above is the sequence's
                            // own first dash, which comes after the tag, so the tag search must begin at the start of the prefix
                            int tagIndex = fullPrefix.indexOf('!');
                            String prefixAfterColon = fullPrefix.substring(tagIndex != -1 && tagIndex < startIndex ? 0 : startIndex);
                            final int tagStartIndex = prefixAfterColon.indexOf('!');
                            if (tagStartIndex != -1) {
                                String tagPrefix = prefixAfterColon.substring(0, tagStartIndex);
                                int i = tagStartIndex;
                                while (i < prefixAfterColon.length() && !Character.isWhitespace(prefixAfterColon.charAt(i))) {
                                    i++;
                                }
                                // Cannot use sse.getTag() here, because it is sometimes expanded, e.g. `!!seq` becomes `tag:yaml.org,2002:seq`
                                String tagName = prefixAfterColon.substring(tagStartIndex, i);
                                // Everything between the tag and the first dash of the sequence belongs to the tag's suffix.
                                // The dash itself is not always included in `fullPrefix`, depending on where SnakeYAML places
                                // the sequence's end mark, in which case all remaining whitespace is the suffix.
                                String afterTagName = prefixAfterColon.substring(i);
                                int dashIndex = commentAwareIndexOf('-', afterTagName);
                                String tagSuffix = dashIndex == -1 ? afterTagName : afterTagName.substring(0, dashIndex);
                                tag = createTag(tagPrefix, Markers.EMPTY, tagName, tagSuffix);
                                // The whitespace preceding the tag is already accounted for by the tag's own prefix. Since the
                                // sequence's prefix is transferred onto its first entry, which is printed after the tag, retain
                                // only the part up to and including the delimiter to avoid printing that whitespace twice.
                                fmt = fmt.substring(0, commentAwareIndexOf(Arrays.asList(':', '-'), fmt) + 1);
                            }
                        }
                        lastEnd = nextLastEnd;
                        blockStack.push(new SequenceBuilder(fmt, markers, startBracketPrefix, anchor, tag));
                        break;
                    }
                    case Alias: {
                        String fmt = newLine + reader.prefix(lastEnd, event);
                        newLine = "";

                        AliasEvent alias = (AliasEvent) event;
                        Yaml.Anchor anchor = anchors.get(alias.getAnchor());
                        if (anchor == null) {
                            throw new ParserException(null, null,
                                    "found undefined alias " + alias.getAnchor(), event.getStartMark());
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
            if (helmTemplateByUuid.isEmpty() && variableByUuid.isEmpty() && singleBraceTemplateByUuid.isEmpty()) {
                return result;
            }

            // Restore UUID placeholders in all prefixes and suffixes
            return (Yaml.Documents) new YamlIsoVisitor<Integer>() {
                private String restoreUuidPlaceholders(String text) {
                    if (StringUtils.isBlank(text)) {
                        return text;
                    }
                    return restorePlaceholders(text, helmTemplateByUuid,
                            commentedHelmUuids, singleBraceTemplateByUuid, variableByUuid);
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
    Characters are read from our own reader by absolute index, as a mark's buffer is only SnakeYAML's sliding window.
    */
    private static boolean shouldUseYamlParserBugWorkaround(FormatPreservingReader reader, SequenceStartEvent event) {
        int startIndex = event.getStartMark().getIndex();
        int endIndex = event.getEndMark().getIndex();
        String startChar = reader.readStringFromBuffer(startIndex, startIndex);
        String endChar = reader.readStringFromBuffer(endIndex, endIndex);
        if (startChar.isEmpty() || endChar.isEmpty() || endChar.charAt(0) == '-') {
            return false;
        }
        if (startChar.charAt(0) == '&' || startChar.charAt(0) == '!') { // anchor or tag
            return endIndex > startIndex && reader.readStringFromBuffer(endIndex - 1, endIndex - 1).equals("-");
        }
        return startChar.charAt(0) == '-';
    }

    private Yaml.Anchor buildYamlAnchor(FormatPreservingReader reader, int lastEnd, String eventPrefix, String anchorKey, int eventEndIndex, boolean isForScalar) {
        int anchorLength = isForScalar ? anchorKey.length() + 1 : anchorKey.length();
        String whitespaceAndScalar = reader.prefix(
                lastEnd + eventPrefix.length() + anchorLength, eventEndIndex);

        StringBuilder postFix = new StringBuilder();
        for (int i = 0; i < whitespaceAndScalar.length(); i++) {
            char c = whitespaceAndScalar.charAt(i);
            if (c != ' ' && c != '\t' && !(isForScalar && (c == '\n' || c == '\r'))) {
                break;
            }
            postFix.append(c);
        }

        String prefix = isForScalar ? "" : eventPrefix.substring(anchorPrefixStart(eventPrefix) + 1);
        return new Yaml.Anchor(randomId(), prefix, postFix.toString(), Markers.EMPTY, anchorKey);
    }

    private static boolean isTagBeforeAnchor(FormatPreservingReader reader, Event event, @Nullable String anchor, @Nullable String tag) {
        int start = event.getStartMark().getIndex();
        return anchor != null && tag != null && "!".equals(reader.readStringFromBuffer(start, start));
    }

    /**
     * Reads the properties of a node whose tag is written before its anchor, as in {@code !!str &a value}. Each property
     * owns the separation after it, so the node still prints valid YAML once one of them is removed or the
     * {@link TagBeforeAnchor} marker is lost.
     */
    private TagBeforeAnchorProperties readTagBeforeAnchor(FormatPreservingReader reader, Event event, String anchorKey,
                                                          String tagPrefix, boolean isForScalar) {
        int start = event.getStartMark().getIndex();
        String text = reader.readStringFromBuffer(start, event.getEndMark().getIndex() - 1);
        int tagEnd = 0;
        while (tagEnd < text.length() && !Character.isWhitespace(text.charAt(tagEnd))) {
            tagEnd++;
        }
        int anchorStart = tagEnd + commentAwareIndexOf('&', text.substring(tagEnd));
        int anchorEnd = anchorStart + 1 + anchorKey.length();
        int postfixEnd = anchorEnd;
        while (postfixEnd < text.length()) {
            char c = text.charAt(postfixEnd);
            if (c != ' ' && c != '\t' && !(isForScalar && (c == '\n' || c == '\r'))) {
                break;
            }
            postfixEnd++;
        }
        String source = text.substring(0, postfixEnd);
        return new TagBeforeAnchorProperties(
                createTag(tagPrefix, Markers.EMPTY, text.substring(0, tagEnd), text.substring(tagEnd, anchorStart)),
                new Yaml.Anchor(randomId(), "", text.substring(anchorEnd, postfixEnd), Markers.EMPTY, anchorKey),
                Markers.build(singletonList(new TagBeforeAnchor(randomId()))),
                source,
                start + source.codePointCount(0, source.length()));
    }

    @Value
    private static class TagBeforeAnchorProperties {
        Yaml.Tag tag;
        Yaml.Anchor anchor;
        Markers markers;
        String source;

        /**
         * The reader index just past {@link #source}, counted in code points like event marks.
         */
        int end;
    }

    private static String prefixBeforeDash(String prefix) {
        int dashIndex = commentAwareIndexOf('-', prefix);
        return dashIndex == -1 ? prefix : prefix.substring(0, dashIndex);
    }

    /**
     * The index within an event prefix of the delimiter introducing the anchored block: a mapping entry's colon or a
     * sequence entry's dash, or -1 when it has neither. Whatever follows the delimiter is the anchor's own prefix.
     */
    private static int anchorPrefixStart(String eventPrefix) {
        int colonIndex = commentAwareIndexOf(':', eventPrefix);
        return colonIndex == -1 ? commentAwareIndexOf('-', eventPrefix) : colonIndex;
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
     * After Helm templates have been replaced with UUIDs, lines consisting entirely
     * of a UUID are standalone control flow directives. A bare UUID on its own line
     * creates invalid YAML, so we prepend # to make it a YAML comment. {@code commentedUuids}
     * receives those UUIDs, so restoration can tell that # from one the source already had.
     */
    private static String convertStandaloneHelmLinesToComments(
            String source,
            Set<String> helmUuids,
            Set<String> commentedUuids) {
        if (helmUuids.isEmpty()) {
            return source;
        }

        StringBuilder result = new StringBuilder(source.length());

        boolean inBlockScalar = false;
        int blockScalarBaseIndent = -1;

        int i = 0;
        while (i < source.length()) {
            int lineEnd = i;
            while (lineEnd < source.length() && source.charAt(lineEnd) != '\n' && source.charAt(lineEnd) != '\r') {
                lineEnd++;
            }

            String lineContent = source.substring(i, lineEnd);
            String trimmed = lineContent.trim();
            int indent = leadingSpaceCount(lineContent);

            if (inBlockScalar) {
                if (!trimmed.isEmpty() && indent <= blockScalarBaseIndent) {
                    inBlockScalar = false;
                } else {
                    result.append(lineContent);
                    i = appendNewline(source, lineEnd, result);
                    continue;
                }
            }

            String firstUuid = onlyHelmUuids(trimmed, helmUuids);
            if (firstUuid != null) {
                // Comment out the line in place, so whitespace around the templates is kept. Only the first
                // placeholder takes the #; any others on the line are restored as usual
                int start = lineContent.indexOf(firstUuid);
                result.append(lineContent, 0, start);
                result.append('#');
                result.append(lineContent, start, lineContent.length());
                commentedUuids.add(firstUuid);
            } else if ((firstUuid = templatesOutputtingEntries(source, i, lineEnd, helmUuids)) != null) {
                // The templates end a "key:" line and more entries for the key follow, so the templates output
                // entries too. Commenting them out leaves "key:" holding the entries written in the file
                int start = lineContent.indexOf(firstUuid);
                result.append(lineContent, 0, start);
                result.append('#');
                result.append(lineContent, start, lineContent.length());
                commentedUuids.add(firstUuid);
            } else {
                result.append(lineContent);
                if (isBlockScalarIndicator(trimmed)) {
                    inBlockScalar = true;
                    blockScalarBaseIndent = blockScalarParentIndent(lineContent);
                }
            }

            i = appendNewline(source, lineEnd, result);
        }

        return result.toString();
    }

    /**
     * @return the first placeholder on a line holding only Helm placeholders, optionally separated by whitespace,
     * or null if the line holds anything else
     */
    private static @Nullable String onlyHelmUuids(String trimmedLine, Set<String> helmUuids) {
        String first = null;
        int i = 0;
        while (i < trimmedLine.length()) {
            if (Character.isWhitespace(trimmedLine.charAt(i))) {
                i++;
                continue;
            }
            int end = i + UUID_LENGTH;
            if (end > trimmedLine.length() || !helmUuids.contains(trimmedLine.substring(i, end))) {
                return null;
            }
            if (first == null) {
                first = trimmedLine.substring(i, end);
            }
            i = end;
        }
        return first;
    }

    /**
     * Finds Helm templates that output mapping or sequence entries in place of a key's value, as in Bitnami's
     * <pre>
     * labels: {{- include "common.labels.standard" . | nindent 4 }}
     *   app.kubernetes.io/component: broker
     * </pre>
     * where the template outputs the standard labels and the line below adds another. With the template as a
     * placeholder value, the entry below it is invalid YAML. A sequence entry such as
     * {@code - {{ toYaml $constraint | nindent 8 | trim }}}, with more of the item's entries below it, is the same.
     *
     * @return the first placeholder, if the line is a {@code key:} or {@code -} followed only by placeholders, and
     * the next line holding anything else is indented further than the key or dash and holds a mapping or sequence
     * entry; otherwise null
     */
    private static @Nullable String templatesOutputtingEntries(String source, int lineStart, int lineEnd,
                                                               Set<String> helmUuids) {
        String line = source.substring(lineStart, lineEnd);
        int keyColumn = keyColumn(line);
        // The entries that follow must be indented further than this
        int parentColumn = keyColumn;
        String first = null;
        for (int colon = line.indexOf(':', keyColumn); colon >= 0; colon = line.indexOf(':', colon + 1)) {
            if (colon + 1 < line.length() && (line.charAt(colon + 1) == ' ' || line.charAt(colon + 1) == '\t')) {
                first = onlyHelmUuids(line.substring(colon + 1).trim(), helmUuids);
                if (first != null) {
                    break;
                }
            }
        }
        if (first == null && keyColumn > leadingSpaceCount(line)) {
            // A sequence entry, "- {{ toYaml . }}", whose templates output the first of the item's entries
            first = onlyHelmUuids(line.substring(keyColumn).trim(), helmUuids);
            parentColumn = line.lastIndexOf('-', keyColumn);
        }
        if (first == null) {
            return null;
        }

        int i = lineEnd;
        while (i < source.length()) {
            while (i < source.length() && (source.charAt(i) == '\n' || source.charAt(i) == '\r')) {
                i++;
            }
            int nextEnd = i;
            while (nextEnd < source.length() && source.charAt(nextEnd) != '\n' && source.charAt(nextEnd) != '\r') {
                nextEnd++;
            }
            String next = source.substring(i, nextEnd);
            String trimmed = next.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || onlyHelmUuids(trimmed, helmUuids) != null) {
                i = nextEnd;
                continue;
            }
            return leadingSpaceCount(next) > parentColumn && isEntry(trimmed) ? first : null;
        }
        return null;
    }

    /**
     * The column of a line's first mapping key or value, after its indent and any sequence entry dashes.
     */
    private static int keyColumn(String line) {
        int i = leadingSpaceCount(line);
        while (i < line.length() && line.charAt(i) == '-' &&
               (i + 1 == line.length() || line.charAt(i + 1) == ' ' || line.charAt(i + 1) == '\t')) {
            i++;
            while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
                i++;
            }
        }
        return i;
    }

    /**
     * Whether a trimmed line starts a sequence entry or holds a mapping key, which cannot continue a plain scalar.
     */
    private static boolean isEntry(String trimmedLine) {
        if ("-".equals(trimmedLine) || trimmedLine.startsWith("- ") || trimmedLine.startsWith("-\t")) {
            return true;
        }
        for (int colon = trimmedLine.indexOf(':'); colon >= 0; colon = trimmedLine.indexOf(':', colon + 1)) {
            if (colon + 1 == trimmedLine.length() || trimmedLine.charAt(colon + 1) == ' ' ||
                trimmedLine.charAt(colon + 1) == '\t') {
                return true;
            }
        }
        return false;
    }

    /**
     * The indent a block scalar's content must exceed, given the line holding its header. That is the column of the
     * key the scalar belongs to, such as {@code expr} in {@code - expr: |-}, or of the last dash when the scalar is a
     * sequence entry itself, as in {@code - |}.
     */
    private static int blockScalarParentIndent(String line) {
        int i = leadingSpaceCount(line);
        int lastDash = -1;
        while (i < line.length() && line.charAt(i) == '-' &&
               (i + 1 == line.length() || line.charAt(i + 1) == ' ' || line.charAt(i + 1) == '\t')) {
            lastDash = i;
            i++;
            while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
                i++;
            }
        }
        if (lastDash == -1) {
            return leadingSpaceCount(line);
        }
        char next = i < line.length() ? line.charAt(i) : '|';
        return next == '|' || next == '>' ? lastDash : i;
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

    /**
     * Restore template/variable UUID placeholders to their original text. A Helm placeholder
     * listed in {@code commentedHelmUuids} also consumes the {@code #} that made its
     * standalone line a comment. Because a restored value can itself embed another UUID
     * (e.g. an asterisk placeholder capturing {@code **${{ ... }}**}, whose value contains a
     * Helm UUID), the replacements are repeated until they reach a fixpoint.
     */
    private static String restorePlaceholders(
            String text,
            Map<String, String> helmTemplateByUuid,
            Set<String> commentedHelmUuids,
            Map<String, String> singleBraceTemplateByUuid,
            Map<String, String> variableByUuid) {
        String result = text;
        String previous;
        do {
            previous = result;
            for (Map.Entry<String, String> entry : helmTemplateByUuid.entrySet()) {
                String key = commentedHelmUuids.contains(entry.getKey()) ? "#" + entry.getKey() : entry.getKey();
                if (result.contains(key)) {
                    result = result.replace(key, entry.getValue());
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
        } while (!result.equals(previous));
        return result;
    }

    private static boolean isBlockScalarIndicator(String trimmedLine) {
        // A block scalar header is | or > (optionally followed by chomp/indent
        // modifiers like +, -, or a digit). It can appear as a mapping value
        // ("key: |"), as a sequence entry value ("- >-"), or bare at the start of
        // a document. Strip any leading sequence-entry dashes, then any mapping key,
        // to isolate the value portion.
        String value = trimmedLine;
        while ("-".equals(value) || value.startsWith("- ")) {
            value = value.length() == 1 ? "" : value.substring(2).trim();
        }
        int colonIndex = value.indexOf(':');
        if (colonIndex != -1) {
            value = value.substring(colonIndex + 1).trim();
        }
        // Strip trailing comment
        int commentIndex = value.indexOf(" #");
        if (commentIndex != -1) {
            value = value.substring(0, commentIndex).trim();
        }
        if (value.isEmpty()) {
            return false;
        }
        char first = value.charAt(0);
        if (first != '|' && first != '>') {
            return false;
        }
        // Check that remaining chars (if any) are valid block scalar modifiers
        for (int j = 1; j < value.length(); j++) {
            char c = value.charAt(j);
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
        return fileName.endsWith(".yml") || fileName.endsWith(".yaml") ||
               path.endsWith(GITHUB_ACTIONS_LOCKFILE);
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
        private final Markers markers;

        @Nullable
        private final String startBracePrefix;


        private final Yaml.@Nullable Anchor anchor;
        private final Yaml.@Nullable Tag tag;

        private final List<Yaml.Mapping.Entry> entries = new ArrayList<>();

        @Nullable
        private YamlKey key;

        private MappingBuilder(String prefix, Markers markers, @Nullable String startBracePrefix, Yaml.@Nullable Anchor anchor, Yaml.@Nullable Tag tag) {
            this.prefix = prefix;
            this.markers = markers;
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
            } else if (key == null) {
                // Complex YAML key (mapping or sequence used as key).
                // The YamlKey interface only supports Scalar and Alias, so convert
                // the complex key to a placeholder Scalar. This won't round-trip
                // and will become a ParseError, but prevents NPE/ClassCastException.
                key = new Yaml.Scalar(randomId(), block.getPrefix(), Markers.EMPTY,
                        Yaml.Scalar.Style.PLAIN, null, null, "");
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
            return new MappingWithPrefix(prefix, markers, startBracePrefix, entries, null, anchor, tag);
        }
    }

    private static class SequenceBuilder implements BlockBuilder {
        private final String prefix;
        private final Markers markers;

        @Nullable
        private final String startBracketPrefix;


        private final Yaml.@Nullable Anchor anchor;
        private final Yaml.@Nullable Tag tag;

        private final List<Yaml.Sequence.Entry> entries = new ArrayList<>();

        private SequenceBuilder(String prefix, Markers markers, @Nullable String startBracketPrefix, Yaml.@Nullable Anchor anchor, Yaml.@Nullable Tag tag) {
            this.prefix = prefix;
            this.markers = markers;
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
            return new SequenceWithPrefix(prefix, markers, startBracketPrefix, entries, null, anchor, tag);
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

        public MappingWithPrefix(String prefix, Markers markers, @Nullable String startBracePrefix, List<Yaml.Mapping.Entry> entries, @Nullable String endBracePrefix, @Nullable Anchor anchor, @Nullable Tag tag) {
            super(randomId(), markers, startBracePrefix, entries, endBracePrefix, anchor, tag);
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

        public SequenceWithPrefix(String prefix, Markers markers, @Nullable String startBracketPrefix, List<Yaml.Sequence.Entry> entries, @Nullable String endBracketPrefix, @Nullable Anchor anchor, @Nullable Tag tag) {
            super(randomId(), markers, startBracketPrefix, entries, endBracketPrefix, anchor, tag);
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
                if (c == '%' && (i == 0 || input.charAt(i - 1) == '\n' || input.charAt(i - 1) == '\r')) {
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

    /**
     * YAML 1.2 allows tabs in the whitespace separating tokens, such as {@code key:\tvalue} or
     * {@code -\titem}, and forbids them only in indentation. SnakeYAML follows YAML 1.1 and rejects a
     * tab wherever it looks for the next token. Each such tab is swapped for a space, one rejection at
     * a time, so only the tabs SnakeYAML trips on change. A tab in indentation is left alone, and the
     * text comes back unchanged for SnakeYAML to report as before.
     */
    private static String replaceTabSeparators(String source) {
        if (source.indexOf('\t') < 0) {
            return source;
        }
        String scannable = source;
        while (true) {
            try {
                Scanner scanner = new ScannerImpl(new StreamReader(scannable), new LoaderOptions());
                while (!scanner.checkToken(Token.ID.StreamEnd)) {
                    scanner.getToken();
                }
                return scannable;
            } catch (ScannerException e) {
                if (e.getProblemMark() == null || e.getProblem() == null ||
                    !e.getProblem().contains("cannot start any token")) {
                    return scannable;
                }
                // The mark counts code points, not chars
                int tab = scannable.offsetByCodePoints(0, e.getProblemMark().getIndex());
                if (tab >= scannable.length() || scannable.charAt(tab) != '\t' || isIndentation(scannable, tab)) {
                    return scannable;
                }
                char[] chars = scannable.toCharArray();
                for (int i = tab; i < chars.length && (chars[i] == '\t' || chars[i] == ' '); i++) {
                    chars[i] = ' ';
                }
                scannable = new String(chars);
            } catch (RuntimeException e) {
                return scannable;
            }
        }
    }

    private static boolean isIndentation(String source, int index) {
        for (int i = index - 1; i >= 0; i--) {
            char c = source.charAt(i);
            if (c == '\n' || c == '\r') {
                return true;
            }
            if (c != ' ' && c != '\t') {
                return false;
            }
        }
        return true;
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
            } else if (c == '#') {
                // A comment line may precede directives, so treat it as prefix rather than as content
                while (i < source.length() && source.charAt(i) != '\n' && source.charAt(i) != '\r') {
                    prefix.append(source.charAt(i));
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
