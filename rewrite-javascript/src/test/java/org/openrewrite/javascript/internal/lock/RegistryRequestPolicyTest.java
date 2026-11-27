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
package org.openrewrite.javascript.internal.lock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.javascript.internal.lock.npm.NpmOptions;
import org.openrewrite.javascript.internal.lock.npm.NpmRelease;
import org.openrewrite.javascript.internal.registry.RegistryTransport;

import java.util.Collections;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.openrewrite.javascript.internal.LockFileRegeneration.Request;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.registry.Environment;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistryRequestPolicyTest {
    @ParameterizedTest
    @ValueSource(strings = {"offline=true", "fetch-timeout=1500"})
    void projectRequestPolicyMustNotFallThroughToDefaultTransport(String setting) {
        NpmOptions options = NpmOptions.fromNpmrc(NpmRelease.PORTED, null, setting + "\n");
        assertThatThrownBy(() -> options.transport(Collections.emptyMap()))
                .isInstanceOf(CannotKnow.class).hasMessageContaining("network policy");
    }

    @ParameterizedTest
    @ValueSource(strings = {"offline=true", "fetch-timeout=1500"})
    void userRequestPolicyMustNotFallThroughToDefaultTransport(String setting) {
        NpmOptions options = NpmOptions.fromNpmrc(NpmRelease.PORTED, setting + "\n", null);
        assertThatThrownBy(() -> options.transport(Collections.emptyMap()))
                .isInstanceOf(CannotKnow.class).hasMessageContaining("network policy");
    }

    @ParameterizedTest
    @ValueSource(strings = {"npm_config_offline", "NPM_CONFIG_OFFLINE", "npm_config_fetch_timeout", "NPM_CONFIG_FETCH_TIMEOUT"})
    void environmentRequestPolicyMustNotFallThroughToDefaultTransport(String name) {
        assertThatThrownBy(() -> RegistryTransport.npm(Collections.emptyMap(),
                Collections.singletonMap(name, name.toLowerCase().endsWith("offline") ? "true" : "1500")))
                .isInstanceOf(CannotKnow.class).hasMessageContaining("network policy");
    }

    @Test
    void explicitOfflineFalseAndUnconfiguredTransportRemainAvailable() {
        assertThatCode(() -> NpmOptions.fromNpmrc(NpmRelease.PORTED, null, "offline=false\n")
                .transport(Collections.emptyMap())).doesNotThrowAnyException();
        assertThatCode(() -> RegistryTransport.npm(Collections.emptyMap(),
                Collections.singletonMap("npm_config_offline", "false"))).doesNotThrowAnyException();
        assertThatCode(() -> RegistryTransport.npm(Collections.emptyMap(),
                Collections.singletonMap("npm_config_offline", " false "))).doesNotThrowAnyException();
        assertThatCode(() -> NpmOptions.DEFAULT.transport(Collections.emptyMap())).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"project-offline", "project-timeout", "user-offline", "user-timeout", "env-offline", "env-timeout"})
    void unsupportedRequestPolicyCannotFetchOrProducePartialFiles(String source) {
        String key = source.endsWith("offline") ? "offline" : "fetch-timeout";
        String value = source.endsWith("offline") ? "true" : "1500";
        Environment environment = new Environment() {
            public String getenv(String name) {
                return source.startsWith("env-") && name.equals("npm_config_" + key.replace('-', '_')) ? value : null;
            }
            public Path userHome() { return Paths.get("/nonexistent-request-policy-test"); }
            public String osName() { return "Linux"; }
        };
        HostInputs.HostInputsBuilder host = HostInputs.builder().now(Instant.EPOCH).system(environment)
                .packageManager(PackageManager.Npm).packageManagerVersion("11.19.1");
        if (source.startsWith("user-")) {
            host.npmrc(new Npmrc(NpmrcScope.User, Collections.singletonMap(key, value)));
        }
        Request request = Request.builder()
                .packageJson("{\"name\":\"request-policy\",\"version\":\"1.0.0\",\"dependencies\":{\"is-number\":\"7.0.0\"}}")
                .packageJsonPath(Paths.get("package.json"))
                .packageManagerFile(Paths.get(".npmrc"), "registry=http://registry.invalid/\n" +
                        (source.startsWith("project-") ? key + "=" + value + "\n" : ""))
                .lock("{\"name\":\"request-policy\",\"version\":\"1.0.0\",\"lockfileVersion\":3,\"packages\":{\"\":{\"name\":\"request-policy\",\"version\":\"1.0.0\"}}}").build();
        AtomicInteger sent = new AtomicInteger();
        Result result = NpmInputs.PORT.regenerate(LockDirectory.of(request), host.build(), new NpmRegistryClient(r -> {
            sent.incrementAndGet();
            throw new IllegalStateException("request policy escaped to a live sender");
        }));
        assertThat(sent).hasValue(0);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailure().getDetail()).contains("network policy");
        assertThat(result.getLockFileContent()).isNull();
        assertThat(result.getFiles()).isEmpty();
    }

    @Test
    void explicitFalseUsesNpmSourcePrecedence() {
        assertThatCode(() -> NpmOptions.fromNpmrc(NpmRelease.PORTED, "offline=true\n", "offline=false\n")
                .transport(Collections.emptyMap())).doesNotThrowAnyException();
        NpmOptions project = NpmOptions.fromNpmrc(NpmRelease.PORTED, null, "offline=true\n");
        assertThatCode(() -> project.transport(Collections.singletonMap("npm_config_offline", "false")))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> project.transport(Collections.singletonMap("npm_config_offline", "")))
                .isInstanceOf(CannotKnow.class);
    }
}
