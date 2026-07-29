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
package org.openrewrite.javascript.internal.registry;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.javascript.JavaScriptExecutionContextView;
import org.openrewrite.javascript.NpmRegistryCredentials;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NpmRegistryConfigTest {

    private static NpmRegistryConfig config(String npmrc, ExecutionContext ctx, Map<String, String> env) {
        return new NpmRegistryConfig(NpmRegistryConfig.parseNpmrc(npmrc), ctx, env::get);
    }

    private static NpmRegistryConfig config(String npmrc) {
        return config(npmrc, new InMemoryExecutionContext(), singletonMap("NPM_TOKEN", "s3cret"));
    }

    @Test
    void defaultsToNpmjs() {
        NpmRegistryConfig config = config("");
        assertThat(config.registryFor("lodash")).isEqualTo("https://registry.npmjs.org/");
        assertThat(config.authorizationFor("https://registry.npmjs.org/")).isNull();
    }

    @Test
    void scopedRegistryWinsOverDefault() {
        NpmRegistryConfig config = config(
          "registry=https://npm.corp.example/\n" +
            "@myorg:registry=https://npm.myorg.example/sub/\n");
        assertThat(config.registryFor("lodash")).isEqualTo("https://npm.corp.example/");
        assertThat(config.registryFor("@myorg/pkg")).isEqualTo("https://npm.myorg.example/sub/");
        assertThat(config.registryFor("@other/pkg")).isEqualTo("https://npm.corp.example/");
    }

    @Test
    void authTokenMatchedByHostAndPathWalk() {
        NpmRegistryConfig config = config(
          "@myorg:registry=https://npm.example.com/org/registry/\n" +
            "//npm.example.com/org/registry/:_authToken=deep-token\n" +
            "//npm.example.com/:_authToken=host-token\n");
        assertThat(config.authorizationFor("https://npm.example.com/org/registry/"))
          .isEqualTo("Bearer deep-token");
        assertThat(config.authorizationFor("https://npm.example.com/elsewhere/"))
          .isEqualTo("Bearer host-token");
        assertThat(config.authorizationFor("https://other.example.com/")).isNull();
    }

    @Test
    void usernamePasswordDecodesBase64Password() {
        String encoded = Base64.getEncoder().encodeToString("hunter2".getBytes(StandardCharsets.UTF_8));
        NpmRegistryConfig config = config(
          "//npm.example.com/:username=alice\n" +
            "//npm.example.com/:_password=" + encoded + "\n");
        String expected = "Basic " + Base64.getEncoder()
          .encodeToString("alice:hunter2".getBytes(StandardCharsets.UTF_8));
        assertThat(config.authorizationFor("https://npm.example.com/")).isEqualTo(expected);
    }

    @Test
    void environmentPlaceholdersExpand() {
        NpmRegistryConfig config = config("//npm.example.com/:_authToken=${NPM_TOKEN}\n");
        assertThat(config.authorizationFor("https://npm.example.com/")).isEqualTo("Bearer s3cret");
    }

    @Test
    void unsetPlaceholderFailsOnlyWhenUsed() {
        NpmRegistryConfig config = config(
          "//npm.example.com/:_authToken=${MISSING_VAR}\n",
          new InMemoryExecutionContext(),
          singletonMap("OTHER", "x"));
        assertThat(config.registryFor("lodash")).isEqualTo("https://registry.npmjs.org/");
        assertThatThrownBy(() -> config.authorizationFor("https://npm.example.com/"))
          .isInstanceOf(NpmRegistryException.class)
          .hasMessageContaining("MISSING_VAR");
    }

    @Test
    void executionContextViewOverridesNpmrc() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        JavaScriptExecutionContextView.view(ctx)
          .setNpmDefaultRegistry("https://mirror.example.com")
          .setNpmScopedRegistries(singletonMap("@myorg", "https://scoped.example.com/"))
          .setRegistryCredentials(singletonList(
            NpmRegistryCredentials.token("mirror.example.com", "view-token")));
        NpmRegistryConfig config = config(
          "registry=https://npm.corp.example/\n" +
            "//mirror.example.com/:_authToken=npmrc-token\n",
          ctx, singletonMap("X", "y"));
        assertThat(config.registryFor("lodash")).isEqualTo("https://mirror.example.com/");
        assertThat(config.registryFor("@myorg/pkg")).isEqualTo("https://scoped.example.com/");
        assertThat(config.authorizationFor("https://mirror.example.com/"))
          .as("host-supplied credentials win over .npmrc")
          .isEqualTo("Bearer view-token");
    }

    @Test
    void commentsAndQuotesInNpmrc() {
        Map<String, String> props = NpmRegistryConfig.parseNpmrc(
          "# comment\n; also comment\nregistry=\"https://npm.example.com/\"\n\nbad-line\n");
        assertThat(props).containsExactly(
          java.util.Map.entry("registry", "https://npm.example.com/"));
    }
}
