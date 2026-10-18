/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.test;

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.TreeShapeValidationService;
import org.openrewrite.tree.ParseError;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.fail;

/**
 * Finds nodes in a recipe's output that its parser builds differently from the text they print, which recipes that
 * run after it don't match the way they match parsed code.
 */
final class TreeShapes {
    private static final int MAX_PER_FILE = 5;
    private static final int SNIPPET_LENGTH = 60;

    private final boolean enabled;
    private final Function<SourceFile, List<Parser.Builder>> parsersFor;
    private final @Nullable Path relativeTo;
    private final ExecutionContext parseCtx;

    TreeShapes(boolean enabled, Function<SourceFile, List<Parser.Builder>> parsersFor, @Nullable Path relativeTo,
               ExecutionContext parseCtx) {
        this.enabled = enabled;
        this.parsersFor = parsersFor;
        this.relativeTo = relativeTo;
        this.parseCtx = parseCtx;
    }

    /**
     * @param sources The sources the recipe ran on, which are parsed again with the ones it changed so that a source
     *                can refer to the others, as a Maven module does to its parent.
     */
    void assertParserShaped(Collection<Result> results, List<SourceFile> sources) {
        if (!enabled) {
            return;
        }
        // each source as the recipe left it
        List<SourceFile> current = new ArrayList<>();
        Set<UUID> changed = new HashSet<>();
        for (Result result : results) {
            if (result.getAfter() != null) {
                current.add(result.getAfter());
            }
            if (result.getBefore() != null) {
                changed.add(result.getBefore().getId());
            }
        }
        for (SourceFile source : sources) {
            if (!changed.contains(source.getId())) {
                current.add(source);
            }
        }
        Map<Parser.Builder, List<Result>> byParser = new LinkedHashMap<>();
        for (Result result : results) {
            SourceFile after = result.getAfter();
            SourceFile before = result.getBefore();
            TreeShapeValidationService service = after == null ? null : service(after);
            if (after == null || service == null || printed(after).trim().isEmpty() ||
                before != null && printed(before).equals(printed(after))) {
                continue;
            }
            List<Parser.Builder> parsers = parsersFor.apply(after);
            if (!parsers.isEmpty() && service.reads(parsers.get(0), after)) {
                byParser.computeIfAbsent(parsers.get(0), p -> new ArrayList<>()).add(result);
            }
        }
        StringBuilder found = new StringBuilder();
        for (Map.Entry<Parser.Builder, List<Result>> group : byParser.entrySet()) {
            Map<UUID, Reparsed> reparsed = reparse(group.getKey(), current);
            for (Result result : group.getValue()) {
                SourceFile after = Objects.requireNonNull(result.getAfter());
                Reparsed parse = reparsed.get(after.getId());
                List<String> problems = parse == null ? Collections.emptyList() :
                        problems(Objects.requireNonNull(service(after)), after, result.getBefore(), parse);
                if (!problems.isEmpty()) {
                    found.append("  ").append(after.getSourcePath()).append(NameMatching.changedBy(result))
                            .append(":\n");
                    for (String problem : problems) {
                        found.append("    ").append(problem).append('\n');
                    }
                }
            }
        }
        if (found.length() > 0) {
            fail("Write code with a template or the format's own parser, or build the nodes the way its parser builds " +
                 "the text they print. The recipe's output holds nodes that its parser makes differently of the " +
                 "same text:\n" + found +
                 "A recipe that runs after it matches the tree, not the text, so it misses or mistakes a node that " +
                 "parsed code never holds. If the output is meant to differ from parsed code, add " +
                 "spec.recipeQualityValidation(RecipeQualityValidation.builder().parserShapedTrees(false).build()).");
        }
    }

    private List<String> problems(TreeShapeValidationService service, SourceFile after, @Nullable SourceFile before,
                                  Reparsed reparsed) {
        List<String> problems = new ArrayList<>();
        if (reparsed.error != null) {
            problems.add("the text it prints doesn't parse: " + reparsed.error);
            return problems;
        }
        compare(service, skeleton(service, after), skeleton(service, Objects.requireNonNull(reparsed.sourceFile)),
                problems);
        // a file that already broke its language's rules, such as JSON5 in a .json file, follows other ones
        TreeShapeValidationService beforeService = before == null ? null : service(before);
        if (before == null || beforeService == null || beforeService.invalid(before).isEmpty()) {
            for (String problem : service.invalid(after)) {
                if (problems.size() < MAX_PER_FILE) {
                    problems.add(problem);
                }
            }
        }
        return problems;
    }

    private static void compare(TreeShapeValidationService service, Node node, Node parsed, List<String> problems) {
        if (problems.size() >= MAX_PER_FILE) {
            return;
        }
        if (node.tree.getClass() != parsed.tree.getClass()) {
            if (!service.equivalent(node.tree, parsed.tree)) {
                problems.add(node + " is a " + Probes.typeName(node.tree.getClass()) + " where the parser makes a " +
                             Probes.typeName(parsed.tree.getClass()));
            }
            return;
        }
        Object value = service.value(node.tree);
        Object parsedValue = service.value(parsed.tree);
        if (!Objects.equals(value, parsedValue)) {
            problems.add(node + " has " + value + " where the parser makes " + parsedValue);
        }
        if (node.children.size() != parsed.children.size()) {
            if (!service.equivalent(node.tree, parsed.tree)) {
                problems.add(node + " holds " + kinds(node.children) + " where the parser makes " +
                             kinds(parsed.children));
            }
            return;
        }
        for (int i = 0; i < node.children.size(); i++) {
            compare(service, node.children.get(i), parsed.children.get(i), problems);
        }
    }

    private static String kinds(List<Node> nodes) {
        if (nodes.isEmpty()) {
            return "nothing";
        }
        StringJoiner kinds = new StringJoiner(", ");
        for (Node node : nodes) {
            kinds.add(Probes.typeName(node.tree.getClass()));
        }
        return kinds.toString();
    }

    /**
     * The nodes of a source file, each with the nodes it holds, leaving out those the language's service skips.
     */
    private static Node skeleton(TreeShapeValidationService service, SourceFile sourceFile) {
        List<Node> roots = new ArrayList<>(1);
        Deque<Node> path = new ArrayDeque<>();
        new TreeVisitor<Tree, Integer>() {
            @Override
            public Tree preVisit(Tree tree, Integer p) {
                if (service.skip(tree)) {
                    stopAfterPreVisit();
                    return tree;
                }
                long ancestors = getCursor().getPathAsStream(o -> o instanceof Tree).count() - 1;
                while (path.size() > ancestors) {
                    path.pop();
                }
                Node node = new Node(tree, getCursor());
                if (path.isEmpty()) {
                    roots.add(node);
                } else {
                    path.peek().children.add(node);
                }
                path.push(node);
                return tree;
            }
        }.visit(sourceFile, 0);
        return roots.isEmpty() ? new Node(sourceFile, new Cursor(null, Cursor.ROOT_VALUE)) : roots.get(0);
    }

    /**
     * A fresh parse, by {@code builder}, of the printed text of each of the {@code current} sources that it parses,
     * by the id of the source, leaving out the ones it doesn't parse back to the same kind of source.
     */
    private Map<UUID, Reparsed> reparse(Parser.Builder builder, List<SourceFile> current) {
        List<SourceFile> sources = new ArrayList<>();
        List<String> printed = new ArrayList<>();
        List<Parser.Input> inputs = new ArrayList<>();
        for (SourceFile source : current) {
            List<Parser.Builder> parsers = parsersFor.apply(source);
            if (parsers.isEmpty() || parsers.get(0) != builder) {
                continue;
            }
            sources.add(source);
            printed.add(printed(source));
            inputs.add(input(source, printed.get(printed.size() - 1)));
        }
        Map<UUID, Reparsed> reparsed = new HashMap<>();
        List<Throwable> errors = new ArrayList<>();
        List<SourceFile> parsed = parse(builder, inputs, errors);
        if (parsed == null) {
            return reparsed;
        }
        for (int i = 0; i < sources.size(); i++) {
            SourceFile source = sources.get(i);
            // parsers return their sources in the order of their inputs, which can share a path
            SourceFile parse = parsed.size() == sources.size() ? parsed.get(i) : samePath(parsed, source);
            if (parse == null || !parse.getSourcePath().equals(source.getSourcePath())) {
                continue;
            }
            Reparsed reparse = reparsed(source, printed.get(i), parse, errors, true);
            if (reparse != null) {
                reparsed.put(source.getId(), reparse);
            }
        }
        return reparsed;
    }

    /**
     * How a fresh parse of {@code printed} compares with what {@code source} is, or null when it can't tell.
     */
    private @Nullable Reparsed reparsed(SourceFile source, String printed, SourceFile parse, List<Throwable> errors,
                                        boolean trySyntaxParser) {
        TreeShapeValidationService service = service(source);
        Parser.Builder syntaxParser = service == null ? null : service.syntaxParser();
        if (parse instanceof ParseError && trySyntaxParser && syntaxParser != null) {
            // a parser that also checks the model, as Maven's does, can reject text that is valid for the format
            List<Throwable> syntaxErrors = new ArrayList<>();
            List<SourceFile> syntax = parse(syntaxParser, Collections.singletonList(input(source, printed)),
                    syntaxErrors);
            if (syntax != null && syntax.size() == 1) {
                return reparsed(source, printed, syntax.get(0), syntaxErrors, false);
            }
        }
        if (parse instanceof ParseError) {
            ParseExceptionResult exception = parse.getMarkers().findFirst(ParseExceptionResult.class).orElse(null);
            return new Reparsed(null, exception == null ? "a ParseError" : firstLine(exception.getMessage()));
        } else if (parse.getClass() != source.getClass()) {
            return null;
        } else if (!printed(parse).equals(printed)) {
            // a parser that reports a syntax error can still return a tree, and one that doesn't print back what it
            // read without one is the parser's to fix
            return errors.isEmpty() ? null : new Reparsed(null, firstLine(String.valueOf(errors.get(0).getMessage())));
        }
        return new Reparsed(parse, null);
    }

    private @Nullable List<SourceFile> parse(Parser.Builder builder, List<Parser.Input> inputs,
                                             List<Throwable> errors) {
        ExecutionContext ctx = new InMemoryExecutionContext(errors::add);
        parseCtx.getMessages().forEach(ctx::putMessage);
        ctx.putMessage(ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT, false);
        try {
            return builder.clone().build().parseInputs(inputs, relativeTo, ctx).collect(toList());
        } catch (Exception | LinkageError e) {
            return null;
        }
    }

    private Parser.Input input(SourceFile source, String printed) {
        Charset charset = source.getCharset() == null ? StandardCharsets.UTF_8 : source.getCharset();
        Path sourcePath = relativeTo == null ? source.getSourcePath() : relativeTo.resolve(source.getSourcePath());
        return Parser.Input.fromString(sourcePath, printed, charset);
    }

    private static @Nullable SourceFile samePath(List<SourceFile> parsed, SourceFile source) {
        SourceFile found = null;
        for (SourceFile parse : parsed) {
            if (parse.getSourcePath().equals(source.getSourcePath())) {
                if (found != null) {
                    return null;
                }
                found = parse;
            }
        }
        return found;
    }

    private static String firstLine(String message) {
        String line = message.trim().split("\\R", 2)[0];
        return line.length() > 200 ? line.substring(0, 200) + "..." : line;
    }

    private static String printed(SourceFile sourceFile) {
        return sourceFile.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED));
    }

    private static @Nullable TreeShapeValidationService service(SourceFile sourceFile) {
        try {
            return sourceFile.service(TreeShapeValidationService.class);
        } catch (UnsupportedOperationException e) {
            return null;
        } catch (RuntimeException e) {
            // Groovy, Kotlin and Scala wrap what the Java source file throws for a service they don't support
            if (e.getCause() instanceof UnsupportedOperationException) {
                return null;
            }
            throw e;
        }
    }

    private static final class Reparsed {
        final @Nullable SourceFile sourceFile;
        final @Nullable String error;

        Reparsed(@Nullable SourceFile sourceFile, @Nullable String error) {
            this.sourceFile = sourceFile;
            this.error = error;
        }
    }

    private static final class Node {
        final Tree tree;
        final Cursor cursor;
        final List<Node> children = new ArrayList<>();

        Node(Tree tree, Cursor cursor) {
            this.tree = tree;
            this.cursor = cursor;
        }

        @Override
        public String toString() {
            String printed;
            try {
                Cursor parent = cursor.getParent();
                printed = tree.printTrimmed(parent == null ? new Cursor(null, Cursor.ROOT_VALUE) : parent);
            } catch (Exception e) {
                printed = "?";
            }
            printed = printed.replaceAll("\\s+", " ");
            if (printed.length() > SNIPPET_LENGTH) {
                printed = printed.substring(0, SNIPPET_LENGTH) + "...";
            }
            return "`" + printed + "`";
        }
    }
}
