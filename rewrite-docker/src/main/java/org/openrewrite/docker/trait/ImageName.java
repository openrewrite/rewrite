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

/**
 * The name part of an image reference, decomposed into the registry it is pulled from and the path
 * within that registry. This is the {@code name} of {@code name:tag@digest}, so a tag and a digest
 * are not part of it.
 * <p>
 * Whether the first path component is a registry cannot be decided from the syntax alone:
 * {@code mcr.microsoft.com/windows/servercore} names a registry, while {@code redhat/ubi9-minimal}
 * names an organisation on Docker Hub. Docker resolves this by treating the component as a registry
 * only when it contains a {@code .} or a {@code :}, is exactly {@code localhost}, or carries an
 * uppercase character, which a path component may not. This class applies that same rule.
 * <p>
 * A name therefore has two other spellings besides the one it was written with:
 * {@link #getFamiliar() familiar}, the shortest form that still resolves to the same image, and
 * {@link #getCanonical() canonical}, the fully qualified form. {@code ubuntu},
 * {@code library/ubuntu} and {@code docker.io/library/ubuntu} share both, which is how a recipe can
 * match one of them having been configured with another.
 */
@Value
public class ImageName {

    /**
     * The registry an image without one is pulled from.
     */
    public static final String DOCKER_HUB = "docker.io";

    /**
     * The namespace an official Docker Hub image lives in.
     */
    public static final String OFFICIAL_NAMESPACE = "library";

    private static final List<String> DOCKER_HUB_ALIASES = Arrays.asList(
            DOCKER_HUB,
            "index.docker.io",
            "registry.hub.docker.com",
            "registry-1.docker.io");

    /**
     * The registry as written, or {@code null} where the name does not name one.
     */
    @Nullable
    String registry;

    /**
     * Every path component but the last, as in {@code library} of {@code library/ubuntu} or
     * {@code dotnet/framework} of {@code dotnet/framework/sdk}, or {@code null} where the path is a
     * single component.
     */
    @Nullable
    String namespace;

    /**
     * The last path component, as in {@code ubuntu} of {@code library/ubuntu}.
     */
    String repository;

    /**
     * Decomposes an image name such as {@code docker.io/library/ubuntu}. Environment variable
     * references are left in the component that holds them, so {@code ${REGISTRY}/app} has a
     * registry of {@code ${REGISTRY}}.
     */
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

    /**
     * Whether a path component names a registry rather than being part of the path, by the rule
     * Docker itself applies: it does when it carries a {@code .} or a {@code :}, is exactly
     * {@code localhost}, or is not all lowercase, which a path component is required to be.
     */
    static boolean isRegistry(String component) {
        return component.indexOf('.') >= 0 ||
                component.indexOf(':') >= 0 ||
                "localhost".equals(component) ||
                !component.equals(component.toLowerCase(Locale.ROOT));
    }

    /**
     * The path within the registry, as in {@code library/ubuntu} of
     * {@code docker.io/library/ubuntu}.
     */
    public String getPath() {
        return namespace == null ? repository : namespace + '/' + repository;
    }

    /**
     * The registry the image is pulled from, which is Docker Hub for a name that does not write one.
     */
    public String getResolvedRegistry() {
        return registry == null ? DOCKER_HUB : registry;
    }

    /**
     * Whether the image is pulled from Docker Hub, whether or not the name says so.
     */
    public boolean isDockerHub() {
        return registry == null || DOCKER_HUB_ALIASES.contains(registry.toLowerCase(Locale.ROOT));
    }

    /**
     * The shortest spelling that resolves to the same image, dropping a Docker Hub registry and the
     * {@code library} namespace of an official image: {@code docker.io/library/ubuntu} is
     * {@code ubuntu}, and {@code docker.io/myuser/myimage} is {@code myuser/myimage}. A name on any
     * other registry is already its own shortest spelling.
     */
    public String getFamiliar() {
        if (!isDockerHub()) {
            return toString();
        }
        return OFFICIAL_NAMESPACE.equals(namespace) ? repository : getPath();
    }

    /**
     * The fully qualified spelling, naming the registry and the namespace even where the name
     * itself leaves them out: {@code ubuntu} is {@code docker.io/library/ubuntu}.
     */
    public String getCanonical() {
        if (isDockerHub()) {
            return DOCKER_HUB + '/' + (namespace == null ? OFFICIAL_NAMESPACE : namespace) + '/' + repository;
        }
        return registry + '/' + getPath();
    }

    /**
     * The name as it was written.
     */
    @Override
    public String toString() {
        return registry == null ? getPath() : registry + '/' + getPath();
    }
}
