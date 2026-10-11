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

import org.jspecify.annotations.Nullable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.javascript.internal.registry.Environment;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.RegistryDiscovery;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

/**
 * The host's side for a recording's user .npmrc: the marker's user scope, as the host reads $HOME/.npmrc, and
 * discovered from there as the host discovers them.
 */
public final class HostRegistries {

    /**
     * A host that names no registry and holds no credentials, its home without config.
     */
    public static final HostInputs NONE = host(null, Paths.get("/nonexistent-home"));

    private HostRegistries() {
    }

    /**
     * @param home the directory standing for the recording's scratch HOME, which holds no other config
     */
    public static NodeRegistries fromUserNpmrc(String npmrc, Path home) {
        return RegistryDiscovery.discover(new InMemoryExecutionContext(), userMarker(npmrc), environment(home));
    }

    /**
     * The host's side of a regeneration whose user .npmrc is a recording's (null for none), its scratch HOME {@code
     * home}; its clock and machine are the port inputs' own.
     */
    public static HostInputs host(@Nullable String npmrc, Path home) {
        HostInputs.HostInputsBuilder host = HostInputs.builder().now(Instant.EPOCH).system(environment(home));
        if (npmrc != null) {
            host.npmrc(new Npmrc(NpmrcScope.User, properties(npmrc)));
        }
        return host.build();
    }

    /**
     * A marker whose only npmrc is a recording's user .npmrc, in the user scope.
     */
    public static NodeResolutionResult userMarker(String npmrc) {
        return nodeResolutionResult(PackageManager.Npm)
          .withNpmrcConfigs(singletonList(new Npmrc(NpmrcScope.User, properties(npmrc))));
    }

    private static Map<String, String> properties(String npmrc) {
        Map<String, String> properties = new LinkedHashMap<>();
        for (Map.Entry<String, @Nullable Object> entry : Ini.decode(npmrc, Ini.Release.V6_0_0).entrySet()) {
            properties.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return properties;
    }

    /**
     * The environment of a recording's scratch HOME: no variables, and no config there but what the recording wrote.
     */
    public static Environment environment(Path home) {
        return new Environment() {
            @Override
            public @Nullable String getenv(String name) {
                return null;
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
}
