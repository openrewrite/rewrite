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

import lombok.Builder;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of braces' expansion: 1.8.5 with expand-range 1.8.2 and preserve 0.2.0 (yarn 1.22.22's micromatch 2.3.11),
 * and 3.0.2 under {@code expand: true} (micromatch 4.0.5 in Yarn 4.10.3's fast-glob), each over its fill-range.
 */
public final class Braces {

    public enum Release {
        V1_8_5,
        V3_0_2
    }

    /**
     * The options each release reads: {@code nodupes} (which 1.8.5 takes as true when unset), 1.8.5's {@code makeRe}
     * and 3.0.2's {@code keepEscaping}.
     */
    @Value
    @Builder
    public static class Options {
        Release release;

        @Nullable
        Boolean nodupes;

        boolean makeRe;
        boolean keepEscaping;

        public static OptionsBuilder builder(Release release) {
            return new OptionsBuilder().release(release);
        }

        // declared so that javadoc, which reads the source before Lombok, finds it
        public static class OptionsBuilder {
        }
    }

    private static final Pattern PATTERN_RE = V8RegExp.of("\\${|( (?=[{,}])|(?=[{,}]) )|{}|{,}|\\\\,(?=.*[{}])|" +
                                                          "\\/\\.(?=.*[{}])|\\\\\\.(?={)|\\\\{|\\\\}");
    private static final Pattern BRACE_RE = V8RegExp.of(".*(\\\\?\\{([^}]+)\\})");
    private static final Pattern ES6_RE = V8RegExp.of("\\$\\{([^}]+)\\}");
    private static final Pattern NESTED = V8RegExp.of("\\{[^{]+\\{");
    private static final Pattern QUOTE = V8RegExp.of("['\"]");
    private static final Pattern DOT_SEGMENT = V8RegExp.of("(\\.[^.\\/])");
    private static final Pattern INNER_BRACES = V8RegExp.of("\\{[^{}]+?\\}");
    private static final Pattern ESCAPED_DOTS = V8RegExp.of("[^\\\\]\\..+\\\\\\.");
    private static final Pattern WORD_COMMA = V8RegExp.of("\\w,");
    private static final Pattern PRESERVED = V8RegExp.of("__ID(.{5})__");

    private static final int MAX_LENGTH = 1024 * 64;
    private static final int RANGE_LIMIT = 1000;

    // 1.8.5's preserve tokens; it draws a random five-digit id for each
    private final Map<String, String> preserved = new HashMap<>();

    /**
     * {@code braces(str, options)}, for 3.0.2 with {@code expand: true}.
     */
    public List<String> braces(String str, Options options) {
        if (options.getRelease() == Release.V1_8_5) {
            return braces185(str, null, new Opts185(options));
        }
        List<String> output = str.length() < 3 ? Collections.singletonList(str) : expand(str, options);
        return Boolean.TRUE.equals(options.getNodupes()) ? new ArrayList<>(new LinkedHashSet<>(output)) :
                new ArrayList<>(output);
    }

    /**
     * 3.0.2's {@code braces.expand(input, options)}.
     */
    public static List<String> expand(String input, Options options) {
        List<String> result = new ArrayList<>();
        for (Object item : walk(parse(input, options.isKeepEscaping()), null)) {
            flatten(item, result);
        }
        return Boolean.TRUE.equals(options.getNodupes()) ? new ArrayList<>(new LinkedHashSet<>(result)) : result;
    }

    /**
     * The ids of 1.8.5's preserve that a regex source still holds, which yarn draws from {@code Math.random()}.
     */
    public List<String> preservedIds(String source) {
        List<String> ids = new ArrayList<>();
        for (String id : preserved.keySet()) {
            if (source.contains("__ID" + id + "__")) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static final class Opts185 {
        final boolean nodupes;
        final boolean makeRe;

        Opts185(@Nullable Options options) {
            nodupes = options == null || options.getNodupes() == null || options.getNodupes();
            makeRe = options != null && options.isMakeRe();
        }
    }

    private List<String> braces185(String str, @Nullable List<String> arrArg, Opts185 opts) {
        if (str.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> arr = arrArg == null ? new ArrayList<>() : arrArg;
        boolean es6 = false;

        Matcher matches = PATTERN_RE.matcher(str);
        String m = matches.find() ? matches.group() : "";
        switch (m) {
            case "\\,":
                return escapeCommas(str, arr, opts);
            case "\\.":
                return escapeDots(str, arr, opts);
            case "/.":
                return escapePaths(str, arr, opts);
            case " ":
                return splitWhitespace(str);
            case "{,}":
                return exponential(str, opts);
            case "{}":
                return braces185(splitJoin(str, "{}", "\\{\\}"), arr, opts);
            case "\\{":
            case "\\}":
                return escapeBraces(str, arr, opts);
            case "${":
                if (!NESTED.matcher(str).find()) {
                    return concat(arr, str);
                }
                es6 = true;
                str = V8RegExp.replace(str, ES6_RE, false, match -> {
                    String id = String.format("%05d", preserved.size());
                    preserved.put(id, match.group());
                    return "__ID" + id + "__";
                });
                break;
            default:
                break;
        }

        Matcher match = BRACE_RE.matcher(str);
        if (!match.find()) {
            return new ArrayList<>(Arrays.asList(str));
        }
        String outter = match.group(1);
        String inner = match.group(2);

        List<String> segs;
        int segsLength;
        if (inner.contains("..")) {
            Object range = expandRange(inner, opts);
            if (range == null) {
                segs = new ArrayList<>(Arrays.asList(EcmaScript.splitString(inner, ",")));
                segsLength = segs.size();
            } else if (range instanceof String) {
                segs = new ArrayList<>();
                for (char c : ((String) range).toCharArray()) {
                    segs.add(String.valueOf(c));
                }
                segsLength = ((String) range).length();
            } else {
                @SuppressWarnings("unchecked")
                List<String> expanded = (List<String>) range;
                segs = expanded;
                segsLength = segs.size();
            }
        } else if (inner.charAt(0) == '"' || inner.charAt(0) == '\'') {
            return concat(arr, QUOTE.matcher(str).replaceAll(""));
        } else {
            segs = new ArrayList<>(Arrays.asList(EcmaScript.splitString(inner, ",")));
            if (opts.makeRe) {
                return braces185(V8RegExp.replace(str, outter, "(" + String.join("|", segs) + ")"), null, opts);
            }
            segsLength = segs.size();
        }

        for (String path : segs) {
            if (DOT_SEGMENT.matcher(path).find()) {
                return segsLength > 1 ? segs : new ArrayList<>(Arrays.asList(str));
            }
            String val = splice(str, outter, path);
            if (INNER_BRACES.matcher(val).find()) {
                arr = braces185(val, arr, opts);
            } else if (!val.isEmpty()) {
                if (opts.nodupes && arr.contains(val)) {
                    continue;
                }
                arr.add(es6 ? after(val) : val);
            }
        }
        return arr;
    }

    private String after(String str) {
        return V8RegExp.replace(str, PRESERVED, true, m -> {
            String token = preserved.get(m.group(1));
            return token == null ? "undefined" : token;
        });
    }

    private static List<String> concat(List<String> arr, String value) {
        List<String> out = new ArrayList<>(arr);
        out.add(value);
        return out;
    }

    private static List<String> map(List<String> arr, UnaryOperator<String> fn) {
        List<String> out = new ArrayList<>(arr.size());
        for (String ele : arr) {
            out.add(fn.apply(ele));
        }
        return out;
    }

    private static String splitJoin(String s, String separator, String with) {
        return String.join(with, EcmaScript.splitString(s, separator));
    }

    private List<String> splitWhitespace(String str) {
        List<String> res = new ArrayList<>();
        for (String seg : EcmaScript.splitString(str, " ")) {
            res.addAll(braces185(seg, null, new Opts185(null)));
        }
        return res;
    }

    private List<String> exponential(String str, Opts185 opts) {
        String esc = "__ESC_EXP__";
        String[] parts = EcmaScript.splitString(str, "{,}");
        if (opts.nodupes) {
            return braces185(String.join("", parts), null, opts);
        }
        double num = Math.pow(2, parts.length - 1);
        List<String> arr = new ArrayList<>();
        for (String ele : braces185(String.join(esc, parts), null, opts)) {
            if (!ele.contains(esc)) {
                arr.add(ele);
                continue;
            }
            ele = splitJoin(ele, esc, "");
            for (int i = 0; i < num; i++) {
                arr.add(ele);
            }
        }
        return arr;
    }

    private List<String> escapeBraces(String str, List<String> arr, Opts185 opts) {
        if (!NESTED.matcher(str).find()) {
            return concat(arr, splitJoin(str, "\\", ""));
        }
        str = splitJoin(str, "\\{", "__LT_BRACE__");
        str = splitJoin(str, "\\}", "__RT_BRACE__");
        return map(braces185(str, arr, opts), ele -> splitJoin(splitJoin(ele, "__LT_BRACE__", "{"),
                "__RT_BRACE__", "}"));
    }

    private List<String> escapeDots(String str, List<String> arr, Opts185 opts) {
        if (!ESCAPED_DOTS.matcher(str).find()) {
            return concat(arr, splitJoin(str, "\\", ""));
        }
        str = splitJoin(str, "\\.", "__ESC_DOT__");
        return map(braces185(str, arr, opts), ele -> splitJoin(ele, "__ESC_DOT__", "."));
    }

    private List<String> escapePaths(String str, List<String> arr, Opts185 opts) {
        str = splitJoin(str, "/.", "__ESC_PATH__");
        return map(braces185(str, arr, opts), ele -> splitJoin(ele, "__ESC_PATH__", "/."));
    }

    private List<String> escapeCommas(String str, List<String> arr, Opts185 opts) {
        if (!WORD_COMMA.matcher(str).find()) {
            return concat(arr, splitJoin(str, "\\", ""));
        }
        str = splitJoin(str, "\\,", "__ESC_COMMA__");
        return map(braces185(str, arr, opts), ele -> splitJoin(ele, "__ESC_COMMA__", ","));
    }

    private static String splice(String str, String token, String replacement) {
        int i = str.indexOf(token);
        return str.substring(0, i) + replacement + str.substring(i + token.length());
    }

    // expand-range 1.8.2: the range's expansion, the range itself when it has more than three parts, or null
    private static @Nullable Object expandRange(String str, Opts185 opts) {
        String[] args = EcmaScript.splitString(str, "..");
        if (args.length > 3) {
            return str;
        }
        return FillRange.fill(args[0], args[1], args.length == 3 ? args[2] : null,
                new FillRange.Options(FillRange.Release.V2_2_4, opts.makeRe));
    }

    private static final class Node {
        String type;
        @Nullable String value;
        @Nullable List<Node> nodes;
        @Nullable Node parent;
        boolean dollar;
        @Nullable Integer commas;
        @Nullable Integer ranges;
        boolean invalid;
        @Nullable List<Object> queue;

        Node(String type, @Nullable String value) {
            this.type = type;
            this.value = value;
        }

        static Node block(String type) {
            Node node = new Node(type, null);
            node.nodes = new ArrayList<>();
            return node;
        }
    }

    private static Node parse(String input, boolean keepEscaping) {
        if (input.length() > MAX_LENGTH) {
            throw new JsError(JsError.SYNTAX_ERROR, "Input length (" + input.length() + "), exceeds max characters (" +
                                                    MAX_LENGTH + ")");
        }
        return new Parser(input, keepEscaping).parse();
    }

    private static final class Parser {
        private final String input;
        private final boolean keepEscaping;
        private final int length;
        private final Node ast = Node.block("root");
        private final List<Node> stack = new ArrayList<>();
        private Node block = ast;
        private Node prev = ast;
        private int index;
        private int depth;

        Parser(String input, boolean keepEscaping) {
            this.input = input;
            this.keepEscaping = keepEscaping;
            this.length = input.length();
        }

        Node parse() {
            stack.add(ast);
            push(new Node("bos", null));

            while (index < length) {
                block = stack.get(stack.size() - 1);
                String value = advance();

                if ("\ufeff".equals(value) || "\u00a0".equals(value)) {
                    continue;
                }

                if ("\\".equals(value)) {
                    push(new Node("text", (keepEscaping ? value : "") + str(advance())));
                    continue;
                }

                if ("]".equals(value)) {
                    push(new Node("text", "\\" + value));
                    continue;
                }

                if ("[".equals(value)) {
                    int brackets = 1;
                    StringBuilder text = new StringBuilder(value);
                    String next;
                    while (index < length && (next = advance()) != null) {
                        text.append(next);
                        if ("[".equals(next)) {
                            brackets++;
                            continue;
                        }
                        if ("\\".equals(next)) {
                            text.append(str(advance()));
                            continue;
                        }
                        if ("]".equals(next)) {
                            brackets--;
                            if (brackets == 0) {
                                break;
                            }
                        }
                    }
                    push(new Node("text", text.toString()));
                    continue;
                }

                if ("(".equals(value)) {
                    block = push(Node.block("paren"));
                    stack.add(block);
                    push(new Node("text", value));
                    continue;
                }

                if (")".equals(value)) {
                    if (!"paren".equals(block.type)) {
                        push(new Node("text", value));
                        continue;
                    }
                    block = stack.remove(stack.size() - 1);
                    push(new Node("text", value));
                    block = stack.get(stack.size() - 1);
                    continue;
                }

                if ("\"".equals(value) || "'".equals(value) || "`".equals(value)) {
                    StringBuilder text = new StringBuilder();
                    String next;
                    while (index < length && (next = advance()) != null) {
                        if ("\\".equals(next)) {
                            text.append(next).append(str(advance()));
                            continue;
                        }
                        if (value.equals(next)) {
                            break;
                        }
                        text.append(next);
                    }
                    push(new Node("text", text.toString()));
                    continue;
                }

                if ("{".equals(value)) {
                    depth++;
                    Node brace = Node.block("brace");
                    brace.dollar = prev.value != null && prev.value.endsWith("$") || block.dollar;
                    brace.commas = 0;
                    brace.ranges = 0;
                    block = push(brace);
                    stack.add(block);
                    push(new Node("open", value));
                    continue;
                }

                if ("}".equals(value)) {
                    if (!"brace".equals(block.type)) {
                        push(new Node("text", value));
                        continue;
                    }
                    block = stack.remove(stack.size() - 1);
                    push(new Node("close", value));
                    depth--;
                    block = stack.get(stack.size() - 1);
                    continue;
                }

                if (",".equals(value) && depth > 0) {
                    if (block.ranges != null && block.ranges > 0) {
                        block.ranges = 0;
                        Node open = block.nodes.remove(0);
                        block.nodes = new ArrayList<>(Arrays.asList(open, new Node("text", stringify(block))));
                    }
                    push(new Node("comma", value));
                    block.commas = block.commas == null ? null : block.commas + 1;
                    continue;
                }

                if (".".equals(value) && depth > 0 && Integer.valueOf(0).equals(block.commas)) {
                    List<Node> siblings = block.nodes;
                    if (siblings.isEmpty()) {
                        push(new Node("text", value));
                        continue;
                    }
                    if ("dot".equals(prev.type)) {
                        prev.value += value;
                        prev.type = "range";
                        if (block.nodes.size() != 3 && block.nodes.size() != 5) {
                            block.invalid = true;
                            block.ranges = 0;
                            prev.type = "text";
                            continue;
                        }
                        block.ranges = block.ranges == null ? null : block.ranges + 1;
                        continue;
                    }
                    if ("range".equals(prev.type)) {
                        siblings.remove(siblings.size() - 1);
                        Node before = siblings.get(siblings.size() - 1);
                        before.value = str(before.value) + prev.value + value;
                        prev = before;
                        block.ranges = block.ranges == null ? null : block.ranges - 1;
                        continue;
                    }
                    push(new Node("dot", value));
                    continue;
                }

                push(new Node("text", value));
            }

            do {
                block = stack.remove(stack.size() - 1);
                if (!"root".equals(block.type)) {
                    for (Node node : block.nodes) {
                        if (node.nodes == null) {
                            node.type = "text";
                            node.invalid = true;
                        }
                    }
                    Node parent = stack.get(stack.size() - 1);
                    int at = parent.nodes.indexOf(block);
                    if (at < 0) {
                        at = Math.max(parent.nodes.size() + at, 0);
                    }
                    if (at < parent.nodes.size()) {
                        parent.nodes.remove(at);
                    }
                    parent.nodes.addAll(at, block.nodes);
                }
            } while (!stack.isEmpty());

            push(new Node("eos", null));
            return ast;
        }

        private @Nullable String advance() {
            return index < length ? String.valueOf(input.charAt(index++)) : advancePastEnd();
        }

        private @Nullable String advancePastEnd() {
            index++;
            return null;
        }

        private @Nullable Node push(Node node) {
            if ("text".equals(node.type) && "dot".equals(prev.type)) {
                prev.type = "text";
            }
            if ("text".equals(prev.type) && "text".equals(node.type)) {
                prev.value = str(prev.value) + node.value;
                return null;
            }
            block.nodes.add(node);
            node.parent = block;
            prev = node;
            return node;
        }
    }

    private static List<Object> walk(Node node, @Nullable Node parent) {
        node.queue = new ArrayList<>();
        Node p = parent;
        List<Object> q = parent == null ? null : parent.queue;
        while (p != null && !"brace".equals(p.type) && !"root".equals(p.type) && p.parent != null) {
            p = p.parent;
            q = p.queue;
        }

        if (node.invalid || node.dollar) {
            push(q, append(pop(q), stringify(node), false));
            return Collections.emptyList();
        }

        if ("brace".equals(node.type) && node.nodes.size() == 2) {
            push(q, append(pop(q), Collections.singletonList("{}"), false));
            return Collections.emptyList();
        }

        if (node.nodes != null && node.ranges != null && node.ranges > 0) {
            List<String> args = reduce(node.nodes);
            if (exceedsLimit(args)) {
                throw new JsError(JsError.RANGE_ERROR, "expanded array length exceeds range limit. Use " +
                                                       "options.rangeLimit to increase or disable the limit.");
            }
            Object range = args.size() < 2 ? Collections.emptyList() : FillRange.fill(args.get(0), args.get(1),
                    args.size() > 2 ? args.get(2) : null, new FillRange.Options(FillRange.Release.V7_0_1, false));
            if (((List<?>) range).isEmpty()) {
                range = stringify(node);
            }
            push(q, append(pop(q), range, false));
            node.nodes = new ArrayList<>();
            return Collections.emptyList();
        }

        boolean enclose = encloseBrace(node);
        List<Object> queue = node.queue;
        Node block = node;
        while (!"brace".equals(block.type) && !"root".equals(block.type) && block.parent != null) {
            block = block.parent;
            queue = block.queue;
        }

        for (int i = 0; i < node.nodes.size(); i++) {
            Node child = node.nodes.get(i);
            if ("comma".equals(child.type) && "brace".equals(node.type)) {
                if (i == 1) {
                    push(queue, "");
                }
                push(queue, "");
                continue;
            }
            if ("close".equals(child.type)) {
                push(q, append(pop(q), queue, enclose));
                continue;
            }
            if (child.value != null && !child.value.isEmpty() && !"open".equals(child.type)) {
                push(queue, append(pop(queue), child.value, false));
                continue;
            }
            if (child.nodes != null) {
                walk(child, node);
            }
        }
        return queue;
    }

    // a block spliced out of the tree at the end of parse was never walked, and has no queue
    private static void push(@Nullable List<Object> queue, Object item) {
        if (queue == null) {
            throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of undefined (reading 'push')");
        }
        queue.add(item);
    }

    private static @Nullable Object pop(@Nullable List<Object> list) {
        return list == null || list.isEmpty() ? null : list.remove(list.size() - 1);
    }

    private static List<Object> append(@Nullable Object queueArg, Object stashArg, boolean enclose) {
        List<Object> queue = concat(queueArg == null ? "" : queueArg);
        List<Object> stash = concat(stashArg);
        if (stash.isEmpty()) {
            return queue;
        }
        if (queue.isEmpty()) {
            if (!enclose) {
                return stash;
            }
            List<String> flat = new ArrayList<>();
            flatten(stash, flat);
            List<Object> out = new ArrayList<>();
            for (String ele : flat) {
                out.add("{" + ele + "}");
            }
            return out;
        }
        List<Object> result = new ArrayList<>();
        for (Object item : queue) {
            if (item instanceof List) {
                for (Object value : (List<?>) item) {
                    result.add(append(value, stash, enclose));
                }
            } else {
                for (Object ele : stash) {
                    if (enclose && ele instanceof String) {
                        ele = "{" + ele + "}";
                    }
                    result.add(ele instanceof List ? append(item, ele, enclose) : str((String) item) + ele);
                }
            }
        }
        List<String> flat = new ArrayList<>();
        flatten(result, flat);
        return new ArrayList<>(flat);
    }

    // [].concat(value)
    private static List<Object> concat(Object value) {
        return value instanceof List ? new ArrayList<>((List<?>) value) :
                new ArrayList<>(Collections.singletonList(value));
    }

    private static void flatten(@Nullable Object value, List<String> out) {
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                flatten(item, out);
            }
        } else if (value != null) {
            out.add((String) value);
        }
    }

    private static String stringify(Node node) {
        if (node.value != null && !node.value.isEmpty()) {
            return node.value;
        }
        StringBuilder output = new StringBuilder();
        if (node.nodes != null) {
            for (Node child : node.nodes) {
                output.append(stringify(child));
            }
        }
        return output.toString();
    }

    // utils.encloseBrace, whose (commas >> 0 + ranges >> 0) shifts commas by ranges
    private static boolean encloseBrace(Node node) {
        if (!"brace".equals(node.type)) {
            return false;
        }
        if ((node.commas >> node.ranges) == 0) {
            node.invalid = true;
            return true;
        }
        return false;
    }

    private static List<String> reduce(List<Node> nodes) {
        List<String> acc = new ArrayList<>();
        for (Node node : nodes) {
            if ("text".equals(node.type)) {
                acc.add(node.value);
            }
            if ("range".equals(node.type)) {
                node.type = "text";
            }
        }
        return acc;
    }

    // utils.exceedsLimit(min, max, step = 1, limit) called with (...args, options.step, rangeLimit)
    private static boolean exceedsLimit(List<String> args) {
        List<@Nullable Object> positional = new ArrayList<>(args);
        positional.add(null);
        positional.add((double) RANGE_LIMIT);
        Object min = positional.get(0);
        Object max = positional.get(1);
        Object step = positional.size() > 2 && positional.get(2) != null ? positional.get(2) : (Object) 1.0;
        Object limit = positional.size() > 3 ? positional.get(3) : null;
        if (!isInteger(min) || !isInteger(max)) {
            return false;
        }
        return limit != null && (EcmaScript.toNumber(max) - EcmaScript.toNumber(min)) / EcmaScript.toNumber(step) >=
                                EcmaScript.toNumber(limit);
    }

    private static boolean isInteger(@Nullable Object num) {
        if (num instanceof Double) {
            return EcmaScript.isInteger((Double) num);
        }
        return num instanceof String && !EcmaScript.trim((String) num).isEmpty() &&
               EcmaScript.isInteger(EcmaScript.toNumber((String) num));
    }

    private static String str(@Nullable String s) {
        return s == null ? "undefined" : s;
    }
}
