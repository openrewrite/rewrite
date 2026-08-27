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
import static org.openrewrite.python.Assertions.pyproject;
import static org.openrewrite.python.Assertions.uvLock;

/**
 * End-to-end recipe test of native uv.lock regeneration against recorded pypi.org
 * responses (fixtures under {@code /uvlock/}); the expected lock is byte-identical
 * to what uv 0.10.11 wrote for the same edit.
 */
class UpgradeDependencyVersionUvLockTest implements RewriteTest {

    ExecutionContext ctx;
    final Map<String, String> routes = new HashMap<>();

    @BeforeEach
    void setUp() {
        routes.clear();
        routes.put("https://pypi.org/simple/six/", resource("http/six-listing-json"));
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
    void markerResolvedDependenciesUpdatedAfterEdit() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("six", "==1.17.0", null, null))
            .executionContext(ctx),
          pyproject(
            """
              [project]
              name = "fixture-i"
              version = "0.1.0"
              requires-python = ">=3.12"
              dependencies = [
                  "six==1.16.0",
                  "iniconfig>=2.0",
              ]

              [tool.uv]
              """,
            """
              [project]
              name = "fixture-i"
              version = "0.1.0"
              requires-python = ">=3.12"
              dependencies = [
                  "six==1.17.0",
                  "iniconfig>=2.0",
              ]

              [tool.uv]
              """,
            s -> s.afterRecipe(doc -> {
                PythonResolutionResult marker = doc.getMarkers()
                        .findFirst(PythonResolutionResult.class).orElseThrow();
                assertThat(marker.getResolvedDependencies())
                        .as("regenerated uv.lock should carry the bumped six among resolved dependencies")
                        .anySatisfy(d -> {
                            assertThat(PythonResolutionResult.normalizeName(d.getName())).isEqualTo("six");
                            assertThat(d.getVersion()).isEqualTo("1.17.0");
                        });
                assertThat(marker.getDependencies())
                        .filteredOn(d -> "six".equals(PythonResolutionResult.normalizeName(d.getName())))
                        .singleElement()
                        .satisfies(d -> assertThat(d.getResolved())
                                .as("declared `six` dep should be linked to its resolved entry")
                                .isNotNull());
            })
          ),
          uvLock(
            resource("i-minimal-update/uv.lock.v1"),
            s -> s.noTrim().after(actual -> resource("i-minimal-update/uv.lock.v2"))
          )
        );
    }

    private static String resource(String name) {
        try (InputStream is = UpgradeDependencyVersionUvLockTest.class.getResourceAsStream("/uvlock/" + name)) {
            assertThat(is).as(name).isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
