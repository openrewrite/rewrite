/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.csharp.marker;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.openrewrite.Tree.randomId;

/**
 * Metadata about a .NET project (.csproj) extracted from MSBuild evaluation.
 * Attached as a marker to the Xml.Document representing the .csproj file in the LST.
 * Analogous to MavenResolutionResult for Maven and GradleProject for Gradle.
 */
@Value
@Builder
@AllArgsConstructor
public class MSBuildProject implements Marker, Serializable, RpcCodec<MSBuildProject> {

    @With
    @Builder.Default
    UUID id = randomId();

    /**
     * The SDK attribute from the Project element (e.g., "Microsoft.NET.Sdk", "Microsoft.NET.Sdk.Web").
     */
    @With
    @Nullable
    String sdk;

    /**
     * MSBuild properties with provenance tracking.
     * The key is the property name, the value includes the resolved value
     * and the file where the property is defined.
     */
    @With
    @Builder.Default
    Map<String, PropertyValue> properties = emptyMap();

    /**
     * NuGet package sources configured for this project, extracted from nuget.config.
     * Used by version upgrade recipes to resolve available package versions.
     * Analogous to MavenRepository list on MavenResolutionResult/GradleProject.
     */
    @With
    @Builder.Default
    List<PackageSource> packageSources = emptyList();

    /**
     * Per-target-framework metadata. Each TFM has its own set of
     * package references, resolved packages, and project references,
     * since MSBuild evaluates the project independently per TFM.
     */
    @With
    @Builder.Default
    List<TargetFramework> targetFrameworks = emptyList();

    @Override
    public void rpcSend(MSBuildProject after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, MSBuildProject::getSdk);
        // Send map as parallel lists: keys then values (each value through its codec)
        q.getAndSendList(after, m -> new ArrayList<>(m.getProperties().keySet()),
                k -> k, k -> q.getAndSend(k, x -> x));
        q.getAndSendList(after, m -> new ArrayList<>(m.getProperties().values()),
                v -> v.getValue(), v -> v.rpcSend(v, q));
        q.getAndSendList(after, MSBuildProject::getPackageSources,
                PackageSource::getKey,
                ps -> ps.rpcSend(ps, q));
        q.getAndSendList(after, MSBuildProject::getTargetFrameworks,
                TargetFramework::getTargetFramework,
                tf -> tf.rpcSend(tf, q));
    }

    @Override
    public MSBuildProject rpcReceive(MSBuildProject before, RpcReceiveQueue q) {
        UUID id = q.receiveAndGet(before.id, UUID::fromString);
        String sdk = q.receive(before.sdk);
        // Receive parallel lists and zip into map
        Map<String, PropertyValue> beforeProps = before.properties != null ? before.properties : emptyMap();
        List<String> keys = q.receiveList(new ArrayList<>(beforeProps.keySet()),
                k -> q.<String, String>receiveAndGet(k, x -> x));
        List<PropertyValue> values = q.receiveList(new ArrayList<>(beforeProps.values()),
                v -> v.rpcReceive(v, q));
        Map<String, PropertyValue> props = new LinkedHashMap<>();
        if (keys != null && values != null) {
            for (int i = 0; i < keys.size(); i++) {
                props.put(keys.get(i), values.get(i));
            }
        }
        return before
                .withId(id)
                .withSdk(sdk)
                .withProperties(props)
                .withPackageSources(q.receiveList(before.packageSources,
                        ps -> ps.rpcReceive(ps, q)))
                .withTargetFrameworks(q.receiveList(before.targetFrameworks,
                        tf -> tf.rpcReceive(tf, q)));
    }

    /**
     * Metadata for a single target framework within this project.
     */
    @Value
    @With
    @Builder
    @AllArgsConstructor
    public static class TargetFramework implements Serializable, RpcCodec<TargetFramework> {
        String targetFramework;

        @Builder.Default
        List<PackageReference> packageReferences = emptyList();

        @Builder.Default
        List<ResolvedPackage> resolvedPackages = emptyList();

        @Builder.Default
        List<ProjectReference> projectReferences = emptyList();

        @Override
        public void rpcSend(TargetFramework after, RpcSendQueue q) {
            q.getAndSend(after, TargetFramework::getTargetFramework);
            q.getAndSendList(after, TargetFramework::getPackageReferences,
                    PackageReference::getInclude,
                    pr -> pr.rpcSend(pr, q));
            q.getAndSendListAsRef(after, TargetFramework::getResolvedPackages,
                    rp -> rp.getName() + "@" + rp.getResolvedVersion(),
                    rp -> rp.rpcSend(rp, q));
            q.getAndSendList(after, TargetFramework::getProjectReferences,
                    ProjectReference::getInclude,
                    pr -> pr.rpcSend(pr, q));
        }

        @Override
        public TargetFramework rpcReceive(TargetFramework before, RpcReceiveQueue q) {
            return before
                    .withTargetFramework(q.receive(before.targetFramework))
                    .withPackageReferences(q.receiveList(before.packageReferences,
                            pr -> pr.rpcReceive(pr, q)))
                    .withResolvedPackages(q.receiveList(before.resolvedPackages,
                            rp -> rp.rpcReceive(rp, q)))
                    .withProjectReferences(q.receiveList(before.projectReferences,
                            pr -> pr.rpcReceive(pr, q)));
        }
    }

    /**
     * A declared NuGet package reference from the .csproj or Directory.Packages.props.
     * Tracks both the raw (requested) and MSBuild-evaluated (resolved) version,
     * enabling recipes to decide whether to update a property or a literal.
     */
    @Value
    @With
    @Builder
    @AllArgsConstructor
    public static class PackageReference implements Serializable, RpcCodec<PackageReference> {
        String include;

        @Nullable
        String requestedVersion;

        @Nullable
        String resolvedVersion;

        @Override
        public void rpcSend(PackageReference after, RpcSendQueue q) {
            q.getAndSend(after, PackageReference::getInclude);
            q.getAndSend(after, PackageReference::getRequestedVersion);
            q.getAndSend(after, PackageReference::getResolvedVersion);
        }

        @Override
        public PackageReference rpcReceive(PackageReference before, RpcReceiveQueue q) {
            return before
                    .withInclude(q.receive(before.include))
                    .withRequestedVersion(q.receive(before.requestedVersion))
                    .withResolvedVersion(q.receive(before.resolvedVersion));
        }
    }

    /**
     * A resolved NuGet package from the transitive dependency tree.
     * Built from project.assets.json after dotnet restore.
     */
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
    @Value
    @With
    @Builder
    @AllArgsConstructor
    public static class ResolvedPackage implements Serializable, RpcCodec<ResolvedPackage> {
        String name;

        String resolvedVersion;

        @Builder.Default
        List<ResolvedPackage> dependencies = emptyList();

        int depth;

        @Override
        public void rpcSend(ResolvedPackage after, RpcSendQueue q) {
            q.getAndSend(after, ResolvedPackage::getName);
            q.getAndSend(after, ResolvedPackage::getResolvedVersion);
            q.getAndSendListAsRef(after, ResolvedPackage::getDependencies,
                    dep -> dep.getName() + "@" + dep.getResolvedVersion(),
                    dep -> dep.rpcSend(dep, q));
            q.getAndSend(after, ResolvedPackage::getDepth);
        }

        @Override
        public ResolvedPackage rpcReceive(ResolvedPackage before, RpcReceiveQueue q) {
            return before
                    .withName(q.receive(before.name))
                    .withResolvedVersion(q.receive(before.resolvedVersion))
                    .withDependencies(q.receiveList(before.dependencies,
                            dep -> dep.rpcReceive(dep, q)))
                    .withDepth(q.receive(before.depth));
        }
    }

    /**
     * A project-to-project reference within a solution.
     */
    @Value
    @With
    @Builder
    @AllArgsConstructor
    public static class ProjectReference implements Serializable, RpcCodec<ProjectReference> {
        String include;

        @Override
        public void rpcSend(ProjectReference after, RpcSendQueue q) {
            q.getAndSend(after, ProjectReference::getInclude);
        }

        @Override
        public ProjectReference rpcReceive(ProjectReference before, RpcReceiveQueue q) {
            return before.withInclude(q.receive(before.include));
        }
    }

    /**
     * An MSBuild property value with provenance tracking.
     * Recipes use the definedIn path to determine which file to edit
     * when updating a property value.
     */
    @Value
    @With
    @Builder
    @AllArgsConstructor
    public static class PropertyValue implements Serializable, RpcCodec<PropertyValue> {
        String value;

        @Nullable
        Path definedIn;

        @Override
        public void rpcSend(PropertyValue after, RpcSendQueue q) {
            q.getAndSend(after, PropertyValue::getValue);
            q.getAndSend(after, p -> {
                Path path = p.getDefinedIn();
                return path == null ? null : path.toString();
            });
        }

        @Override
        public PropertyValue rpcReceive(PropertyValue before, RpcReceiveQueue q) {
            return before
                    .withValue(q.receive(before.value))
                    .withDefinedIn(q.<Path, String>receiveAndGet(
                            before.definedIn, s -> s == null ? null : Paths.get(s)));
        }
    }

    /**
     * A NuGet package source from nuget.config.
     * Analogous to MavenRepository in the Maven/Gradle ecosystems.
     */
    @Value
    @With
    @Builder
    @AllArgsConstructor
    public static class PackageSource implements Serializable, RpcCodec<PackageSource> {
        /**
         * The key/name of the package source (e.g., "nuget.org", "mycompany-feed").
         */
        String key;

        /**
         * The NuGet V3 service index URL
         * (e.g., "https://api.nuget.org/v3/index.json").
         */
        String url;

        @Override
        public void rpcSend(PackageSource after, RpcSendQueue q) {
            q.getAndSend(after, PackageSource::getKey);
            q.getAndSend(after, PackageSource::getUrl);
        }

        @Override
        public PackageSource rpcReceive(PackageSource before, RpcReceiveQueue q) {
            return before
                    .withKey(q.receive(before.key))
                    .withUrl(q.receive(before.url));
        }
    }
}
