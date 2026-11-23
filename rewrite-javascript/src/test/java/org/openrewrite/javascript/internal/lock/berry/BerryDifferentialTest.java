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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.GlobParent;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.JsYaml;
import org.openrewrite.javascript.internal.lock.PicomatchScan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The port against Yarn 4.10.3's own code, row by row, from {@code generate/differential.js} and {@code
 * generate/globs.js}; {@code REWRITE_BERRY_DIFFERENTIAL} names a directory of more such rows.
 */
class BerryDifferentialTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    @Test
    void symlParsesAsYarnDoes() {
        List<String> differences = new ArrayList<>();
        for (String[] row : rows("syml-parse.tsv.gz")) {
            String expected = row[1];
            Object actual;
            try {
                actual = Syml.parseSyml(row[0]);
            } catch (RuntimeException e) {
                if (!expected.startsWith("!") || (e instanceof JsYaml.YamlException &&
                                                  !expected.substring(1).startsWith(e.getMessage()))) {
                    differences.add(row[0] + " => " + expected + " / threw " + e);
                }
                continue;
            }
            if (expected.startsWith("!") || !tree(expected).equals(ordered(actual))) {
                differences.add(row[0] + " => " + expected + " / " + ordered(actual));
            }
        }
        assertThat(differences).isEmpty();
    }

    @Test
    void symlStringifiesAsYarnDoes() {
        List<String> differences = new ArrayList<>();
        for (String[] row : rows("syml-stringify.tsv.gz")) {
            String actual = Syml.stringifySyml(value(tree(row[0])));
            if (!actual.equals(row[1])) {
                differences.add(row[0] + " => " + row[1] + " / " + actual);
            }
        }
        assertThat(differences).isEmpty();
    }

    @Test
    void structUtilsAnswersAsYarnDoes() {
        List<String> differences = new ArrayList<>();
        for (String[] row : rows("struct.tsv.gz")) {
            String s = row[1];
            JsonNode actual = answer(() -> {
                switch (row[0]) {
                    case "parseIdent":
                        return StructUtils.stringifyIdent(StructUtils.parseIdent(s));
                    case "parseDescriptor": {
                        Descriptor d = StructUtils.parseDescriptor(s);
                        return list(d.scope, d.name, d.range, StructUtils.stringifyDescriptor(d));
                    }
                    case "parseDescriptorStrict": {
                        Descriptor d = StructUtils.parseDescriptor(s, true);
                        return list(d.scope, d.name, d.range);
                    }
                    case "parseLocatorStrict": {
                        Locator l = StructUtils.parseLocator(s, true);
                        return list(l.scope, l.name, l.reference, StructUtils.stringifyLocator(l));
                    }
                    case "parseRange": {
                        StructUtils.Range r = StructUtils.parseRange(s);
                        Map<String, Object> out = new LinkedHashMap<>();
                        out.put("protocol", r.getProtocol());
                        out.put("source", r.getSource());
                        out.put("selector", r.getSelector());
                        out.put("params", r.getParams());
                        return out;
                    }
                    case "makeRange": {
                        StructUtils.Range r = StructUtils.parseRange(s);
                        return StructUtils.makeRange(r.getProtocol(), r.getSource(), r.getSelector(), r.getParams());
                    }
                    case "convertToManifestRange":
                        return StructUtils.convertToManifestRange(s);
                    default:
                        throw new IllegalStateException(row[0]);
                }
            });
            if (!answers(row[2], actual)) {
                differences.add(row[0] + " " + s + " => " + row[2] + " / " + actual);
            }
        }
        assertThat(differences).isEmpty();
    }

    @Test
    void manifestReadsARegistryDocumentAsYarnDoes() {
        List<String> differences = new ArrayList<>();
        for (String[] row : rows("manifest.tsv.gz")) {
            JsonNode actual = answer(() -> {
                Manifest m = new Manifest();
                m.load(Js.jsonParse(row[0]), false);
                Map<String, Object> exported = m.exportTo(false);
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("conditions", m.getConditions());
                for (String field : new String[]{"dependencies", "peerDependencies", "dependenciesMeta",
                        "peerDependenciesMeta", "bin", "scripts"}) {
                    if (exported.containsKey(field)) {
                        out.put(field, exported.get(field));
                    }
                }
                return out;
            });
            if (!answers(row[1], actual)) {
                differences.add(row[0] + " => " + row[1] + " / " + actual);
            }
        }
        assertThat(differences).isEmpty();
    }

    @Test
    void normalizesARegistryPackageAsYarnDoes() {
        Configuration configuration = BerryScenarios.restricted(Configuration.defaults(), YarnRelease.of("4.10.3"));
        List<String> differences = new ArrayList<>();
        for (String[] row : rows("normalize.tsv.gz")) {
            JsonNode actual = answer(() -> {
                Object doc = Js.jsonParse(row[0]);
                String version = (String) Js.get(doc, "version");
                Locator locator = StructUtils.makeLocator(StructUtils.parseIdent((String) Js.get(doc, "name")),
                        "npm:" + version);
                Package pkg = configuration.normalizePackage(
                        NpmSemverResolver.fromManifest(locator, version, doc, configuration));
                Map<String, Object> out = new LinkedHashMap<>();
                List<Object> dependencies = new ArrayList<>();
                pkg.dependencies.values().forEach(d -> dependencies.add(StructUtils.stringifyDescriptor(d)));
                List<Object> peerDependencies = new ArrayList<>();
                pkg.peerDependencies.values().forEach(d -> peerDependencies.add(StructUtils.stringifyDescriptor(d)));
                out.put("dependencies", dependencies);
                out.put("peerDependencies", peerDependencies);
                out.put("peerDependenciesMeta", pkg.peerDependenciesMeta);
                return out;
            });
            if (!answers(row[1], actual)) {
                differences.add(row[0] + " => " + row[1] + " / " + actual);
            }
        }
        assertThat(differences).isEmpty();
    }

    @Test
    void bracesExpandAsYarnsBundleDoes() {
        List<String> differences = new ArrayList<>();
        for (String[] row : rows("glob-braces.tsv.gz")) {
            String actual;
            try {
                actual = MAPPER.writeValueAsString(FastGlob.braces(row[0]));
            } catch (JsError e) {
                actual = "!" + e;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (!actual.equals(row[1]) && !(actual.startsWith("[") && tree(actual).equals(tree(row[1])))) {
                differences.add(row[0] + " => " + row[1] + " / " + actual);
            }
        }
        assertThat(differences).isEmpty();
    }

    @Test
    void globParentAndScanAnswerAsYarnsBundleDoes() {
        List<String> differences = new ArrayList<>();
        for (String[] row : rows("glob-parent.tsv.gz")) {
            String base = GlobParent.globParent(row[0], GlobParent.Release.V5_1_2);
            JsonNode parts = NODES.arrayNode().addAll(PicomatchScan.scan(row[0], true).getParts().stream()
                    .map(NODES::textNode).collect(Collectors.toList()));
            boolean dynamic = FastGlob.isDynamicPattern(row[0]);
            if (!base.equals(row[1]) || !parts.equals(tree(row[2])) || dynamic != "1".equals(row[3])) {
                differences.add(row[0] + " => " + row[1] + " " + row[2] + " " + row[3] + " / " + base + " " + parts +
                                " " + dynamic);
            }
        }
        assertThat(differences).isEmpty();
    }

    /**
     * A row Yarn answers by which of two fast-glob tasks reports a directory first may be one the port cannot
     * know, as may a pattern that reaches outside the project; every other row is answered exactly.
     */
    @Test
    void workspacesAreFoundAsYarnsBundleFindsThem() {
        List<String> differences = new ArrayList<>();
        for (String[] row : rows("glob-workspaces.tsv.gz")) {
            YarnBerryLockRegenerator.Inputs.InputsBuilder inputs = YarnBerryLockRegenerator.Inputs.builder()
                    .manifest("", "{\"private\": true, \"workspaces\": " + row[1] + "}")
                    .now(Instant.EPOCH);
            for (JsonNode directory : tree(row[0])) {
                inputs.manifest(directory.asText(), "{}");
            }
            String actual;
            try {
                List<String> members = new ArrayList<>();
                for (Workspace workspace : new YarnBerryLockRegenerator(inputs.build(), BerryScenarios.NO_REGISTRY)
                        .load(YarnRelease.of("4.10.3"), Collections.emptyMap()).getProject().workspaces) {
                    if (!".".equals(workspace.getRelativeCwd())) {
                        members.add(workspace.getRelativeCwd());
                    }
                }
                actual = MAPPER.writeValueAsString(members);
            } catch (CannotKnow e) {
                if ("1".equals(row[3]) || e.getMessage().contains("outside the project") ||
                    e.getMessage().contains("an absolute path")) {
                    continue;
                }
                actual = "!" + e;
            } catch (JsError e) {
                actual = "!" + e;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            // a file system error names the absolute path
            String expected = row[2].contains("<root>") ? row[2].substring(0, row[2].indexOf(", ")) : row[2];
            boolean same = expected.startsWith("!") ? actual.startsWith(expected) :
                    !actual.startsWith("!") && tree(actual).equals(tree(expected));
            if (!same) {
                differences.add(row[0] + " " + row[1] + " => " + row[2] + " / " + actual);
            }
        }
        assertThat(differences).isEmpty();
    }

    private static boolean answers(String expected, JsonNode actual) {
        if (expected.startsWith("!")) {
            return actual.isTextual() && actual.asText().equals(expected);
        }
        return tree(expected).equals(actual);
    }

    private static JsonNode answer(Supplier<Object> compute) {
        try {
            return ordered(compute.get());
        } catch (RuntimeException e) {
            return NODES.textNode("!" + e.getMessage());
        }
    }

    private static List<Object> list(@Nullable Object... items) {
        List<Object> out = new ArrayList<>();
        for (Object item : items) {
            out.add(item);
        }
        return out;
    }

    /**
     * The generator's ordered JSON of a value: objects as {@code {"object": [[key, value], ...]}} without their
     * undefined fields, arrays as {@code {"array": [...]}}.
     */
    private static JsonNode ordered(@Nullable Object value) {
        if (value == null) {
            return NODES.nullNode();
        }
        if (value instanceof Map) {
            ArrayNode fields = NODES.arrayNode();
            for (Map.Entry<String, @Nullable Object> entry : Js.entries(value)) {
                if (entry.getValue() != Syml.UNDEFINED) {
                    fields.add(NODES.arrayNode().add(entry.getKey()).add(ordered(entry.getValue())));
                }
            }
            return NODES.objectNode().set("object", fields);
        }
        if (value instanceof List) {
            ArrayNode items = NODES.arrayNode();
            for (Object item : (List<?>) value) {
                items.add(ordered(item));
            }
            return NODES.objectNode().set("array", items);
        }
        if (value instanceof Boolean) {
            return NODES.booleanNode((Boolean) value);
        }
        return NODES.textNode(value.toString());
    }

    private static @Nullable Object value(JsonNode node) {
        if (node.isNull()) {
            return null;
        }
        if (node.isObject() && node.has("object")) {
            Map<String, @Nullable Object> out = new LinkedHashMap<>();
            for (JsonNode field : node.get("object")) {
                out.put(field.get(0).asText(), value(field.get(1)));
            }
            return out;
        }
        if (node.isObject() && node.has("array")) {
            List<@Nullable Object> out = new ArrayList<>();
            for (JsonNode item : node.get("array")) {
                out.add(value(item));
            }
            return out;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        return node.asText();
    }

    private static JsonNode tree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String[]> rows(String file) {
        List<String[]> out = new ArrayList<>();
        read(BerryDifferentialTest.class.getClassLoader().getResourceAsStream("yarn-berry/differential/" + file), out);
        String corpus = System.getenv("REWRITE_BERRY_DIFFERENTIAL");
        if (corpus != null && Files.isRegularFile(Paths.get(corpus, file))) {
            try {
                read(Files.newInputStream(Paths.get(corpus, file)), out);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        assertThat(out).as(file).isNotEmpty();
        return out;
    }

    private static void read(@Nullable InputStream gzipped, List<String[]> out) {
        if (gzipped == null) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(gzipped),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                String[] fields = line.split("\t", -1);
                for (int i = 0; i < fields.length; i++) {
                    fields[i] = unescape(fields[i]);
                }
                out.add(fields);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String unescape(String field) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            if (c == '\\' && i + 1 < field.length()) {
                char e = field.charAt(++i);
                out.append(e == 't' ? '\t' : e == 'n' ? '\n' : e == 'r' ? '\r' : e);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
