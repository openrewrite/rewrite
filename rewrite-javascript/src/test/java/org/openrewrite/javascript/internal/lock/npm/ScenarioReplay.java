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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.provider.Arguments;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Replays a recorded scenario: the registry stub served from its {@code http/}, the host it was recorded on,
 * the regenerator's inputs and the builder for an edit, and the builder's lock-loading prefix ({@code #initTree}
 * and {@code #fixDepFlags} for an install that changes nothing).
 */
final class ScenarioReplay {

    static final String REG = "https://registry.npmjs.org/";

    /**
     * Where the corpus was recorded before {@code recorded-with} said so.
     */
    static final Host RECORDED_ON = Host.builder().os("darwin").cpu("arm64").build();

    private ScenarioReplay() {
    }

    static Stream<Arguments> edits() {
        List<Arguments> out = new ArrayList<>();
        for (NpmScenarios.Scenario scenario : NpmScenarios.discover()) {
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

    /**
     * The first word of the {@code expect} line: {@code exact} when there is none.
     */
    static String kind(@Nullable String expect) {
        if (expect == null || expect.isEmpty()) {
            return "exact";
        }
        return expect.split("\\s+")[0];
    }

    // "malformed override key <key>" or "malformed override reference <$ref> reached from <name>"
    static String malformedKey(String expect) {
        String[] words = expect.split("\\s+");
        return expect.startsWith("malformed override reference ") ? words[3] : words[words.length - 1];
    }

    /**
     * A registry answering from the scenario's recorded responses, reporting every route it lacks.
     */
    static final class ScenarioRegistry implements HttpSender {
        private final NpmScenarios.Scenario scenario;
        final List<String> missing = new ArrayList<>();
        final List<String> requests = new ArrayList<>();

        ScenarioRegistry(NpmScenarios.Scenario scenario) {
            this.scenario = scenario;
        }

        @Override
        public Response send(Request request) {
            String url = request.getUrl().toString();
            String file = file(url, "application/json".equals(header(request, "Accept")));
            requests.add(file);
            String body = scenario.http(file);
            if (body == null) {
                missing.add(file);
            }
            return new Response(body == null ? 404 : 200,
              new ByteArrayInputStream((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)), () -> {
            });
        }

        /**
         * Whether a route the replay asked for is not recorded, for a scenario recorded elsewhere than by this
         * module's recorder, with no cache to fill it from: the replay could not be served.
         */
        boolean unservedElsewhereRecording() {
            return !missing.isEmpty() && scenario.recordedWith() == null && scenario.getHttpCache() == null;
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

    static String missing(ScenarioRegistry http) {
        return http.missing.isEmpty() ? "" : " [routes not recorded: " + String.join(", ", http.missing) + "]";
    }

    static PackumentSource registry(ScenarioRegistry http) {
        NodeRegistry registry = new NodeRegistry(null, REG, null, null, null, null, false, null, true, false);
        return new RegistryPackuments(new NodeRegistries(registry, Collections.emptyMap(), null, null, null),
          new NpmRegistryClient(http));
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
        return NpmOptions.fromNpmrc(npmrc + scenario.argsAfter());
    }

    static NpmLockRegenerator.Inputs inputs(NpmScenarios.Edit edit) {
        NpmScenarios.Scenario scenario = edit.getScenario();
        String npmrc = scenario.npmrc() == null ? "" : scenario.npmrc() + "\n";
        return NpmLockRegenerator.Inputs.builder()
          .lock(edit.getStart().getText())
          .yarnLock(edit.getInputs().getYarnLock())
          .manifests(edit.manifests())
          .npmrc(npmrc + scenario.argsAfter())
          .host(host(scenario))
          .now(now(scenario))
          .build();
    }

    static IdealTreeBuilder builder(NpmScenarios.Edit edit, ScenarioRegistry http, String path) {
        return builder(edit, http, path, options(edit.getScenario()));
    }

    static IdealTreeBuilder builder(NpmScenarios.Edit edit, ScenarioRegistry http, String path, NpmOptions options) {
        NpmScenarios.Scenario scenario = edit.getScenario();
        VirtualTreeLoader.ProjectFiles files = NpmLockRegenerator.projectFiles(new LockFile(edit.getStart().getText()),
          edit.getInputs().getYarnLock(), edit.manifests());
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
     * Arborist's {@code #initTree} and {@code #fixDepFlags} for an install that adds and removes nothing.
     */
    static Node load(NpmScenarios.Lock lock, NpmOptions options, String path) {
        VirtualTreeLoader.ProjectFiles files = NpmLockRegenerator.projectFiles(new LockFile(lock.getText()),
          lock.getYarnLock(), lock.manifests());
        PackageJson manifest = files.manifest("");
        Node root = new Node(Node.options()
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
