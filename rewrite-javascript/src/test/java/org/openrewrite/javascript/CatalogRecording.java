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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.JsonParser;
import org.openrewrite.marker.Markup;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.yaml.YamlParser;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;
import static org.openrewrite.javascript.Assertions.packageJson;
import static org.openrewrite.javascript.Assertions.packageLock;
import static org.openrewrite.javascript.Assertions.pnpmLock;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.yaml.Assertions.yaml;

/**
 * A recording under {@code lock/catalogs/<family>/<scenario>/} (or another recipe's directory) as a recipe run's
 * sources: each file before the edit and as the package manager left it, and its registry responses served offline.
 */
final class CatalogRecording {

    private static final String REGISTRY = "https://registry.npmjs.org/";
    private static final String YARN_REGISTRY = "https://registry.yarnpkg.com/";

    private final Path dir;
    private final PackageManager pm;
    private String workspaceBefore = "workspace-before";
    final List<String> unserved = new ArrayList<>();

    CatalogRecording(String family, String scenario) {
        this("catalogs", family, scenario);
    }

    CatalogRecording(String recipes, String family, String scenario) {
        URL url = CatalogRecording.class.getResource("/lock/" + recipes + "/" + family + "/" + scenario);
        if (url == null) {
            throw new IllegalArgumentException(family + "/" + scenario);
        }
        try {
            this.dir = Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        this.pm = "pnpm".equals(family) ? PackageManager.Pnpm : "bun".equals(family) ? PackageManager.Bun :
                "npm".equals(family) ? PackageManager.Npm : "yarn-classic".equals(family) ? PackageManager.YarnClassic :
                        PackageManager.YarnBerry;
    }

    private String lockName() {
        return pm == PackageManager.Pnpm ? "pnpm-lock.yaml" : pm == PackageManager.Bun ? "bun.lock" :
                pm == PackageManager.Npm ? "package-lock.json" : "yarn.lock";
    }

    ExecutionContext ctx() {
        HttpSender http = request -> {
            String url = request.getUrl().toString();
            byte[] body = url.startsWith(REGISTRY) ? served(url.substring(REGISTRY.length())) :
                    url.startsWith(YARN_REGISTRY) ? served(url.substring(YARN_REGISTRY.length())) : null;
            if (body == null) {
                unserved.add(url);
            }
            return new HttpSender.Response(body == null ? 404 : 200,
                    new ByteArrayInputStream(body == null ? new byte[0] : body), () -> {
            });
        };
        ExecutionContext ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        NodeExecutionContextView.view(ctx).setRegistries(singletonList(
                new NodeRegistry(null, REGISTRY, null, null, null, null, false, null, true, false)));
        return ctx;
    }

    private byte @Nullable [] served(String route) {
        String[] parts = route.split("/");
        String file = parts.length == 1 ? parts[0] : parts.length == 2 ? parts[0] + "-" + parts[1] :
                parts[parts.length - 1];
        Path path = dir.resolve("http").resolve(file);
        try {
            return Files.isRegularFile(path) ? Files.readAllBytes(path) : null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * This recording run over the pnpm-workspace.yaml it was given, which the package manager's first install rewrote.
     */
    CatalogRecording fromTheGivenWorkspace() {
        workspaceBefore = "workspace-given";
        return this;
    }

    /**
     * Every file the recording holds, before the edit and as the package manager left it ({@code null} for one it
     * left unchanged); the lock must regenerate without a warning.
     */
    SourceSpecs[] sources() {
        return sources(null, null);
    }

    /**
     * Run {@code recipes} over the recording's files in every order a host can visit them: each order leaves every
     * file as the package manager did, without a warning, and a second run over that changes nothing.
     */
    void assertEveryVisitOrder(Recipe... recipes) {
        Map<String, String> before = new LinkedHashMap<>();
        Map<String, String> after = new LinkedHashMap<>();
        before.put("package.json", read("pkg-before"));
        after.put("package.json", read("pkg"));
        Map<String, String> members = new TreeMap<>(tree("tree-before"));
        members.keySet().removeIf(path -> !path.endsWith("package.json"));
        Map<String, String> membersAfter = new TreeMap<>(members);
        membersAfter.putAll(tree("tree"));
        for (String member : members.keySet()) {
            before.put(member, members.get(member));
            after.put(member, membersAfter.get(member));
        }
        String workspace = workspace();
        if (workspace != null) {
            before.put("pnpm-workspace.yaml", workspace);
            after.put("pnpm-workspace.yaml", has("workspace") ? read("workspace") : workspace);
        }
        if (has("yarnrc")) {
            before.put(".yarnrc.yml", read("yarnrc"));
            after.put(".yarnrc.yml", read(has("yarnrc-after") ? "yarnrc-after" : "yarnrc"));
        }
        String lock = lockName();
        before.put(lock, read("before"));
        after.put(lock, read("after"));
        List<String> failures = new ArrayList<>();
        for (List<String> order : orders(new ArrayList<>(before.keySet()))) {
            List<SourceFile> sources = new ArrayList<>();
            for (String path : order) {
                sources.add(parse(path, before.get(path), listed(members.keySet())));
            }
            List<SourceFile> edited = run(sources, recipes);
            for (SourceFile sf : edited) {
                String path = sf.getSourcePath().toString();
                if (!after.get(path).equals(printed(sf))) {
                    failures.add(order + ": " + path + " is\n" + printed(sf));
                }
                sf.getMarkers().findFirst(Markup.class).ifPresent(m -> failures.add(order + ": " + path +
                        " is marked " + m.getMessage()));
            }
            List<SourceFile> again = run(edited, recipes);
            for (int i = 0; i < again.size(); i++) {
                if (again.get(i) != edited.get(i)) {
                    failures.add(order + ": a second run changes " + again.get(i).getSourcePath() + " to\n" +
                            printed(again.get(i)));
                }
            }
        }
        assertThat(failures).as("unserved " + unserved).isEmpty();
    }

    private List<SourceFile> run(List<SourceFile> sources, Recipe... recipes) {
        RecipeRun run = new CompositeRecipe(Arrays.asList(recipes)).run(new InMemoryLargeSourceSet(sources), ctx(), 1);
        Map<Path, SourceFile> edited = new HashMap<>();
        for (Result result : run.getChangeset().getAllResults()) {
            if (result.getBefore() != null && result.getAfter() != null) {
                edited.put(result.getBefore().getSourcePath(), result.getAfter());
            }
        }
        List<SourceFile> out = new ArrayList<>();
        for (SourceFile sf : sources) {
            out.add(edited.getOrDefault(sf.getSourcePath(), sf));
        }
        return out;
    }

    private SourceFile parse(String path, String text, List<String> members) {
        Parser parser = path.endsWith(".json") ? JsonParser.builder().build() :
                path.endsWith(".yaml") || path.endsWith(".yml") ? YamlParser.builder().build() :
                        PlainTextParser.builder().build();
        SourceFile sf = parser.parse(ctx(), text).findFirst().orElseThrow(IllegalStateException::new)
                .withSourcePath(Paths.get(path));
        if (path.endsWith("package.json")) {
            sf = sf.withMarkers(sf.getMarkers().add("package.json".equals(path) ?
                    nodeResolutionResult(pm, members) : nodeResolutionResult(pm)));
        }
        return sf;
    }

    // the pnpm-workspace.yaml the edit starts from; `workspace` alone holds for both phases
    private @Nullable String workspace() {
        return has(workspaceBefore) ? read(workspaceBefore) : has("workspace") ? read("workspace") : null;
    }

    // the member manifests the host's root marker names: for pnpm those pnpm-workspace.yaml's packages match
    private List<String> listed(Collection<String> manifests) {
        String workspace = workspace();
        Object packages = pm != PackageManager.Pnpm || workspace == null ? null :
                ((Map<?, ?>) new Yaml().load(workspace)).get("packages");
        if (!(packages instanceof List)) {
            return new ArrayList<>(manifests);
        }
        List<String> listed = new ArrayList<>();
        for (String manifest : manifests) {
            String dir = manifest.substring(0, manifest.length() - "/package.json".length());
            for (Object glob : (List<?>) packages) {
                if (dir.matches(String.valueOf(glob).replace(".", "\\.").replace("*", "[^/]*"))) {
                    listed.add(manifest);
                    break;
                }
            }
        }
        return listed;
    }

    private static List<List<String>> orders(List<String> paths) {
        List<List<String>> orders = new ArrayList<>();
        if (paths.size() <= 1) {
            orders.add(new ArrayList<>(paths));
            return orders;
        }
        for (int i = 0; i < paths.size(); i++) {
            List<String> rest = new ArrayList<>(paths);
            String first = rest.remove(i);
            for (List<String> order : orders(rest)) {
                order.add(0, first);
                orders.add(order);
            }
        }
        return orders;
    }

    private static String printed(SourceFile sf) {
        return sf.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED));
    }

    /**
     * {@link #sources()} where the recipe declines the edit of {@code member} with a warning naming {@code warning},
     * leaving its text as it was.
     */
    SourceSpecs[] sources(@Nullable String member, @Nullable String warning) {
        List<SourceSpecs> sources = new ArrayList<>();
        Map<String, String> before = tree("tree-before");
        Map<String, String> after = new TreeMap<>(before);
        after.putAll(tree("tree"));
        List<String> members = new ArrayList<>();
        for (String path : before.keySet()) {
            if (path.endsWith("package.json")) {
                members.add(path);
            }
        }
        boolean failed = !has("after");
        sources.add(packageJson(read("pkg-before"), changed(read("pkg-before"), failed ? null : read("pkg")),
                nodeResolutionResult(pm, listed(members)), s -> s.noTrim()));
        for (String path : members) {
            String text = before.get(path);
            if (path.equals(member)) {
                sources.add(packageJson(text, null, nodeResolutionResult(pm), s -> s.path(path).noTrim()
                        .after(actual -> actual).afterRecipe(doc -> {
                            assertThat(doc.printAll(new PrintOutputCapture<>(0,
                                    PrintOutputCapture.MarkerPrinter.SANITIZED))).isEqualTo(text);
                            assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                                    .hasValueSatisfying(w -> assertThat(w.getMessage()).contains(warning));
                        })));
                continue;
            }
            sources.add(packageJson(text, changed(text, failed ? null : after.get(path)), nodeResolutionResult(pm),
                    s -> s.path(path).noTrim()));
        }
        String workspace = workspace();
        if (workspace != null) {
            String edited = failed ? null : has("workspace") ? read("workspace") : workspace;
            sources.add(yamlFile(workspace, changed(workspace, edited), "pnpm-workspace.yaml"));
        }
        if (has("yarnrc")) {
            String edited = failed ? null : has("yarnrc-after") ? read("yarnrc-after") : read("yarnrc");
            sources.add(yamlFile(read("yarnrc"), changed(read("yarnrc"), edited), ".yarnrc.yml"));
        }
        String lock = failed ? null : changed(read("before"), read("after"));
        if (pm == PackageManager.Pnpm) {
            sources.add(pnpmLock(read("before"), lock, s -> s.noTrim().afterRecipe(doc -> assertThat(
                    doc.getMarkers().findFirst(Markup.Warn.class)).as("unserved " + unserved).isEmpty())));
        } else if (pm == PackageManager.Npm) {
            sources.add(packageLock(read("before"), lock, s -> s.noTrim().afterRecipe(doc -> assertThat(
                    doc.getMarkers().findFirst(Markup.Warn.class)).as("unserved " + unserved).isEmpty())));
        } else {
            Consumer<SourceSpec<PlainText>> spec = s -> s.path(lockName())
                    .noTrim().afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                            .as("unserved " + unserved).isEmpty());
            sources.add(lock == null ? text(read("before"), spec) : text(read("before"), lock, spec));
        }
        return sources.toArray(new SourceSpecs[0]);
    }

    /**
     * The pnpm files as the package manager's command left them, where the install over them fails with
     * {@code failure}: each manifest the command changed and the lock, left as it was, marked with it.
     */
    SourceSpecs[] sourcesWhoseInstallFails(String failure) {
        List<SourceSpecs> sources = new ArrayList<>();
        Map<String, String> before = tree("tree-before");
        Map<String, String> after = new TreeMap<>(before);
        after.putAll(tree("tree"));
        List<String> members = new ArrayList<>();
        for (String path : before.keySet()) {
            if (path.endsWith("package.json")) {
                members.add(path);
            }
        }
        sources.add(failing(read("pkg-before"), read("pkg"), nodeResolutionResult(pm, members), "package.json",
                failure));
        for (String path : members) {
            sources.add(failing(before.get(path), after.get(path), nodeResolutionResult(pm), path, failure));
        }
        sources.add(yamlFile(read("workspace-before"), changed(read("workspace-before"), read("workspace")),
                "pnpm-workspace.yaml"));
        sources.add(pnpmLock(read("before"), null, s -> s.noTrim().after(actual -> actual).afterRecipe(doc -> {
            assertThat(doc.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)))
                    .isEqualTo(read("before"));
            assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                    .hasValueSatisfying(w -> assertThat(w.getMessage()).contains(failure));
        })));
        return sources.toArray(new SourceSpecs[0]);
    }

    private static SourceSpecs failing(String before, String after, NodeResolutionResult marker, String path,
                                       String failure) {
        if (before.equals(after)) {
            return packageJson(before, null, marker, s -> s.path(path).noTrim());
        }
        return packageJson(before, null, marker, s -> s.path(path).noTrim().after(actual -> actual)
                .afterRecipe(doc -> {
                    assertThat(doc.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)))
                            .isEqualTo(after);
                    assertThat(doc.getMarkers().findFirst(Markup.Warn.class))
                            .hasValueSatisfying(w -> assertThat(w.getMessage()).contains(failure));
                }));
    }

    // a yaml spec whose null after is no change, not a deletion
    private static SourceSpecs yamlFile(String before, @Nullable String after, String path) {
        return after == null ? yaml(before, s -> s.path(path).noTrim()) :
                yaml(before, after, s -> s.path(path).noTrim());
    }

    private static @Nullable String changed(@Nullable String before, @Nullable String after) {
        return after == null || after.equals(before) ? null : after;
    }

    private boolean has(String file) {
        return Files.isRegularFile(dir.resolve(file));
    }

    String read(String file) {
        try {
            return new String(Files.readAllBytes(dir.resolve(file)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Map<String, String> tree(String name) {
        Map<String, String> files = new TreeMap<>();
        Path root = dir.resolve(name);
        if (!Files.isDirectory(root)) {
            return files;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path file : walk.filter(Files::isRegularFile).collect(toList())) {
                files.put(root.relativize(file).toString().replace('\\', '/'),
                        new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files;
    }
}
