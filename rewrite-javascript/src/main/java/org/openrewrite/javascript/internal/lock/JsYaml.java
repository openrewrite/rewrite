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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.EcmaScript.repeat;

/**
 * Port of js-yaml's loader: {@code safeLoad} under {@code FAILSAFE_SCHEMA} of 3.13.1 (yarn 1.22.22) and 3.14.1
 * (@yarnpkg/parsers 3.0.3), {@code load} under {@code DEFAULT_SCHEMA} of @zkochan/js-yaml 0.0.11 (pnpm 11.2.2).
 */
public final class JsYaml {

    /**
     * The release; @zkochan/js-yaml 0.0.11 is js-yaml 4.1.0's loader with {@code setProperty} in mergeMappings.
     */
    public enum Release {
        V3_13_1,
        V3_14_1,
        ZKOCHAN_0_0_11
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

    private static final Options PNPM = new Options(Release.ZKOCHAN_0_0_11, false, LinkedHashMap::new);

    /**
     * js-yaml's {@code YAMLException}, its message carrying the position and the snippet as each release formats them.
     */
    @Getter
    public static final class YamlException extends RuntimeException {
        private final String reason;

        YamlException(String reason, String message) {
            super(message);
            this.reason = reason;
        }

        @Override
        public String toString() {
            return "YAMLException: " + getMessage();
        }
    }

    /**
     * A JavaScript {@code Date} the timestamp type constructs: milliseconds since the epoch.
     */
    @Getter
    @EqualsAndHashCode
    public static final class Timestamp {
        private final double time;

        public Timestamp(double time) {
            this.time = time;
        }

        public String toIsoString() {
            return V8Date.toISOString(time);
        }
    }

    static final String TAG_STR = "tag:yaml.org,2002:str";
    static final String TAG_SEQ = "tag:yaml.org,2002:seq";
    static final String TAG_MAP = "tag:yaml.org,2002:map";
    static final String TAG_NULL = "tag:yaml.org,2002:null";
    static final String TAG_BOOL = "tag:yaml.org,2002:bool";
    static final String TAG_INT = "tag:yaml.org,2002:int";
    static final String TAG_FLOAT = "tag:yaml.org,2002:float";
    static final String TAG_TIMESTAMP = "tag:yaml.org,2002:timestamp";
    static final String TAG_MERGE = "tag:yaml.org,2002:merge";
    static final String TAG_BINARY = "tag:yaml.org,2002:binary";
    static final String TAG_OMAP = "tag:yaml.org,2002:omap";
    static final String TAG_PAIRS = "tag:yaml.org,2002:pairs";
    static final String TAG_SET = "tag:yaml.org,2002:set";

    static final List<String> IMPLICIT = Collections.unmodifiableList(Arrays.asList(
            TAG_NULL, TAG_BOOL, TAG_INT, TAG_FLOAT, TAG_TIMESTAMP, TAG_MERGE));

    private static final Map<String, Map<String, String>> TYPE_MAP = new HashMap<>();
    private static final Map<String, Map<String, String>> FAILSAFE_TYPE_MAP = new HashMap<>();
    private static final String PROTO = "__proto__";

    private static final int CONTEXT_FLOW_IN = 1;
    private static final int CONTEXT_FLOW_OUT = 2;
    private static final int CONTEXT_BLOCK_IN = 3;
    private static final int CONTEXT_BLOCK_OUT = 4;

    private static final int CHOMPING_CLIP = 1;
    private static final int CHOMPING_STRIP = 2;
    private static final int CHOMPING_KEEP = 3;

    private static final String[] SIMPLE_ESCAPES = new String[256];
    private static final Pattern PATTERN_TAG_HANDLE = Pattern.compile("^(?:!|!!|![a-z\\-]+!)\\z",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern YAML_VERSION = Pattern.compile("^([0-9]+)\\.([0-9]+)\\z");
    private static final String TAG_URI_CHARS = "-#;/?:@&=+$,_.!~*'()[]";
    private static final Pattern YAML_FLOAT_PATTERN = Pattern.compile(
            "^(?:[-+]?(?:[0-9][0-9_]*)(?:\\.[0-9_]*)?(?:[eE][-+]?[0-9]+)?" +
            "|\\.[0-9_]+(?:[eE][-+]?[0-9]+)?" +
            "|[-+]?\\.(?:inf|Inf|INF)" +
            "|\\.(?:nan|NaN|NAN))\\z");
    private static final Pattern YAML_DATE_REGEXP = Pattern.compile(
            "^([0-9][0-9][0-9][0-9])-([0-9][0-9])-([0-9][0-9])\\z");
    private static final Pattern YAML_TIMESTAMP_REGEXP = Pattern.compile(
            "^([0-9][0-9][0-9][0-9])" +
            "-([0-9][0-9]?)" +
            "-([0-9][0-9]?)" +
            "(?:[Tt]|[ \\t]+)" +
            "([0-9][0-9]?)" +
            ":([0-9][0-9])" +
            ":([0-9][0-9])" +
            "(?:\\.([0-9]*))?" +
            "(?:[ \\t]*(Z|([-+])([0-9][0-9]?)" +
            "(?::([0-9][0-9]))?))?\\z");
    private static final String BASE64_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=\n\r";

    static {
        Map<String, String> scalar = new HashMap<>();
        for (String tag : new String[]{TAG_STR, TAG_NULL, TAG_BOOL, TAG_INT, TAG_FLOAT, TAG_TIMESTAMP, TAG_MERGE,
                TAG_BINARY}) {
            scalar.put(tag, "scalar");
        }
        Map<String, String> sequence = new HashMap<>();
        for (String tag : new String[]{TAG_SEQ, TAG_OMAP, TAG_PAIRS}) {
            sequence.put(tag, "sequence");
        }
        Map<String, String> mapping = new HashMap<>();
        for (String tag : new String[]{TAG_MAP, TAG_SET}) {
            mapping.put(tag, "mapping");
        }
        Map<String, String> fallback = new HashMap<>();
        fallback.putAll(scalar);
        fallback.putAll(sequence);
        fallback.putAll(mapping);
        TYPE_MAP.put("scalar", scalar);
        TYPE_MAP.put("sequence", sequence);
        TYPE_MAP.put("mapping", mapping);
        TYPE_MAP.put("fallback", fallback);
        Map<String, String> failsafe = new HashMap<>();
        for (String[] type : new String[][]{{TAG_STR, "scalar"}, {TAG_SEQ, "sequence"}, {TAG_MAP, "mapping"}}) {
            FAILSAFE_TYPE_MAP.put(type[1], Collections.singletonMap(type[0], type[1]));
            failsafe.put(type[0], type[1]);
        }
        FAILSAFE_TYPE_MAP.put("fallback", failsafe);

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
        SIMPLE_ESCAPES['_'] = "\u00a0";
        SIMPLE_ESCAPES['L'] = "\u2028";
        SIMPLE_ESCAPES['P'] = "\u2029";
    }

    private JsYaml() {
    }

    /**
     * The fork's {@code load}: the stream's one document, {@link EcmaScript#UNDEFINED} for a stream without documents.
     */
    public static @Nullable Object load(String input) {
        List<@Nullable Object> documents = loadDocuments(input, PNPM);
        if (documents.isEmpty()) {
            return EcmaScript.UNDEFINED;
        }
        return single(documents);
    }

    /**
     * 3.x's {@code safeLoad} under {@code FAILSAFE_SCHEMA}: the stream's one document, null for a stream without any.
     */
    public static @Nullable Object safeLoadFailsafe(String input, Options options) {
        List<@Nullable Object> documents = loadDocuments(input, options);
        return documents.isEmpty() ? null : single(documents);
    }

    private static @Nullable Object single(List<@Nullable Object> documents) {
        if (documents.size() == 1) {
            return documents.get(0);
        }
        throw new YamlException("expected a single document in the stream, but found more",
                "expected a single document in the stream, but found more");
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
        if (options.getRelease() != Release.V3_13_1 && nullpos != -1) {
            state.position = nullpos;
            throw state.error("null byte is not allowed in input");
        }
        state.terminated = true;
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
        final boolean v4;
        final int length;
        int position;
        int line;
        int lineStart;
        int lineIndent;
        int firstTabInLine = -1;
        // 4.1.0 appends the \0 that ends its input after the null byte check
        boolean terminated;
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
            this.v4 = options.getRelease() == Release.ZKOCHAN_0_0_11;
            this.length = input.length();
            this.input = input + '\0';
        }

        int ch(int i) {
            return i >= 0 && i < input.length() ? input.charAt(i) : -1;
        }

        YamlException error(String message) {
            if (!v4) {
                String mark = "at line " + (line + 1) + ", column " + (position - lineStart + 1);
                String snippet = snippet3(terminated ? input : input.substring(0, input.length() - 1));
                return new YamlException(message, message + " " + (snippet == null ? mark : mark + ":\n" + snippet));
            }
            String buffer = input.substring(0, input.length() - (terminated ? 1 : 2));
            int column = position - lineStart;
            String snippet = makeSnippet(buffer, position, line);
            String where = "(" + (line + 1) + ":" + (column + 1) + ")";
            if (snippet != null) {
                where += "\n\n" + snippet;
            }
            return new YamlException(message, message + " " + where);
        }

        // 3.x Mark#getSnippet(4, 75): the line around the position, cut to 37 characters each way
        private @Nullable String snippet3(String buffer) {
            if (buffer.isEmpty()) {
                return null;
            }
            String head = "";
            int start = position;
            while (start > 0 && start <= buffer.length() && !isBreak3(buffer.charAt(start - 1))) {
                start -= 1;
                if (position - start > 36.5) {
                    head = " ... ";
                    start += 5;
                    break;
                }
            }
            String tail = "";
            int end = position;
            while (end < buffer.length() && !isBreak3(buffer.charAt(end))) {
                end += 1;
                if (end - position > 36.5) {
                    tail = " ... ";
                    end -= 5;
                    break;
                }
            }
            return repeat(" ", 4) + head + EcmaScript.slice(buffer, start, end) + tail + "\n" +
                   repeat(" ", 4 + position - start + head.length()) + "^";
        }

        private boolean isBreak3(char c) {
            return c == 0 || c == '\r' || c == '\n' || c == 0x85 || c == 0x2028 || c == 0x2029;
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
            if (v4) {
                try {
                    prefix = UriComponent.decodeURIComponent(prefix);
                } catch (IllegalArgumentException e) {
                    throw error("tag prefix is malformed: " + prefix);
                }
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

        @SuppressWarnings("unchecked")
        void mergeMappings(Map<String, @Nullable Object> destination, @Nullable Object source,
                           Set<String> overridableKeys) {
            if (!(source instanceof Map) && !(source instanceof List) && !(source instanceof Timestamp) &&
                !(source instanceof byte[])) {
                throw error("cannot merge mappings; the provided source object is unacceptable");
            }
            for (String key : EcmaScript.ownKeys(source)) {
                if (!destination.containsKey(key)) {
                    Object value = source instanceof Map ? ((Map<String, Object>) source).get(key) :
                            source instanceof List ? ((List<Object>) source).get(Integer.parseInt(key)) : null;
                    destination.put(key, value);
                    overridableKeys.add(key);
                }
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, @Nullable Object> storeMappingPair(@Nullable Map<String, @Nullable Object> mapping,
                                                       Set<String> overridableKeys, @Nullable String keyTag,
                                                       @Nullable Object keyNode, @Nullable Object valueNode,
                                                       int startLine, int startLineStart, int startPos) {
            if (keyNode instanceof List) {
                List<@Nullable Object> copy = new ArrayList<>((List<Object>) keyNode);
                for (int index = 0; index < copy.size(); index++) {
                    if (copy.get(index) instanceof List) {
                        throw error("nested arrays are not supported inside keys");
                    }
                    if (copy.get(index) instanceof Map) {
                        copy.set(index, "[object Object]");
                    }
                }
                keyNode = copy;
            }
            String key = keyNode instanceof Map ? "[object Object]" : string(keyNode);
            if (mapping == null) {
                mapping = options.getObjects().get();
            }
            if (TAG_MERGE.equals(keyTag)) {
                if (valueNode instanceof List) {
                    for (Object source : (List<Object>) valueNode) {
                        mergeMappings(mapping, source, overridableKeys);
                    }
                } else {
                    mergeMappings(mapping, valueNode, overridableKeys);
                }
            } else {
                if (!options.isJson() && !overridableKeys.contains(key) && mapping.containsKey(key)) {
                    line = startLine != 0 ? startLine : line;
                    lineStart = v4 && startLineStart != 0 ? startLineStart : lineStart;
                    position = startPos != 0 ? startPos : position;
                    throw error("duplicated mapping key");
                }
                // 3.x assigns `__proto__` as {}.__proto__ = value, which makes no key
                if (v4 || !PROTO.equals(key)) {
                    mapping.put(key, valueNode);
                }
                overridableKeys.remove(key);
            }
            return mapping;
        }

        // 3.x's anchorMap is a {} as well
        void putAnchor(String name, @Nullable Object value) {
            if (v4 || !PROTO.equals(name)) {
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
            firstTabInLine = -1;
        }

        int skipSeparationSpace(boolean allowComments) {
            int lineBreaks = 0;
            int c = ch(position);
            while (c != 0) {
                while (isWhiteSpace(c)) {
                    if (c == 0x09 && firstTabInLine == -1) {
                        firstTabInLine = position;
                    }
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

        boolean readSingleQuotedScalar() {
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

        boolean readDoubleQuotedScalar() {
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
            Set<String> overridableKeys = new HashSet<>();
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
                collection = options.getObjects().get();
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
                } else if (v4 && c == ',') {
                    throw error("expected the node content, but found ','");
                }
                String keyTag;
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
                int savedLineStart = lineStart;
                int savedPos = position;
                composeNode(nodeIndent, CONTEXT_FLOW_IN, false, true);
                keyTag = tag;
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
                // 3.x reports a duplicated key where the reading stands, 4.1.0 where the key starts
                if (!v4) {
                    savedLine = savedLineStart = savedPos = 0;
                }
                if (isMapping) {
                    storeMappingPair((Map<String, @Nullable Object>) collection, overridableKeys, keyTag, keyNode,
                            valueNode, savedLine, savedLineStart, savedPos);
                } else if (isPair) {
                    ((List<@Nullable Object>) collection).add(storeMappingPair(null, overridableKeys, keyTag, keyNode,
                            valueNode, savedLine, savedLineStart, savedPos));
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
            if (v4 && firstTabInLine != -1) {
                return false;
            }
            if (anchor != null) {
                putAnchor(anchor, sequence);
            }
            int c = ch(position);
            while (c != 0) {
                if (v4 && firstTabInLine != -1) {
                    position = firstTabInLine;
                    throw error("tab characters must not be used in indentation");
                }
                if (c != '-' || !isWsOrEol(ch(position + 1))) {
                    break;
                }
                detected = true;
                position++;
                if (skipSeparationSpace(true) != 0) {
                    if (lineIndent <= nodeIndent) {
                        sequence.add(null);
                        c = ch(position);
                        continue;
                    }
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
            Map<String, @Nullable Object> mapping = options.getObjects().get();
            Set<String> overridableKeys = new HashSet<>();
            String keyTag = null;
            Object keyNode = null;
            Object valueNode = null;
            boolean atExplicitKey = false;
            boolean detected = false;
            boolean allowCompact = false;
            int keyLine = 0;
            int keyLineStart = 0;
            int keyPos = 0;
            if (v4 && firstTabInLine != -1) {
                return false;
            }
            if (anchor != null) {
                putAnchor(anchor, mapping);
            }
            int c = ch(position);
            while (c != 0) {
                if (v4 && !atExplicitKey && firstTabInLine != -1) {
                    position = firstTabInLine;
                    throw error("tab characters must not be used in indentation");
                }
                int following = ch(position + 1);
                int savedLine = line;
                int savedPos = position;
                if ((c == '?' || c == ':') && isWsOrEol(following)) {
                    if (c == '?') {
                        if (atExplicitKey) {
                            storeMappingPair(mapping, overridableKeys, keyTag, keyNode, null, v4 ? keyLine : 0,
                                    keyLineStart, v4 ? keyPos : 0);
                            keyTag = null;
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
                } else {
                    keyLine = line;
                    keyLineStart = lineStart;
                    keyPos = position;
                    if (!composeNode(flowIndent, CONTEXT_FLOW_OUT, false, true)) {
                        break;
                    }
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
                                storeMappingPair(mapping, overridableKeys, keyTag, keyNode, null, v4 ? keyLine : 0,
                                        keyLineStart, v4 ? keyPos : 0);
                                keyTag = null;
                                keyNode = valueNode = null;
                            }
                            detected = true;
                            atExplicitKey = false;
                            allowCompact = false;
                            keyTag = tag;
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
                }
                if (line == savedLine || lineIndent > nodeIndent) {
                    if (atExplicitKey) {
                        keyLine = line;
                        keyLineStart = lineStart;
                        keyPos = position;
                    }
                    if (composeNode(nodeIndent, CONTEXT_BLOCK_OUT, true, allowCompact)) {
                        if (atExplicitKey) {
                            keyNode = result;
                        } else {
                            valueNode = result;
                        }
                    }
                    if (!atExplicitKey) {
                        storeMappingPair(mapping, overridableKeys, keyTag, keyNode, valueNode,
                                v4 ? keyLine : savedLine, keyLineStart, v4 ? keyPos : savedPos);
                        keyTag = null;
                        keyNode = valueNode = null;
                    }
                    skipSeparationSpace(true);
                    c = ch(position);
                }
                if ((v4 && line == savedLine || lineIndent > nodeIndent) && c != 0) {
                    throw error("bad indentation of a mapping entry");
                } else if (lineIndent < nodeIndent) {
                    break;
                }
            }
            if (atExplicitKey) {
                storeMappingPair(mapping, overridableKeys, keyTag, keyNode, null, v4 ? keyLine : 0, keyLineStart,
                        v4 ? keyPos : 0);
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
            if (v4) {
                try {
                    tagName = UriComponent.decodeURIComponent(tagName);
                } catch (IllegalArgumentException e) {
                    throw error("tag name is malformed: " + tagName);
                }
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
            if (options.getRelease() == Release.V3_13_1 && anchorMap.containsKey("hasOwnProperty")) {
                throw new JsError(JsError.TYPE_ERROR, "state.anchorMap.hasOwnProperty is not a function");
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
                            readSingleQuotedScalar() || readDoubleQuotedScalar()) {
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
            if (tag == null) {
                if (v4 && anchor != null) {
                    putAnchor(anchor, result);
                }
            } else if ("?".equals(tag)) {
                if (options.getRelease() != Release.V3_13_1 && result != null && !"scalar".equals(kind)) {
                    throw error("unacceptable node kind for !<?> tag; it should be \"scalar\", not \"" + kind + "\"");
                }
                for (String type : v4 ? IMPLICIT : Collections.<String>emptyList()) {
                    if (resolve(type, result)) {
                        result = construct(type, result);
                        tag = type;
                        if (anchor != null) {
                            putAnchor(anchor, result);
                        }
                        break;
                    }
                }
            } else if (!"!".equals(tag)) {
                String typeKind = (v4 ? TYPE_MAP : FAILSAFE_TYPE_MAP).get(kind == null ? "fallback" : kind).get(tag);
                if (typeKind == null) {
                    throw error("unknown tag !<" + tag + ">");
                }
                if (result != null && !typeKind.equals(kind)) {
                    throw error("unacceptable node kind for !<" + tag + "> tag; it should be \"" + typeKind +
                                "\", not \"" + kind + "\"");
                }
                if (!resolve(tag, result)) {
                    throw error("cannot resolve a node with !<" + tag + "> explicit tag");
                } else {
                    result = result == null && TAG_MAP.equals(tag) ? options.getObjects().get() :
                            construct(tag, result);
                    if (anchor != null) {
                        putAnchor(anchor, result);
                    }
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

    /**
     * The {@code resolve} of a {@code DEFAULT_SCHEMA} type.
     */
    @SuppressWarnings("unchecked")
    static boolean resolve(String tag, @Nullable Object data) {
        switch (tag) {
            case TAG_NULL:
                return data == null || "~".equals(data) || "null".equals(data) || "Null".equals(data) ||
                       "NULL".equals(data);
            case TAG_BOOL:
                return data != null && ("true".equals(data) || "True".equals(data) || "TRUE".equals(data) ||
                                        "false".equals(data) || "False".equals(data) || "FALSE".equals(data));
            case TAG_INT:
                return data != null && resolveInteger((String) data);
            case TAG_FLOAT:
                if (data == null) {
                    return false;
                }
                String s = (String) data;
                return YAML_FLOAT_PATTERN.matcher(s).find() && s.charAt(s.length() - 1) != '_';
            case TAG_TIMESTAMP:
                return data != null && (YAML_DATE_REGEXP.matcher((String) data).find() ||
                                        YAML_TIMESTAMP_REGEXP.matcher((String) data).find());
            case TAG_MERGE:
                return data == null || "<<".equals(data);
            case TAG_BINARY:
                return data != null && resolveBinary((String) data);
            case TAG_OMAP:
                return data == null || resolveOmap((List<Object>) data);
            case TAG_PAIRS:
                if (data == null) {
                    return true;
                }
                for (Object pair : (List<Object>) data) {
                    if (!(pair instanceof Map) || ((Map<String, Object>) pair).size() != 1) {
                        return false;
                    }
                }
                return true;
            case TAG_SET:
                if (data == null) {
                    return true;
                }
                for (Object value : ((Map<String, Object>) data).values()) {
                    if (value != null) {
                        return false;
                    }
                }
                return true;
            default:
                return true;
        }
    }

    /**
     * The {@code construct} of a {@code DEFAULT_SCHEMA} type.
     */
    @SuppressWarnings("unchecked")
    static @Nullable Object construct(String tag, @Nullable Object data) {
        switch (tag) {
            case TAG_STR:
                return data != null ? data : "";
            case TAG_SEQ:
            case TAG_OMAP:
                return data != null ? data : new ArrayList<>();
            case TAG_MAP:
            case TAG_SET:
                return data != null ? data : new LinkedHashMap<>();
            case TAG_NULL:
                return null;
            case TAG_BOOL:
                return "true".equals(data) || "True".equals(data) || "TRUE".equals(data);
            case TAG_INT:
                return constructInteger((String) data);
            case TAG_FLOAT:
                return constructFloat((String) data);
            case TAG_TIMESTAMP:
                return constructTimestamp((String) data);
            case TAG_BINARY:
                return constructBinary((String) data);
            case TAG_PAIRS: {
                List<Object> result = new ArrayList<>();
                if (data == null) {
                    return result;
                }
                for (Object pair : (List<Object>) data) {
                    Map<String, Object> object = (Map<String, Object>) pair;
                    String key = EcmaScript.keys(object).get(0);
                    List<Object> entry = new ArrayList<>();
                    entry.add(key);
                    entry.add(object.get(key));
                    result.add(entry);
                }
                return result;
            }
            default:
                return data;
        }
    }

    private static boolean resolveInteger(String data) {
        int max = data.length();
        int index = 0;
        boolean hasDigits = false;
        if (max == 0) {
            return false;
        }
        char ch = data.charAt(index);
        if (ch == '-' || ch == '+') {
            ch = ++index < max ? data.charAt(index) : 0;
        }
        if (ch == '0') {
            if (index + 1 == max) {
                return true;
            }
            ch = data.charAt(++index);
            if (ch == 'b' || ch == 'x' || ch == 'o') {
                char base = ch;
                index++;
                for (; index < max; index++) {
                    ch = data.charAt(index);
                    if (ch == '_') {
                        continue;
                    }
                    boolean digit = base == 'b' ? ch == '0' || ch == '1' :
                            base == 'o' ? '0' <= ch && ch <= '7' :
                                    ('0' <= ch && ch <= '9') || ('A' <= ch && ch <= 'F') || ('a' <= ch && ch <= 'f');
                    if (!digit) {
                        return false;
                    }
                    hasDigits = true;
                }
                return hasDigits && ch != '_';
            }
        }
        if (ch == '_') {
            return false;
        }
        for (; index < max; index++) {
            ch = data.charAt(index);
            if (ch == '_') {
                continue;
            }
            if (ch < '0' || ch > '9') {
                return false;
            }
            hasDigits = true;
        }
        return hasDigits && ch != '_';
    }

    private static double constructInteger(String data) {
        String value = data.replace("_", "");
        double sign = 1;
        char ch = value.charAt(0);
        if (ch == '-' || ch == '+') {
            if (ch == '-') {
                sign = -1;
            }
            value = value.substring(1);
            ch = value.charAt(0);
        }
        if ("0".equals(value)) {
            return 0;
        }
        if (ch == '0') {
            if (value.charAt(1) == 'b') {
                return sign * EcmaScript.parseInt(value.substring(2), 2);
            }
            if (value.charAt(1) == 'x') {
                return sign * EcmaScript.parseInt(value.substring(2), 16);
            }
            if (value.charAt(1) == 'o') {
                return sign * EcmaScript.parseInt(value.substring(2), 8);
            }
        }
        return sign * EcmaScript.parseInt(value, 10);
    }

    private static double constructFloat(String data) {
        String value = data.replace("_", "").toLowerCase();
        double sign = value.charAt(0) == '-' ? -1 : 1;
        if ("+-".indexOf(value.charAt(0)) >= 0) {
            value = value.substring(1);
        }
        if (".inf".equals(value)) {
            return sign == 1 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        } else if (".nan".equals(value)) {
            return Double.NaN;
        }
        return sign * Double.parseDouble(value.startsWith(".") ? "0" + value : value);
    }

    private static Timestamp constructTimestamp(String data) {
        Matcher match = YAML_DATE_REGEXP.matcher(data);
        if (!match.find()) {
            match = YAML_TIMESTAMP_REGEXP.matcher(data);
            if (!match.find()) {
                throw new IllegalStateException("Date resolve error");
            }
        }
        double year = Integer.parseInt(match.group(1));
        double month = Integer.parseInt(match.group(2)) - 1;
        double day = Integer.parseInt(match.group(3));
        if (match.groupCount() < 4 || match.group(4) == null) {
            return new Timestamp(V8Date.utc(year, month, day, 0, 0, 0, 0));
        }
        double hour = Integer.parseInt(match.group(4));
        double minute = Integer.parseInt(match.group(5));
        double second = Integer.parseInt(match.group(6));
        double fraction = 0;
        if (match.group(7) != null && !match.group(7).isEmpty()) {
            StringBuilder f = new StringBuilder(match.group(7).length() > 3 ? match.group(7).substring(0, 3) :
                    match.group(7));
            while (f.length() < 3) {
                f.append('0');
            }
            fraction = Integer.parseInt(f.toString());
        }
        double delta = 0;
        if (match.group(9) != null) {
            double tzHour = Integer.parseInt(match.group(10));
            double tzMinute = match.group(11) == null ? 0 : Integer.parseInt(match.group(11));
            delta = (tzHour * 60 + tzMinute) * 60000;
            if ("-".equals(match.group(9))) {
                delta = -delta;
            }
        }
        double time = V8Date.utc(year, month, day, hour, minute, second, fraction);
        if (delta != 0) {
            time = V8Date.timeClip(time - delta);
        }
        return new Timestamp(time);
    }

    private static boolean resolveBinary(String data) {
        int bitlen = 0;
        for (int idx = 0; idx < data.length(); idx++) {
            int code = BASE64_MAP.indexOf(data.charAt(idx));
            if (code > 64) {
                continue;
            }
            if (code < 0) {
                return false;
            }
            bitlen += 6;
        }
        return bitlen % 8 == 0;
    }

    private static byte[] constructBinary(String data) {
        String input = data.replaceAll("[\\r\\n=]", "");
        int max = input.length();
        int bits = 0;
        List<Integer> result = new ArrayList<>();
        for (int idx = 0; idx < max; idx++) {
            if (idx % 4 == 0 && idx != 0) {
                result.add((bits >> 16) & 0xFF);
                result.add((bits >> 8) & 0xFF);
                result.add(bits & 0xFF);
            }
            bits = (bits << 6) | BASE64_MAP.indexOf(input.charAt(idx));
        }
        int tailbits = (max % 4) * 6;
        if (tailbits == 0) {
            result.add((bits >> 16) & 0xFF);
            result.add((bits >> 8) & 0xFF);
            result.add(bits & 0xFF);
        } else if (tailbits == 18) {
            result.add((bits >> 10) & 0xFF);
            result.add((bits >> 2) & 0xFF);
        } else if (tailbits == 12) {
            result.add((bits >> 4) & 0xFF);
        }
        byte[] bytes = new byte[result.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (int) result.get(i);
        }
        return bytes;
    }

    @SuppressWarnings("unchecked")
    private static boolean resolveOmap(List<Object> data) {
        List<String> objectKeys = new ArrayList<>();
        for (Object pair : data) {
            if (!(pair instanceof Map)) {
                return false;
            }
            Map<String, Object> object = (Map<String, Object>) pair;
            if (object.size() != 1) {
                return false;
            }
            String pairKey = EcmaScript.keys(object).get(0);
            if (objectKeys.contains(pairKey)) {
                return false;
            }
            objectKeys.add(pairKey);
        }
        return true;
    }

    // snippet.js makeSnippet with its defaults
    private static @Nullable String makeSnippet(String buffer, int position, int markLine) {
        int maxLength = 79;
        int indent = 1;
        int linesBefore = 3;
        int linesAfter = 2;
        Matcher re = Pattern.compile("\\r?\\n|\\r|\\x00").matcher(buffer);
        List<Integer> lineStarts = new ArrayList<>();
        List<Integer> lineEnds = new ArrayList<>();
        lineStarts.add(0);
        int foundLineNo = -1;
        while (re.find()) {
            lineEnds.add(re.start());
            lineStarts.add(re.end());
            if (position <= re.start() && foundLineNo < 0) {
                foundLineNo = lineStarts.size() - 2;
            }
        }
        if (foundLineNo < 0) {
            foundLineNo = lineStarts.size() - 1;
        }
        if (buffer.isEmpty()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        int lineNoLength = String.valueOf(Math.min(markLine + linesAfter, lineEnds.size())).length();
        int maxLineLength = maxLength - (indent + lineNoLength + 3);
        for (int i = 1; i <= linesBefore; i++) {
            if (foundLineNo - i < 0) {
                break;
            }
            String[] line = getLine(buffer, lineStarts.get(foundLineNo - i), lineEnds.get(foundLineNo - i),
                    position - (lineStarts.get(foundLineNo) - lineStarts.get(foundLineNo - i)), maxLineLength);
            result.insert(0, repeat(" ", indent) + padStart(String.valueOf(markLine - i + 1), lineNoLength) +
                             " | " + line[0] + "\n");
        }
        String[] line = getLine(buffer, lineStarts.get(foundLineNo),
                foundLineNo < lineEnds.size() ? lineEnds.get(foundLineNo) : buffer.length(), position, maxLineLength);
        result.append(repeat(" ", indent)).append(padStart(String.valueOf(markLine + 1), lineNoLength))
                .append(" | ").append(line[0]).append('\n');
        result.append(repeat("-", indent + lineNoLength + 3 + Integer.parseInt(line[1]))).append("^\n");
        for (int i = 1; i <= linesAfter; i++) {
            if (foundLineNo + i >= lineEnds.size()) {
                break;
            }
            String[] after = getLine(buffer, lineStarts.get(foundLineNo + i), lineEnds.get(foundLineNo + i),
                    position - (lineStarts.get(foundLineNo) - lineStarts.get(foundLineNo + i)), maxLineLength);
            result.append(repeat(" ", indent)).append(padStart(String.valueOf(markLine + i + 1), lineNoLength))
                    .append(" | ").append(after[0]).append('\n');
        }
        String out = result.toString();
        return out.endsWith("\n") ? out.substring(0, out.length() - 1) : out;
    }

    private static String[] getLine(String buffer, int lineStart, int lineEnd, int position, int maxLineLength) {
        String head = "";
        String tail = "";
        int maxHalfLength = (int) Math.floor(maxLineLength / 2.0) - 1;
        if (position - lineStart > maxHalfLength) {
            head = " ... ";
            lineStart = position - maxHalfLength + head.length();
        }
        if (lineEnd - position > maxHalfLength) {
            tail = " ...";
            lineEnd = position + maxHalfLength - tail.length();
        }
        String str = head + EcmaScript.slice(buffer, lineStart, lineEnd).replace('\t', '→') + tail;
        return new String[]{str, String.valueOf(position - lineStart + head.length())};
    }

    private static String padStart(String s, int max) {
        return repeat(" ", max - s.length()) + s;
    }

    /**
     * {@code String(value)} of a loaded value; a date's text follows the host's time zone, which is cannot-know.
     */
    public static String string(@Nullable Object value) {
        if (holdsDate(value)) {
            throw new CannotKnow("the text of a date (Date.prototype.toString follows the host's time zone)");
        }
        return EcmaScript.string(value);
    }

    private static boolean holdsDate(@Nullable Object value) {
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (holdsDate(item)) {
                    return true;
                }
            }
        }
        return value instanceof Timestamp;
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

    private static String charFromCodepoint(int c) {
        if (c <= 0xFFFF) {
            return String.valueOf((char) c);
        }
        return new String(new char[]{(char) (((c - 0x010000) >> 10) + 0xD800),
                (char) (((c - 0x010000) & 0x03FF) + 0xDC00)});
    }

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
