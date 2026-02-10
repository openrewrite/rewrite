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
 * The model separates requests ({@link Dependency}) from resolutions ({@link ResolvedDependency}):
 * <ul>
 *   <li>The dependency lists contain {@link Dependency} objects (what was requested in pyproject.toml)</li>
 *   <li>The resolvedDependencies list contains what was actually locked (from uv.lock)</li>
 * </ul>
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

    /**
     * SPDX license expression from [project].license (PEP 639),
     * or the text value from the deprecated license = {text = "..."} form.
     */
    @Nullable String license;

    @ToString.Include
    String path;

    @Nullable String requiresPython;

    @Nullable String buildBackend;

    List<Dependency> buildRequires;

    List<Dependency> dependencies;

    /**
     * Published extras from [project.optional-dependencies].
     * Keys are extra names (e.g. "security"), values are PEP 508 dependency lists.
     */
    Map<String, List<Dependency>> optionalDependencies;

    /**
     * Unpublished dependency groups from [dependency-groups] (PEP 735).
     * Keys are group names (e.g. "dev", "test"), values are PEP 508 dependency lists.
     * Note: {include-group = "..."} entries are not represented here.
     */
    Map<String, List<Dependency>> dependencyGroups;

    List<ResolvedDependency> resolvedDependencies;

    @Nullable PackageManager packageManager;

    @Nullable List<SourceIndex> sourceIndexes;

    /**
     * Look up a resolved dependency by package name.
     *
     * @param packageName The name of the package to look up (case-insensitive, normalized per PEP 503)
     * @return The resolved dependency, or null if not found
     */
    public @Nullable ResolvedDependency getResolvedDependency(String packageName) {
        String normalized = normalizeName(packageName);
        return resolvedDependencies.stream()
                .filter(r -> normalizeName(r.getName()).equals(normalized))
                .findFirst()
                .orElse(null);
    }

    /**
     * Normalize a Python package name per PEP 503: lowercase, dashes/dots/underscores are equivalent.
     */
    public static String normalizeName(String name) {
        return name.toLowerCase().replace('-', '_').replace('.', '_');
    }

    @Override
    public void rpcSend(PythonResolutionResult after, RpcSendQueue q) {
        q.getAndSend(after, PythonResolutionResult::getId);
        q.getAndSend(after, PythonResolutionResult::getName);
        q.getAndSend(after, PythonResolutionResult::getVersion);
        q.getAndSend(after, PythonResolutionResult::getDescription);
        q.getAndSend(after, PythonResolutionResult::getLicense);
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
        q.getAndSend(after, PythonResolutionResult::getDependencyGroups);
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
                .withLicense(q.receive(before.license))
                .withPath(q.receive(before.path))
                .withRequiresPython(q.receive(before.requiresPython))
                .withBuildBackend(q.receive(before.buildBackend))
                .withBuildRequires(q.receiveList(before.buildRequires,
                        dep -> dep.rpcReceive(dep, q)))
                .withDependencies(q.receiveList(before.dependencies,
                        dep -> dep.rpcReceive(dep, q)))
                .withOptionalDependencies(q.receive(before.optionalDependencies))
                .withDependencyGroups(q.receive(before.dependencyGroups))
                .withResolvedDependencies(q.receiveList(before.resolvedDependencies,
                        resolved -> resolved.rpcReceive(resolved, q)))
                .withPackageManager(q.receiveAndGet(before.packageManager, toEnum(PackageManager.class)))
                .withSourceIndexes(q.receiveList(before.sourceIndexes,
                        si -> si.rpcReceive(si, q)));
    }
}
