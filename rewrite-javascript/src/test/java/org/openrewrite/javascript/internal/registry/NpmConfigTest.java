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

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NpmConfigTest {

    private static Environment env(Map<String, String> vars) {
        return new Environment() {
            @Override
            public @Nullable String getenv(String name) {
                return vars.get(name);
            }

            @Override
            public Path userHome() {
                return Path.of("/nonexistent");
            }

            @Override
            public String osName() {
                return "Mac OS X";
            }
        };
    }

    private static NpmConfig config(Map<String, String> props) {
        return new NpmConfig(props, env(Map.of()));
    }

    @Test
    void defaultRegistry() {
        assertThat(config(Map.of("registry", "https://r.example/")).defaultRegistry().value)
                .isEqualTo("https://r.example/");
    }

    @Test
    void scopeRegistryRouting() {
        NpmConfig config = config(Map.of(
                "registry", "https://r.example/",
                "@angular:registry", "https://ng.example/"));
        assertThat(config.scopeRegistries().get("@angular").value).isEqualTo("https://ng.example/");
    }

    @Test
    void nerfDartMostSpecificPathWins() {
        NpmConfig config = config(Map.of(
                "//host.example/a/b/:_authToken", "deep",
                "//host.example/:_authToken", "root"));
        assertThat(config.authFor("https://host.example/a/b/").authToken).isEqualTo("deep");
        assertThat(config.authFor("https://host.example/x/").authToken).isEqualTo("root");
    }

    @Test
    void bearerToken() {
        NpmConfig config = config(Map.of("//host.example/:_authToken", "tok123"));
        NpmConfig.Auth auth = config.authFor("https://host.example/");
        assertThat(auth).isNotNull();
        assertThat(auth.authToken).isEqualTo("tok123");
    }

    @Test
    void globalAuthBase64FallsBack() {
        NpmConfig config = config(Map.of("_auth", "dXNlcjpwYXNz"));
        NpmConfig.Auth auth = config.authFor("https://host.example/");
        assertThat(auth).isNotNull();
        assertThat(auth.authBase64).isEqualTo("dXNlcjpwYXNz");
    }

    @Test
    void usernameAndBase64PasswordDecoded() {
        NpmConfig config = config(Map.of(
                "//host.example/:username", "bob",
                "//host.example/:_password", "c2VjcmV0"));
        NpmConfig.Auth auth = config.authFor("https://host.example/");
        assertThat(auth).isNotNull();
        assertThat(auth.username).isEqualTo("bob");
        assertThat(auth.password).isEqualTo("secret");
    }

    @Test
    void alwaysAuthPerRegistryAndGlobal() {
        NpmConfig perRegistry = config(Map.of(
                "//host.example/:_authToken", "t",
                "//host.example/:always-auth", "true"));
        assertThat(perRegistry.authFor("https://host.example/").alwaysAuth).isTrue();

        assertThat(config(Map.of("always-auth", "true")).isAlwaysAuth()).isTrue();
        assertThat(config(Map.of()).isAlwaysAuth()).isFalse();
    }

    @Test
    void strictSslDefaultsTrue() {
        assertThat(config(Map.of()).isStrictSsl()).isTrue();
        assertThat(config(Map.of("strict-ssl", "false")).isStrictSsl()).isFalse();
    }

    @Test
    void cafileAndProxyCarried() {
        NpmConfig config = config(Map.of(
                "cafile", "/etc/certs/corp.pem",
                "proxy", "http://proxy.example:8080",
                "https-proxy", "http://proxy.example:8443",
                "no-proxy", "localhost"));
        assertThat(config.getCafile()).isEqualTo("/etc/certs/corp.pem");
        assertThat(config.getProxy()).isEqualTo("http://proxy.example:8080");
        assertThat(config.getHttpsProxy()).isEqualTo("http://proxy.example:8443");
        assertThat(config.getNoProxy()).isEqualTo("localhost");
    }

    @Test
    void envExpandedRegistry() {
        NpmConfig config = new NpmConfig(Map.of("registry", "https://${NPM_HOST}/"),
                env(Map.of("NPM_HOST", "r.example")));
        EnvExpansion.Expansion resolved = config.defaultRegistry();
        assertThat(resolved.value).isEqualTo("https://r.example/");
        assertThat(resolved.unresolvedPlaceholders).isFalse();
    }

    @Test
    void envDefaultForm() {
        NpmConfig config = new NpmConfig(Map.of("registry", "${NPM_REG:-https://fallback.example/}"),
                env(Map.of()));
        EnvExpansion.Expansion resolved = config.defaultRegistry();
        assertThat(resolved.value).isEqualTo("https://fallback.example/");
        assertThat(resolved.unresolvedPlaceholders).isFalse();
    }

    @Test
    void unsetEnvFlagsUnresolved() {
        NpmConfig config = new NpmConfig(Map.of("registry", "https://${NPM_HOST}/"), env(Map.of()));
        EnvExpansion.Expansion resolved = config.defaultRegistry();
        assertThat(resolved.value).isEqualTo("https://${NPM_HOST}/");
        assertThat(resolved.unresolvedPlaceholders).isTrue();
    }

    @Test
    void unsetEnvInAuthFlagsUnresolved() {
        NpmConfig config = new NpmConfig(Map.of("//host.example/:_authToken", "${NPM_TOKEN}"), env(Map.of()));
        NpmConfig.Auth auth = config.authFor("https://host.example/");
        assertThat(auth).isNotNull();
        assertThat(auth.unresolvedPlaceholders).isTrue();
    }
}
