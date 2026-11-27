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
package org.openrewrite.csharp.marker;

import org.junit.jupiter.api.Test;
import org.openrewrite.csharp.marker.MSBuildProject.ResolvedPackage;
import org.openrewrite.csharp.marker.MSBuildProject.TargetFramework;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class MSBuildProjectRpcTest {

    @Test
    void roundTripSourceRepository() {
        // No source repository on the first package and one on the second, so a field that the
        // sender and the receiver do not agree on shifts everything read after it.
        ResolvedPackage legacy = ResolvedPackage.builder()
                .name("Legacy.Pkg")
                .resolvedVersion("2.0.0")
                .projectUrl("https://example.org/legacy")
                .build();
        ResolvedPackage newtonsoft = ResolvedPackage.builder()
                .name("Newtonsoft.Json")
                .resolvedVersion("13.0.3")
                .depth(1)
                .repositoryUrl("https://github.com/JamesNK/Newtonsoft.Json")
                .repositoryType("git")
                .repositoryCommit("0a2e291c0d9c0c7675d445703e51750363a549ef")
                .projectUrl("https://www.newtonsoft.com/json")
                .build();
        MSBuildProject marker = MSBuildProject.builder()
                .sdk("Microsoft.NET.Sdk")
                .targetFrameworks(singletonList(TargetFramework.builder()
                        .targetFramework("net8.0")
                        .resolvedPackages(asList(legacy, newtonsoft))
                        .build()))
                .build();

        MSBuildProject received = sendAndReceive(marker);

        List<ResolvedPackage> packages = received.getTargetFrameworks().get(0).getResolvedPackages();
        assertThat(packages).extracting(ResolvedPackage::getName).containsExactly("Legacy.Pkg", "Newtonsoft.Json");
        assertThat(packages.get(0))
                .extracting(ResolvedPackage::getRepositoryUrl, ResolvedPackage::getRepositoryType,
                        ResolvedPackage::getRepositoryCommit, ResolvedPackage::getProjectUrl)
                .containsExactly(null, null, null, "https://example.org/legacy");
        assertThat(packages.get(1))
                .extracting(ResolvedPackage::getRepositoryUrl, ResolvedPackage::getRepositoryType,
                        ResolvedPackage::getRepositoryCommit, ResolvedPackage::getProjectUrl)
                .containsExactly("https://github.com/JamesNK/Newtonsoft.Json", "git",
                        "0a2e291c0d9c0c7675d445703e51750363a549ef", "https://www.newtonsoft.com/json");
        assertThat(packages.get(1).getDepth()).isEqualTo(1);
    }

    private static MSBuildProject sendAndReceive(MSBuildProject marker) {
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue sq = new RpcSendQueue(1_000_000, batches::addLast, new IdentityHashMap<>(), null, false);
        sq.send(marker, null, null);
        sq.flush();

        // The wire carries a UUID as a string, as the transport's JSON encoding would.
        List<RpcObjectData> all = new ArrayList<>();
        while (!batches.isEmpty()) {
            for (RpcObjectData data : batches.removeFirst()) {
                all.add(data.getValue() instanceof UUID ?
                        new RpcObjectData(data.getState(), data.getValueType(), data.getValue().toString(), data.getRef(), false) :
                        data);
            }
        }
        Deque<List<RpcObjectData>> drain = new ArrayDeque<>();
        drain.add(all);

        return new RpcReceiveQueue(new HashMap<>(), drain::removeFirst, null, null).receive(null);
    }
}
