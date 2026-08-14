/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.ruby;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.tree.ParseError;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Parses every Ruby file under {@code -Druby.corpus.dir=...} and prints a parse rate plus a
 * histogram of failure causes. Opt-in: it is a measurement tool for prioritising parser work, not
 * an assertion about any particular corpus.
 */
@EnabledIfSystemProperty(named = "ruby.corpus.dir", matches = ".+")
public class RubyCorpusTest {

    @Test
    void parseCorpus() throws IOException {
        Path root = Paths.get(System.getProperty("ruby.corpus.dir"));
        RubyParser parser = RubyParser.builder().build();

        List<Path> paths = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile).filter(parser::accept).forEach(paths::add);
        }

        int parsed = 0;
        int failed = 0;
        Map<String, Integer> causes = new LinkedHashMap<>();
        Map<String, String> examples = new LinkedHashMap<>();
        Map<String, String> details = new LinkedHashMap<>();
        List<String> failures = new ArrayList<>();
        List<String> messages = new ArrayList<>();

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
            if (!(source instanceof ParseError)) {
                if (source != null) {
                    parsed++;
                    continue;
                }
            }
            failed++;
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
            int detail = Integer.getInteger("ruby.corpus.detail", 300);
            details.computeIfAbsent(cause, c ->
                    message.substring(0, Math.min(detail, message.length())).replace("\n", "\n      "));
        }

        StringBuilder report = new StringBuilder();
        report.append("Ruby corpus: ").append(root).append('\n');
        report.append(String.format("files=%d parsed=%d (%.1f%%) failed=%d%n",
                paths.size(), parsed, paths.isEmpty() ? 0d : 100d * parsed / paths.size(), failed));
        report.append("top failure causes:\n");
        causes.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(15)
                .forEach(e -> report.append(String.format("  %5d  %s%n      e.g. %s%n      %s%n",
                        e.getValue(), e.getKey(), examples.get(e.getKey()),
                        details.getOrDefault(e.getKey(), ""))));

        System.out.println(report);
        // Gradle swallows test stdout by default, so the report is also written where it can be read
        Path out = Paths.get(System.getProperty("ruby.corpus.report", "build/ruby-corpus-report.txt"));
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        Files.write(out, report.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // one `cause<TAB>path` line per failure, so a bucket can be worked through file by file,
        // and the full messages next to it, since the histogram only keeps one sample per cause
        failures.sort(Comparator.naturalOrder());
        Files.write(Paths.get(out + ".failures"),
                String.join("\n", failures).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Files.write(Paths.get(out + ".messages"),
                String.join("\n", messages).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Collapses a stack trace down to the one line that says what the parser could not do, so the
     * histogram groups by cause rather than by file.
     */
    private static String classify(String message) {
        for (String line : message.split("\n")) {
            if (line.contains("Prism node type")) {
                int start = line.indexOf("Prism node type");
                int end = line.indexOf(" is not yet implemented");
                return "unimplemented: " + (end > start ? line.substring(start + 16, end) : line.trim());
            }
            if (line.contains("Cursor desync")) {
                int at = line.indexOf("(start of ");
                return at < 0 ? "cursor desync" :
                        "cursor desync at " + line.substring(at + 10, line.indexOf(')', at));
            }
            if (line.contains("is not print idempotent")) {
                return "not print idempotent";
            }
            if (line.contains("[") && line.contains("/ERROR_")) {
                int open = line.indexOf('[');
                int slash = line.indexOf('/', open);
                return "syntax error: " + line.substring(open + 1, slash);
            }
            if (line.contains("Exception:") || line.contains("Error:")) {
                String trimmed = line.trim();
                int colon = trimmed.lastIndexOf("Exception:");
                if (colon < 0) {
                    colon = trimmed.lastIndexOf("Error:");
                }
                return trimmed.substring(Math.max(0, trimmed.lastIndexOf('.', colon) + 1));
            }
        }
        return message.split("\n")[0];
    }
}
