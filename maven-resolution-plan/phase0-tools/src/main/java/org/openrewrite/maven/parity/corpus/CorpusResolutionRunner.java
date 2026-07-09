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
import org.jspecify.annotations.Nullable;
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
import org.openrewrite.maven.internal.ResolutionEngineSelector;
import org.openrewrite.maven.tree.MavenResolutionResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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

    // Canonical <os>/<jdk> profile-activation snapshot: a copy of the host System.getProperties() with the
    // activation-relevant keys pinned to Linux/amd64/JDK-17 so the SHADOW census is byte-reproducible across machines
    // (only os.*/java.version leak into the resolved model; everything else the model builder reads for interpolation is
    // kept from the host). Documented canonical set for NEW-2.
    private static final Map<String, String> CANONICAL_ACTIVATION_PROPERTIES = canonicalActivationProperties();

    private static Map<String, String> canonicalActivationProperties() {
        Map<String, String> props = new java.util.LinkedHashMap<>();
        System.getProperties().forEach((k, v) -> props.put(String.valueOf(k), String.valueOf(v)));
        props.put("os.name", "Linux");
        props.put("os.arch", "amd64");
        props.put("os.version", "5.15.0");
        props.put("sun.arch.data.model", "64");
        props.put("java.version", "17.0.9");
        // A canonical sources-free JDK home so <file><exists>${java.home}/.../src.zip</exists> JDK profiles (guava's
        // srczip system dep, NEW-4) never activate in the census — a modern JDK without bundled sources, the common CI
        // shape. Only the model builder's activation copy sees this; the real JVM java.home is untouched.
        props.put("java.home", "/opt/canonical-jdk-17");
        return props;
    }

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
        String engine = System.getProperty(ResolutionEngineSelector.ENGINE_KEY);
        boolean shadow = "shadow".equalsIgnoreCase(engine);
        Set<String> filter = new HashSet<>(Arrays.asList(args));
        CorpusManifest manifest = CorpusManifest.load(CorpusPaths.manifest());
        Files.createDirectories(CorpusPaths.snapshots());

        // Local-build wiring proof: ResolutionEngineSelector ships only in this worktree's
        // rewrite-maven (the released artifact has no dual-engine seam). Loading it here confirms
        // the composite-build substitution took.
        System.out.println("[wiring] ResolutionEngineSelector loaded from " +
                ResolutionEngineSelector.class.getProtectionDomain().getCodeSource().getLocation());
        System.out.println("[run] mode=" + mode + " engine=" + (engine == null ? "legacy" : engine));

        List<String> findings = new ArrayList<>();
        List<String> census = new ArrayList<>();
        for (CorpusManifest.Entry entry : manifest.getEntries()) {
            if (!filter.isEmpty() && !filter.contains(entry.getName())) {
                continue;
            }
            if (entry.isDeferred() || entry.isFetchOnly()) {
                System.out.println("[run] SKIP " + entry.getName());
                continue;
            }
            try {
                runEntry(entry, record, shadow, engine, findings, census);
            } catch (Throwable e) {
                findings.add(entry.getName() + ": " + e);
                census.add(entry.getName() + "\tERROR\t" + firstLine(String.valueOf(e)));
                System.err.println("[run] FAILED " + entry.getName() + ": " + e);
            }
        }

        if (shadow && !record) {
            writeCensus(census);
        }
        if (!findings.isEmpty()) {
            System.err.println("[run] FINDINGS (" + findings.size() + "):");
            findings.forEach(f -> System.err.println("  - " + f));
            System.exit(1);
        }
        System.out.println("[run] all entries green in " + mode + " mode" + (shadow ? " (shadow)" : ""));
    }

    private static void runEntry(CorpusManifest.Entry entry, boolean record, boolean shadow, @Nullable String engine,
                                 List<String> findings, List<String> census) throws IOException {
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
            try {
                resolveToSnapshot(entry, poms, relativeTo, engine, RecordingHttpSender.record(CorpusPaths.store(),
                        new HttpUrlConnectionSender(Duration.ofSeconds(10), Duration.ofSeconds(60))));
            } catch (AssertionError e) {
                // A shadow assertion fires only after both engines have made (and recorded) their
                // HTTP for the offending pom, so the exchange is captured; the diff is not a record
                // failure. (Reactors abort at the first diff — top-up runs engine=maven instead.)
                System.out.println("[run] recorded " + entry.getName() + " (shadow diff: " + firstLine(e.getMessage()) + ")");
                return;
            }
            System.out.println("[run] recorded " + entry.getName() + " in " + (System.currentTimeMillis() - start) + "ms");
            return;
        }

        if (shadow) {
            Outcome first = capture(entry, poms, relativeTo, engine);
            Outcome second = capture(entry, poms, relativeTo, engine);
            censusEntry(entry, first, second, findings, census);
            System.out.println("[run] shadow-replayed " + entry.getName() + " twice in " +
                               (System.currentTimeMillis() - start) + "ms -> " + first.status());
            return;
        }

        byte[] first = resolveToSnapshot(entry, poms, relativeTo, engine, RecordingHttpSender.replay(CorpusPaths.store()));
        byte[] second = resolveToSnapshot(entry, poms, relativeTo, engine, RecordingHttpSender.replay(CorpusPaths.store()));
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

    /** One shadow-REPLAY pass: either a clean snapshot or the facade's unexplained-diff assertion text. */
    private static final class Outcome {
        final byte @Nullable [] snapshot;
        final @Nullable String assertion;

        Outcome(byte @Nullable [] snapshot, @Nullable String assertion) {
            this.snapshot = snapshot;
            this.assertion = assertion;
        }

        boolean unexplained() {
            return assertion != null;
        }

        String status() {
            return unexplained() ? "UNEXPLAINED" : "clean/masked";
        }
    }

    private static Outcome capture(CorpusManifest.Entry entry, List<Path> poms, Path relativeTo, @Nullable String engine) {
        try {
            return new Outcome(resolveToSnapshot(entry, poms, relativeTo, engine, RecordingHttpSender.replay(CorpusPaths.store())), null);
        } catch (AssertionError e) {
            // The shadow facade throws on any difference the ledgered masks do not explain; that
            // text IS the census evidence for this entry.
            return new Outcome(null, e.getMessage() == null ? e.toString() : e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void censusEntry(CorpusManifest.Entry entry, Outcome first, Outcome second,
                                    List<String> findings, List<String> census) throws IOException {
        String name = entry.getName();
        if (first.unexplained() != second.unexplained()) {
            findings.add(name + ": shadow outcome differs across twice-run (one asserted, one clean)");
            census.add(name + "\tNONDETERMINISTIC-OUTCOME");
            return;
        }
        if (first.unexplained()) {
            Files.createDirectories(CorpusPaths.corpus().resolve("census"));
            Files.write(CorpusPaths.corpus().resolve("census").resolve(name + ".diff"),
                    first.assertion.getBytes(StandardCharsets.UTF_8));
            boolean deterministic = first.assertion.equals(second.assertion);
            if (!deterministic) {
                findings.add(name + ": NONDETERMINISTIC shadow assertion text across twice-run");
            }
            census.add(name + "\tUNEXPLAINED" + (deterministic ? "" : "\tNONDETERMINISTIC"));
            return;
        }
        Path snapshot = CorpusPaths.snapshots().resolve(name + ".json");
        Files.write(snapshot, first.snapshot);
        boolean deterministic = Arrays.equals(first.snapshot, second.snapshot);
        if (!deterministic) {
            Files.write(CorpusPaths.snapshots().resolve(name + ".nondeterministic.json"), second.snapshot);
            findings.add(name + ": NONDETERMINISTIC output across twice-run with fresh caches");
        }
        census.add(name + "\tCLEAN" + (deterministic ? "" : "\tNONDETERMINISTIC"));
    }

    private static void writeCensus(List<String> census) throws IOException {
        Files.createDirectories(CorpusPaths.corpus().resolve("census"));
        Files.write(CorpusPaths.corpus().resolve("census").resolve("summary.tsv"),
                String.join("\n", census).getBytes(StandardCharsets.UTF_8));
        System.out.println("[census]");
        census.forEach(c -> System.out.println("  " + c));
    }

    private static byte[] resolveToSnapshot(CorpusManifest.Entry entry, List<Path> poms, Path relativeTo,
                                            @Nullable String engine, HttpSender sender) throws IOException {
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            // Errors surface as ParseExceptionResult markers captured in the snapshot.
        });
        if (engine != null) {
            ctx.putMessage(ResolutionEngineSelector.ENGINE_KEY, engine);
        }
        HttpSenderExecutionContextView.view(ctx).setHttpSender(sender);
        MavenExecutionContextView mavenCtx = MavenExecutionContextView.view(ctx);
        mavenCtx.setPomCache(new InMemoryMavenPomCache());
        mavenCtx.setAddLocalRepository(false);
        // Pin the engine's <os>/<jdk> profile-activation snapshot to a canonical Linux/x86_64/JDK-17 set so the census
        // is byte-reproducible across machines (netty/hadoop <os>-profile activation is otherwise host-dependent).
        mavenCtx.setActivationSystemProperties(CANONICAL_ACTIVATION_PROPERTIES);

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
