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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.javascript.NodeExecutionContextView;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.NodeRegistryCredentials;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class RegistryDiscoveryTest {

    @TempDir
    Path home;

    private static ExecutionContext ctx() {
        return new InMemoryExecutionContext(Throwable::printStackTrace);
    }

    private Environment env(Map<String, String> vars) {
        return new Environment() {
            @Override
            public @Nullable String getenv(String name) {
                return vars.get(name);
            }

            @Override
            public Path userHome() {
                return home;
            }

            @Override
            public String osName() {
                return "Mac OS X";
            }
        };
    }

    private static NodeResolutionResult marker(Npmrc... configs) {
        return new NodeResolutionResult(UUID.randomUUID(), null, null, null, "package.json",
                null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
                null, null, Arrays.asList(configs));
    }

    private static Npmrc npmrc(Map<String, String> props) {
        return new Npmrc(NpmrcScope.Project, props);
    }

    @Test
    void explicitRegistriesOnViewWinOutright() {
        ExecutionContext ctx = ctx();
        NodeRegistry hostDefault = new NodeRegistry(null, "https://corp.example/", "tok", null, null, null,
                true, null, true, false);
        NodeRegistry hostScoped = new NodeRegistry("@corp", "https://corp.example/scoped/", "tok2", null, null, null,
                true, null, true, false);
        NodeExecutionContextView.view(ctx).setRegistries(List.of(hostDefault, hostScoped));

        NodeRegistries registries = RegistryDiscovery.discover(ctx,
                marker(npmrc(Map.of("registry", "https://ignored.example/"))), env(Map.of()));

        assertThat(registries.registryFor("lodash")).isSameAs(hostDefault);
        assertThat(registries.registryFor("@corp/thing")).isSameAs(hostScoped);
    }

    @Test
    void defaultsToNpmjsWhenNoRegistryConfigured() {
        NodeRegistries registries = RegistryDiscovery.discover(ctx(), null, env(Map.of()));
        assertThat(registries.getDefaultRegistry().getUrl()).isEqualTo("https://registry.npmjs.org/");
        assertThat(registries.registryFor("lodash").getUrl()).isEqualTo("https://registry.npmjs.org/");
    }

    @Test
    void defaultRegistryFromNpmrc() {
        NodeRegistries registries = RegistryDiscovery.discover(ctx(),
                marker(npmrc(Map.of("registry", "https://mirror.example/"))), env(Map.of()));
        assertThat(registries.getDefaultRegistry().getUrl()).isEqualTo("https://mirror.example/");
    }

    @Test
    void scopeRegistryRouting() {
        NodeRegistries registries = RegistryDiscovery.discover(ctx(),
                marker(npmrc(Map.of(
                        "registry", "https://mirror.example/",
                        "@angular:registry", "https://ng.example/"))),
                env(Map.of()));
        assertThat(registries.registryFor("@angular/core").getUrl()).isEqualTo("https://ng.example/");
        assertThat(registries.registryFor("@angular/core").getScope()).isEqualTo("@angular");
        assertThat(registries.registryFor("lodash").getUrl()).isEqualTo("https://mirror.example/");
    }

    @Test
    void higherScopeNpmrcOverridesLower() {
        NodeRegistries registries = RegistryDiscovery.discover(ctx(),
                marker(new Npmrc(NpmrcScope.User, Map.of("registry", "https://user.example/")),
                        new Npmrc(NpmrcScope.Project, Map.of("registry", "https://project.example/"))),
                env(Map.of()));
        assertThat(registries.getDefaultRegistry().getUrl()).isEqualTo("https://project.example/");
    }

    @Test
    void nerfDartAuthTokenAppliedToRegistry() {
        NodeRegistries registries = RegistryDiscovery.discover(ctx(),
                marker(npmrc(Map.of(
                        "registry", "https://corp.example/npm/",
                        "//corp.example/npm/:_authToken", "s3cret"))),
                env(Map.of()));
        assertThat(registries.getDefaultRegistry().getAuthToken()).isEqualTo("s3cret");
    }

    @Test
    void base64PasswordDecodedOntoRegistry() {
        NodeRegistries registries = RegistryDiscovery.discover(ctx(),
                marker(npmrc(Map.of(
                        "registry", "https://corp.example/",
                        "//corp.example/:username", "bob",
                        "//corp.example/:_password", "c2VjcmV0"))),
                env(Map.of()));
        assertThat(registries.getDefaultRegistry().getUsername()).isEqualTo("bob");
        assertThat(registries.getDefaultRegistry().getPassword()).isEqualTo("secret");
    }

    @Test
    void alwaysAuthSurfaced() {
        NodeRegistries registries = RegistryDiscovery.discover(ctx(),
                marker(npmrc(Map.of(
                        "registry", "https://corp.example/",
                        "//corp.example/:_authToken", "t",
                        "//corp.example/:always-auth", "true"))),
                env(Map.of()));
        assertThat(registries.getDefaultRegistry().isAlwaysAuth()).isTrue();
    }

    @Test
    void envExpandedRegistryUrl() {
        NodeRegistries registries = RegistryDiscovery.discover(ctx(),
                marker(npmrc(Map.of("registry", "https://${NPM_HOST}/"))),
                env(Map.of("NPM_HOST", "resolved.example")));
        assertThat(registries.getDefaultRegistry().getUrl()).isEqualTo("https://resolved.example/");
        assertThat(registries.getDefaultRegistry().isUnresolvedPlaceholders()).isFalse();
    }

    @Test
    void unsetEnvFlagsRegistryUnresolved() {
        NodeRegistries registries = RegistryDiscovery.discover(ctx(),
                marker(npmrc(Map.of("registry", "https://${NPM_HOST}/"))),
                env(Map.of()));
        assertThat(registries.getDefaultRegistry().getUrl()).isEqualTo("https://${NPM_HOST}/");
        assertThat(registries.getDefaultRegistry().isUnresolvedPlaceholders()).isTrue();
    }

    @Test
    void urlEmbeddedCredentialsExtractedAndStripped() {
        NodeRegistries registries = RegistryDiscovery.discover(ctx(),
                marker(npmrc(Map.of("registry", "https://alice:hunter2@corp.example/"))),
                env(Map.of()));
        NodeRegistry registry = registries.getDefaultRegistry();
        assertThat(registry.getUrl()).isEqualTo("https://corp.example/");
        assertThat(registry.getUsername()).isEqualTo("alice");
        assertThat(registry.getPassword()).isEqualTo("hunter2");
    }

    @Test
    void viewCredentialsFillByHost() {
        ExecutionContext ctx = ctx();
        NodeExecutionContextView.view(ctx).setRegistryCredentials(List.of(
                new NodeRegistryCredentials("corp.example", "vtok", null, null)));

        NodeRegistries registries = RegistryDiscovery.discover(ctx,
                marker(npmrc(Map.of("registry", "https://corp.example/"))), env(Map.of()));
        assertThat(registries.getDefaultRegistry().getAuthToken()).isEqualTo("vtok");
    }

    @Test
    void npmrcAuthWinsOverViewCredentials() {
        ExecutionContext ctx = ctx();
        NodeExecutionContextView.view(ctx).setRegistryCredentials(List.of(
                new NodeRegistryCredentials("corp.example", "vtok", null, null)));

        NodeRegistries registries = RegistryDiscovery.discover(ctx,
                marker(npmrc(Map.of(
                        "registry", "https://corp.example/",
                        "//corp.example/:_authToken", "npmrctok"))),
                env(Map.of()));
        assertThat(registries.getDefaultRegistry().getAuthToken()).isEqualTo("npmrctok");
    }

    @Test
    void netrcLastResortByHost() throws IOException {
        Files.writeString(home.resolve(".netrc"), "machine corp.example login bob password sekret\n");

        NodeRegistries registries = RegistryDiscovery.discover(ctx(),
                marker(npmrc(Map.of("registry", "https://corp.example/"))), env(Map.of()));
        assertThat(registries.getDefaultRegistry().getUsername()).isEqualTo("bob");
        assertThat(registries.getDefaultRegistry().getPassword()).isEqualTo("sekret");
    }

    @Test
    void cafileCarriedOntoRegistry() {
        NodeRegistries registries = RegistryDiscovery.discover(ctx(),
                marker(npmrc(Map.of(
                        "registry", "https://corp.example/",
                        "cafile", "/etc/certs/corp.pem",
                        "strict-ssl", "false"))),
                env(Map.of()));
        assertThat(registries.getDefaultRegistry().getCafile()).isEqualTo("/etc/certs/corp.pem");
        assertThat(registries.getDefaultRegistry().isStrictSsl()).isFalse();
    }
}
