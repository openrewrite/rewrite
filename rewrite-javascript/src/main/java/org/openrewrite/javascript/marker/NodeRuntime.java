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
package org.openrewrite.javascript.marker;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

import static org.openrewrite.rpc.RpcReceiveQueue.toEnum;

/**
 * The machine and the package manager release that built a Node.js project's LST, as {@code JavaVersion} records the
 * JDK that built a Java one. The lock regeneration installs the project as that release, on that Node and platform.
 * <p>
 * The package.json parser attaches one to every package.json document it produces, whether or not the document gets a
 * {@link NodeResolutionResult}: one instance per project root, which its workspace members share.
 */
@Value
@With
public class NodeRuntime implements Marker, RpcCodec<NodeRuntime> {
    UUID id;

    /**
     * {@code process.version} of the parser, such as {@code v24.19.0}.
     */
    String nodeVersion;

    /**
     * {@code process.platform} of the parser, such as {@code linux} or {@code darwin}.
     */
    String os;

    /**
     * {@code process.arch} of the parser, such as {@code x64} or {@code arm64}.
     */
    String cpu;

    /**
     * {@code glibc} or {@code musl} on Linux, as npm-install-checks 8.0.0's {@code lib/current-env.js} detects it;
     * null on other platforms and where it cannot be told.
     */
    @Nullable
    String libc;

    /**
     * The package manager the parser found for the project by its lock file, as {@link NodeResolutionResult} names
     * it; null when it found none.
     */
    @Nullable
    PackageManager packageManager;

    /**
     * What that package manager's {@code --version} answered when run in the project's root directory, the release
     * that installs the project on the parser's machine; null when it is not installed, fails, or answers something
     * that is no version.
     */
    @Nullable
    String packageManagerVersion;

    @Override
    public void rpcSend(NodeRuntime after, RpcSendQueue q) {
        q.getAndSend(after, NodeRuntime::getId);
        q.getAndSend(after, NodeRuntime::getNodeVersion);
        q.getAndSend(after, NodeRuntime::getOs);
        q.getAndSend(after, NodeRuntime::getCpu);
        q.getAndSend(after, NodeRuntime::getLibc);
        q.getAndSend(after, NodeRuntime::getPackageManager);
        q.getAndSend(after, NodeRuntime::getPackageManagerVersion);
    }

    @Override
    public NodeRuntime rpcReceive(NodeRuntime before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.id, UUID::fromString))
                .withNodeVersion(q.receive(before.nodeVersion))
                .withOs(q.receive(before.os))
                .withCpu(q.receive(before.cpu))
                .withLibc(q.receive(before.libc))
                .withPackageManager(q.receiveAndGet(before.packageManager, toEnum(PackageManager.class)))
                .withPackageManagerVersion(q.receive(before.packageManagerVersion));
    }
}
