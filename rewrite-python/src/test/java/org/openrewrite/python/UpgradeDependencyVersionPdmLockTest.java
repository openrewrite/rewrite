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
package org.openrewrite.python;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.test.RewriteTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.python.Assertions.pdmLock;
import static org.openrewrite.python.Assertions.pyproject;

/**
 * End-to-end recipe test of native pdm.lock regeneration against recorded pypi.org responses
 * (fixtures under {@code /pdmlock/}); the expected lock is byte-identical to what PDM 2.28.0 wrote
 * for the same edit.
 */
class UpgradeDependencyVersionPdmLockTest implements RewriteTest {

    ExecutionContext ctx;
    final Map<String, String> routes = new HashMap<>();

    @BeforeEach
    void setUp() {
        routes.clear();
        routes.put("https://pypi.org/simple/six/", resource("http/six-simple-json"));
        routes.put("https://files.pythonhosted.org/packages/b7/ce/149a00dd41f10bc29e5921b496af8b574d8413afcd5e30dfa0ed46c2cc5e/six-1.17.0-py2.py3-none-any.whl.metadata",
          resource("http/six-1.17.0-py2.py3-none-any.whl.metadata"));
        HttpSender http = request -> {
            String body = routes.get(request.getUrl().toString());
            return new HttpSender.Response(body == null ? 404 : 200,
              new ByteArrayInputStream((body == null ? "" : body).getBytes(StandardCharsets.UTF_8)), () -> {
            });
        };
        ctx = new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        });
        HttpSenderExecutionContextView.view(ctx).setHttpSender(http);
        PythonExecutionContextView.view(ctx).setPackageIndexes(List.of(
          new PythonPackageIndex("pypi", "https://pypi.org/simple", true, null, null, false)));
    }

    @Test
    @Timeout(120)
    void pinBumpRegeneratesPdmLockByteForByte() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("six", "==1.17.0", null, null))
            .executionContext(ctx),
          pyproject(
            resource("g-upgrade/pyproject.toml.before"),
            resource("g-upgrade/pyproject.toml.after"),
            s -> s.afterRecipe(doc -> {
                PythonResolutionResult marker = doc.getMarkers()
                        .findFirst(PythonResolutionResult.class).orElseThrow();
                assertThat(marker.getPackageManager())
                        .isEqualTo(PythonResolutionResult.PackageManager.Pdm);
                assertThat(marker.getResolvedDependencies())
                        .anySatisfy(d -> {
                            assertThat(PythonResolutionResult.normalizeName(d.getName())).isEqualTo("six");
                            assertThat(d.getVersion()).isEqualTo("1.17.0");
                        });
            })
          ),
          pdmLock(
            resource("g-upgrade/pdm.lock.before"),
            s -> s.noTrim().after(actual -> resource("g-upgrade/pdm.lock.after"))
          )
        );
    }

    private static String resource(String name) {
        try (InputStream is = UpgradeDependencyVersionPdmLockTest.class.getResourceAsStream("/pdmlock/" + name)) {
            assertThat(is).as(name).isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
