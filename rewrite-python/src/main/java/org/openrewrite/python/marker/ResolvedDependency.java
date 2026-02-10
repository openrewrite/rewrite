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
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * A resolved (locked) dependency from uv.lock.
 * <p>
 * Python resolution is flat: each package name appears exactly once with one version.
 * The {@code dependencies} list links directly to other {@code ResolvedDependency}
 * instances (self-referential, like Maven's model), enabling graph traversal.
 */
@Value
@With
@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public class ResolvedDependency implements RpcCodec<ResolvedDependency> {
    @ToString.Include
    String name;

    @ToString.Include
    String version;

    @Nullable String source;

    /**
     * Direct dependencies of this resolved package. Each entry is a reference
     * to another {@code ResolvedDependency} in the flat resolution list.
     * Null when the package has no dependencies in the lock file.
     */
    @Nullable List<ResolvedDependency> dependencies;

    @Override
    public void rpcSend(ResolvedDependency after, RpcSendQueue q) {
        q.getAndSend(after, ResolvedDependency::getName);
        q.getAndSend(after, ResolvedDependency::getVersion);
        q.getAndSend(after, ResolvedDependency::getSource);
        q.getAndSendListAsRef(after, r -> r.getDependencies() != null ? r.getDependencies() : emptyList(),
                dep -> dep.getName() + "@" + dep.getVersion(),
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
