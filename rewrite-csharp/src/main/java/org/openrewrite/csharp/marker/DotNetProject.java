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

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.List;
import java.util.UUID;

/**
 * Identifies the .NET project a source file belongs to.
 * Attached to every C# source file parsed from a project,
 * analogous to {@code JavaProject} for Java source files.
 */
@Value
@With
public class DotNetProject implements Marker, RpcCodec<DotNetProject> {
    UUID id;

    String projectName;

    /**
     * Target framework monikers this project targets
     * (e.g., "net8.0", "net9.0"). A multi-targeting project
     * will have multiple entries.
     */
    List<String> targetFrameworks;

    /**
     * The SDK attribute from the Project element
     * (e.g., "Microsoft.NET.Sdk", "Microsoft.NET.Sdk.Web").
     */
    @Nullable
    String sdk;

    @Override
    public void rpcSend(DotNetProject after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, DotNetProject::getProjectName);
        q.getAndSendList(after, DotNetProject::getTargetFrameworks, s -> s, s ->
                q.getAndSend(s, x -> x));
        q.getAndSend(after, DotNetProject::getSdk);
    }

    @Override
    public DotNetProject rpcReceive(DotNetProject before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.id, UUID::fromString))
                .withProjectName(q.receive(before.projectName))
                .withTargetFrameworks(q.receiveList(before.targetFrameworks, s ->
                        q.<String, String>receiveAndGet(s, x -> x)))
                .withSdk(q.receive(before.sdk));
    }
}
