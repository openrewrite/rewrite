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
package org.openrewrite.golang;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.golang.marker.GoResolutionResult;
import org.openrewrite.golang.marker.GoResolutionResult.Exclude;
import org.openrewrite.golang.marker.GoResolutionResult.Replace;
import org.openrewrite.golang.marker.GoResolutionResult.ResolvedDependency;
import org.openrewrite.golang.marker.GoResolutionResult.Retract;
import org.openrewrite.golang.marker.GoResolutionResult.Require;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Parses Go {@code go.mod} (and optionally {@code go.sum}) files into a {@link PlainText}
 * document with an attached {@link GoResolutionResult} marker.
 * <p>
 * <b>Why PlainText and not a dedicated tree?</b> go.mod has a very small syntax and doesn't benefit
 * from lossless AST reconstruction. The marker captures all structured data recipes need.
 * <p>
 * <b>go.sum lookup:</b> if {@code includeSumHashes} is set, the parser looks for a sibling
 * {@code go.sum} file next to each go.mod and parses recorded hashes into
 * {@link ResolvedDependency} entries on the marker.
 * <p>
 * <pre>
 *   GO.MOD GRAMMAR (relevant subset)
 *
 *   module github.com/foo/bar              ← module directive
 *   go 1.21                                ← toolchain version
 *   toolchain go1.22.0                     ← optional minimum toolchain
 *
 *   require github.com/x/y v1.2.3          ← single-line require
 *   require (                              ← block require
 *       github.com/a/b v1.0.0
 *       github.com/c/d v2.0.0 // indirect
 *   )
 *
 *   replace github.com/old => github.com/new v1.0.0
 *   replace github.com/old v1 => ../local/path
 *   replace ( ... )                        ← block form
 *
 *   exclude github.com/bad v1.2.3
 *   exclude ( ... )                        ← block form
 *
 *   retract v1.0.0 // buggy
 *   retract [v1.0.0, v1.1.0]
 *   retract ( ... )                        ← block form
 * </pre>
 * <p>
 * Comments ({@code // ...}) are preserved in rationale fields where meaningful
 * (retract lines) and otherwise stripped.
 */
public class GoModParser implements Parser {

    private static final Pattern MODULE_LINE = Pattern.compile("^\\s*module\\s+(\\S+)\\s*(?://.*)?$");
    private static final Pattern GO_LINE = Pattern.compile("^\\s*go\\s+(\\S+)\\s*(?://.*)?$");
    private static final Pattern TOOLCHAIN_LINE = Pattern.compile("^\\s*toolchain\\s+(\\S+)\\s*(?://.*)?$");

    private static final Pattern REQUIRE_SINGLE = Pattern.compile(
            "^\\s*require\\s+(\\S+)\\s+(\\S+)\\s*(//\\s*indirect)?\\s*(?://.*)?$");
    private static final Pattern REQUIRE_ENTRY = Pattern.compile(
            "^\\s*(\\S+)\\s+(\\S+)\\s*(//\\s*indirect)?\\s*(?://.*)?$");

    private static final Pattern REPLACE_SINGLE = Pattern.compile(
            "^\\s*replace\\s+(\\S+)(?:\\s+(\\S+))?\\s+=>\\s+(\\S+)(?:\\s+(\\S+))?\\s*(?://.*)?$");
    private static final Pattern REPLACE_ENTRY = Pattern.compile(
            "^\\s*(\\S+)(?:\\s+(\\S+))?\\s+=>\\s+(\\S+)(?:\\s+(\\S+))?\\s*(?://.*)?$");

    private static final Pattern EXCLUDE_SINGLE = Pattern.compile(
            "^\\s*exclude\\s+(\\S+)\\s+(\\S+)\\s*(?://.*)?$");
    private static final Pattern EXCLUDE_ENTRY = Pattern.compile(
            "^\\s*(\\S+)\\s+(\\S+)\\s*(?://.*)?$");

    private static final Pattern RETRACT_SINGLE = Pattern.compile(
            "^\\s*retract\\s+(\\S+|\\[[^\\]]+\\])\\s*(//.*)?$");
    private static final Pattern RETRACT_ENTRY = Pattern.compile(
            "^\\s*(\\S+|\\[[^\\]]+\\])\\s*(//.*)?$");

    private static final Pattern GO_SUM_LINE = Pattern.compile(
            "^\\s*(\\S+)\\s+(\\S+?)(/go\\.mod)?\\s+h1:(\\S+)\\s*$");

    private final PlainTextParser delegate = new PlainTextParser();

    @Override
    public Stream<SourceFile> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo, ExecutionContext ctx) {
        return delegate.parseInputs(sources, relativeTo, ctx).map(sf -> {
            if (!(sf instanceof PlainText)) {
                return sf;
            }
            PlainText pt = (PlainText) sf;
            GoResolutionResult marker = parseMarker(pt);
            if (marker == null) {
                return sf;
            }
            return pt.withMarkers(pt.getMarkers().addIfAbsent(marker));
        });
    }

    @Override
    public boolean accept(Path path) {
        String filename = path.getFileName().toString();
        return "go.mod".equals(filename);
    }

    @Override
    public Path sourcePathFromSourceText(Path prefix, String sourceCode) {
        return prefix.resolve("go.mod");
    }

    static @Nullable GoResolutionResult parseMarker(PlainText doc) {
        String content = doc.getText();
        if (content == null || content.isEmpty()) {
            return null;
        }

        String modulePath = null;
        String goVersion = null;
        String toolchain = null;
        List<Require> requires = new ArrayList<>();
        List<Replace> replaces = new ArrayList<>();
        List<Exclude> excludes = new ArrayList<>();
        List<Retract> retracts = new ArrayList<>();

        String[] lines = content.split("\\r?\\n", -1);
        BlockState block = BlockState.NONE;

        for (String rawLine : lines) {
            String line = stripInlineComment(rawLine);
            if (line.isEmpty()) {
                continue;
            }

            // Close block on trailing ')'
            if (block != BlockState.NONE && line.trim().equals(")")) {
                block = BlockState.NONE;
                continue;
            }

            // Inside a block: parse entries without leading keyword
            if (block != BlockState.NONE) {
                parseBlockEntry(block, rawLine, line, requires, replaces, excludes, retracts);
                continue;
            }

            // Top-level: identify directive.
            // Retract rationale is preserved by matching RETRACT_SINGLE against rawLine.
            Matcher m;
            if ((m = MODULE_LINE.matcher(line)).matches()) {
                modulePath = m.group(1);
            } else if ((m = GO_LINE.matcher(line)).matches()) {
                goVersion = m.group(1);
            } else if ((m = TOOLCHAIN_LINE.matcher(line)).matches()) {
                toolchain = m.group(1);
            } else if (line.trim().startsWith("require") && line.trim().endsWith("(")) {
                block = BlockState.REQUIRE;
            } else if (line.trim().startsWith("replace") && line.trim().endsWith("(")) {
                block = BlockState.REPLACE;
            } else if (line.trim().startsWith("exclude") && line.trim().endsWith("(")) {
                block = BlockState.EXCLUDE;
            } else if (line.trim().startsWith("retract") && line.trim().endsWith("(")) {
                block = BlockState.RETRACT;
            } else if ((m = REQUIRE_SINGLE.matcher(line)).matches()) {
                requires.add(new Require(m.group(1), m.group(2), m.group(3) != null));
            } else if ((m = REPLACE_SINGLE.matcher(line)).matches()) {
                String oldPath = m.group(1);
                String oldVersion = m.group(2);
                String newPath = m.group(3);
                String newVersion = m.group(4);
                replaces.add(new Replace(oldPath, oldVersion, newPath, newVersion));
            } else if ((m = EXCLUDE_SINGLE.matcher(line)).matches()) {
                excludes.add(new Exclude(m.group(1), m.group(2)));
            } else if ((m = RETRACT_SINGLE.matcher(rawLine)).matches()) {
                String rationale = m.group(2) != null ? trimComment(m.group(2)) : null;
                retracts.add(new Retract(m.group(1), rationale));
            }
        }

        if (modulePath == null) {
            return null;
        }

        List<ResolvedDependency> resolved = parseSumSibling(doc.getSourcePath());

        return new GoResolutionResult(
                Tree.randomId(),
                modulePath,
                goVersion,
                toolchain,
                doc.getSourcePath().toString(),
                requires,
                replaces,
                excludes,
                retracts,
                resolved
        );
    }

    private static void parseBlockEntry(BlockState block, String rawLine, String line,
                                        List<Require> requires, List<Replace> replaces,
                                        List<Exclude> excludes, List<Retract> retracts) {
        Matcher m;
        switch (block) {
            case REQUIRE:
                if ((m = REQUIRE_ENTRY.matcher(line)).matches()) {
                    requires.add(new Require(m.group(1), m.group(2), m.group(3) != null));
                }
                break;
            case REPLACE:
                if ((m = REPLACE_ENTRY.matcher(line)).matches()) {
                    replaces.add(new Replace(m.group(1), m.group(2), m.group(3), m.group(4)));
                }
                break;
            case EXCLUDE:
                if ((m = EXCLUDE_ENTRY.matcher(line)).matches()) {
                    excludes.add(new Exclude(m.group(1), m.group(2)));
                }
                break;
            case RETRACT:
                // Raw line so we can extract rationale from an inline comment preserved there
                if ((m = RETRACT_ENTRY.matcher(rawLine)).matches()) {
                    String rationale = m.group(2) != null ? trimComment(m.group(2)) : null;
                    retracts.add(new Retract(m.group(1), rationale));
                }
                break;
            case NONE:
                break;
        }
    }

    private static List<ResolvedDependency> parseSumSibling(Path goModPath) {
        List<ResolvedDependency> resolved = new ArrayList<>();
        Path sumPath = goModPath.resolveSibling("go.sum");
        java.io.File sumFile = sumPath.toFile();
        if (!sumFile.isFile()) {
            return resolved;
        }
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(sumFile))) {
            // go.sum format: "<module> <version>[/go.mod] h1:<hash>"
            // Each module version appears on two lines: one for the module zip, one for its go.mod file.
            java.util.Map<String, String[]> byKey = new java.util.LinkedHashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = GO_SUM_LINE.matcher(line);
                if (!m.matches()) {
                    continue;
                }
                String module = m.group(1);
                String version = m.group(2);
                boolean isGoMod = m.group(3) != null;
                String hash = m.group(4);
                String key = module + "@" + version;
                String[] slot = byKey.computeIfAbsent(key, k -> new String[2]);
                if (isGoMod) {
                    slot[1] = "h1:" + hash;
                } else {
                    slot[0] = "h1:" + hash;
                }
            }
            for (java.util.Map.Entry<String, String[]> e : byKey.entrySet()) {
                String[] parts = e.getKey().split("@", 2);
                resolved.add(new ResolvedDependency(parts[0], parts[1], e.getValue()[0], e.getValue()[1]));
            }
        } catch (java.io.IOException ignored) {
            // go.sum read failures are non-fatal — return whatever we collected
        }
        return resolved;
    }

    /**
     * Strip {@code // ...} comment but leave {@code // indirect} marker intact for require directives.
     * The caller is expected to further handle the "// indirect" suffix via pattern matching.
     */
    private static String stripInlineComment(String line) {
        int idx = line.indexOf("//");
        if (idx < 0) {
            return line;
        }
        // Preserve "// indirect" marker for requires
        String tail = line.substring(idx).trim();
        if (tail.startsWith("// indirect")) {
            return line;
        }
        String head = line.substring(0, idx);
        int end = head.length();
        while (end > 0 && Character.isWhitespace(head.charAt(end - 1))) {
            end--;
        }
        return head.substring(0, end);
    }

    private static String trimComment(String comment) {
        String trimmed = comment.trim();
        if (trimmed.startsWith("//")) {
            trimmed = trimmed.substring(2).trim();
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private enum BlockState {
        NONE, REQUIRE, REPLACE, EXCLUDE, RETRACT
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Parser.Builder {
        public Builder() {
            super(PlainText.class);
        }

        @Override
        public GoModParser build() {
            return new GoModParser();
        }

        @Override
        public String getDslName() {
            return "gomod";
        }
    }
}
