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
package org.openrewrite.maven.parity.corpus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.tree.MavenResolutionResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.stream.Collectors.toList;

/**
 * Phase-0 corpus runner: resolves every corpus entry with the released rewrite-maven engine
 * (MavenParser), serializes the resulting {@link MavenResolutionResult} marker to deterministic
 * JSON, and writes {@code .corpus/snapshots/<entry>.json}. In {@code record} mode resolution goes
 * through {@link RecordingHttpSender} RECORD (populating the store from the network); in
 * {@code replay} mode (the default) every entry runs TWICE with fresh caches against the store
 * only, comparing outputs byte-wise. Any replay miss or nondeterminism is a finding, not
 * something to paper over.
 * <p>
 * The JSON here is a plain Jackson projection with stable key order; the formal
 * ResolutionSnapshot from slice A replaces it when the slices merge into rewrite-maven.
 */
public class CorpusResolutionRunner {
    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .serializationInclusion(NON_NULL)
            .addMixIn(MavenResolutionResult.class, FlattenReactorLinks.class)
            .build();

    /**
     * Reactor linkage is captured as GAV references in the document node instead; serializing
     * parent/modules would embed the whole reactor graph into every module's snapshot
     * (quadratic — OOMs on dubbo's ~190 modules).
     */
    abstract static class FlattenReactorLinks {
        @JsonIgnore
        abstract MavenResolutionResult getParent();

        @JsonIgnore
        abstract List<MavenResolutionResult> getModules();
    }

    public static void main(String[] args) throws Exception {
        String mode = System.getProperty("corpus.mode", "replay");
        boolean record = "record".equals(mode);
        Set<String> filter = new HashSet<>(Arrays.asList(args));
        CorpusManifest manifest = CorpusManifest.load(CorpusPaths.manifest());
        Files.createDirectories(CorpusPaths.snapshots());

        List<String> findings = new ArrayList<>();
        for (CorpusManifest.Entry entry : manifest.getEntries()) {
            if (!filter.isEmpty() && !filter.contains(entry.getName())) {
                continue;
            }
            if (entry.isDeferred() || entry.isFetchOnly()) {
                System.out.println("[run] SKIP " + entry.getName());
                continue;
            }
            try {
                runEntry(entry, record, findings);
            } catch (Exception e) {
                findings.add(entry.getName() + ": " + e);
                System.err.println("[run] FAILED " + entry.getName() + ": " + e);
            }
        }

        if (!findings.isEmpty()) {
            System.err.println("[run] FINDINGS (" + findings.size() + "):");
            findings.forEach(f -> System.err.println("  - " + f));
            System.exit(1);
        }
        System.out.println("[run] all entries green in " + mode + " mode");
    }

    private static void runEntry(CorpusManifest.Entry entry, boolean record, List<String> findings) throws IOException {
        List<Path> poms;
        Path relativeTo;
        if (entry.isPom()) {
            Path pom = CorpusFetch.pomFile(entry);
            if (!Files.exists(pom)) {
                throw new IOException("Pom not fetched yet: " + pom + " (run corpusFetch first)");
            }
            poms = List.of(pom);
            relativeTo = pom.getParent();
        } else {
            Path reactor = CorpusPaths.reactors().resolve(entry.getName());
            poms = reactorPoms(reactor);
            relativeTo = reactor;
        }

        long start = System.currentTimeMillis();
        if (record) {
            resolveToSnapshot(entry, poms, relativeTo, RecordingHttpSender.record(CorpusPaths.store(),
                    new HttpUrlConnectionSender(Duration.ofSeconds(10), Duration.ofSeconds(60))));
            System.out.println("[run] recorded " + entry.getName() + " in " + (System.currentTimeMillis() - start) + "ms");
            return;
        }

        byte[] first = resolveToSnapshot(entry, poms, relativeTo, RecordingHttpSender.replay(CorpusPaths.store()));
        byte[] second = resolveToSnapshot(entry, poms, relativeTo, RecordingHttpSender.replay(CorpusPaths.store()));
        Path snapshot = CorpusPaths.snapshots().resolve(entry.getName() + ".json");
        Files.write(snapshot, first);
        if (!Arrays.equals(first, second)) {
            Files.write(CorpusPaths.snapshots().resolve(entry.getName() + ".nondeterministic.json"), second);
            findings.add(entry.getName() + ": NONDETERMINISTIC output across twice-run with fresh caches (see " +
                         entry.getName() + ".nondeterministic.json)");
        }
        System.out.println("[run] replayed " + entry.getName() + " twice in " +
                           (System.currentTimeMillis() - start) + "ms -> " + snapshot);
    }

    private static byte[] resolveToSnapshot(CorpusManifest.Entry entry, List<Path> poms, Path relativeTo,
                                            HttpSender sender) throws IOException {
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            // Errors surface as ParseExceptionResult markers captured in the snapshot.
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(sender);
        MavenExecutionContextView mavenCtx = MavenExecutionContextView.view(ctx);
        mavenCtx.setPomCache(new InMemoryMavenPomCache());
        mavenCtx.setAddLocalRepository(false);

        List<Parser.Input> inputs = poms.stream()
                .map(Parser.Input::fromFile)
                .collect(toList());
        List<SourceFile> parsed = MavenParser.builder().build()
                .parseInputs(inputs, relativeTo, ctx)
                .collect(toList());

        ObjectNode root = MAPPER.createObjectNode();
        root.put("entry", entry.getName());
        ArrayNode documents = root.putArray("documents");
        parsed.stream()
                .sorted((a, b) -> a.getSourcePath().compareTo(b.getSourcePath()))
                .forEach(sourceFile -> documents.add(documentNode(sourceFile)));
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
    }

    private static ObjectNode documentNode(SourceFile sourceFile) {
        ObjectNode doc = MAPPER.createObjectNode();
        doc.put("sourcePath", sourceFile.getSourcePath().toString());
        Optional<ParseExceptionResult> parseError = sourceFile.getMarkers().findFirst(ParseExceptionResult.class);
        parseError.ifPresent(e -> {
            ObjectNode error = doc.putObject("parseError");
            error.put("exceptionType", e.getExceptionType());
            error.put("message", firstLine(e.getMessage()));
        });
        Optional<MavenResolutionResult> resolution = sourceFile.getMarkers().findFirst(MavenResolutionResult.class);
        resolution.ifPresent(marker -> {
            if (marker.getParent() != null) {
                doc.put("parentGav", marker.getParent().getPom().getGav().toString());
            }
            if (!marker.getModules().isEmpty()) {
                ArrayNode moduleGavs = doc.putArray("moduleGavs");
                marker.getModules().stream()
                        .map(m -> m.getPom().getGav().toString())
                        .sorted()
                        .forEach(moduleGavs::add);
            }
            doc.set("resolution", normalize(MAPPER.valueToTree(marker)));
        });
        return doc;
    }

    /**
     * The only nondeterministic-by-construction field is the marker's random UUID id; everything
     * else must reproduce byte-wise or it is a finding.
     */
    private static JsonNode normalize(JsonNode node) {
        if (node instanceof ObjectNode) {
            ObjectNode obj = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if ("id".equals(field.getKey()) && field.getValue().isTextual() &&
                    UUID_PATTERN.matcher(field.getValue().asText()).matches()) {
                    obj.put("id", "00000000-0000-0000-0000-000000000000");
                } else {
                    normalize(field.getValue());
                }
            }
        } else if (node instanceof ArrayNode) {
            for (JsonNode child : node) {
                normalize(child);
            }
        }
        return node;
    }

    private static String firstLine(String message) {
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }

    private static List<Path> reactorPoms(Path reactorDir) throws IOException {
        if (!Files.isDirectory(reactorDir)) {
            throw new IOException("Reactor not fetched yet: " + reactorDir + " (run corpusFetch first)");
        }
        try (Stream<Path> walk = Files.walk(reactorDir)) {
            return walk.filter(p -> "pom.xml".equals(p.getFileName().toString()))
                    .filter(p -> {
                        String rel = reactorDir.relativize(p).toString();
                        return !rel.contains("src/") && !rel.contains("target/") && !rel.startsWith(".");
                    })
                    .sorted()
                    .collect(toList());
        }
    }
}
