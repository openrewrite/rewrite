/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock;

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of js-yaml lib/js-yaml/loader.js {@code safeLoad} under {@code FAILSAFE_SCHEMA}: 3.13.1, which yarn 1.22.22
 * bundles, and 3.14.1, which @yarnpkg/parsers 3.0.3 calls with {@code json: true}.
 */
public final class JsYaml {

    public enum Release {
        V3_13_1,
        V3_14_1
    }

    /**
     * The release, the {@code json} option (a duplicated key overrides the earlier one in place), and the objects a
     * mapping becomes.
     */
    @Value
    public static class Options {
        Release release;
        boolean json;
        Supplier<Map<String, @Nullable Object>> objects;
    }

    /**
     * The TypeError js-yaml's own code raises.
     */
    public static final class TypeError extends RuntimeException {
        TypeError(String message) {
            super(message);
        }
    }


    public static final class YamlException extends RuntimeException {
        YamlException(String message) {
            super(message);
        }

        @Override
        public String toString() {
            return "YAMLException: " + getMessage();
        }
    }

    private static final String PROTO = "__proto__";

    private static final int CONTEXT_FLOW_IN = 1;
    private static final int CONTEXT_FLOW_OUT = 2;
    private static final int CONTEXT_BLOCK_IN = 3;
    private static final int CONTEXT_BLOCK_OUT = 4;

    private static final int CHOMPING_CLIP = 1;
    private static final int CHOMPING_STRIP = 2;
    private static final int CHOMPING_KEEP = 3;

    private static final Map<String, String> TYPE_KINDS = new HashMap<>();
    private static final String[] SIMPLE_ESCAPES = new String[256];
    private static final Pattern PATTERN_TAG_HANDLE = Pattern.compile("^(?:!|!!|![a-z\\-]+!)\\z",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern YAML_VERSION = Pattern.compile("^([0-9]+)\\.([0-9]+)\\z");
    private static final String TAG_URI_CHARS = "-#;/?:@&=+$,_.!~*'()[]";

    static {
        TYPE_KINDS.put("tag:yaml.org,2002:str", "scalar");
        TYPE_KINDS.put("tag:yaml.org,2002:seq", "sequence");
        TYPE_KINDS.put("tag:yaml.org,2002:map", "mapping");
        SIMPLE_ESCAPES['0'] = "\0";
        SIMPLE_ESCAPES['a'] = "\u0007";
        SIMPLE_ESCAPES['b'] = "\b";
        SIMPLE_ESCAPES['t'] = "\t";
        SIMPLE_ESCAPES['\t'] = "\t";
        SIMPLE_ESCAPES['n'] = "\n";
        SIMPLE_ESCAPES['v'] = "\u000B";
        SIMPLE_ESCAPES['f'] = "\f";
        SIMPLE_ESCAPES['r'] = "\r";
        SIMPLE_ESCAPES['e'] = "\u001B";
        SIMPLE_ESCAPES[' '] = " ";
        SIMPLE_ESCAPES['"'] = "\"";
        SIMPLE_ESCAPES['/'] = "/";
        SIMPLE_ESCAPES['\\'] = "\\";
        SIMPLE_ESCAPES['N'] = "\u0085";
        SIMPLE_ESCAPES['_'] = "\u00A0";
        SIMPLE_ESCAPES['L'] = "\u2028";
        SIMPLE_ESCAPES['P'] = "\u2029";
    }

    private JsYaml() {
    }

    /**
     * The stream's one document, null for a stream without documents.
     */
    public static @Nullable Object safeLoadFailsafe(String input, Options options) {
        List<@Nullable Object> documents = loadDocuments(input, options);
        if (documents.isEmpty()) {
            return null;
        }
        if (documents.size() == 1) {
            return documents.get(0);
        }
        throw new YamlException("expected a single document in the stream, but found more");
    }

    private static List<@Nullable Object> loadDocuments(String input, Options options) {
        if (!input.isEmpty()) {
            char last = input.charAt(input.length() - 1);
            if (last != '\n' && last != '\r') {
                input += "\n";
            }
            if (input.charAt(0) == '\uFEFF') {
                input = input.substring(1);
            }
        }
        State state = new State(input, options);
        int nullpos = input.indexOf('\0');
        if (options.release != Release.V3_13_1 && nullpos != -1) {
            state.position = nullpos;
            throw state.error("null byte is not allowed in input");
        }
        while (state.ch(state.position) == ' ') {
            state.lineIndent += 1;
            state.position += 1;
        }
        while (state.position < state.length - 1) {
            state.readDocument();
        }
        return state.documents;
    }

    private static final class State {
        final String input;
        final Options options;
        final int length;
        int position;
        int line;
        int lineStart;
        int lineIndent;
        final List<@Nullable Object> documents = new ArrayList<>();

        @Nullable
        String version;
        Map<String, String> tagMap = new HashMap<>();
        Map<String, @Nullable Object> anchorMap = new HashMap<>();
        @Nullable
        String tag;
        @Nullable
        String anchor;
        @Nullable
        String kind;
        @Nullable
        Object result;

        State(String input, Options options) {
            this.options = options;
            this.length = input.length();
            this.input = input + '\0';
        }

        // charCodeAt, -1 standing for its NaN past either end
        int ch(int i) {
            return i >= 0 && i < input.length() ? input.charAt(i) : -1;
        }

        YamlException error(String message) {
            return new YamlException(message);
        }

        void append(String s) {
            result = result + s;
        }

        void yamlDirective(List<String> args) {
            if (version != null) {
                throw error("duplication of %YAML directive");
            }
            if (args.size() != 1) {
                throw error("YAML directive accepts exactly one argument");
            }
            Matcher match = YAML_VERSION.matcher(args.get(0));
            if (!match.find()) {
                throw error("ill-formed argument of the YAML directive");
            }
            if (!"1".equals(match.group(1).replaceFirst("^0+(?=.)", ""))) {
                throw error("unacceptable YAML version of the document");
            }
            version = args.get(0);
        }

        void tagDirective(List<String> args) {
            if (args.size() != 2) {
                throw error("TAG directive accepts exactly two arguments");
            }
            String handle = args.get(0);
            String prefix = args.get(1);
            if (!PATTERN_TAG_HANDLE.matcher(handle).find()) {
                throw error("ill-formed tag handle (first argument) of the TAG directive");
            }
            if (tagMap.containsKey(handle)) {
                throw error("there is a previously declared suffix for \"" + handle + "\" tag handle");
            }
            if (!isTagUri(prefix)) {
                throw error("ill-formed tag prefix (second argument) of the TAG directive");
            }
            tagMap.put(handle, prefix);
        }

        void captureSegment(int start, int end, boolean checkJson) {
            if (start < end) {
                String segment = input.substring(start, end);
                if (checkJson) {
                    for (int i = 0; i < segment.length(); i++) {
                        char c = segment.charAt(i);
                        if (!(c == 0x09 || 0x20 <= c)) {
                            throw error("expected valid JSON character");
                        }
                    }
                } else if (isNonPrintable(segment)) {
                    throw error("the stream contains non-printable characters");
                }
                append(segment);
            }
        }

        Map<String, @Nullable Object> storeMappingPair(@Nullable Map<String, @Nullable Object> mapping,
                                                       @Nullable Object keyNode, @Nullable Object valueNode) {
            String key;
            if (keyNode instanceof List) {
                StringBuilder joined = new StringBuilder();
                List<?> items = (List<?>) keyNode;
                for (int index = 0; index < items.size(); index++) {
                    Object item = items.get(index);
                    if (item instanceof List) {
                        throw error("nested arrays are not supported inside keys");
                    }
                    if (index > 0) {
                        joined.append(',');
                    }
                    joined.append(item == null ? "" : item instanceof Map ? "[object Object]" : item);
                }
                key = joined.toString();
            } else if (keyNode instanceof Map) {
                key = "[object Object]";
            } else {
                key = String.valueOf(keyNode);
            }
            if (mapping == null) {
                mapping = options.objects.get();
            }
            if (!options.json && mapping.containsKey(key)) {
                throw error("duplicated mapping key");
            }
            // `{}.__proto__ = value` makes no key
            if (!PROTO.equals(key)) {
                mapping.put(key, valueNode);
            }
            return mapping;
        }

        // anchorMap[name] = value, which for `__proto__` likewise makes no key
        void putAnchor(String name, @Nullable Object value) {
            if (!PROTO.equals(name)) {
                anchorMap.put(name, value);
            }
        }

        void readLineBreak() {
            int c = ch(position);
            if (c == 0x0A) {
                position++;
            } else if (c == 0x0D) {
                position++;
                if (ch(position) == 0x0A) {
                    position++;
                }
            } else {
                throw error("a line break is expected");
            }
            line += 1;
            lineStart = position;
        }

        int skipSeparationSpace(boolean allowComments) {
            int lineBreaks = 0;
            int c = ch(position);
            while (c != 0) {
                while (isWhiteSpace(c)) {
                    c = ch(++position);
                }
                if (allowComments && c == '#') {
                    do {
                        c = ch(++position);
                    } while (c != 0x0A && c != 0x0D && c != 0);
                }
                if (isEol(c)) {
                    readLineBreak();
                    c = ch(position);
                    lineBreaks++;
                    lineIndent = 0;
                    while (c == ' ') {
                        lineIndent++;
                        c = ch(++position);
                    }
                } else {
                    break;
                }
            }
            return lineBreaks;
        }

        boolean testDocumentSeparator() {
            int p = position;
            int c = ch(p);
            if ((c == '-' || c == '.') && c == ch(p + 1) && c == ch(p + 2)) {
                p += 3;
                c = ch(p);
                return c == 0 || isWsOrEol(c);
            }
            return false;
        }

        void writeFoldedLines(int count) {
            if (count == 1) {
                append(" ");
            } else if (count > 1) {
                append(repeat("\n", count - 1));
            }
        }

        boolean readPlainScalar(int nodeIndent, boolean withinFlowCollection) {
            String savedKind = kind;
            Object savedResult = result;
            int c = ch(position);
            if (isWsOrEol(c) || isFlowIndicator(c) || c == '#' || c == '&' || c == '*' || c == '!' || c == '|' ||
                c == '>' || c == '\'' || c == '"' || c == '%' || c == '@' || c == '`') {
                return false;
            }
            if (c == '?' || c == '-') {
                int following = ch(position + 1);
                if (isWsOrEol(following) || withinFlowCollection && isFlowIndicator(following)) {
                    return false;
                }
            }
            kind = "scalar";
            result = "";
            int captureStart = position;
            int captureEnd = position;
            boolean hasPendingContent = false;
            int savedLine = 0;
            while (c != 0) {
                if (c == ':') {
                    int following = ch(position + 1);
                    if (isWsOrEol(following) || withinFlowCollection && isFlowIndicator(following)) {
                        break;
                    }
                } else if (c == '#') {
                    if (isWsOrEol(ch(position - 1))) {
                        break;
                    }
                } else if ((position == lineStart && testDocumentSeparator()) ||
                           withinFlowCollection && isFlowIndicator(c)) {
                    break;
                } else if (isEol(c)) {
                    savedLine = line;
                    int savedLineStart = lineStart;
                    int savedLineIndent = lineIndent;
                    skipSeparationSpace(false);
                    if (lineIndent >= nodeIndent) {
                        hasPendingContent = true;
                        c = ch(position);
                        continue;
                    } else {
                        position = captureEnd;
                        line = savedLine;
                        lineStart = savedLineStart;
                        lineIndent = savedLineIndent;
                        break;
                    }
                }
                if (hasPendingContent) {
                    captureSegment(captureStart, captureEnd, false);
                    writeFoldedLines(line - savedLine);
                    captureStart = captureEnd = position;
                    hasPendingContent = false;
                }
                if (!isWhiteSpace(c)) {
                    captureEnd = position + 1;
                }
                c = ch(++position);
            }
            captureSegment(captureStart, captureEnd, false);
            if (result instanceof String && !((String) result).isEmpty()) {
                return true;
            }
            kind = savedKind;
            result = savedResult;
            return false;
        }

        boolean readSingleQuotedScalar(int nodeIndent) {
            int c = ch(position);
            if (c != '\'') {
                return false;
            }
            kind = "scalar";
            result = "";
            position++;
            int captureStart = position;
            int captureEnd = position;
            while ((c = ch(position)) != 0) {
                if (c == '\'') {
                    captureSegment(captureStart, position, true);
                    c = ch(++position);
                    if (c == '\'') {
                        captureStart = position;
                        position++;
                        captureEnd = position;
                    } else {
                        return true;
                    }
                } else if (isEol(c)) {
                    captureSegment(captureStart, captureEnd, true);
                    writeFoldedLines(skipSeparationSpace(false));
                    captureStart = captureEnd = position;
                } else if (position == lineStart && testDocumentSeparator()) {
                    throw error("unexpected end of the document within a single quoted scalar");
                } else {
                    position++;
                    captureEnd = position;
                }
            }
            throw error("unexpected end of the stream within a single quoted scalar");
        }

        boolean readDoubleQuotedScalar(int nodeIndent) {
            int c = ch(position);
            if (c != '"') {
                return false;
            }
            kind = "scalar";
            result = "";
            position++;
            int captureStart = position;
            int captureEnd = position;
            while ((c = ch(position)) != 0) {
                if (c == '"') {
                    captureSegment(captureStart, position, true);
                    position++;
                    return true;
                } else if (c == '\\') {
                    captureSegment(captureStart, position, true);
                    c = ch(++position);
                    int tmp;
                    if (isEol(c)) {
                        skipSeparationSpace(false);
                    } else if (c >= 0 && c < 256 && SIMPLE_ESCAPES[c] != null) {
                        append(SIMPLE_ESCAPES[c]);
                        position++;
                    } else if ((tmp = escapedHexLen(c)) > 0) {
                        int hexLength = tmp;
                        int hexResult = 0;
                        for (; hexLength > 0; hexLength--) {
                            c = ch(++position);
                            if ((tmp = fromHexCode(c)) >= 0) {
                                hexResult = (hexResult << 4) + tmp;
                            } else {
                                throw error("expected hexadecimal character");
                            }
                        }
                        append(charFromCodepoint(hexResult));
                        position++;
                    } else {
                        throw error("unknown escape sequence");
                    }
                    captureStart = captureEnd = position;
                } else if (isEol(c)) {
                    captureSegment(captureStart, captureEnd, true);
                    writeFoldedLines(skipSeparationSpace(false));
                    captureStart = captureEnd = position;
                } else if (position == lineStart && testDocumentSeparator()) {
                    throw error("unexpected end of the document within a double quoted scalar");
                } else {
                    position++;
                    captureEnd = position;
                }
            }
            throw error("unexpected end of the stream within a double quoted scalar");
        }

        @SuppressWarnings("unchecked")
        boolean readFlowCollection(int nodeIndent) {
            boolean readNext = true;
            String savedTag = tag;
            String savedAnchor = anchor;
            int c = ch(position);
            int terminator;
            boolean isMapping;
            Object collection;
            if (c == '[') {
                terminator = ']';
                isMapping = false;
                collection = new ArrayList<@Nullable Object>();
            } else if (c == '{') {
                terminator = '}';
                isMapping = true;
                collection = options.objects.get();
            } else {
                return false;
            }
            if (anchor != null) {
                putAnchor(anchor, collection);
            }
            c = ch(++position);
            while (c != 0) {
                skipSeparationSpace(true);
                c = ch(position);
                if (c == terminator) {
                    position++;
                    tag = savedTag;
                    anchor = savedAnchor;
                    kind = isMapping ? "mapping" : "sequence";
                    result = collection;
                    return true;
                } else if (!readNext) {
                    throw error("missed comma between flow collection entries");
                }
                Object keyNode;
                Object valueNode = null;
                boolean isPair = false;
                boolean isExplicitPair = false;
                if (c == '?') {
                    if (isWsOrEol(ch(position + 1))) {
                        isPair = isExplicitPair = true;
                        position++;
                        skipSeparationSpace(true);
                    }
                }
                int savedLine = line;
                composeNode(nodeIndent, CONTEXT_FLOW_IN, false, true);
                keyNode = result;
                skipSeparationSpace(true);
                c = ch(position);
                if ((isExplicitPair || line == savedLine) && c == ':') {
                    isPair = true;
                    ++position;
                    skipSeparationSpace(true);
                    composeNode(nodeIndent, CONTEXT_FLOW_IN, false, true);
                    valueNode = result;
                }
                if (isMapping) {
                    storeMappingPair((Map<String, @Nullable Object>) collection, keyNode, valueNode);
                } else if (isPair) {
                    ((List<@Nullable Object>) collection).add(storeMappingPair(null, keyNode, valueNode));
                } else {
                    ((List<@Nullable Object>) collection).add(keyNode);
                }
                skipSeparationSpace(true);
                c = ch(position);
                if (c == ',') {
                    readNext = true;
                    c = ch(++position);
                } else {
                    readNext = false;
                }
            }
            throw error("unexpected end of the stream within a flow collection");
        }

        boolean readBlockScalar(int nodeIndent) {
            int chomping = CHOMPING_CLIP;
            boolean didReadContent = false;
            boolean detectedIndent = false;
            int textIndent = nodeIndent;
            int emptyLines = 0;
            boolean atMoreIndented = false;
            int c = ch(position);
            boolean folding;
            if (c == '|') {
                folding = false;
            } else if (c == '>') {
                folding = true;
            } else {
                return false;
            }
            kind = "scalar";
            result = "";
            while (c != 0) {
                c = ch(++position);
                int tmp;
                if (c == '+' || c == '-') {
                    if (CHOMPING_CLIP == chomping) {
                        chomping = c == '+' ? CHOMPING_KEEP : CHOMPING_STRIP;
                    } else {
                        throw error("repeat of a chomping mode identifier");
                    }
                } else if ((tmp = fromDecimalCode(c)) >= 0) {
                    if (tmp == 0) {
                        throw error("bad explicit indentation width of a block scalar; it cannot be less than one");
                    } else if (!detectedIndent) {
                        textIndent = nodeIndent + tmp - 1;
                        detectedIndent = true;
                    } else {
                        throw error("repeat of an indentation width identifier");
                    }
                } else {
                    break;
                }
            }
            if (isWhiteSpace(c)) {
                do {
                    c = ch(++position);
                } while (isWhiteSpace(c));
                if (c == '#') {
                    do {
                        c = ch(++position);
                    } while (!isEol(c) && c != 0);
                }
            }
            while (c != 0) {
                readLineBreak();
                lineIndent = 0;
                c = ch(position);
                while ((!detectedIndent || lineIndent < textIndent) && c == ' ') {
                    lineIndent++;
                    c = ch(++position);
                }
                if (!detectedIndent && lineIndent > textIndent) {
                    textIndent = lineIndent;
                }
                if (isEol(c)) {
                    emptyLines++;
                    continue;
                }
                if (lineIndent < textIndent) {
                    if (chomping == CHOMPING_KEEP) {
                        append(repeat("\n", didReadContent ? 1 + emptyLines : emptyLines));
                    } else if (chomping == CHOMPING_CLIP && didReadContent) {
                        append("\n");
                    }
                    break;
                }
                if (folding) {
                    if (isWhiteSpace(c)) {
                        atMoreIndented = true;
                        append(repeat("\n", didReadContent ? 1 + emptyLines : emptyLines));
                    } else if (atMoreIndented) {
                        atMoreIndented = false;
                        append(repeat("\n", emptyLines + 1));
                    } else if (emptyLines == 0) {
                        if (didReadContent) {
                            append(" ");
                        }
                    } else {
                        append(repeat("\n", emptyLines));
                    }
                } else {
                    append(repeat("\n", didReadContent ? 1 + emptyLines : emptyLines));
                }
                didReadContent = true;
                detectedIndent = true;
                emptyLines = 0;
                int captureStart = position;
                while (!isEol(c) && c != 0) {
                    c = ch(++position);
                }
                captureSegment(captureStart, position, false);
            }
            return true;
        }

        boolean readBlockSequence(int nodeIndent) {
            String savedTag = tag;
            String savedAnchor = anchor;
            List<@Nullable Object> sequence = new ArrayList<>();
            boolean detected = false;
            if (anchor != null) {
                putAnchor(anchor, sequence);
            }
            int c = ch(position);
            while (c != 0) {
                if (c != '-' || !isWsOrEol(ch(position + 1))) {
                    break;
                }
                detected = true;
                position++;
                if (skipSeparationSpace(true) != 0 && lineIndent <= nodeIndent) {
                    sequence.add(null);
                    c = ch(position);
                    continue;
                }
                int savedLine = line;
                composeNode(nodeIndent, CONTEXT_BLOCK_IN, false, true);
                sequence.add(result);
                skipSeparationSpace(true);
                c = ch(position);
                if ((line == savedLine || lineIndent > nodeIndent) && c != 0) {
                    throw error("bad indentation of a sequence entry");
                } else if (lineIndent < nodeIndent) {
                    break;
                }
            }
            if (detected) {
                tag = savedTag;
                anchor = savedAnchor;
                kind = "sequence";
                result = sequence;
                return true;
            }
            return false;
        }

        boolean readBlockMapping(int nodeIndent, int flowIndent) {
            String savedTag = tag;
            String savedAnchor = anchor;
            Map<String, @Nullable Object> mapping = options.objects.get();
            Object keyNode = null;
            Object valueNode = null;
            boolean atExplicitKey = false;
            boolean detected = false;
            boolean allowCompact = false;
            if (anchor != null) {
                putAnchor(anchor, mapping);
            }
            int c = ch(position);
            while (c != 0) {
                int following = ch(position + 1);
                int savedLine = line;
                if ((c == '?' || c == ':') && isWsOrEol(following)) {
                    if (c == '?') {
                        if (atExplicitKey) {
                            storeMappingPair(mapping, keyNode, null);
                            keyNode = valueNode = null;
                        }
                        detected = true;
                        atExplicitKey = true;
                        allowCompact = true;
                    } else if (atExplicitKey) {
                        atExplicitKey = false;
                        allowCompact = true;
                    } else {
                        throw error("incomplete explicit mapping pair; a key node is missed; or followed by a " +
                                    "non-tabulated empty line");
                    }
                    position += 1;
                    c = following;
                } else if (composeNode(flowIndent, CONTEXT_FLOW_OUT, false, true)) {
                    if (line == savedLine) {
                        c = ch(position);
                        while (isWhiteSpace(c)) {
                            c = ch(++position);
                        }
                        if (c == ':') {
                            c = ch(++position);
                            if (!isWsOrEol(c)) {
                                throw error("a whitespace character is expected after the key-value separator " +
                                            "within a block mapping");
                            }
                            if (atExplicitKey) {
                                storeMappingPair(mapping, keyNode, null);
                                valueNode = null;
                            }
                            detected = true;
                            atExplicitKey = false;
                            allowCompact = false;
                            keyNode = result;
                        } else if (detected) {
                            throw error("can not read an implicit mapping pair; a colon is missed");
                        } else {
                            tag = savedTag;
                            anchor = savedAnchor;
                            return true;
                        }
                    } else if (detected) {
                        throw error("can not read a block mapping entry; a multiline key may not be an implicit key");
                    } else {
                        tag = savedTag;
                        anchor = savedAnchor;
                        return true;
                    }
                } else {
                    break;
                }

                if (line == savedLine || lineIndent > nodeIndent) {
                    if (composeNode(nodeIndent, CONTEXT_BLOCK_OUT, true, allowCompact)) {
                        if (atExplicitKey) {
                            keyNode = result;
                        } else {
                            valueNode = result;
                        }
                    }
                    if (!atExplicitKey) {
                        storeMappingPair(mapping, keyNode, valueNode);
                        keyNode = valueNode = null;
                    }
                    skipSeparationSpace(true);
                    c = ch(position);
                }
                if (lineIndent > nodeIndent && c != 0) {
                    throw error("bad indentation of a mapping entry");
                } else if (lineIndent < nodeIndent) {
                    break;
                }
            }
            if (atExplicitKey) {
                storeMappingPair(mapping, keyNode, null);
            }
            if (detected) {
                tag = savedTag;
                anchor = savedAnchor;
                kind = "mapping";
                result = mapping;
            }
            return detected;
        }

        boolean readTagProperty() {
            int c = ch(position);
            if (c != '!') {
                return false;
            }
            if (tag != null) {
                throw error("duplication of a tag property");
            }
            c = ch(++position);
            boolean isVerbatim = false;
            boolean isNamed = false;
            String tagHandle = null;
            if (c == '<') {
                isVerbatim = true;
                c = ch(++position);
            } else if (c == '!') {
                isNamed = true;
                tagHandle = "!!";
                c = ch(++position);
            } else {
                tagHandle = "!";
            }
            int start = position;
            String tagName;
            if (isVerbatim) {
                do {
                    c = ch(++position);
                } while (c != 0 && c != '>');
                if (position < length) {
                    tagName = input.substring(start, position);
                    ++position;
                } else {
                    throw error("unexpected end of the stream within a verbatim tag");
                }
            } else {
                while (c != 0 && !isWsOrEol(c)) {
                    if (c == '!') {
                        if (!isNamed) {
                            tagHandle = input.substring(start - 1, position + 1);
                            if (!PATTERN_TAG_HANDLE.matcher(tagHandle).find()) {
                                throw error("named tag handle cannot contain such characters");
                            }
                            isNamed = true;
                            start = position + 1;
                        } else {
                            throw error("tag suffix cannot contain exclamation marks");
                        }
                    }
                    c = ch(++position);
                }
                tagName = input.substring(start, position);
                for (int i = 0; i < tagName.length(); i++) {
                    if (isFlowIndicator(tagName.charAt(i))) {
                        throw error("tag suffix cannot contain flow indicator characters");
                    }
                }
            }
            if (!tagName.isEmpty() && !isTagUri(tagName)) {
                throw error("tag name cannot contain such characters: " + tagName);
            }
            if (isVerbatim) {
                tag = tagName;
            } else if (tagMap.containsKey(tagHandle)) {
                tag = tagMap.get(tagHandle) + tagName;
            } else if ("!".equals(tagHandle)) {
                tag = "!" + tagName;
            } else if ("!!".equals(tagHandle)) {
                tag = "tag:yaml.org,2002:" + tagName;
            } else {
                throw error("undeclared tag handle \"" + tagHandle + "\"");
            }
            return true;
        }

        boolean readAnchorProperty() {
            int c = ch(position);
            if (c != '&') {
                return false;
            }
            if (anchor != null) {
                throw error("duplication of an anchor property");
            }
            c = ch(++position);
            int start = position;
            while (c != 0 && !isWsOrEol(c) && !isFlowIndicator(c)) {
                c = ch(++position);
            }
            if (position == start) {
                throw error("name of an anchor node must contain at least one character");
            }
            anchor = input.substring(start, position);
            return true;
        }

        boolean readAlias() {
            int c = ch(position);
            if (c != '*') {
                return false;
            }
            c = ch(++position);
            int start = position;
            while (c != 0 && !isWsOrEol(c) && !isFlowIndicator(c)) {
                c = ch(++position);
            }
            if (position == start) {
                throw error("name of an alias node must contain at least one character");
            }
            String alias = input.substring(start, position);
            // 3.13.1's alias test calls anchorMap.hasOwnProperty, which an anchor of that name shadows
            if (options.release == Release.V3_13_1 && anchorMap.containsKey("hasOwnProperty")) {
                throw new TypeError("state.anchorMap.hasOwnProperty is not a function");
            }
            if (!anchorMap.containsKey(alias)) {
                throw error("unidentified alias \"" + alias + "\"");
            }
            result = anchorMap.get(alias);
            skipSeparationSpace(true);
            return true;
        }

        boolean composeNode(int parentIndent, int nodeContext, boolean allowToSeek, boolean allowCompact) {
            int indentStatus = 1;
            boolean atNewLine = false;
            boolean hasContent = false;
            tag = null;
            anchor = null;
            kind = null;
            result = null;
            boolean allowBlockStyles = CONTEXT_BLOCK_OUT == nodeContext || CONTEXT_BLOCK_IN == nodeContext;
            boolean allowBlockScalars = allowBlockStyles;
            boolean allowBlockCollections = allowBlockStyles;
            if (allowToSeek && skipSeparationSpace(true) != 0) {
                atNewLine = true;
                indentStatus = Integer.compare(lineIndent, parentIndent);
            }
            if (indentStatus == 1) {
                while (readTagProperty() || readAnchorProperty()) {
                    if (skipSeparationSpace(true) != 0) {
                        atNewLine = true;
                        allowBlockCollections = allowBlockStyles;
                        indentStatus = Integer.compare(lineIndent, parentIndent);
                    } else {
                        allowBlockCollections = false;
                    }
                }
            }
            if (allowBlockCollections) {
                allowBlockCollections = atNewLine || allowCompact;
            }
            if (indentStatus == 1 || CONTEXT_BLOCK_OUT == nodeContext) {
                int flowIndent = CONTEXT_FLOW_IN == nodeContext || CONTEXT_FLOW_OUT == nodeContext ?
                        parentIndent : parentIndent + 1;
                int blockIndent = position - lineStart;
                if (indentStatus == 1) {
                    if (allowBlockCollections && (readBlockSequence(blockIndent) ||
                                                  readBlockMapping(blockIndent, flowIndent)) ||
                        readFlowCollection(flowIndent)) {
                        hasContent = true;
                    } else {
                        if ((allowBlockScalars && readBlockScalar(flowIndent)) ||
                            readSingleQuotedScalar(flowIndent) || readDoubleQuotedScalar(flowIndent)) {
                            hasContent = true;
                        } else if (readAlias()) {
                            hasContent = true;
                            if (tag != null || anchor != null) {
                                throw error("alias node should not have any properties");
                            }
                        } else if (readPlainScalar(flowIndent, CONTEXT_FLOW_IN == nodeContext)) {
                            hasContent = true;
                            if (tag == null) {
                                tag = "?";
                            }
                        }
                        if (anchor != null) {
                            putAnchor(anchor, result);
                        }
                    }
                } else if (indentStatus == 0) {
                    hasContent = allowBlockCollections && readBlockSequence(blockIndent);
                }
            }
            // FAILSAFE_SCHEMA has no implicit types, so `?` resolves nothing
            if (options.release != Release.V3_13_1 && "?".equals(tag) && result != null && !"scalar".equals(kind)) {
                throw error("unacceptable node kind for !<?> tag; it should be \"scalar\", not \"" + kind + "\"");
            }
            if (tag != null && !"!".equals(tag) && !"?".equals(tag)) {
                String typeKind = TYPE_KINDS.get(tag);
                if (typeKind == null || (kind != null && !typeKind.equals(kind))) {
                    throw error("unknown tag !<" + tag + ">");
                }
                if (result != null && !typeKind.equals(kind)) {
                    throw error("unacceptable node kind for !<" + tag + "> tag; it should be \"" + typeKind +
                                "\", not \"" + kind + "\"");
                }
                if (result == null) {
                    result = "scalar".equals(typeKind) ? "" : "sequence".equals(typeKind) ?
                            new ArrayList<>() : options.objects.get();
                }
                if (anchor != null) {
                    putAnchor(anchor, result);
                }
            }
            return tag != null || anchor != null || hasContent;
        }

        void readDocument() {
            version = null;
            tagMap = new HashMap<>();
            anchorMap = new HashMap<>();
            boolean hasDirectives = false;
            int c;
            while ((c = ch(position)) != 0) {
                skipSeparationSpace(true);
                c = ch(position);
                if (lineIndent > 0 || c != '%') {
                    break;
                }
                hasDirectives = true;
                c = ch(++position);
                int start = position;
                while (c != 0 && !isWsOrEol(c)) {
                    c = ch(++position);
                }
                String directiveName = input.substring(start, position);
                List<String> directiveArgs = new ArrayList<>();
                if (directiveName.length() < 1) {
                    throw error("directive name must not be less than one character in length");
                }
                while (c != 0) {
                    while (isWhiteSpace(c)) {
                        c = ch(++position);
                    }
                    if (c == '#') {
                        do {
                            c = ch(++position);
                        } while (c != 0 && !isEol(c));
                        break;
                    }
                    if (isEol(c)) {
                        break;
                    }
                    start = position;
                    while (c != 0 && !isWsOrEol(c)) {
                        c = ch(++position);
                    }
                    directiveArgs.add(input.substring(start, position));
                }
                if (c != 0) {
                    readLineBreak();
                }
                // any other directive is only a warning
                if ("YAML".equals(directiveName)) {
                    yamlDirective(directiveArgs);
                } else if ("TAG".equals(directiveName)) {
                    tagDirective(directiveArgs);
                }
            }
            skipSeparationSpace(true);
            if (lineIndent == 0 && ch(position) == '-' && ch(position + 1) == '-' && ch(position + 2) == '-') {
                position += 3;
                skipSeparationSpace(true);
            } else if (hasDirectives) {
                throw error("directives end mark is expected");
            }
            composeNode(lineIndent - 1, CONTEXT_BLOCK_OUT, false, true);
            skipSeparationSpace(true);
            documents.add(result);
            if (position == lineStart && testDocumentSeparator()) {
                if (ch(position) == '.') {
                    position += 3;
                    skipSeparationSpace(true);
                }
                return;
            }
            if (position < length - 1) {
                throw error("end of the stream or a document separator is expected");
            }
        }
    }

    private static boolean isEol(int c) {
        return c == 0x0A || c == 0x0D;
    }

    private static boolean isWhiteSpace(int c) {
        return c == 0x09 || c == 0x20;
    }

    private static boolean isWsOrEol(int c) {
        return isWhiteSpace(c) || isEol(c);
    }

    private static boolean isFlowIndicator(int c) {
        return c == ',' || c == '[' || c == ']' || c == '{' || c == '}';
    }

    private static int fromHexCode(int c) {
        if ('0' <= c && c <= '9') {
            return c - '0';
        }
        int lc = c | 0x20;
        return 'a' <= lc && lc <= 'f' ? lc - 'a' + 10 : -1;
    }

    private static int escapedHexLen(int c) {
        return c == 'x' ? 2 : c == 'u' ? 4 : c == 'U' ? 8 : 0;
    }

    private static int fromDecimalCode(int c) {
        return '0' <= c && c <= '9' ? c - '0' : -1;
    }

    // String.fromCharCode, each code unit taken modulo 2^16
    private static String charFromCodepoint(int c) {
        if (c <= 0xFFFF) {
            return String.valueOf((char) c);
        }
        return new String(new char[]{(char) (((c - 0x010000) >> 10) + 0xD800),
                (char) (((c - 0x010000) & 0x03FF) + 0xDC00)});
    }

    private static String repeat(String s, int count) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < count; i++) {
            out.append(s);
        }
        return out.toString();
    }

    // PATTERN_NON_PRINTABLE, over UTF-16 code units as JavaScript's regex reads them
    private static boolean isNonPrintable(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c <= 0x08 || c == 0x0B || c == 0x0C || (0x0E <= c && c <= 0x1F) || (0x7F <= c && c <= 0x84) ||
                (0x86 <= c && c <= 0x9F) || c == 0xFFFE || c == 0xFFFF ||
                (Character.isHighSurrogate(c) && (i + 1 >= s.length() || !Character.isLowSurrogate(s.charAt(i + 1)))) ||
                (Character.isLowSurrogate(c) && (i == 0 || !Character.isHighSurrogate(s.charAt(i - 1))))) {
                return true;
            }
        }
        return false;
    }

    // PATTERN_TAG_URI, case-insensitively and by UTF-16 code unit
    private static boolean isTagUri(String s) {
        if (s.isEmpty() || ",[]{}".indexOf(s.charAt(0)) >= 0) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length() && fromHexCode(s.charAt(i + 1)) >= 0 &&
                fromHexCode(s.charAt(i + 2)) >= 0) {
                i += 2;
            } else if (!(('0' <= c && c <= '9') || ('a' <= (c | 0x20) && (c | 0x20) <= 'z') ||
                         TAG_URI_CHARS.indexOf(c) >= 0)) {
                return false;
            }
        }
        return true;
    }
}
