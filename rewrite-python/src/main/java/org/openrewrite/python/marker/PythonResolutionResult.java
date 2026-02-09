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
package org.openrewrite.python.marker;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.ToString;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.openrewrite.rpc.RpcReceiveQueue.toEnum;

/**
 * Contains metadata about a Python project, parsed from pyproject.toml and uv.lock.
 * Attached as a marker to Toml.Document to provide dependency context for recipes.
 * <p>
 * The model separates requests (Dependency) from resolutions (ResolvedDependency):
 * - The dependency lists contain Dependency objects (what was requested in pyproject.toml)
 * - The resolvedDependencies list contains what was actually locked (from uv.lock)
 */
@Value
@With
public class PythonResolutionResult implements Marker, RpcCodec<PythonResolutionResult> {
    UUID id;

    @ToString.Include
    @Nullable String name;

    @ToString.Include
    @Nullable String version;

    @Nullable String description;

    @ToString.Include
    String path;

    @Nullable String requiresPython;

    @Nullable String buildBackend;

    List<Dependency> buildRequires;

    List<Dependency> dependencies;

    Map<String, List<Dependency>> optionalDependencies;

    List<ResolvedDependency> resolvedDependencies;

    @Nullable PackageManager packageManager;

    @Nullable List<SourceIndex> sourceIndexes;

    /**
     * Look up a resolved dependency by package name.
     *
     * @param packageName The name of the package to look up (case-insensitive, normalized)
     * @return The resolved dependency, or null if not found
     */
    public @Nullable ResolvedDependency getResolvedDependency(String packageName) {
        String normalized = normalizeName(packageName);
        return resolvedDependencies.stream()
                .filter(r -> normalizeName(r.getName()).equals(normalized))
                .findFirst()
                .orElse(null);
    }

    public static String normalizeName(String name) {
        return name.toLowerCase().replace('-', '_').replace('.', '_');
    }

    @Override
    public void rpcSend(PythonResolutionResult after, RpcSendQueue q) {
        q.getAndSend(after, PythonResolutionResult::getId);
        q.getAndSend(after, PythonResolutionResult::getName);
        q.getAndSend(after, PythonResolutionResult::getVersion);
        q.getAndSend(after, PythonResolutionResult::getDescription);
        q.getAndSend(after, PythonResolutionResult::getPath);
        q.getAndSend(after, PythonResolutionResult::getRequiresPython);
        q.getAndSend(after, PythonResolutionResult::getBuildBackend);
        q.getAndSendListAsRef(after, PythonResolutionResult::getBuildRequires,
                dep -> dep.getName() + "@" + dep.getVersionConstraint(),
                dep -> dep.rpcSend(dep, q));
        q.getAndSendListAsRef(after, PythonResolutionResult::getDependencies,
                dep -> dep.getName() + "@" + dep.getVersionConstraint(),
                dep -> dep.rpcSend(dep, q));
        q.getAndSend(after, PythonResolutionResult::getOptionalDependencies);
        q.getAndSendListAsRef(after, PythonResolutionResult::getResolvedDependencies,
                resolved -> resolved.getName() + "@" + resolved.getVersion(),
                resolved -> resolved.rpcSend(resolved, q));
        q.getAndSend(after, PythonResolutionResult::getPackageManager);
        q.getAndSendList(after, p -> p.getSourceIndexes() != null ? p.getSourceIndexes() : emptyList(),
                SourceIndex::getName,
                si -> si.rpcSend(si, q));
    }

    @Override
    public PythonResolutionResult rpcReceive(PythonResolutionResult before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.id, UUID::fromString))
                .withName(q.receive(before.name))
                .withVersion(q.receive(before.version))
                .withDescription(q.receive(before.description))
                .withPath(q.receive(before.path))
                .withRequiresPython(q.receive(before.requiresPython))
                .withBuildBackend(q.receive(before.buildBackend))
                .withBuildRequires(q.receiveList(before.buildRequires,
                        dep -> dep.rpcReceive(dep, q)))
                .withDependencies(q.receiveList(before.dependencies,
                        dep -> dep.rpcReceive(dep, q)))
                .withOptionalDependencies(q.receive(before.optionalDependencies))
                .withResolvedDependencies(q.receiveList(before.resolvedDependencies,
                        resolved -> resolved.rpcReceive(resolved, q)))
                .withPackageManager(q.receiveAndGet(before.packageManager, toEnum(PackageManager.class)))
                .withSourceIndexes(q.receiveList(before.sourceIndexes,
                        si -> si.rpcReceive(si, q)));
    }

    /**
     * Represents a dependency request as declared in pyproject.toml.
     * Parsed from PEP 508 dependency specification strings.
     */
    @Value
    @With
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    public static class Dependency implements RpcCodec<Dependency> {
        String name;
        @Nullable String versionConstraint;
        @Nullable List<String> extras;
        @Nullable String marker;

        @ToString.Exclude
        @Nullable ResolvedDependency resolved;

        @Override
        public void rpcSend(Dependency after, RpcSendQueue q) {
            q.getAndSend(after, Dependency::getName);
            q.getAndSend(after, Dependency::getVersionConstraint);
            q.getAndSend(after, Dependency::getExtras);
            q.getAndSend(after, Dependency::getMarker);
            q.getAndSend(after, Dependency::getResolved);
        }

        @Override
        public Dependency rpcReceive(Dependency before, RpcReceiveQueue q) {
            return before
                    .withName(q.receive(before.name))
                    .withVersionConstraint(q.receive(before.versionConstraint))
                    .withExtras(q.receive(before.extras))
                    .withMarker(q.receive(before.marker))
                    .withResolved(q.receive(before.resolved));
        }
    }

    /**
     * Represents a resolved dependency from uv.lock.
     */
    @Value
    @With
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    public static class ResolvedDependency implements RpcCodec<ResolvedDependency> {
        @ToString.Include
        String name;

        @ToString.Include
        String version;

        @Nullable String source;

        @Nullable List<Dependency> dependencies;

        @Override
        public void rpcSend(ResolvedDependency after, RpcSendQueue q) {
            q.getAndSend(after, ResolvedDependency::getName);
            q.getAndSend(after, ResolvedDependency::getVersion);
            q.getAndSend(after, ResolvedDependency::getSource);
            q.getAndSendListAsRef(after, r -> r.getDependencies() != null ? r.getDependencies() : emptyList(),
                    dep -> dep.getName() + "@" + dep.getVersionConstraint(),
                    dep -> dep.rpcSend(dep, q));
        }

        @Override
        public ResolvedDependency rpcReceive(ResolvedDependency before, RpcReceiveQueue q) {
            return before
                    .withName(q.receive(before.name))
                    .withVersion(q.receive(before.version))
                    .withSource(q.receive(before.source))
                    .withDependencies(q.receiveList(before.dependencies,
                            dep -> dep.rpcReceive(dep, q)));
        }
    }

    public enum PackageManager {
        Uv,
        Pip,
        Pipenv,
        Poetry,
        Pdm
    }

    @Value
    @With
    public static class SourceIndex implements RpcCodec<SourceIndex> {
        String name;
        String url;
        boolean defaultIndex;

        @Override
        public void rpcSend(SourceIndex after, RpcSendQueue q) {
            q.getAndSend(after, SourceIndex::getName);
            q.getAndSend(after, SourceIndex::getUrl);
            q.getAndSend(after, SourceIndex::isDefaultIndex);
        }

        @Override
        public SourceIndex rpcReceive(SourceIndex before, RpcReceiveQueue q) {
            return before
                    .withName(q.receive(before.name))
                    .withUrl(q.receive(before.url))
                    .withDefaultIndex(q.receive(before.defaultIndex));
        }
    }
}
