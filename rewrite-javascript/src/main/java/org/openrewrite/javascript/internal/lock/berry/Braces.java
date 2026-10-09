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
package org.openrewrite.javascript.internal.lock.berry;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Port of braces 3.0.2 for {@code braces(pattern, {expand: true, nodupes: true, keepEscaping: true})}.
 */
final class Braces {

    private static final int MAX_LENGTH = 1024 * 64;
    private static final int RANGE_LIMIT = 1000;

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

    private Braces() {
    }

    static List<String> braces(String input) {
        List<String> output = input.length() < 3 ? Collections.singletonList(input) : expand(input);
        return new ArrayList<>(new LinkedHashSet<>(output));
    }

    private static List<String> expand(String input) {
        List<String> result = new ArrayList<>();
        for (Object item : walk(parse(input), null)) {
            flatten(item, result);
        }
        return new ArrayList<>(new LinkedHashSet<>(result));
    }

    private static Node parse(String input) {
        if (input.length() > MAX_LENGTH) {
            throw new JsError(JsError.SYNTAX_ERROR, "Input length (" + input.length() + "), exceeds max characters (" +
                                                    MAX_LENGTH + ")");
        }
        return new Parser(input).parse();
    }

    private static final class Parser {
        private final String input;
        private final int length;
        private final Node ast = Node.block("root");
        private final List<Node> stack = new ArrayList<>();
        private Node block = ast;
        private Node prev = ast;
        private int index;
        private int depth;

        Parser(String input) {
            this.input = input;
            this.length = input.length();
        }

        Node parse() {
            stack.add(ast);
            push(new Node("bos", null));

            while (index < length) {
                block = stack.get(stack.size() - 1);
                String value = advance();

                if ("\uFEFF".equals(value) || "\u00A0".equals(value)) {
                    continue;
                }

                if ("\\".equals(value)) {
                    push(new Node("text", value + str(advance())));
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
            Object range = FillRange.fill(args);
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
        return limit != null && (toNumber(max) - toNumber(min)) / toNumber(step) >= toNumber(limit);
    }

    private static boolean isInteger(@Nullable Object num) {
        if (num instanceof Double) {
            return Js.isInteger((Double) num);
        }
        return num instanceof String && !Js.trim((String) num).isEmpty() &&
               Js.isInteger(Js.toNumber((String) num));
    }

    private static double toNumber(Object value) {
        return value instanceof Double ? (Double) value : Js.toNumber((String) value);
    }

    private static String str(@Nullable String s) {
        return s == null ? "undefined" : s;
    }
}
