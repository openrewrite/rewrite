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
package org.openrewrite.coldfusion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.coldfusion.tree.Cf;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.tree.ParseError;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parses every CFML file under {@code -Dcoldfusion.corpus.dir=...} and prints a parse rate plus a
 * histogram of failure causes. Opt-in: it measures where parser work is needed, and asserts nothing
 * about any particular corpus.
 */
class ColdFusionCorpusTest {

    @Test
    @EnabledIfSystemProperty(named = "coldfusion.corpus.dir", matches = ".+")
    void parseCorpus() throws IOException {
        Path root = Paths.get(System.getProperty("coldfusion.corpus.dir"));
        ColdFusionParser parser = ColdFusionParser.builder().build();

        List<Path> paths = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile).filter(parser::accept).sorted().forEach(paths::add);
        }

        int parsed = 0;
        Map<String, Integer> causes = new HashMap<>();
        Map<String, String> examples = new HashMap<>();
        Map<String, String> details = new HashMap<>();
        List<String> failures = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        int detail = Integer.getInteger("coldfusion.corpus.detail", 300);
        Map<String, Integer> suspicions = new TreeMap<>();
        Map<String, String> suspicionExamples = new HashMap<>();

        for (Path path : paths) {
            // swallowing errors keeps one bad file from ending the run
            ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            });
            SourceFile source;
            try {
                source = parser.parseInputs(List.of(new Parser.Input(path, () -> {
                    try {
                        return Files.newInputStream(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })), root, ctx).findFirst().orElse(null);
            } catch (Throwable t) {
                source = null;
            }
            if (source != null && !(source instanceof ParseError)) {
                parsed++;
                census(root.relativize(path).toString(), source, suspicions, suspicionExamples);
                continue;
            }
            String message = source == null ? "no result" :
                    source.getMarkers().findFirst(ParseExceptionResult.class)
                            .map(ParseExceptionResult::getMessage)
                            .orElse("unknown");
            String cause = classify(message);
            causes.merge(cause, 1, Integer::sum);
            String example = root.relativize(path).toString();
            failures.add(cause + '\t' + example);
            messages.add("==== " + example + '\n' + message);
            examples.merge(cause, example, (a, b) -> a.split(", ").length < 3 ? a + ", " + b : a);
            details.computeIfAbsent(cause, c -> message.substring(0, Math.min(detail, message.length()))
                    .replace("\n", "\n      "));
        }

        StringBuilder report = new StringBuilder();
        report.append("ColdFusion corpus: ").append(root).append('\n');
        report.append(String.format("files=%d parsed=%d (%.1f%%) failed=%d%n", paths.size(), parsed,
                paths.isEmpty() ? 0d : 100d * parsed / paths.size(), paths.size() - parsed));
        report.append("top failure causes:\n");
        causes.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(30)
                .forEach(e -> report.append(String.format("  %5d  %s%n      e.g. %s%n      %s%n",
                        e.getValue(), e.getKey(), examples.get(e.getKey()), details.getOrDefault(e.getKey(), ""))));

        // a file can print back byte for byte and still be misparsed, so count shapes that suggest it
        report.append("suspicious shapes in parsed files:\n");
        suspicions.forEach((shape, count) -> report.append(String.format("  %5d  %s%n      e.g. %s%n",
                count, shape, suspicionExamples.get(shape))));

        System.out.println(report);
        // Gradle swallows test stdout by default, so the report is also written where it can be read
        Path out = Paths.get(System.getProperty("coldfusion.corpus.report", "build/coldfusion-corpus-report.txt"));
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        Files.write(out, report.toString().getBytes(StandardCharsets.UTF_8));
        failures.sort(Comparator.naturalOrder());
        Files.write(Paths.get(out + ".failures"), String.join("\n", failures).getBytes(StandardCharsets.UTF_8));
        Files.write(Paths.get(out + ".messages"), String.join("\n", messages).getBytes(StandardCharsets.UTF_8));
        if (!CONTEXT.isEmpty()) {
            Files.write(Paths.get(out + ".context"), String.join("\n", CONTEXT).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static final List<String> CONTEXT = new ArrayList<>();

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "if", "else", "for", "while", "do", "switch", "case", "try", "catch", "finally", "return",
            "function", "var", "component", "interface", "property", "import", "new"));

    private static void census(String path, SourceFile source, Map<String, Integer> suspicions, Map<String, String> examples) {
        Set<String> found = new HashSet<>();
        new ColdFusionIsoVisitor<Integer>() {
            @Override
            public Cf.Text visitText(Cf.Text text, Integer p) {
                if (text.getText().toLowerCase(Locale.ROOT).matches("(?s).*</?cf[a-z_].*")) {
                    found.add("template text holding a <cf tag");
                }
                return super.visitText(text, p);
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                Object parent = getCursor().getParentTreeCursor().getValue();
                // a keyword-named function, parameter, member, type or attribute is ordinary CFML
                if (KEYWORDS.contains(identifier.getSimpleName().toLowerCase(Locale.ROOT)) &&
                    !(parent instanceof J.FieldAccess || parent instanceof Cf.TagAttribute || parent instanceof J.Annotation ||
                      parent instanceof J.MethodDeclaration || parent instanceof J.VariableDeclarations ||
                      parent instanceof J.VariableDeclarations.NamedVariable || parent instanceof Cf.KeyValue ||
                      parent instanceof Cf.ScriptTag || parent instanceof J.MethodInvocation &&
                                                       ((J.MethodInvocation) parent).getSelect() != null)) {
                    found.add("an identifier named " + identifier.getSimpleName().toLowerCase(Locale.ROOT) +
                              " in a " + parent.getClass().getSimpleName());
                }
                return identifier;
            }

            @Override
            public Cf.ExpressionStatement visitExpressionStatement(Cf.ExpressionStatement statement, Integer p) {
                if (statement.getExpression() instanceof J.Identifier) {
                    found.add("a bare identifier as a statement: " +
                              ((J.Identifier) statement.getExpression()).getSimpleName().toLowerCase(Locale.ROOT));
                    if (System.getProperty("coldfusion.corpus.context") != null) {
                        Object parent = getCursor().getParentTreeCursor().getValue();
                        String around = parent instanceof J ?
                                ((J) parent).printTrimmed(getCursor().getParentTreeCursor().getParentOrThrow()) : "";
                        int at = around.indexOf(((J.Identifier) statement.getExpression()).getSimpleName());
                        CONTEXT.add(path + ": " + around.substring(Math.max(0, at - 120),
                                Math.min(around.length(), at + 160)).replace("\n", "\\n"));
                    }
                }
                return super.visitExpressionStatement(statement, p);
            }
        }.visit(source, 0);
        if (path.toLowerCase(Locale.ROOT).endsWith(".cfc") && source instanceof Cf.CompilationUnit) {
            boolean component = false;
            boolean componentTag = false;
            for (Statement statement : ((Cf.CompilationUnit) source).getStatements()) {
                component |= statement instanceof J.ClassDeclaration;
                componentTag |= statement instanceof Cf.Tag && (((Cf.Tag) statement).isNamed("cfcomponent") ||
                                                                 ((Cf.Tag) statement).isNamed("cfinterface"));
            }
            if (!component && !componentTag) {
                found.add("a .cfc with neither a component nor a <cfcomponent>");
                if (System.getProperty("coldfusion.corpus.context") != null) {
                    String printed = source.printAll().replaceAll("(?s)<!---.*?--->", "").trim();
                    CONTEXT.add("COMPONENTLESS " + path + ": " + printed.substring(0, Math.min(80, printed.length()))
                            .replace("\n", "\\n"));
                }
            }
        }
        for (String shape : found) {
            suspicions.merge(shape, 1, Integer::sum);
            examples.merge(shape, path, (a, b) -> a.split(", ").length < 3 ? a + ", " + b : a);
        }
    }

    /**
     * Collapses a failure to the one line that says what the parser could not do, so the histogram
     * groups by cause rather than by file.
     */
    static String classify(String message) {
        for (String line : message.split("\n")) {
            int at = line.indexOf("ColdFusionParsingException: ");
            if (at >= 0) {
                String what = line.substring(at + "ColdFusionParsingException: ".length());
                int where = what.indexOf(" at ");
                return where < 0 ? what : what.substring(0, where);
            }
            if (line.contains("is not print idempotent")) {
                return "not print idempotent";
            }
        }
        for (String line : message.split("\n")) {
            if (line.contains("Exception") || line.contains("Error")) {
                String trimmed = line.trim();
                return trimmed.substring(Math.max(0, trimmed.lastIndexOf('.', Math.max(0, trimmed.indexOf(':'))) + 1));
            }
        }
        return message.split("\n")[0];
    }

    @Test
    void classifyKeepsWhatTheParserCouldNotDo() {
        assertThat(classify("org.openrewrite.coldfusion.ColdFusionParser$ColdFusionParsingException: " +
                            "expected ')' at template.cfm:1:12 near 'x'\n\tat org.openrewrite.Foo"))
                .isEqualTo("expected ')'");
        assertThat(classify("java.lang.IllegalStateException: template.cfm is not print idempotent. \ndiff"))
                .isEqualTo("not print idempotent");
    }
}
