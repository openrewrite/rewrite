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
package org.openrewrite.javascript;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.internal.NodeDependencyScan;
import org.openrewrite.javascript.internal.PackageJsonHelper;
import org.openrewrite.javascript.internal.PackageJsonHelper.EditAndRegenerateResult;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.JsonParser;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.javascript.Assertions.pnpmLock;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * A package-manager file a recipe edits travels as the lock does: the regeneration reads the edited version and the
 * recipe emits it in the same cycle, byte for byte as pnpm 11.2.2 wrote {@code lock/pnpm/small-overrides-ws-direct}.
 */
class EditedPackageManagerFileLockRegenTest implements RewriteTest {

    private static final String SCENARIO = "lock/pnpm/small-overrides-ws-direct/";
    private static final Path WORKSPACE_MANIFEST = Paths.get("pnpm-workspace.yaml");

    private ExecutionContext ctx;
    private final List<String> requested = new ArrayList<>();

    @BeforeEach
    void setUp() {
        requested.clear();
        HttpSender http = request -> {
            String url = request.getUrl().toString();
            requested.add(url);
            String body = "https://registry.npmjs.org/wrappy".equals(url) ? resource(SCENARIO + "http/wrappy") : null;
            return new HttpSender.Response(body == null ? 404 : 200,
                    new ByteArrayInputStream((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)), () -> {
            });
        };
        ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        NodeExecutionContextView.view(ctx).setRegistries(singletonList(
                new NodeRegistry(null, "https://registry.npmjs.org/", null, null, null, null, false, null, true,
                        false)));
    }

    @Test
    void anEditedWorkspaceManifestReachesThePnpmPortAndIsEmitted() {
        rewriteRun(
                spec -> spec.recipe(new EditFile(WORKSPACE_MANIFEST.toString(), resource(SCENARIO + "workspace")))
                        .executionContext(ctx),
                packageJson(resource(SCENARIO + "pkg"), null,
                        nodeResolutionResult(PackageManager.Pnpm, dependency("wrappy", "^1.0.2"))),
                yaml(resource(SCENARIO + "workspace-before"),
                        resource(SCENARIO + "workspace"), s -> s.path(WORKSPACE_MANIFEST).noTrim()),
                pnpmLock(resource(SCENARIO + "before"), resource(SCENARIO + "after"),
                        s -> s.noTrim().afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                .as("regeneration must not warn; requested " + requested).isEmpty()))
        );
    }

    @Test
    void anEditOfOnlyThePackageManagerFileLeavesThePackageJsonAsItIs() {
        SourceFile packageJson = JsonParser.builder().build().parse(resource(SCENARIO + "pkg")).findFirst()
                .orElseThrow(IllegalStateException::new)
                .withSourcePath(Paths.get("package.json"))
                .withMarkers(Markers.build(singletonList(
                        nodeResolutionResult(PackageManager.Pnpm, dependency("wrappy", "^1.0.2")))));
        NodeDependencyScan.ProjectFiles files = new NodeDependencyScan.ProjectFiles(Collections.emptyMap(),
                Collections.singletonMap(WORKSPACE_MANIFEST, resource(SCENARIO + "workspace-before")), null);

        EditAndRegenerateResult result = PackageJsonHelper.editAndRegenerate(packageJson, Function.identity(),
                Collections.singletonMap(WORKSPACE_MANIFEST, resource(SCENARIO + "workspace")),
                resource(SCENARIO + "before"), files, ctx);

        assertThat(result.isChanged()).isTrue();
        assertThat(result.getModifiedPackageJson()).isNull();
        assertThat(result.getEditedFiles()).containsOnlyKeys(WORKSPACE_MANIFEST);
        assertThat(requireNonNull(result.getRegenResult()).getLockFileContent())
                .isEqualTo(resource(SCENARIO + "after"));
    }

    /**
     * Replaces one package-manager file's text and regenerates the lock of the package.json beside it.
     */
    @Value
    @EqualsAndHashCode(callSuper = false)
    static class EditFile extends ScanningRecipe<EditFile.Accumulator> {
        String path;
        String content;

        static class Accumulator {
            final NodeDependencyScan.Accumulator scan = new NodeDependencyScan.Accumulator();
            @Nullable EditAndRegenerateResult result;
        }

        @Override
        public String getDisplayName() {
            return "Edit a package-manager file";
        }

        @Override
        public String getDescription() {
            return "Replaces one package-manager file's text and regenerates the lock.";
        }

        @Override
        public Accumulator getInitialValue(ExecutionContext ctx) {
            return new Accumulator();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
            return new TreeVisitor<Tree, ExecutionContext>() {
                @Override
                public Tree preVisit(Tree tree, ExecutionContext ctx) {
                    stopAfterPreVisit();
                    SourceFile sf = (SourceFile) tree;
                    String name = String.valueOf(sf.getSourcePath().getFileName());
                    if (NodeDependencyScan.capturePackageManagerFile(acc.scan, sf)) {
                        return tree;
                    }
                    if (PackageJsonHelper.isLockFile(name)) {
                        NodeDependencyScan.captureLock(acc.scan, sf);
                    } else if (sf instanceof Json.Document && "package.json".equals(name) &&
                               sf.getMarkers().findFirst(NodeResolutionResult.class).isPresent()) {
                        acc.scan.projects.computeIfAbsent(sf.getSourcePath(), k ->
                                new NodeDependencyScan.ProjectState()).capturedPackageJson = sf;
                    }
                    return tree;
                }
            };
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
            NodeDependencyScan.linkWorkspaceMembers(acc.scan);
            return new TreeVisitor<Tree, ExecutionContext>() {
                @Override
                public Tree preVisit(Tree tree, ExecutionContext ctx) {
                    stopAfterPreVisit();
                    SourceFile sf = (SourceFile) tree;
                    EditAndRegenerateResult result = result(acc, ctx);
                    SourceFile edited = result.editedFile(sf);
                    if (edited != null) {
                        return edited;
                    }
                    if (PackageJsonHelper.isLockFile(String.valueOf(sf.getSourcePath().getFileName())) &&
                        result.getRegenResult() != null) {
                        return result.getRegenResult().isSuccess() ?
                                PackageJsonHelper.reparseLock(sf, result.getRegenResult().getLockFileContent()) :
                                Markup.warn(sf, new RuntimeException(result.getRegenResult().getErrorMessage()));
                    }
                    return tree;
                }
            };
        }

        private EditAndRegenerateResult result(Accumulator acc, ExecutionContext ctx) {
            if (acc.result == null) {
                Path manifest = Paths.get("package.json");
                NodeDependencyScan.ProjectState ps = acc.scan.projects.get(manifest);
                acc.result = PackageJsonHelper.editAndRegenerate(ps.capturedPackageJson, Function.identity(),
                        Collections.singletonMap(Paths.get(path), content), ps.capturedLockContent,
                        NodeDependencyScan.projectFiles(acc.scan, p -> null, manifest), ctx);
            }
            return acc.result;
        }
    }

    private static String resource(String path) {
        try (InputStream in = EditedPackageManagerFileLockRegenTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException(path);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
