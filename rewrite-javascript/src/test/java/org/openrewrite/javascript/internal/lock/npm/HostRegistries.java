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
package org.openrewrite.javascript.internal.lock.npm;

import org.jspecify.annotations.Nullable;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.javascript.internal.lock.Ini;
import org.openrewrite.javascript.internal.registry.Environment;
import org.openrewrite.javascript.internal.registry.NodeRegistries;
import org.openrewrite.javascript.internal.registry.RegistryDiscovery;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;
import org.openrewrite.javascript.marker.NodeResolutionResult.NpmrcScope;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.openrewrite.javascript.Assertions.nodeResolutionResult;

/**
 * The host's registries for a recording's user .npmrc: the marker's user scope, as the host reads $HOME/.npmrc, and
 * discovered from there as the host discovers them.
 */
public final class HostRegistries {

    private HostRegistries() {
    }

    /**
     * @param home the directory standing for the recording's scratch HOME, which holds no other config
     */
    public static NodeRegistries fromUserNpmrc(String npmrc, Path home) {
        return RegistryDiscovery.discover(new InMemoryExecutionContext(), userMarker(npmrc), environment(home));
    }

    /**
     * A marker whose only npmrc is a recording's user .npmrc, in the user scope.
     */
    public static NodeResolutionResult userMarker(String npmrc) {
        Map<String, String> properties = new LinkedHashMap<>();
        for (Map.Entry<String, @Nullable Object> entry : Ini.decode(npmrc).entrySet()) {
            properties.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return nodeResolutionResult(PackageManager.Npm)
          .withNpmrcConfigs(singletonList(new Npmrc(NpmrcScope.User, properties)));
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
