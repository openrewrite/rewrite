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
package org.openrewrite.javascript.internal;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.Expect;
import org.openrewrite.javascript.internal.lock.LockScenario;
import org.openrewrite.javascript.internal.lock.LockScenarioTest;
import org.openrewrite.javascript.internal.lock.RecordedRegistry;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.JsonParser;
import org.openrewrite.text.PlainText;
import org.openrewrite.yaml.YamlParser;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

/**
 * Replays the recorded edits under {@code lock/repository/<package manager>/}: a repository whose lock lies anywhere
 * in it, scanned as the dependency recipes scan it, with markers that name the package manager and nothing of its
 * workspaces, and written by their visitor. Each lock is byte for byte the package manager's, or the regeneration
 * fails as the expect line names. Each is replayed again as a host that parsed it after an install marks it: every
 * manifest without a lock beside it is npm's.
 */
class RepositoryScenarioTest extends LockScenarioTest {

    private static final Map<String, PackageManager> FAMILIES = new LinkedHashMap<>();

    static {
        FAMILIES.put("npm", PackageManager.Npm);
        FAMILIES.put("yarn-classic", PackageManager.YarnClassic);
        FAMILIES.put("yarn-berry", PackageManager.YarnBerry);
        FAMILIES.put("pnpm", PackageManager.Pnpm);
        FAMILIES.put("bun", PackageManager.Bun);
    }

    RepositoryScenarioTest() {
        super("REWRITE_LOCK_REPOSITORY_RESULTS", "the package manager");
    }

    /**
     * A repository before and after an edit of its manifests: {@code tree-before/}, {@code tree/} over it, the lock of
     * {@code lock-dir} as the package manager wrote it before ({@code before}) and after ({@code after}).
     */
    static final class Scenario extends LockScenario {
        final PackageManager pm;

        Scenario(PackageManager pm, String name, String label, Path dir, @Nullable Path httpCache) {
            super(name, label, dir, httpCache);
            this.pm = pm;
        }

        Path lock() {
            String lockDir = file("lock-dir");
            String name = requireNonNull(LockFileRegeneration.forPackageManager(pm)).getLockFile();
            return lockDir == null || lockDir.trim().isEmpty() ? Paths.get(name) : Paths.get(lockDir.trim(), name);
        }
    }

    static Stream<Arguments> scenarios() {
        List<Arguments> out = new ArrayList<>();
        for (Map.Entry<String, PackageManager> family : FAMILIES.entrySet()) {
            for (Scenario scenario : LockScenario.<Scenario>discover("repository/" + family.getKey(),
                    dir -> Files.isDirectory(dir.resolve("tree-before")), (name, label, dir, httpCache) ->
                            new Scenario(family.getValue(), name, label, dir, httpCache))) {
                String label = family.getKey() + "/" + scenario.getLabel();
                out.add(Arguments.of(label, scenario, false));
                if (family.getValue() != PackageManager.Npm) {
                    out.add(Arguments.of(label + " (manifests without a lock marked npm)", scenario, true));
                }
            }
        }
        return out.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void regeneratesWhatThePackageManagerWrote(String label, Scenario scenario, boolean lockless) {
        Expect expect = Expect.of(scenario.expect());
        skipUnlessReplayed(label, scenario, expect);
        Map<String, String> edited = scenario.tree("tree");
        assertThat(edited.keySet()).as("the edit of %s", label).allMatch(path -> path.endsWith("package.json"));

        Registry http = new Registry(scenario);
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);

        List<Failure> failures = new ArrayList<>();
        NodeDependencyEdits edits = new NodeDependencyEdits(new NodeDependencyEdits.Rows() {
            @Override
            public void failure(ExecutionContext c, Path packageJson, Failure failure) {
                failures.add(failure);
            }

            @Override
            public void declined(ExecutionContext c, Path path, String reason) {
                throw new AssertionError(path + " declined: " + reason);
            }
        });
        NodeDependencyEdits.Decider decider = (decision, c) -> {
            String text = edited.get(decision.getPath().toString().replace('\\', '/'));
            if (text != null && !text.equals(PackageJsonHelper.printed(decision.getManifest()))) {
                decision.editManifest(doc -> PackageJsonHelper.reparseJson(doc, text));
            }
        };

        Map<Path, SourceFile> repository = new TreeMap<>();
        Path lock = scenario.lock();
        for (Map.Entry<String, String> file : scenario.tree("tree-before").entrySet()) {
            Path path = Paths.get(file.getKey());
            repository.put(path, parse(lockless && !path.equals(lock.resolveSibling("package.json")) ?
                    PackageManager.Npm : scenario.pm, path, file.getValue()));
        }
        String before = requireNonNull(scenario.file("before"), "before");
        repository.put(lock, parse(scenario.pm, lock, before));
        for (SourceFile sf : repository.values()) {
            edits.scan(sf);
        }
        assertThat(edits.decideAll(ctx, decider)).as("files created for %s", label).isEmpty();
        TreeVisitor<Tree, ExecutionContext> visitor = edits.visitor(decider);
        Map<String, String> written = new LinkedHashMap<>();
        for (Map.Entry<Path, SourceFile> file : repository.entrySet()) {
            SourceFile out = (SourceFile) requireNonNull(visitor.visit(file.getValue(), ctx));
            written.put(file.getKey().toString().replace('\\', '/'), PackageJsonHelper.printed(out));
        }

        for (Map.Entry<String, String> manifest : edited.entrySet()) {
            assertThat(written.get(manifest.getKey())).as("%s of %s", manifest.getKey(), label)
                    .isEqualTo(manifest.getValue());
        }
        String lockText = written.get(lock.toString().replace('\\', '/'));
        Result result = failures.isEmpty() ? Result.success(lockText) : Result.failure(failures.get(0));
        if (!failures.isEmpty()) {
            assertThat(lockText).as("the lock of %s after a failed regeneration", label).isEqualTo(before);
        }
        check(label, expect, scenario.file("after"), result, http);
    }

    @Override
    protected boolean names(Expect expect, Failure failure, RecordedRegistry<?> http) {
        return "cannot-know".equals(expect.getKind()) && failure.getKind() == Kind.CANNOT_KNOW &&
               failure.getDetail().contains(expect.getRest());
    }

    // each file as the type a host's parsers give it: a manifest with its marker, a lock by its format, text otherwise
    private static SourceFile parse(PackageManager pm, Path path, String text) {
        String name = path.getFileName().toString();
        if ("package.json".equals(name)) {
            SourceFile manifest = json(path, text);
            return manifest.withMarkers(manifest.getMarkers().add(nodeResolutionResult(pm)));
        }
        if ("package-lock.json".equals(name) || "bun.lock".equals(name)) {
            return json(path, text);
        }
        if (name.endsWith(".yaml") || name.endsWith(".yml") || "yarn.lock".equals(name) && text.contains("__metadata:")) {
            return YamlParser.builder().build().parse(text).findFirst()
                    .orElseThrow(() -> new IllegalStateException(path.toString())).withSourcePath(path);
        }
        return PlainText.builder().sourcePath(path).text(text).build();
    }

    private static SourceFile json(Path path, String text) {
        return new JsonParser().parse(text).findFirst().orElseThrow(() -> new IllegalStateException(path.toString()))
                .withSourcePath(path);
    }

    /**
     * registry.npmjs.org and registry.yarnpkg.com answering from the scenario's {@code http/}: a packument as
     * {@code <name>}, {@code <name>@full} when all of it is asked for, a version's manifest as
     * {@code <name>-<version>}, a tarball as {@code <name>-<version>.tgz}.
     */
    private static final class Registry extends RecordedRegistry<Scenario> implements HttpSender {

        Registry(Scenario scenario) {
            super(scenario);
        }

        @Override
        public Response send(Request request) {
            String url = request.getUrl().toString();
            String path = url.replaceFirst("^https://registry\\.(?:npmjs\\.org|yarnpkg\\.com)/", "");
            byte[] body = path.equals(url) ? null : scenario.httpBytes(file(path, fullPackument(request)));
            if (body == null) {
                notRecorded(url);
            }
            return new Response(body == null ? 404 : 200, new ByteArrayInputStream(body == null ? new byte[0] : body),
                    () -> {
                    });
        }

        private static boolean fullPackument(Request request) {
            for (Map.Entry<String, String> header : request.getRequestHeaders().entrySet()) {
                if ("Accept".equalsIgnoreCase(header.getKey())) {
                    return "application/json".equals(header.getValue());
                }
            }
            return false;
        }

        private static String file(String path, boolean full) {
            String[] parts = path.replace("%2F", "/").replace("%2f", "/").replace("%40", "@").split("/");
            int nameParts = parts[0].startsWith("@") ? 2 : 1;
            String name = String.join("/", Arrays.asList(parts).subList(0, Math.min(nameParts, parts.length)));
            if (parts.length == nameParts) {
                return full ? name + "@full" : name;
            }
            if ("-".equals(parts[nameParts])) {
                String tarball = parts[parts.length - 1];
                return nameParts == 2 ? parts[0] + "/" + tarball : tarball;
            }
            return name + "-" + parts[nameParts];
        }
    }
}
