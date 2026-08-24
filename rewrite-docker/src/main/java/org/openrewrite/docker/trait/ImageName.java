/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.docker.trait;

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/// The `name` of `name:tag@digest`, decomposed into the registry it is pulled from and the path
/// within that registry. `ubuntu`, `library/ubuntu` and `docker.io/library/ubuntu` share a
/// [#getFamiliar()] and a [#getCanonical()] spelling, which is how a recipe can match one of them
/// having been configured with another.
@Value
public class ImageName {

    public static final String DOCKER_HUB = "docker.io";

    public static final String OFFICIAL_NAMESPACE = "library";

    private static final List<String> DOCKER_HUB_ALIASES = Arrays.asList(
            DOCKER_HUB,
            "index.docker.io",
            "registry.hub.docker.com",
            "registry-1.docker.io");

    @Nullable
    String registry;

    /// Every path component but the last, as in `dotnet/framework` of `dotnet/framework/sdk`.
    @Nullable
    String namespace;

    String repository;

    /// A variable reference is left in the component that holds it, so `${REGISTRY}/app` has a
    /// registry of `${REGISTRY}`.
    public static ImageName parse(String name) {
        String registry = null;
        String path = name;
        int slash = name.indexOf('/');
        if (slash >= 0 && isRegistry(name.substring(0, slash))) {
            registry = name.substring(0, slash);
            path = name.substring(slash + 1);
        }
        int lastSlash = path.lastIndexOf('/');
        return new ImageName(
                registry,
                lastSlash < 0 ? null : path.substring(0, lastSlash),
                path.substring(lastSlash + 1));
    }

    /// Registry-ness cannot be told from the syntax: `mcr.microsoft.com/windows/servercore` names a
    /// registry, while `redhat/ubi9-minimal` names an organisation on Docker Hub. This is the rule
    /// Docker itself settles that by.
    static boolean isRegistry(String component) {
        return component.indexOf('.') >= 0 ||
                component.indexOf(':') >= 0 ||
                "localhost".equals(component) ||
                !component.equals(component.toLowerCase(Locale.ROOT));
    }

    public String getPath() {
        return namespace == null ? repository : namespace + '/' + repository;
    }

    public String getResolvedRegistry() {
        return registry == null ? DOCKER_HUB : registry;
    }

    public boolean isDockerHub() {
        return registry == null || DOCKER_HUB_ALIASES.contains(registry.toLowerCase(Locale.ROOT));
    }

    /// The shortest spelling that resolves to the same image: `docker.io/library/ubuntu` is `ubuntu`.
    public String getFamiliar() {
        if (!isDockerHub()) {
            return toString();
        }
        return OFFICIAL_NAMESPACE.equals(namespace) ? repository : getPath();
    }

    /// Names the registry and namespace even where the name leaves them out: `ubuntu` is
    /// `docker.io/library/ubuntu`.
    public String getCanonical() {
        if (isDockerHub()) {
            return DOCKER_HUB + '/' + (namespace == null ? OFFICIAL_NAMESPACE : namespace) + '/' + repository;
        }
        return registry + '/' + getPath();
    }

    @Override
    public String toString() {
        return registry == null ? getPath() : registry + '/' + getPath();
    }
}
