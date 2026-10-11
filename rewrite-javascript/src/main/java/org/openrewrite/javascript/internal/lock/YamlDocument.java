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

import org.jspecify.annotations.Nullable;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;

/**
 * Port of yaml 2.9.0's parseDocument (its lexer's tokens read from an OpenRewrite YAML tree) and Document.toString,
 * with @pnpm/yaml.document-sync's patchDocument, as pnpm writes pnpm-workspace.yaml.
 */
public final class YamlDocument {

    private static final Pattern PLAIN_UNSAFE = Pattern.compile(
            "^[\\n\\t ,\\[\\]{}#&*!|>'\"%@`]|^[?-]$|^[?-][ \\t]|[\\n:][ \\t]|[ \\t]\\n|[\\n\\t ]#|[\\n\\t :]$");
    private static final Pattern CORE_SCHEMA = Pattern.compile(
            "^(?:~|[Nn]ull|NULL)?$|^(?:[Tt]rue|TRUE|[Ff]alse|FALSE)$|^0o[0-7]+$|^[-+]?[0-9]+$|^0x[0-9a-fA-F]+$|" +
            "^(?:[-+]?\\.(?:inf|Inf|INF)|\\.nan|\\.NaN|\\.NAN)$|" +
            "^[-+]?(?:\\.[0-9]+|[0-9]+(?:\\.[0-9]*)?)[eE][-+]?[0-9]+$|^[-+]?(?:\\.[0-9]+|[0-9]+\\.[0-9]*)$");

    private static final String INDENT_STEP = "  ";
    private static final Pattern NULL = Pattern.compile("^(?:~|[Nn]ull|NULL)?$");
    private static final Pattern BOOL = Pattern.compile("^(?:[Tt]rue|TRUE|[Ff]alse|FALSE)$");
    private static final Pattern INT_OCT = Pattern.compile("^0o[0-7]+$");
    private static final Pattern INT = Pattern.compile("^[-+]?[0-9]+$");
    private static final Pattern INT_HEX = Pattern.compile("^0x[0-9a-fA-F]+$");
    private static final Pattern FLOAT_NAN = Pattern.compile("^(?:[-+]?\\.(?:inf|Inf|INF)|\\.nan|\\.NaN|\\.NAN)$");
    private static final Pattern FLOAT_EXP = Pattern.compile(
            "^[-+]?(?:\\.[0-9]+|[0-9]+(?:\\.[0-9]*)?)[eE][-+]?[0-9]+$");
    private static final Pattern FLOAT = Pattern.compile("^[-+]?(?:\\.[0-9]+|[0-9]+\\.[0-9]*)$");
    private static final Pattern CONTROL = Pattern.compile("[\\x00-\\x08\\x0b-\\x1f\\x7f-\\x9f\\ud800-\\udfff]");
    private static final Pattern FLOW_INDICATOR = Pattern.compile("[\\[\\]{},]");
    private static final Pattern DOCUMENT_MARKER = Pattern.compile("^(%|---|\\.\\.\\.)");

    private @Nullable String commentBefore;
    private @Nullable Node contents;
    private @Nullable String comment;

    /**
     * The document yaml composes from {@code docs}; an {@link IllegalArgumentException} names what is not read here.
     */
    public static YamlDocument parse(Yaml.Documents docs) {
        Parser parser = new Parser();
        new Lexer(parser).lex(docs);
        return new Composer().compose(parser.document, parser.prelude);
    }

    /**
     * {@code document.toJSON()}: maps in document order, strings, numbers as doubles, booleans and null.
     */
    public @Nullable Object toJs() {
        return contents == null ? null : contents.toJs();
    }

    /**
     * patchDocument: the document changed in place to hold {@code target}, comments kept on the nodes it keeps.
     */
    public void patch(@Nullable Object target) {
        contents = patchNode(contents, target);
    }

    /**
     * The top-level keys in order; none when the document holds no mapping.
     */
    public List<String> keys() {
        List<String> keys = new ArrayList<>();
        for (Pair pair : contents instanceof YamlMap ? ((YamlMap) contents).items : new ArrayList<Pair>()) {
            keys.add((String) pair.key.value);
        }
        return keys;
    }

    /**
     * Whether the top-level key at {@code index} has a blank line before it ({@code spaceBefore}).
     */
    public boolean blankBefore(int index) {
        return contents instanceof YamlMap && ((YamlMap) contents).items.get(index).key.spaceBefore;
    }

    /**
     * Gives the top-level key at {@code index} a blank line before it.
     */
    public void blankLineBefore(int index) {
        if (contents instanceof YamlMap) {
            ((YamlMap) contents).items.get(index).key.spaceBefore = true;
        }
    }

    // stringifyDocument, with lineWidth 0 and singleQuote true
    @Override
    public String toString() {
        if (contents == null) {
            throw unsupported("no content");
        }
        List<String> lines = new ArrayList<>();
        Context ctx = new Context("", false, false);
        if (commentBefore != null) {
            lines.add("");
            lines.add(0, indentComment(commentString(commentBefore), ""));
        }
        if (contents.commentBefore != null) {
            lines.add(indentComment(commentString(contents.commentBefore), ""));
        }
        String body = contents.stringify(ctx);
        if (contents.comment != null && !contents.printsComment(ctx)) {
            body += lineComment(body, "", commentString(contents.comment));
        }
        lines.add(body);
        if (comment != null) {
            if (!lines.get(lines.size() - 1).isEmpty()) {
                lines.add("");
            }
            lines.add(indentComment(commentString(comment), ""));
        }
        return String.join("\n", lines) + "\n";
    }

    private static IllegalArgumentException unsupported(String what) {
        return new IllegalArgumentException("pnpm-workspace.yaml has " + what +
                                            ", which is not laid out here as pnpm's yaml writes it");
    }

    private static @Nullable Node patchNode(@Nullable Node node, @Nullable Object target) {
        if (node == null) {
            return createNode(target);
        }
        if (target == null) {
            return null;
        }
        return node.patched(target);
    }

    private static Node createNode(@Nullable Object value) {
        if (value instanceof Map) {
            YamlMap map = new YamlMap(false);
            for (String key : EcmaScript.keys(asMap(value))) {
                map.items.add(new Pair(new Scalar(key), createNode(asMap(value).get(key))));
            }
            return map;
        }
        if (value instanceof List) {
            YamlSeq seq = new YamlSeq(false);
            for (Object item : (List<?>) value) {
                seq.items.add(createNode(item));
            }
            return seq;
        }
        return new Scalar(value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> asMap(Object value) {
        return (Map<String, @Nullable Object>) value;
    }

    // stringifyComment
    private static String commentString(String comment) {
        StringBuilder out = new StringBuilder();
        String[] lines = comment.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                out.append('\n');
            }
            if (!lines[i].isEmpty()) {
                out.append('#').append(" ".equals(lines[i]) ? "" : lines[i]);
            }
        }
        return out.toString();
    }

    private static String indentComment(String comment, String indent) {
        if (comment.matches("\n+")) {
            return comment.substring(1);
        }
        if (indent.isEmpty()) {
            return comment;
        }
        StringBuilder out = new StringBuilder();
        String[] lines = comment.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                out.append('\n');
            }
            out.append(lines[i].trim().isEmpty() && lines[i].indexOf('\t') < 0 ? "" : indent).append(lines[i]);
        }
        return out.toString();
    }

    private static String lineComment(String str, String indent, String comment) {
        if (str.endsWith("\n")) {
            return indentComment(comment, indent);
        }
        if (comment.contains("\n")) {
            return "\n" + indentComment(comment, indent);
        }
        return (str.endsWith(" ") ? "" : " ") + comment;
    }

    /**
     * The stringify context: the indent of what is written, whether inside a flow collection, whether a key.
     */
    private static final class Context {
        final String indent;
        final boolean inFlow;
        final boolean implicitKey;

        Context(String indent, boolean inFlow, boolean implicitKey) {
            this.indent = indent;
            this.inFlow = inFlow;
            this.implicitKey = implicitKey;
        }
    }

    /**
     * A node with the blank line and comment before it and the comment after it.
     */
    private abstract static class Node {
        boolean spaceBefore;
        @Nullable String commentBefore;
        @Nullable String comment;

        abstract @Nullable Object toJs();

        abstract @Nullable Node patched(Object target);

        abstract String stringify(Context ctx);

        // whether stringify writes the node's comment itself, as a block collection does
        boolean printsComment(Context ctx) {
            return false;
        }
    }

    private static final class Scalar extends Node {
        @Nullable Object value;
        final Yaml.Scalar.@Nullable Style type;
        final @Nullable String source;
        final boolean hex;
        final boolean oct;
        final int minFractionDigits;

        Scalar(@Nullable Object value) {
            this(value, null, null, false, false, 0);
        }

        Scalar(@Nullable Object value, Yaml.Scalar.@Nullable Style type, @Nullable String source, boolean hex,
               boolean oct, int minFractionDigits) {
            this.value = value;
            this.type = type;
            this.source = source;
            this.hex = hex;
            this.oct = oct;
            this.minFractionDigits = minFractionDigits;
        }

        // composeScalar over the core schema's tags
        static Scalar of(Yaml.Scalar scalar) {
            String text = scalar.getValue();
            switch (scalar.getStyle()) {
                case SINGLE_QUOTED:
                    return new Scalar(text.replace("''", "'"), Yaml.Scalar.Style.SINGLE_QUOTED, null, false, false,
                            0);
                case DOUBLE_QUOTED:
                    if (text.replace("\\\\", "").replace("\\\"", "").replace("\\/", "").indexOf('\\') >= 0) {
                        throw unsupported("a double-quoted escape other than \\\\, \\\" and \\/");
                    }
                    return new Scalar(text.replace("\\\\", "\u0000").replace("\\\"", "\"").replace("\\/", "/")
                            .replace('\u0000', '\\'), Yaml.Scalar.Style.DOUBLE_QUOTED, null, false, false, 0);
                case PLAIN:
                    break;
                default:
                    return new Scalar(blockValue(text, scalar.getStyle() == Yaml.Scalar.Style.FOLDED),
                            scalar.getStyle(), null, false, false, 0);
            }
            Yaml.Scalar.Style plain = Yaml.Scalar.Style.PLAIN;
            if (NULL.matcher(text).matches()) {
                return new Scalar(null, plain, text, false, false, 0);
            }
            if (BOOL.matcher(text).matches()) {
                return new Scalar(text.charAt(0) == 't' || text.charAt(0) == 'T', plain, text, false, false, 0);
            }
            if (INT_OCT.matcher(text).matches() || INT.matcher(text).matches() || INT_HEX.matcher(text).matches()) {
                boolean oct = text.startsWith("0o");
                boolean hex = text.startsWith("0x");
                String digits = oct || hex ? text.substring(2) : text;
                if (digits.replaceFirst("^[-+]?0*", "").length() > 15) {
                    throw unsupported("an integer of more than 15 digits");
                }
                double parsed = Long.parseLong(digits.startsWith("+") ? digits.substring(1) : digits,
                        oct ? 8 : hex ? 16 : 10);
                return new Scalar(parsed == 0 && text.startsWith("-") ? -0.0 : parsed, plain, text, hex, oct, 0);
            }
            if (FLOAT_NAN.matcher(text).matches()) {
                return new Scalar(text.endsWith("nan") || text.endsWith("NaN") || text.endsWith("NAN") ? Double.NaN :
                        text.startsWith("-") ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY, plain, text,
                        false, false, 0);
            }
            if (FLOAT_EXP.matcher(text).matches()) {
                throw unsupported("a number with an exponent");
            }
            if (FLOAT.matcher(text).matches()) {
                if (text.replaceAll("[^0-9]", "").replaceFirst("^0*", "").length() > 15) {
                    throw unsupported("a number of more than 15 digits");
                }
                int dot = text.indexOf('.');
                return new Scalar(Double.parseDouble(text), plain, text, false, false,
                        text.endsWith("0") ? text.length() - dot - 1 : 0);
            }
            if (text.isEmpty() || "{[&*!|>'\"%@`".indexOf(text.charAt(0)) >= 0) {
                throw unsupported("a plain scalar the yaml reader here does not read: " + text);
            }
            return new Scalar(text, plain, text, false, false, 0);
        }

        // resolveBlockScalar for a header without an indentation indicator or keep chomping, and folded text without
        // more-indented lines; the tree holds the header's indicators, then the lines as written
        private static String blockValue(String text, boolean folded) {
            int nl = text.indexOf('\n');
            String header = nl < 0 ? text : text.substring(0, nl);
            if (nl < 0 || !header.isEmpty() && !"-".equals(header)) {
                throw unsupported("a block scalar header other than |, >, |- and >-");
            }
            String[] lines = text.substring(nl + 1).split("\n", -1);
            int last = lines.length - 1;
            while (last >= 0 && lines[last].isEmpty()) {
                last--;
            }
            if (last < 0) {
                throw unsupported("an empty block scalar");
            }
            String indent = lines[0].substring(0, lines[0].length() - lines[0].replaceFirst("^ +", "").length());
            StringBuilder value = new StringBuilder();
            for (int i = 0; i <= last; i++) {
                String line = lines[i];
                boolean empty = line.isEmpty();
                if (indent.isEmpty() || !empty && (!line.startsWith(indent) || line.trim().isEmpty() ||
                                                   line.indexOf('\t') >= 0 ||
                                                   folded && line.charAt(indent.length()) == ' ')) {
                    throw unsupported("a block scalar it does not read");
                }
                if (i > 0) {
                    value.append(folded && !empty && !lines[i - 1].isEmpty() ? " " : folded && !empty ? "" : "\n");
                }
                value.append(empty ? "" : line.substring(indent.length()));
            }
            return "-".equals(header) ? value.toString() : value + "\n";
        }

        @Override
        @Nullable
        Object toJs() {
            return value;
        }

        // patchScalar
        @Override
        Node patched(Object target) {
            if (EcmaScript.strictEquals(value, target)) {
                return this;
            }
            if (target instanceof Boolean || target instanceof String || target instanceof Double) {
                value = target;
                return this;
            }
            return createNode(target);
        }

        @Override
        String stringify(Context ctx) {
            if (value == null) {
                return source != null && NULL.matcher(source).matches() ? source : "null";
            }
            if (value instanceof Boolean) {
                boolean sourced = source != null && BOOL.matcher(source).matches() &&
                                  (source.charAt(0) == 't' || source.charAt(0) == 'T') == (Boolean) value;
                return sourced ? source : (Boolean) value ? "true" : "false";
            }
            if (value instanceof Double) {
                return number((Double) value);
            }
            return string((String) value, ctx);
        }

        // intStringify for hex and octal, stringifyNumber otherwise
        private String number(double d) {
            if ((hex || oct) && d >= 0 && d == Math.floor(d) && !Double.isInfinite(d)) {
                return (hex ? "0x" : "0o") + Long.toString((long) d, hex ? 16 : 8);
            }
            if (Double.isNaN(d)) {
                return ".nan";
            }
            if (Double.isInfinite(d)) {
                return d < 0 ? "-.inf" : ".inf";
            }
            String n = d == 0 && 1 / d < 0 ? "-0" : EcmaScript.numberToString(d);
            if (!hex && !oct && minFractionDigits > 0 && Character.isDigit(n.charAt(0))) {
                StringBuilder digits = new StringBuilder(n);
                int i = n.indexOf('.');
                if (i < 0) {
                    i = n.length();
                    digits.append('.');
                }
                for (int missing = minFractionDigits - (digits.length() - i - 1); missing > 0; missing--) {
                    digits.append('0');
                }
                n = digits.toString();
            }
            return n;
        }

        // stringifyString
        private String string(String value, Context ctx) {
            if (CONTROL.matcher(value).find()) {
                throw unsupported("a string with a control character");
            }
            if ((type == Yaml.Scalar.Style.LITERAL || type == Yaml.Scalar.Style.FOLDED) && !ctx.implicitKey &&
                !ctx.inFlow) {
                return block(value, ctx.indent);
            }
            if (value.indexOf('\n') >= 0) {
                throw unsupported("a string over more than one line");
            }
            if (type == Yaml.Scalar.Style.DOUBLE_QUOTED) {
                return doubleQuoted(value);
            }
            if (type == Yaml.Scalar.Style.SINGLE_QUOTED) {
                return "'" + value.replace("'", "''") + "'";
            }
            if (ctx.inFlow && FLOW_INDICATOR.matcher(value).find() ||
                DOCUMENT_MARKER.matcher(value).find() && ctx.implicitKey && INDENT_STEP.equals(ctx.indent)) {
                return value.contains("'") && !value.contains("\"") ? doubleQuoted(value) :
                        "'" + value.replace("'", "''") + "'";
            }
            if (DOCUMENT_MARKER.matcher(value).find() && ctx.indent.isEmpty()) {
                throw unsupported("a document marker yaml writes as a block scalar");
            }
            return scalar(value);
        }

        // blockString of a value without leading white space that ends in clip or strip chomping
        private String block(String value, String indent) {
            boolean literal = type == Yaml.Scalar.Style.LITERAL;
            String text = value.endsWith("\n") ? value.substring(0, value.length() - 1) : value;
            if (text.isEmpty() || text.endsWith("\n") || " \t\n".indexOf(text.charAt(0)) >= 0 ||
                " \t".indexOf(text.charAt(text.length() - 1)) >= 0 || text.contains("\n ") || text.contains("\n\t")) {
                throw unsupported("a block scalar it does not write as yaml does");
            }
            return (literal ? "|" : ">") + (text.equals(value) ? "-" : "") + "\n" + indent +
                   text.replaceAll("\n+", literal ? "$0" + indent : "\n$0" + indent);
        }
    }

    private static final class Pair {
        final Scalar key;
        @Nullable Node value;

        Pair(Scalar key, @Nullable Node value) {
            this.key = key;
            this.value = value;
        }

        // stringifyPair of an implicit key
        String stringify(Context ctx) {
            Node value = this.value;
            if (value == null) {
                throw unsupported("a key without a value");
            }
            String str = key.stringify(new Context(ctx.indent + INDENT_STEP, ctx.inFlow, true));
            if (!ctx.inFlow && str.length() > 1024) {
                throw unsupported("a key of more than 1024 characters");
            }
            str += ":";
            Context valueCtx = new Context(ctx.indent + INDENT_STEP, ctx.inFlow, false);
            String valueStr = value.stringify(valueCtx);
            String ws = " ";
            if (value.spaceBefore || value.commentBefore != null) {
                ws = value.spaceBefore ? "\n" : "";
                if (value.commentBefore != null) {
                    ws += "\n" + indentComment(commentString(value.commentBefore), valueCtx.indent);
                }
                if (valueStr.isEmpty() && !ctx.inFlow) {
                    if ("\n".equals(ws) && value.comment != null) {
                        ws = "\n\n";
                    }
                } else {
                    ws += "\n" + valueCtx.indent;
                }
            } else if (value instanceof Collection) {
                if (valueStr.indexOf('\n') >= 0 || !(ctx.inFlow || ((Collection) value).flowOrEmpty())) {
                    ws = "\n" + valueCtx.indent;
                }
            } else if (valueStr.isEmpty() || valueStr.charAt(0) == '\n') {
                ws = "";
            }
            str += ws + valueStr;
            if (!ctx.inFlow && value.comment != null && !value.printsComment(valueCtx)) {
                str += lineComment(str, valueCtx.indent, commentString(value.comment));
            }
            return str;
        }
    }

    private abstract static class Collection extends Node {
        final boolean flow;

        Collection(boolean flow) {
            this.flow = flow;
        }

        abstract List<?> entries();

        abstract String itemPrefix();

        abstract String itemIndent(Context ctx);

        abstract String brackets();

        boolean flowOrEmpty() {
            return flow || entries().isEmpty();
        }

        @Override
        boolean printsComment(Context ctx) {
            return !(ctx.inFlow || flow) && comment != null;
        }

        @Override
        String stringify(Context ctx) {
            return ctx.inFlow || flow ? flowString(ctx) : blockString(ctx);
        }

        // stringifyBlockCollection
        private String blockString(Context ctx) {
            String itemIndent = itemIndent(ctx);
            Context itemCtx = new Context(itemIndent, ctx.inFlow, false);
            List<String> lines = new ArrayList<>();
            for (Object item : entries()) {
                Node before = item instanceof Node ? (Node) item : ((Pair) item).key;
                if (before.spaceBefore) {
                    lines.add("");
                }
                if (before.commentBefore != null) {
                    lines.add(indentComment(commentString(before.commentBefore), ctx.indent).replaceFirst("^\\s+",
                            ""));
                }
                String str;
                if (item instanceof Node) {
                    Node node = (Node) item;
                    str = node.stringify(itemCtx);
                    if (node.comment != null && !node.printsComment(itemCtx)) {
                        str += lineComment(str, itemIndent, commentString(node.comment));
                    }
                } else {
                    str = ((Pair) item).stringify(itemCtx);
                }
                lines.add(itemPrefix() + str);
            }
            StringBuilder str = new StringBuilder();
            if (lines.isEmpty()) {
                str.append(brackets());
            } else {
                str.append(lines.get(0));
                for (int i = 1; i < lines.size(); i++) {
                    str.append(lines.get(i).isEmpty() ? "\n" : "\n" + ctx.indent + lines.get(i));
                }
            }
            if (comment != null) {
                str.append('\n').append(indentComment(commentString(comment), ctx.indent));
            }
            return str.toString();
        }

        // stringifyFlowCollection of one line, which lineWidth 0 never folds
        private String flowString(Context ctx) {
            Context itemCtx = new Context(itemIndent(ctx) + INDENT_STEP, true, ctx.implicitKey);
            List<String> lines = new ArrayList<>();
            List<?> entries = entries();
            for (int i = 0; i < entries.size(); i++) {
                Object item = entries.get(i);
                String str = item instanceof Node ? ((Node) item).stringify(itemCtx) : ((Pair) item).stringify(itemCtx);
                lines.add(i < entries.size() - 1 ? str + "," : str);
            }
            String brackets = brackets();
            return lines.isEmpty() ? brackets : brackets.charAt(0) + " " + String.join(" ", lines) + " " +
                                                brackets.charAt(1);
        }
    }

    private static final class YamlMap extends Collection {
        List<Pair> items = new ArrayList<>();

        YamlMap(boolean flow) {
            super(flow);
        }

        @Override
        List<?> entries() {
            return items;
        }

        @Override
        String itemPrefix() {
            return "";
        }

        @Override
        String itemIndent(Context ctx) {
            return ctx.indent;
        }

        @Override
        String brackets() {
            return "{}";
        }

        @Override
        Object toJs() {
            Map<String, @Nullable Object> map = new LinkedHashMap<>();
            for (Pair pair : items) {
                map.put((String) pair.key.value, pair.value == null ? null : pair.value.toJs());
            }
            return map;
        }

        // patchMap
        @Override
        @Nullable
        Node patched(Object target) {
            if (!(target instanceof Map)) {
                return createNode(target);
            }
            Map<String, @Nullable Object> map = asMap(target);
            if (map.isEmpty()) {
                return null;
            }
            Map<String, Pair> existing = new HashMap<>();
            for (Pair pair : items) {
                existing.put((String) pair.key.value, pair);
            }
            List<Pair> patched = new ArrayList<>();
            for (String key : EcmaScript.keys(map)) {
                Pair pair = existing.get(key);
                if (pair == null) {
                    pair = new Pair(new Scalar(key), createNode(map.get(key)));
                } else {
                    pair.value = patchNode(pair.value, map.get(key));
                }
                if (pair.value != null) {
                    patched.add(pair);
                }
            }
            items = patched;
            return this;
        }
    }

    private static final class YamlSeq extends Collection {
        List<Node> items = new ArrayList<>();

        YamlSeq(boolean flow) {
            super(flow);
        }

        @Override
        List<?> entries() {
            return items;
        }

        @Override
        String itemPrefix() {
            return "- ";
        }

        @Override
        String itemIndent(Context ctx) {
            return ctx.indent + INDENT_STEP;
        }

        @Override
        String brackets() {
            return "[]";
        }

        @Override
        Object toJs() {
            List<@Nullable Object> list = new ArrayList<>();
            for (Node item : items) {
                list.add(item.toJs());
            }
            return list;
        }

        // patchSeq
        @Override
        Node patched(Object target) {
            if (!(target instanceof List)) {
                return createNode(target);
            }
            List<?> list = (List<?>) target;
            boolean primitive = true;
            for (Object item : list) {
                primitive &= item == null || item instanceof String || item instanceof Double ||
                             item instanceof Boolean;
            }
            List<Node> patched = new ArrayList<>();
            if (primitive) {
                Map<Object, Deque<Scalar>> byValue = new HashMap<>();
                for (Node item : items) {
                    if (item instanceof Scalar && ((Scalar) item).value != null) {
                        byValue.computeIfAbsent(((Scalar) item).value, v -> new ArrayDeque<>()).add((Scalar) item);
                    }
                }
                for (Object item : list) {
                    if (item != null) {
                        Deque<Scalar> nodes = byValue.get(item);
                        patched.add(nodes == null || nodes.isEmpty() ? new Scalar(item) : nodes.poll());
                    }
                }
            } else {
                for (int i = 0; i < Math.max(items.size(), list.size()); i++) {
                    Node next = patchNode(i < items.size() ? items.get(i) : null, i < list.size() ? list.get(i) : null);
                    if (next != null) {
                        patched.add(next);
                    }
                }
            }
            items = patched;
            return this;
        }
    }

    /**
     * A token of yaml's concrete syntax tree, and the collections and documents the parser builds of them.
     */
    private static final class Token {
        final String type;
        final int indent;
        final String source;
        final Yaml.@Nullable Block block;
        @Nullable List<Token> end;
        final List<Item> items = new ArrayList<>();
        final List<Token> start = new ArrayList<>();
        @Nullable Token value;

        Token(String type, int indent, String source, Yaml.@Nullable Block block) {
            this.type = type;
            this.indent = indent;
            this.source = source;
            this.block = block;
        }

        Item last() {
            return items.get(items.size() - 1);
        }
    }

    private static final class Item {
        final List<Token> start;
        @Nullable Token key;
        @Nullable List<Token> sep;
        @Nullable Token value;

        Item(List<Token> start) {
            this.start = start;
        }

        Item(List<Token> start, Token key, List<Token> sep) {
            this.start = start;
            this.key = key;
            this.sep = sep;
        }
    }

    /**
     * Port of yaml 2.9.0 src/parse/parser.ts for the block mappings, sequences and scalars of a workspace manifest.
     */
    private static final class Parser {
        static final String DOCUMENT = "document";
        static final String BLOCK_MAP = "block-map";
        static final String BLOCK_SEQ = "block-seq";
        static final String FLOW = "flow-collection";
        static final String SCALAR = "scalar";
        static final String BLOCK_SCALAR = "block-scalar";
        static final String SPACE = "space";
        static final String COMMENT = "comment";
        static final String NEWLINE = "newline";
        static final String MAP_VALUE = "map-value-ind";
        static final String SEQ_ITEM = "seq-item-ind";
        static final String DOC_MODE = "doc-mode";

        final List<String> prelude = new ArrayList<>();
        @Nullable Token document;
        private final List<Token> stack = new ArrayList<>();
        private boolean atNewLine = true;
        private int indent;
        private boolean onKeyLine;
        private String type = "";
        private String source = "";
        private Yaml.@Nullable Block block;

        void next(String type, String source, Yaml.@Nullable Block block) {
            this.type = type;
            this.source = source;
            this.block = block;
            if (SCALAR.equals(type)) {
                atNewLine = false;
                step();
                return;
            }
            if (BLOCK_SCALAR.equals(type)) {
                step();
                pop();
                atNewLine = true;
                indent = 0;
                return;
            }
            step();
            switch (type) {
                case NEWLINE:
                    atNewLine = true;
                    indent = 0;
                    break;
                case SPACE:
                case MAP_VALUE:
                case SEQ_ITEM:
                    if (atNewLine) {
                        indent += source.length();
                    }
                    break;
                case DOC_MODE:
                    break;
                default:
                    atNewLine = false;
            }
        }

        void end() {
            while (!stack.isEmpty()) {
                pop();
            }
        }

        private Token sourceToken() {
            return new Token(type, indent, source, block);
        }

        private @Nullable Token peek(int n) {
            return stack.size() < n ? null : stack.get(stack.size() - n);
        }

        private void step() {
            Token top = peek(1);
            if (top == null) {
                stream();
                return;
            }
            switch (top.type) {
                case DOCUMENT:
                    document(top);
                    return;
                case SCALAR:
                    scalar(top);
                    return;
                case BLOCK_MAP:
                    blockMap(top);
                    return;
                case BLOCK_SEQ:
                    blockSequence(top);
                    return;
                case FLOW:
                    flowCollection(top);
                    return;
                default:
                    pop();
            }
        }

        private void pop() {
            Token token = stack.remove(stack.size() - 1);
            Token top = peek(1);
            if (top == null) {
                document = token;
                return;
            }
            switch (top.type) {
                case DOCUMENT:
                    top.value = token;
                    break;
                case BLOCK_MAP: {
                    Item it = top.last();
                    if (it.value != null) {
                        top.items.add(new Item(new ArrayList<>(), token, new ArrayList<>()));
                        onKeyLine = true;
                        return;
                    } else if (it.sep != null) {
                        it.value = token;
                    } else {
                        it.key = token;
                        it.sep = new ArrayList<>();
                        onKeyLine = true;
                        return;
                    }
                    break;
                }
                case BLOCK_SEQ: {
                    Item it = top.last();
                    if (it.value != null) {
                        Item item = new Item(new ArrayList<>());
                        item.value = token;
                        top.items.add(item);
                    } else {
                        it.value = token;
                    }
                    break;
                }
                default:
                    throw unsupported("a " + token.type + " inside a " + top.type);
            }
            if ((BLOCK_MAP.equals(token.type) || BLOCK_SEQ.equals(token.type)) && !token.items.isEmpty()) {
                Item last = token.last();
                if (last.sep == null && last.value == null && !last.start.isEmpty() && onlyBlank(last.start) &&
                    (token.indent == 0 || commentsBelow(last.start, token.indent))) {
                    if (DOCUMENT.equals(top.type)) {
                        top.end = last.start;
                    } else {
                        top.items.add(new Item(last.start));
                    }
                    token.items.remove(token.items.size() - 1);
                }
            }
        }

        private void stream() {
            switch (type) {
                case SPACE:
                    return;
                case COMMENT:
                case NEWLINE:
                    prelude.add(source);
                    return;
                case DOC_MODE:
                    stack.add(new Token(DOCUMENT, 0, "", null));
                    return;
                default:
                    throw unsupported("a " + type + " outside a document");
            }
        }

        private void document(Token doc) {
            if (doc.value != null) {
                lineEnd(doc);
                return;
            }
            if (SPACE.equals(type) || COMMENT.equals(type) || NEWLINE.equals(type)) {
                doc.start.add(sourceToken());
                return;
            }
            Token bv = startBlockValue();
            if (bv == null) {
                throw unsupported("a " + type + " starting the document");
            }
            stack.add(bv);
        }

        private void scalar(Token scalar) {
            if (!MAP_VALUE.equals(type)) {
                lineEnd(scalar);
                return;
            }
            List<Token> start = firstKeyStartProps(prevProps(requireParent()));
            List<Token> sep;
            if (scalar.end != null) {
                sep = scalar.end;
                sep.add(sourceToken());
                scalar.end = null;
            } else {
                sep = new ArrayList<>(singletonList(sourceToken()));
            }
            Token map = new Token(BLOCK_MAP, scalar.indent, "", null);
            map.items.add(new Item(start, scalar, sep));
            onKeyLine = true;
            stack.set(stack.size() - 1, map);
        }

        private Token requireParent() {
            Token parent = peek(2);
            if (parent == null) {
                throw unsupported("a key outside a document");
            }
            return parent;
        }

        private void blockMap(Token map) {
            Item it = map.last();
            switch (type) {
                case NEWLINE:
                    onKeyLine = false;
                    if (it.value != null) {
                        List<Token> end = it.value.end;
                        if (end != null && !end.isEmpty() && COMMENT.equals(end.get(end.size() - 1).type)) {
                            end.add(sourceToken());
                        } else {
                            map.items.add(new Item(new ArrayList<>(singletonList(sourceToken()))));
                        }
                    } else if (it.sep != null) {
                        it.sep.add(sourceToken());
                    } else {
                        it.start.add(sourceToken());
                    }
                    return;
                case SPACE:
                case COMMENT:
                    if (it.value != null) {
                        map.items.add(new Item(new ArrayList<>(singletonList(sourceToken()))));
                    } else if (it.sep != null) {
                        it.sep.add(sourceToken());
                    } else if (!appendedToPreviousValue(map, it)) {
                        it.start.add(sourceToken());
                    }
                    return;
                default:
            }
            if (indent >= map.indent) {
                boolean atMapIndent = !onKeyLine && indent == map.indent;
                boolean atNextItem = atMapIndent && it.sep != null && !SEQ_ITEM.equals(type);
                List<Token> start = new ArrayList<>();
                if (atNextItem && it.value == null) {
                    List<Integer> nl = new ArrayList<>();
                    for (int i = 0; i < it.sep.size(); i++) {
                        Token st = it.sep.get(i);
                        if (NEWLINE.equals(st.type)) {
                            nl.add(i);
                        } else if (COMMENT.equals(st.type) ? st.indent > map.indent : !SPACE.equals(st.type)) {
                            nl.clear();
                        }
                    }
                    if (nl.size() >= 2) {
                        List<Token> moved = it.sep.subList(nl.get(1), it.sep.size());
                        start = new ArrayList<>(moved);
                        moved.clear();
                    }
                }
                if (MAP_VALUE.equals(type)) {
                    if (it.sep == null || it.value != null || atNextItem || includes(it.sep, MAP_VALUE)) {
                        throw unsupported("a key it does not read");
                    }
                    it.sep.add(sourceToken());
                    onKeyLine = true;
                    return;
                }
                if (SCALAR.equals(type)) {
                    Token fs = sourceToken();
                    if (atNextItem || it.value != null) {
                        map.items.add(new Item(start, fs, new ArrayList<>()));
                        onKeyLine = true;
                    } else if (it.sep != null) {
                        stack.add(fs);
                    } else {
                        it.key = fs;
                        it.sep = new ArrayList<>();
                        onKeyLine = true;
                    }
                    return;
                }
                Token bv = startBlockValue();
                if (bv != null) {
                    if (BLOCK_SEQ.equals(bv.type)) {
                        if (it.sep != null && !includes(it.sep, NEWLINE)) {
                            throw unsupported("a sequence on its key's line");
                        }
                    } else if (atMapIndent) {
                        map.items.add(new Item(start));
                    }
                    stack.add(bv);
                    return;
                }
            }
            pop();
            step();
        }

        private void blockSequence(Token seq) {
            Item it = seq.last();
            switch (type) {
                case NEWLINE:
                    if (it.value != null) {
                        List<Token> end = it.value.end;
                        if (end != null && !end.isEmpty() && COMMENT.equals(end.get(end.size() - 1).type)) {
                            end.add(sourceToken());
                        } else {
                            seq.items.add(new Item(new ArrayList<>(singletonList(sourceToken()))));
                        }
                    } else {
                        it.start.add(sourceToken());
                    }
                    return;
                case SPACE:
                case COMMENT:
                    if (it.value != null) {
                        seq.items.add(new Item(new ArrayList<>(singletonList(sourceToken()))));
                    } else if (!appendedToPreviousValue(seq, it)) {
                        it.start.add(sourceToken());
                    }
                    return;
                case SEQ_ITEM:
                    if (indent != seq.indent) {
                        break;
                    }
                    if (it.value != null || includes(it.start, SEQ_ITEM)) {
                        seq.items.add(new Item(new ArrayList<>(singletonList(sourceToken()))));
                    } else {
                        it.start.add(sourceToken());
                    }
                    return;
                default:
            }
            if (indent > seq.indent) {
                Token bv = startBlockValue();
                if (bv != null) {
                    stack.add(bv);
                    return;
                }
            }
            pop();
            step();
        }

        // atIndentedComment: a comment indented below a value, with nothing but blank lines before it, is the value's
        private boolean appendedToPreviousValue(Token collection, Item it) {
            if (!COMMENT.equals(type) || indent <= collection.indent || !onlyBlank(it.start) ||
                includes(it.start, COMMENT)) {
                return false;
            }
            Item prev = collection.items.size() < 2 ? null : collection.items.get(collection.items.size() - 2);
            List<Token> end = prev == null || prev.value == null ? null : prev.value.end;
            if (end == null) {
                return false;
            }
            end.addAll(it.start);
            end.add(sourceToken());
            collection.items.remove(collection.items.size() - 1);
            return true;
        }

        // only the closing bracket is read here: a flow collection of one line is one token
        private void flowCollection(Token fc) {
            Token parent = requireParent();
            if (BLOCK_MAP.equals(parent.type) && (MAP_VALUE.equals(type) && parent.indent == fc.indent ||
                                                  NEWLINE.equals(type) && parent.last().sep == null)) {
                pop();
                step();
            } else if (MAP_VALUE.equals(type)) {
                throw unsupported("a flow collection as a key");
            } else {
                lineEnd(fc);
            }
        }

        private @Nullable Token startBlockValue() {
            switch (type) {
                case SCALAR:
                case BLOCK_SCALAR:
                    return sourceToken();
                case FLOW: {
                    Token fc = sourceToken();
                    fc.end = new ArrayList<>(singletonList(new Token("flow-end", indent, "]", null)));
                    return fc;
                }
                case SEQ_ITEM: {
                    Token seq = new Token(BLOCK_SEQ, indent, "", null);
                    seq.items.add(new Item(new ArrayList<>(singletonList(sourceToken()))));
                    return seq;
                }
                default:
                    return null;
            }
        }

        private void lineEnd(Token token) {
            if (MAP_VALUE.equals(type)) {
                pop();
                step();
                return;
            }
            if (NEWLINE.equals(type)) {
                onKeyLine = false;
            }
            if (token.end == null) {
                token.end = new ArrayList<>();
            }
            token.end.add(sourceToken());
            if (NEWLINE.equals(type)) {
                pop();
            }
        }

        private static List<Token> prevProps(Token parent) {
            if (BLOCK_MAP.equals(parent.type)) {
                Item it = parent.last();
                return it.sep != null ? it.sep : it.start;
            }
            return BLOCK_SEQ.equals(parent.type) ? parent.last().start : parent.start;
        }

        // getFirstKeyStartProps
        private static List<Token> firstKeyStartProps(List<Token> prev) {
            int i = prev.size();
            while (--i >= 0) {
                String t = prev.get(i).type;
                if (NEWLINE.equals(t) || MAP_VALUE.equals(t) || SEQ_ITEM.equals(t)) {
                    break;
                }
            }
            i++;
            while (i < prev.size() && SPACE.equals(prev.get(i).type)) {
                i++;
            }
            List<Token> moved = prev.subList(i, prev.size());
            List<Token> start = new ArrayList<>(moved);
            moved.clear();
            return start;
        }

        private static boolean onlyBlank(List<Token> tokens) {
            for (Token token : tokens) {
                if (!SPACE.equals(token.type) && !COMMENT.equals(token.type) && !NEWLINE.equals(token.type)) {
                    return false;
                }
            }
            return true;
        }

        private static boolean commentsBelow(List<Token> tokens, int indent) {
            for (Token token : tokens) {
                if (COMMENT.equals(token.type) && token.indent >= indent) {
                    return false;
                }
            }
            return true;
        }

        private static boolean includes(List<Token> tokens, String type) {
            for (Token token : tokens) {
                if (type.equals(token.type)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * The tokens yaml's lexer reads from the file, in order, taken from the OpenRewrite tree of it.
     */
    private static final class Lexer {
        private final Parser parser;

        Lexer(Parser parser) {
            this.parser = parser;
        }

        void lex(Yaml.Documents docs) {
            String text = docs.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED));
            if (text.indexOf('\r') >= 0) {
                throw unsupported("CR line ends");
            }
            if (docs.getDocuments().size() > 1) {
                throw unsupported("document markers");
            }
            if (docs.getDocuments().isEmpty()) {
                return;
            }
            Yaml.Document doc = docs.getDocuments().get(0);
            if (doc.isExplicit() || doc.getEnd().isExplicit()) {
                throw unsupported("document markers");
            }
            Yaml.Block block = doc.getBlock();
            String before = doc.getPrefix() + block.getPrefix();
            boolean empty = block instanceof Yaml.Scalar ? isEmpty((Yaml.Scalar) block) &&
                                                           ((Yaml.Scalar) block).getAnchor() == null &&
                                                           ((Yaml.Scalar) block).getTag() == null :
                    block instanceof Yaml.Mapping && ((Yaml.Mapping) block).getOpeningBracePrefix() == null &&
                    ((Yaml.Mapping) block).getEntries().isEmpty();
            if (empty) {
                gap(before + doc.getEnd().getPrefix());
                return;
            }
            if (!(block instanceof Yaml.Mapping) || ((Yaml.Mapping) block).getOpeningBracePrefix() != null ||
                ((Yaml.Mapping) block).getEntries().isEmpty()) {
                throw unsupported("no block mapping at the top");
            }
            Yaml.Mapping mapping = (Yaml.Mapping) block;
            before += mapping.getEntries().get(0).getPrefix();
            if (!before.substring(before.lastIndexOf('\n') + 1).isEmpty()) {
                throw unsupported("its first key indented");
            }
            gap(before);
            parser.next(Parser.DOC_MODE, "", null);
            mapping(mapping, true);
            gap(doc.getEnd().getPrefix());
            parser.end();
        }

        private void mapping(Yaml.Mapping mapping, boolean prefixRead) {
            if (mapping.getAnchor() != null || mapping.getTag() != null) {
                throw unsupported("an anchor or a tag");
            }
            List<Yaml.Mapping.Entry> entries = mapping.getEntries();
            for (int i = 0; i < entries.size(); i++) {
                Yaml.Mapping.Entry entry = entries.get(i);
                if (i > 0 || !prefixRead) {
                    gap(entry.getPrefix());
                }
                if (!(entry.getKey() instanceof Yaml.Scalar) || isEmpty((Yaml.Scalar) entry.getKey())) {
                    throw unsupported("a key it does not read");
                }
                scalar((Yaml.Scalar) entry.getKey());
                if (!blank(entry.getBeforeMappingValueIndicator())) {
                    throw unsupported("a key it does not read");
                }
                gap(entry.getBeforeMappingValueIndicator());
                parser.next(Parser.MAP_VALUE, ":", null);
                value(entry.getValue());
            }
        }

        private void value(Yaml.Block value) {
            if (value instanceof Yaml.Scalar) {
                gap(value.getPrefix());
                if (!isEmpty((Yaml.Scalar) value)) {
                    scalar((Yaml.Scalar) value);
                }
            } else if (value instanceof Yaml.Mapping && ((Yaml.Mapping) value).getOpeningBracePrefix() != null) {
                gap(((Yaml.Mapping) value).getOpeningBracePrefix());
                flow(value);
                parser.next(Parser.FLOW, "{}", value);
            } else if (value instanceof Yaml.Mapping) {
                mapping((Yaml.Mapping) value, false);
            } else if (value instanceof Yaml.Sequence && ((Yaml.Sequence) value).getOpeningBracketPrefix() != null) {
                gap(((Yaml.Sequence) value).getOpeningBracketPrefix());
                flow(value);
                parser.next(Parser.FLOW, "[]", value);
            } else if (value instanceof Yaml.Sequence) {
                Yaml.Sequence seq = (Yaml.Sequence) value;
                if (seq.getAnchor() != null || seq.getTag() != null) {
                    throw unsupported("an anchor or a tag");
                }
                for (Yaml.Sequence.Entry entry : seq.getEntries()) {
                    if (!entry.isDash()) {
                        throw unsupported("a sequence entry it does not read");
                    }
                    gap(entry.getPrefix());
                    parser.next(Parser.SEQ_ITEM, "-", null);
                    value(entry.getBlock());
                }
            } else {
                throw unsupported("an alias");
            }
        }

        // a flow collection of one line without comments, of scalars and flow sequences, or {}
        private void flow(Yaml.Block value) {
            if (value instanceof Yaml.Mapping) {
                Yaml.Mapping map = (Yaml.Mapping) value;
                if (!map.getEntries().isEmpty() || map.getAnchor() != null || map.getTag() != null ||
                    !blank(map.getOpeningBracePrefix()) || !blank(map.getClosingBracePrefix())) {
                    throw unsupported("a flow mapping");
                }
                return;
            }
            if (!(value instanceof Yaml.Sequence)) {
                Yaml.Scalar scalar = (Yaml.Scalar) value;
                if (isEmpty(scalar) || !separator(scalar.getPrefix())) {
                    throw unsupported("a flow sequence it does not read");
                }
                Scalar.of(scalar);
                return;
            }
            Yaml.Sequence seq = (Yaml.Sequence) value;
            if (seq.getOpeningBracketPrefix() == null || seq.getAnchor() != null || seq.getTag() != null ||
                !blank(seq.getClosingBracketPrefix())) {
                throw unsupported("a flow sequence it does not read");
            }
            for (Yaml.Sequence.Entry entry : seq.getEntries()) {
                if (!blank(entry.getPrefix()) || entry.getTrailingCommaPrefix() != null &&
                                                 !blank(entry.getTrailingCommaPrefix())) {
                    throw unsupported("a flow sequence it does not read");
                }
                if (entry.getBlock() instanceof Yaml.Sequence) {
                    if (!separator(entry.getBlock().getPrefix())) {
                        throw unsupported("a flow sequence it does not read");
                    }
                    flow(entry.getBlock());
                } else if (entry.getBlock() instanceof Yaml.Scalar ||
                           entry.getBlock() instanceof Yaml.Mapping &&
                           ((Yaml.Mapping) entry.getBlock()).getOpeningBracePrefix() != null) {
                    flow(entry.getBlock());
                } else {
                    throw unsupported("a flow sequence it does not read");
                }
            }
        }

        private void scalar(Yaml.Scalar scalar) {
            if (scalar.getAnchor() != null || scalar.getTag() != null) {
                throw unsupported("an anchor or a tag");
            }
            boolean block = scalar.getStyle() == Yaml.Scalar.Style.LITERAL ||
                            scalar.getStyle() == Yaml.Scalar.Style.FOLDED;
            if (scalar.getValue().indexOf('\n') >= 0 && !block) {
                throw unsupported("a scalar over more than one line");
            }
            Scalar.of(scalar);
            parser.next(block ? Parser.BLOCK_SCALAR : Parser.SCALAR, scalar.getValue(), scalar);
        }

        // the white space and comments between tokens
        private void gap(@Nullable String text) {
            if (text == null) {
                return;
            }
            int i = 0;
            while (i < text.length()) {
                char c = text.charAt(i);
                int j = i + 1;
                if (c == ' ') {
                    while (j < text.length() && text.charAt(j) == ' ') {
                        j++;
                    }
                    parser.next(Parser.SPACE, text.substring(i, j), null);
                } else if (c == '\n') {
                    parser.next(Parser.NEWLINE, "\n", null);
                } else if (c == '#') {
                    j = text.indexOf('\n', i);
                    j = j < 0 ? text.length() : j;
                    parser.next(Parser.COMMENT, text.substring(i, j), null);
                } else {
                    throw unsupported(c == '\t' ? "a tab" : "a character between tokens it does not read");
                }
                i = j;
            }
        }

        private static boolean blank(@Nullable String text) {
            return text == null || text.trim().isEmpty() && text.indexOf('\n') < 0 && text.indexOf('\t') < 0;
        }

        // the text between a flow collection's items, which the tree keeps with a comma at times
        private static boolean separator(@Nullable String text) {
            return text == null || blank(text.replaceFirst("^( *),", "$1"));
        }

        private static boolean isEmpty(Yaml.Scalar scalar) {
            return scalar.getStyle() == Yaml.Scalar.Style.PLAIN && scalar.getValue().isEmpty();
        }
    }

    /**
     * Port of yaml 2.9.0 src/compose (composer, compose-doc, compose-node, resolve-props, resolve-end,
     * resolve-block-map, resolve-block-seq) for the tokens the parser above builds.
     */
    private static final class Composer {

        YamlDocument compose(@Nullable Token docToken, List<String> prelude) {
            YamlDocument doc = new YamlDocument();
            if (docToken != null) {
                Props props = resolveProps(docToken.start, "doc-start", true);
                doc.contents = docToken.value != null ? node(docToken.value, props) : emptyNode(props);
                String end = resolveEnd(docToken.end);
                doc.comment = end.isEmpty() ? null : end;
            }
            decorate(doc, prelude);
            return doc;
        }

        // decorate with the stream's prelude
        private void decorate(YamlDocument doc, List<String> prelude) {
            StringBuilder comment = new StringBuilder();
            boolean atComment = false;
            boolean afterEmptyLine = false;
            for (String source : prelude) {
                if (source.startsWith("#")) {
                    if (comment.length() > 0) {
                        comment.append(afterEmptyLine ? "\n\n" : "\n");
                    }
                    comment.append(source.length() > 1 ? source.substring(1) : " ");
                    atComment = true;
                    afterEmptyLine = false;
                } else {
                    if (!atComment) {
                        afterEmptyLine = true;
                    }
                    atComment = false;
                }
            }
            if (comment.length() == 0) {
                return;
            }
            Node contents = doc.contents;
            if (afterEmptyLine || contents == null) {
                doc.commentBefore = comment.toString();
                return;
            }
            Node first = contents;
            if (contents instanceof Collection && !((Collection) contents).flow &&
                !((Collection) contents).entries().isEmpty()) {
                Object item = ((Collection) contents).entries().get(0);
                first = item instanceof Pair ? ((Pair) item).key : (Node) item;
            }
            first.commentBefore = first.commentBefore == null ? comment.toString() : comment + "\n" +
                                                                                    first.commentBefore;
        }

        private Node node(Token token, Props props) {
            Node node;
            switch (token.type) {
                case Parser.SCALAR:
                case Parser.BLOCK_SCALAR:
                    node = Scalar.of((Yaml.Scalar) token.block);
                    String end = resolveEnd(token.end);
                    node.comment = end.isEmpty() ? null : end;
                    break;
                case Parser.BLOCK_MAP:
                    node = blockMap(token);
                    break;
                case Parser.BLOCK_SEQ:
                    node = blockSeq(token);
                    break;
                default:
                    node = flow(token.block);
                    List<Token> after = token.end == null ? new ArrayList<>() : token.end;
                    String trailing = resolveEnd(after.subList(Math.min(1, after.size()), after.size()));
                    node.comment = trailing.isEmpty() ? null : trailing;
            }
            if (props.spaceBefore) {
                node.spaceBefore = true;
            }
            if (!props.comment.isEmpty()) {
                node.commentBefore = props.comment;
            }
            return node;
        }

        private Node emptyNode(Props props) {
            Scalar node = new Scalar(null, Yaml.Scalar.Style.PLAIN, "", false, false, 0);
            node.spaceBefore = props.spaceBefore;
            node.comment = props.comment.isEmpty() ? null : props.comment;
            return node;
        }

        private Node flow(Yaml.@Nullable Block block) {
            if (block instanceof Yaml.Mapping) {
                return new YamlMap(true);
            }
            if (block instanceof Yaml.Scalar) {
                return Scalar.of((Yaml.Scalar) block);
            }
            YamlSeq seq = new YamlSeq(true);
            for (Yaml.Sequence.Entry entry : ((Yaml.Sequence) block).getEntries()) {
                seq.items.add(flow(entry.getBlock()));
            }
            return seq;
        }

        private YamlMap blockMap(Token bm) {
            YamlMap map = new YamlMap(false);
            for (Item item : bm.items) {
                Props keyProps = resolveProps(item.start, "explicit-key-ind", true);
                if (item.sep == null) {
                    if (!keyProps.comment.isEmpty()) {
                        map.comment = map.comment == null ? keyProps.comment : map.comment + "\n" + keyProps.comment;
                    }
                    continue;
                }
                if (item.key == null || !Parser.SCALAR.equals(item.key.type)) {
                    throw unsupported("a key it does not read");
                }
                Node key = node(item.key, keyProps);
                if (!(key instanceof Scalar) || !(((Scalar) key).value instanceof String)) {
                    throw unsupported("a key that is no string, which pnpm's patchDocument stops at");
                }
                for (Pair pair : map.items) {
                    if (pair.key.value.equals(((Scalar) key).value)) {
                        throw unsupported("the key " + pair.key.value + " twice");
                    }
                }
                Props valueProps = resolveProps(item.sep, Parser.MAP_VALUE, false);
                if (!valueProps.found) {
                    throw unsupported("a key without a value indicator");
                }
                map.items.add(new Pair((Scalar) key, item.value != null ? node(item.value, valueProps) :
                        emptyNode(valueProps)));
            }
            return map;
        }

        private YamlSeq blockSeq(Token bs) {
            YamlSeq seq = new YamlSeq(false);
            for (Item item : bs.items) {
                Props props = resolveProps(item.start, Parser.SEQ_ITEM, true);
                if (!props.found) {
                    if (item.value != null) {
                        throw unsupported("a sequence item without its indicator");
                    }
                    if (!props.comment.isEmpty()) {
                        seq.comment = props.comment;
                    }
                    continue;
                }
                seq.items.add(item.value != null ? node(item.value, props) : emptyNode(props));
            }
            return seq;
        }

        // resolveProps
        private Props resolveProps(List<Token> tokens, String indicator, boolean startOnNewline) {
            Props props = new Props();
            boolean atNewline = startOnNewline;
            StringBuilder comment = new StringBuilder();
            StringBuilder commentSep = new StringBuilder();
            for (Token token : tokens) {
                switch (token.type) {
                    case Parser.SPACE:
                        break;
                    case Parser.COMMENT:
                        if (comment.length() > 0) {
                            comment.append(commentSep);
                        }
                        comment.append(token.source.length() > 1 ? token.source.substring(1) : " ");
                        commentSep.setLength(0);
                        atNewline = false;
                        break;
                    case Parser.NEWLINE:
                        if (atNewline) {
                            if (comment.length() > 0) {
                                comment.append(token.source);
                            } else if (!props.found || !Parser.SEQ_ITEM.equals(indicator)) {
                                props.spaceBefore = true;
                            }
                        } else {
                            commentSep.append(token.source);
                        }
                        atNewline = true;
                        break;
                    default:
                        if (!indicator.equals(token.type) || props.found) {
                            throw unsupported("a " + token.type + " it does not read");
                        }
                        props.found = true;
                        atNewline = Parser.SEQ_ITEM.equals(indicator);
                }
            }
            props.comment = comment.toString();
            return props;
        }

        private String resolveEnd(@Nullable List<Token> end) {
            StringBuilder comment = new StringBuilder();
            StringBuilder sep = new StringBuilder();
            for (Token token : end == null ? new ArrayList<Token>() : end) {
                if (Parser.COMMENT.equals(token.type)) {
                    if (comment.length() > 0) {
                        comment.append(sep);
                    }
                    comment.append(token.source.length() > 1 ? token.source.substring(1) : " ");
                    sep.setLength(0);
                } else if (Parser.NEWLINE.equals(token.type)) {
                    if (comment.length() > 0) {
                        sep.append(token.source);
                    }
                } else if (!Parser.SPACE.equals(token.type)) {
                    throw unsupported("a " + token.type + " after a value");
                }
            }
            return comment.toString();
        }
    }

    private static final class Props {
        boolean spaceBefore;
        String comment = "";
        boolean found;
    }

    /**
     * stringifyString of a new scalar under {@code singleQuote: true}: plain where it reads back as that string, else
     * single quoted, double quoted when it holds only a single quote; refused where it would need an escape.
     */
    public static String scalar(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c >= 0x7f && c <= 0x9f || c == 0x2028 || c == 0x2029) {
                throw new IllegalArgumentException("yaml would need an escape for " + value);
            }
        }
        if (PLAIN_UNSAFE.matcher(value).find() || CORE_SCHEMA.matcher(value).find()) {
            return value.contains("'") && !value.contains("\"") ? doubleQuoted(value) :
                    "'" + value.replace("'", "''") + "'";
        }
        return value;
    }

    public static String doubleQuoted(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
