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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.*;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.JsonParser;
import org.openrewrite.json.tree.Json;
import org.openrewrite.text.PlainText;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.dependency;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

class YarnBerryNestedProjectAtomicityTest {
    @ParameterizedTest(name = "root edited: {0}, source order: {1}, nested edited: {2}")
    @CsvSource({"false,0,true", "false,1,true", "false,2,true", "true,0,true", "true,1,true", "true,2,true",
            "true,0,false", "true,1,false", "true,2,false"})
    void anUnregisteredNestedProjectCannotLeaveDependencyEdits(boolean editRoot, int order,
                                                               boolean editNested) throws Exception {
        String root = resource("pkg-before");
        if (!editRoot) root = root.replace(",\n    \"ms\": \"^2.1.1\"", "");
        String nested = """
          {"name":"golf","private":true,"dependencies":{"ms":"2.0.0"}}
          """;
        if (!editNested) nested = nested.replace("2.0.0", "2.1.3");
        String lock = resource("before");
        ExecutionContext ctx = new InMemoryExecutionContext(t -> { throw new RuntimeException(t); });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(request -> {
            String url = request.getUrl().toString();
            byte[] body = null;
            String prefix = "https://registry.npmjs.org/";
            if (url.startsWith(prefix)) {
                String path = url.substring(prefix.length());
                String file = path.equals("ms") ? "ms@full" : path.equals("ms/2.1.3") ? "ms-2.1.3" :
                        path.endsWith("ms-2.1.3.tgz") ? "ms-2.1.3.tgz" : null;
                if (file != null) {
                    try (InputStream in = getClass().getResourceAsStream("/lock/yarn-berry/merged-rehead/http/" + file)) {
                        if (in != null) body = in.readAllBytes();
                    } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
                }
            }
            return new HttpSender.Response(body == null ? 404 : 200,
                    new ByteArrayInputStream(body == null ? new byte[0] : body), () -> {});
        });
        Json.Document rootDoc = document(ctx, "package.json", root, "^2.1.1");
        Json.Document nestedDoc = document(ctx, "examples/website/golf/package.json", nested, "2.0.0");
        SourceFile lockDoc = PlainText.builder().text(lock).build().withSourcePath(Paths.get("yarn.lock"));
        List<SourceFile> sources = order == 0 ? Arrays.asList(rootDoc, nestedDoc, lockDoc) :
                order == 1 ? Arrays.asList(nestedDoc, lockDoc, rootDoc) : Arrays.asList(lockDoc, rootDoc, nestedDoc);
        RecipeRun run = new UpgradeDependencyVersion("ms", null, "2.1.3")
                .run(new InMemoryLargeSourceSet(sources), ctx);
        Map<String, String> expected = new HashMap<>();
        expected.put("package.json", root);
        expected.put("examples/website/golf/package.json", nested);
        expected.put("yarn.lock", lock);
        Map<String, String> actual = new HashMap<>(expected);
        if (!editNested) {
            expected.put("package.json", resource("pkg"));
            expected.put("yarn.lock", resource("after"));
        }
        for (Result result : run.getChangeset().getAllResults()) {
            SourceFile after = Objects.requireNonNull(result.getAfter());
            actual.put(after.getSourcePath().toString(), after.printAll(
                    new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED)));
        }
        assertThat(actual).isEqualTo(expected);
        if (editNested) {
            assertThat(run.getDataTableRows(NodeLockRegenerationFailures.class))
                    .anySatisfy(row -> assertThat(row.getDetail()).contains("nearest package directory")
                            .contains("examples/website/golf"));
        } else {
            assertThat(run.getDataTableRows(NodeLockRegenerationFailures.class)).isEmpty();
        }
    }

    private static Json.Document document(ExecutionContext ctx, String path, String text, String range) {
        Json.Document doc = (Json.Document) JsonParser.builder().build().parse(ctx, text).findFirst().orElseThrow();
        return doc.withSourcePath(Paths.get(path)).withMarkers(doc.getMarkers().add(
                nodeResolutionResult(PackageManager.YarnBerry, dependency("ms", range))));
    }

    private static String resource(String file) throws Exception {
        try (InputStream in = YarnBerryNestedProjectAtomicityTest.class.getResourceAsStream(
                "/lock/yarn-berry/merged-rehead/" + file)) {
            return new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
