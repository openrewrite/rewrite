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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * An install that changes nothing, up to the point where the ideal tree builder would start placing:
 * the root from the manifest, the tree from the lock, the flags recalculated, extraneous nodes pruned,
 * the lock written back. For every lockfileVersion 2 and 3 lock npm wrote that {@link NpmScenarios}
 * finds, the bytes are the lock's own, or those npm itself writes on a second install where it rewrites
 * the lock ({@link NpmScenarios.Lock#rewritten()}). Where that second install also placed a node
 * ({@link #PLACED_ON_SECOND_INSTALL}), what npm writes is the builder's to reproduce, and the data alone
 * must still write back the lock. A scenario whose {@code expect} says its override key is malformed is
 * one npm writes an empty lock for; its locks load only with the manifest before the edit. A lock whose
 * second install npm records as failing to load ({@link NpmScenarios.Lock#loadError()}) fails to load
 * here with the same error.
 */
class NpmLockRoundTripTest {

    /**
     * Locks whose recorded second install moved or added an entry, so that its rewrite is not the data's
     * alone: the lock's {@code again} diff names the entry.
     */
    static final Set<String> PLACED_ON_SECOND_INSTALL = new LinkedHashSet<>(Arrays.asList(
      // again adds packages/m/node_modules/is-odd and drops node_modules/is-odd: the root override reaches the member
      "override-ws-member-add-root-overrides-direct/after",
      // after-again moves node_modules/is-odd/node_modules/is-number to node_modules/is-number: a file: override's link
      // re-placed
      "open-override-value-directory/after",
      "open-override-value-directory-up/after"));

    static Stream<Arguments> locks() {
        List<Arguments> out = new ArrayList<>();
        for (NpmScenarios.Scenario scenario : NpmScenarios.discover()) {
            for (NpmScenarios.Lock lock : scenario.locks()) {
                if (!lock.isInput() && lockfileVersion(lock.getText()) >= 2) {
                    out.add(Arguments.of(lock.toString(), lock));
                }
            }
        }
        return out.stream();
    }

    @SuppressWarnings("unchecked")
    static int lockfileVersion(String text) {
        Object value = parse(text);
        if (value instanceof Map) {
            double v = Js.toNumber(((Map<String, Object>) value).get("lockfileVersion"));
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
    @SuppressWarnings("unchecked")
    static String projectPath(String lock) {
        Object value = parse(lock);
        Object name = value instanceof Map ? ((Map<String, Object>) value).get("name") : null;
        return "/project/" + (name instanceof String && !((String) name).isEmpty() ? name : "project");
    }

    /**
     * The manifests the lock itself holds, for a scenario without the root's: its root entry, as arborist
     * falls back to it without a package.json, and the entries its links resolve to outside node_modules.
     */
    @SuppressWarnings("unchecked")
    static Map<String, String> manifestsFromLock(String lock) {
        Map<String, String> manifests = new LinkedHashMap<>();
        Object value = parse(lock);
        Object packages = value instanceof Map ? ((Map<String, Object>) value).get("packages") : null;
        if (!(packages instanceof Map)) {
            return manifests;
        }
        for (Map.Entry<String, Object> entry : ((Map<String, Object>) packages).entrySet()) {
            Object meta = entry.getValue();
            if (!(meta instanceof Map)) {
                continue;
            }
            Object resolved = ((Map<String, Object>) meta).get("resolved");
            if (entry.getKey().isEmpty()) {
                manifests.put("", JsonStringifyNice.stringify(meta));
            } else if (Boolean.TRUE.equals(((Map<String, Object>) meta).get("link")) && resolved instanceof String &&
                       !((String) resolved).contains("node_modules/") && ((Map<String, Object>) packages)
                         .get(resolved) instanceof Map) {
                manifests.put((String) resolved, JsonStringifyNice.stringify(((Map<String, Object>) packages)
                  .get(resolved)));
            }
        }
        return manifests;
    }

    /**
     * Arborist's {@code #initTree} and {@code #fixDepFlags} for an install that adds and removes nothing.
     */
    static Node load(NpmScenarios.Lock lock, NpmOptions options, String path) {
        Map<String, String> manifests = lock.manifests();
        if (!manifests.containsKey("")) {
            Map<String, String> fromLock = manifestsFromLock(lock.getText());
            fromLock.putAll(manifests);
            manifests = fromLock;
        }
        VirtualTreeLoader.ProjectFiles files = VirtualTreeLoader.ProjectFiles.builder()
          .lock(new LockFile(lock.getText()))
          .yarnLock(lock.getYarnLock())
          .manifests(manifests)
          .build();
        PackageJson manifest = manifests.containsKey("") ? PackageJson.normalize(manifests.get("")) : null;
        Node root = new Node(Node.options()
          .path(path).realpath(path)
          .pkg(manifest == null ? new LinkedHashMap<>() : manifest.getContent())
          .extraneous(false).dev(false).devOptional(false).peer(false).optional(false)
          .installLinks(options.isInstallLinks()).legacyPeerDeps(options.isLegacyPeerDeps())
          .loadOverrides(true)
          .build());
        VirtualTreeLoader.setWorkspaces(root, manifests);
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

    static String roundTrip(NpmScenarios.Lock lock, NpmOptions options) {
        return load(lock, options, projectPath(lock.getText())).getMeta().toString();
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("locks")
    void writesBackWhatNpmWrites(String name, NpmScenarios.Lock lock) {
        String expect = lock.getScenario().expect();
        NpmOptions options = NpmOptions.fromNpmrc(lock.getNpmrc());
        if (expect != null && expect.startsWith("malformed override key ") && !lock.isBefore()) {
            String key = expect.substring("malformed override key ".length());
            assertThatThrownBy(() -> roundTrip(lock, options)).as(expect)
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining(key);
            return;
        }
        String loadError = lock.loadError();
        if (loadError != null) {
            assertThatThrownBy(() -> roundTrip(lock, options)).as("npm-again.log")
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage(loadError);
            return;
        }
        String key = lock.getScenario().getName() + "/" + lock.getFileName();
        String rewritten = PLACED_ON_SECOND_INSTALL.contains(key) ? null : lock.rewritten();
        String expected = rewritten != null ? rewritten : lock.getText();
        assertThat(load(lock, options, projectPath(expected)).getMeta().toString()).isEqualTo(expected);
    }
}
