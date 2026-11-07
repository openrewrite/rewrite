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
package org.openrewrite.javascript.internal.lock.npm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.provider.Arguments;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.HostInputs;
import org.openrewrite.javascript.internal.lock.NpmRegistryPackuments;
import org.openrewrite.javascript.internal.lock.RecordedRegistry;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Replays a recorded scenario: the registry stub from its {@code http/}, the host it was recorded on, the regenerator's
 * inputs and the builder for an edit, and the builder's lock-loading prefix.
 */
final class ScenarioReplay {

    static final String REG = "https://registry.npmjs.org/";

    /**
     * Where the corpus was recorded before {@code recorded-with} said so.
     */
    static final Host RECORDED_ON = Host.builder().os("darwin").cpu("arm64").build();

    private static final ObjectMapper JSON = new ObjectMapper();

    private ScenarioReplay() {
    }

    static Stream<Arguments> edits() {
        return edits(NpmScenarios.discover());
    }

    /**
     * The edits of every scenario replayed as another release than the one that recorded it.
     */
    static Stream<Arguments> releaseEdits() {
        return edits(NpmScenarios.discoverReleases());
    }

    private static Stream<Arguments> edits(List<NpmScenarios.Scenario> scenarios) {
        List<Arguments> out = new ArrayList<>();
        for (NpmScenarios.Scenario scenario : scenarios) {
            for (NpmScenarios.Edit edit : scenario.edits()) {
                out.add(Arguments.of(edit.toString(), edit));
            }
        }
        return out.stream();
    }

    static NpmScenarios.Edit edit(String scenario) {
        for (NpmScenarios.Scenario s : NpmScenarios.discover()) {
            if (s.getName().equals(scenario)) {
                return s.edits().get(0);
            }
        }
        throw new IllegalArgumentException(scenario);
    }

    // "malformed override key <key>", "malformed override reference <$ref> reached from <name>", or
    // "malformed manifest: <npm's message>"
    static String malformedKey(String expect) {
        if (expect.startsWith("malformed manifest: ")) {
            return expect.substring("malformed manifest: ".length());
        }
        String[] words = expect.split("\\s+");
        return expect.startsWith("malformed override reference ") ? words[3] : words[words.length - 1];
    }

    /**
     * A registry answering from the scenario's recorded responses, {@code http-<label>/} for another registry that
     * recorded its own, reporting every route it lacks.
     */
    static final class ScenarioRegistry extends RecordedRegistry<NpmScenarios.Scenario> implements HttpSender {
        final List<String> requests = new ArrayList<>();

        ScenarioRegistry(NpmScenarios.Scenario scenario) {
            super(scenario);
        }

        NpmScenarios.Scenario recording() {
            return scenario;
        }

        @Override
        public Response send(Request request) {
            String url = request.getUrl().toString();
            boolean full = "application/json".equals(header(request, "Accept"));
            String mirror = url.startsWith(REG) ? null : mirror(url, "npmjs");
            String file = mirror == null ? file(url, full) : mirror + "/" + file(url.replaceFirst(
              "^[a-z]+://[^/]+/", ""), full);
            requests.add(file);
            // an abbreviated packument whose versions an older recording kept as empty objects, as the sweep's
            // registry filled them for the releases that ask for it
            String filled = mirror == null && !full ? scenario.http(file + "@abbreviated") : null;
            String body = filled != null ? filled : mirror == null ? scenario.http(file) : scenario.file(file);
            if (body == null) {
                notRecorded(file);
            }
            return new Response(body == null ? 404 : 200,
              new ByteArrayInputStream((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)), () -> {
            });
        }

        private static @Nullable String header(Request request, String name) {
            for (Map.Entry<String, String> entry : request.getRequestHeaders().entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue();
                }
            }
            return null;
        }

        // http/<name> (abbreviated), http/<name>@full, http/<name>-<version>
        static String file(String url, boolean full) {
            String path = url.startsWith(REG) ? url.substring(REG.length()) : url;
            path = path.replace("%2F", "/").replace("%2f", "/").replace("%40", "@");
            String[] parts = path.split("/");
            int nameParts = parts[0].startsWith("@") ? 2 : 1;
            StringBuilder name = new StringBuilder(parts[0]);
            for (int i = 1; i < nameParts && i < parts.length; i++) {
                name.append('/').append(parts[i]);
            }
            if (parts.length > nameParts) {
                return name + "-" + parts[nameParts];
            }
            return full ? name + "@full" : name.toString();
        }
    }

    static PackumentSource registry(ScenarioRegistry http) {
        return new NpmRegistryPackuments(http.recording().host().transport(), new NpmRegistryClient(http));
    }

    /**
     * The registries a default and scoped registry URLs name, without credentials, as the host hands them over.
     */
    static NodeRegistries registries(String defaultRegistry, Map<String, String> scoped) {
        Map<String, NodeRegistry> byScope = new LinkedHashMap<>();
        scoped.forEach((scope, url) -> byScope.put(scope, registry(scope, url)));
        return new NodeRegistries(registry(null, defaultRegistry), byScope, null, null, null);
    }

    private static NodeRegistry registry(@Nullable String scope, String url) {
        return new NodeRegistry(scope, url, null, null, null, null, false, null, true, false);
    }

    /**
     * The machine the scenario was recorded on, with the Node version npm ran under.
     */
    static Host host(NpmScenarios.Scenario scenario) {
        Host platform = scenario.platform();
        return (platform != null ? platform : RECORDED_ON).toBuilder().nodeVersion(scenario.nodeVersion()).build();
    }

    static Instant now(NpmScenarios.Scenario scenario) {
        Instant recordedAt = scenario.recordedAt();
        return recordedAt != null ? recordedAt : Instant.now();
    }

    static NpmOptions options(NpmScenarios.Scenario scenario) {
        String npmrc = scenario.npmrc() == null ? "" : scenario.npmrc() + "\n";
        return NpmOptions.fromNpmrc(scenario.release(), scenario.host().userNpmrc(HostInputs.GLOBAL_AND_USER),
                npmrc + scenario.argsAfter());
    }

    static NpmOptions options(@Nullable String npmrc) {
        return options(NpmRelease.PORTED, npmrc);
    }

    /**
     * An .npmrc as the config of npm {@code release} reads it, as the release that recorded a scenario loads its locks.
     */
    static NpmOptions options(NpmRelease release, @Nullable String npmrc) {
        return NpmOptions.fromNpmrc(release, null, npmrc);
    }

    static NpmLockRegenerator.Inputs inputs(NpmScenarios.Edit edit) {
        NpmScenarios.Scenario scenario = edit.getScenario();
        String npmrc = scenario.npmrc() == null ? "" : scenario.npmrc() + "\n";
        return NpmLockRegenerator.Inputs.builder()
          .release(scenario.release())
          .lock(edit.getStart().getText())
          .yarnLock(edit.getInputs().getYarnLock())
          .manifests(edit.manifests())
          .npmrc(npmrc + scenario.argsAfter())
          .userNpmrc(scenario.host().userNpmrc(HostInputs.GLOBAL_AND_USER))
          .host(host(scenario))
          .now(now(scenario))
          .directoryName(directoryName(scenario))
          .paths(paths(edit))
          .build();
    }

    /**
     * The name of the directory each release installed the scenario in ({@code sweep-npm.js}): {@code project}, or,
     * where a root manifest has no name, the name npm gave the lock it recorded there. npm before 7.20.6 writes the
     * root's name in the lock only when the directory is named otherwise.
     */
    static String directoryName(NpmScenarios.Scenario scenario) {
        Path dir = scenario.getDir();
        String name = "project";
        String[][] written = {{"before", "pkg-before"},
                {"after", Files.isRegularFile(dir.resolve("pkg")) ? "pkg" : "pkg-after"}};
        for (String[] lockAndManifest : written) {
            Path lock = dir.resolve(lockAndManifest[0]);
            Path manifest = dir.resolve(lockAndManifest[1]);
            if (!Files.isRegularFile(lock) || !Files.isRegularFile(manifest)) {
                continue;
            }
            try {
                JsonNode pkg = JSON.readTree(manifest.toFile());
                if (pkg == null || pkg.isNull() || pkg.isObject() && pkg.has("name")) {
                    continue;
                }
                JsonNode named = JSON.readTree(lock.toFile()).get("name");
                if (named != null && named.isTextual() && named.asText().matches("[A-Za-z0-9._-]+")) {
                    name = named.asText();
                }
            } catch (IOException | RuntimeException e) {
                // a manifest or lock that does not parse names nothing
            }
        }
        return name;
    }

    /**
     * The files of the directory npm installed in, as the recorder laid it out for the edit: package.json, the lock,
     * the .npmrc, the yarn.lock and the tree's files.
     */
    static Set<String> paths(NpmScenarios.Edit edit) {
        Set<String> paths = new LinkedHashSet<>();
        // the oldest layout records a member's manifest alone, and a root package.json held unread is cannot-know
        if (edit.manifests().containsKey("")) {
            paths.add("package.json");
        }
        paths.add("package-lock.json");
        if (edit.getScenario().npmrc() != null) {
            paths.add(".npmrc");
        }
        if (edit.getInputs().getYarnLock() != null) {
            paths.add("yarn.lock");
        }
        paths.addAll(edit.getInputs().getTree().keySet());
        return paths;
    }

    /**
     * npm's own devEngines check, which it runs before the arborist builds anything.
     */
    static void checkDevEngines(NpmScenarios.Edit edit) {
        NpmScenarios.Scenario scenario = edit.getScenario();
        NpmLockRegenerator.checkDevEngines(edit.manifests().get(""), options(scenario), host(scenario));
    }

    static IdealTreeBuilder builder(NpmScenarios.Edit edit, ScenarioRegistry http, String path) {
        return builder(edit, http, path, options(edit.getScenario()));
    }

    static IdealTreeBuilder builder(NpmScenarios.Edit edit, ScenarioRegistry http, String path, NpmOptions options) {
        NpmScenarios.Scenario scenario = edit.getScenario();
        VirtualTreeLoader.ProjectFiles files = NpmLockRegenerator.projectFiles(new LockFile(edit.getStart().getText()),
          edit.getInputs().getYarnLock(), edit.manifests()).readBy(options.getRelease(), paths(edit));
        return new IdealTreeBuilder(path, files, options, registry(http), host(scenario), now(scenario));
    }

    static int lockfileVersion(String text) {
        Object value = parse(text);
        if (value instanceof Map) {
            double v = Js.toNumber(Js.get(value, "lockfileVersion"));
            return Double.isNaN(v) ? 0 : (int) v;
        }
        return 0;
    }

    private static @Nullable Object parse(String json) {
        try {
            return JsonParseEvenBetterErrors.parse(json).getValue();
        } catch (JsonParseEvenBetterErrors.JsonParseException e) {
            return null;
        }
    }

    /**
     * The directory npm ran in, so that a root without a name is named as npm named it.
     */
    static String projectPath(String lock) {
        Object value = parse(lock);
        Object name = value instanceof Map ? Js.get(value, "name") : null;
        return "/project/" + (name instanceof String && !((String) name).isEmpty() ? name : "project");
    }

    /**
     * Arborist's {@code #initTree} and {@code #fixDepFlags} for an install that adds and removes nothing, as the
     * release the options are read by runs them.
     */
    static Node load(NpmScenarios.Lock lock, NpmOptions options, String path) {
        VirtualTreeLoader.ProjectFiles files = NpmLockRegenerator.projectFiles(new LockFile(lock.getText()),
          lock.getYarnLock(), lock.manifests()).readBy(options.getRelease(), null);
        PackageJson manifest;
        try {
            manifest = files.manifest("");
        } catch (JsonParseEvenBetterErrors.JsonParseException | PackageJson.NeedsPackageFiles e) {
            throw e;
        } catch (RuntimeException e) {
            manifest = null;
        }
        Node root = new Node(Node.options()
          .release(options.getRelease())
          .path(path).realpath(path)
          .pkg(manifest != null ? manifest.getContent() : new LinkedHashMap<>())
          .extraneous(false).dev(false).devOptional(false).peer(false).optional(false)
          .installLinks(options.isInstallLinks()).legacyPeerDeps(options.isLegacyPeerDeps())
          .loadOverrides(true)
          .build());
        NpmLockRegenerator.setWorkspaces(root, files);
        VirtualTreeLoader loader = new VirtualTreeLoader(path, options);
        Node tree = loader.loadVirtual(files, root);
        if (manifest != null) {
            tree.getMeta().inferFormattingOptions(manifest);
        }
        tree.unsetDepFlags();
        if (loader.isFlagsSuspect()) {
            for (Node node : tree.getInventory().values()) {
                if (node.isExtraneous()) {
                    node.setParent(null);
                }
            }
        }
        return tree;
    }

    /**
     * The edges the ideal tree builder queues their node for: missing or invalid, reached from the root
     * through edges, as its {@code depth} scan finds them.
     */
    static List<Edge> problemEdges(Node tree) {
        List<Edge> problems = new ArrayList<>();
        Set<Node> seen = new LinkedHashSet<>();
        Deque<Node> queue = new ArrayDeque<>();
        queue.add(tree);
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (!seen.add(node)) {
                continue;
            }
            for (Edge edge : node.getEdgesOut().values()) {
                if (edge.getTo() == null || !edge.isValid()) {
                    problems.add(edge);
                }
                if (edge.getTo() != null) {
                    queue.add(edge.getTo());
                }
            }
        }
        return problems;
    }
}
