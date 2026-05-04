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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.openrewrite.golang.marker.GoResolutionResult;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Java-side driver for the shared go.mod / go.sum conformance corpus.
 * <p>
 * Iterates every {@code *.gomod} under
 * {@code src/test/resources/gomod-conformance/}, parses it (with sibling
 * {@code *.gosum} when present), and asserts the resulting marker matches
 * the canonical JSON shape in the case's {@code *.gomod.json} golden.
 * <p>
 * The Go-side {@code TestGoModConformanceCorpus} runs the same corpus.
 * Drift between the two indicates a parser parity bug.
 */
class GoModConformanceTest {

    private static final Path CORPUS_DIR = Paths.get("src/test/resources/gomod-conformance");
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    @TestFactory
    Stream<DynamicTest> conformanceCorpus() throws IOException {
        try (Stream<Path> entries = Files.list(CORPUS_DIR)) {
            List<Path> gomods = entries
                    .filter(p -> p.toString().endsWith(".gomod"))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .collect(Collectors.toList());
            assertThat(gomods).as("conformance corpus is non-empty").isNotEmpty();
            return gomods.stream().map(p -> DynamicTest.dynamicTest(stripGomod(p.getFileName().toString()), () -> runCase(p)));
        }
    }

    private static String stripGomod(String name) {
        return name.substring(0, name.length() - ".gomod".length());
    }

    private static void runCase(Path goModPath) throws IOException {
        String caseName = stripGomod(goModPath.getFileName().toString());
        String modContent = new String(Files.readAllBytes(goModPath), StandardCharsets.UTF_8);

        // Parse via the production GoModParser path (PlainText delegate +
        // marker decoration). Use a virtual source path so parseSumSibling
        // doesn't accidentally read a real go.sum on disk.
        PlainTextParser delegate = new PlainTextParser();
        ExecutionContext ctx = new InMemoryExecutionContext();
        SourceFile sf = delegate.parse(modContent).iterator().next();
        PlainText pt = (PlainText) sf;
        GoResolutionResult marker = GoModParser.parseMarker(pt);
        assertThat(marker).as("parseMarker for case %s", caseName).isNotNull();

        // If a sibling .gosum is present, attach its parsed contents.
        Path sumPath = goModPath.resolveSibling(caseName + ".gosum");
        if (Files.isRegularFile(sumPath)) {
            String sumContent = new String(Files.readAllBytes(sumPath), StandardCharsets.UTF_8);
            marker = marker.withResolvedDependencies(GoModParser.parseSumContent(sumContent));
        }

        ObjectNode actual = toConformance(marker);

        Path goldenPath = goModPath.resolveSibling(caseName + ".gomod.json");
        ObjectNode expected = (ObjectNode) MAPPER.readTree(goldenPath.toFile());

        assertThat(actual).as("conformance shape for case %s", caseName).isEqualTo(expected);
    }

    /**
     * Convert a {@link GoResolutionResult} marker into the canonical
     * conformance JSON shape. Fields and ordering MUST stay in sync with
     * the Go-side {@code conformanceShape} in
     * {@code test/gomod_conformance_test.go}.
     */
    private static ObjectNode toConformance(GoResolutionResult m) {
        ObjectNode out = MAPPER.createObjectNode();
        out.put("modulePath", m.getModulePath());
        out.put("goVersion", m.getGoVersion() == null ? "" : m.getGoVersion());
        out.put("toolchain", m.getToolchain() == null ? "" : m.getToolchain());
        out.set("requires", MAPPER.valueToTree(toRequires(m.getRequires())));
        out.set("replaces", MAPPER.valueToTree(toReplaces(m.getReplaces())));
        out.set("excludes", MAPPER.valueToTree(toExcludes(m.getExcludes())));
        out.set("retracts", MAPPER.valueToTree(toRetracts(m.getRetracts())));
        out.set("resolvedDependencies", MAPPER.valueToTree(toResolved(m.getResolvedDependencies())));
        return out;
    }

    private static List<ObjectNode> toRequires(List<GoResolutionResult.Require> reqs) {
        List<ObjectNode> out = new ArrayList<>();
        if (reqs == null) return out;
        for (GoResolutionResult.Require r : reqs) {
            ObjectNode n = MAPPER.createObjectNode();
            n.put("modulePath", r.getModulePath());
            n.put("version", r.getVersion());
            n.put("indirect", r.isIndirect());
            out.add(n);
        }
        return out;
    }

    private static List<ObjectNode> toReplaces(List<GoResolutionResult.Replace> reps) {
        List<ObjectNode> out = new ArrayList<>();
        if (reps == null) return out;
        for (GoResolutionResult.Replace r : reps) {
            ObjectNode n = MAPPER.createObjectNode();
            n.put("oldPath", r.getOldPath());
            putNullable(n, "oldVersion", r.getOldVersion());
            n.put("newPath", r.getNewPath());
            putNullable(n, "newVersion", r.getNewVersion());
            out.add(n);
        }
        return out;
    }

    private static List<ObjectNode> toExcludes(List<GoResolutionResult.Exclude> excs) {
        List<ObjectNode> out = new ArrayList<>();
        if (excs == null) return out;
        for (GoResolutionResult.Exclude e : excs) {
            ObjectNode n = MAPPER.createObjectNode();
            n.put("modulePath", e.getModulePath());
            n.put("version", e.getVersion());
            out.add(n);
        }
        return out;
    }

    private static List<ObjectNode> toRetracts(List<GoResolutionResult.Retract> rets) {
        List<ObjectNode> out = new ArrayList<>();
        if (rets == null) return out;
        for (GoResolutionResult.Retract r : rets) {
            ObjectNode n = MAPPER.createObjectNode();
            n.put("versionRange", r.getVersionRange());
            putNullable(n, "rationale", r.getRationale());
            out.add(n);
        }
        return out;
    }

    private static List<ObjectNode> toResolved(List<GoResolutionResult.ResolvedDependency> deps) {
        List<ObjectNode> out = new ArrayList<>();
        if (deps == null) return out;
        for (GoResolutionResult.ResolvedDependency d : deps) {
            ObjectNode n = MAPPER.createObjectNode();
            n.put("modulePath", d.getModulePath());
            n.put("version", d.getVersion());
            putNullable(n, "moduleHash", d.getModuleHash());
            putNullable(n, "goModHash", d.getGoModHash());
            out.add(n);
        }
        return out;
    }

    private static void putNullable(ObjectNode n, String key, String value) {
        if (value == null) {
            n.putNull(key);
        } else {
            n.put(key, value);
        }
    }
}
