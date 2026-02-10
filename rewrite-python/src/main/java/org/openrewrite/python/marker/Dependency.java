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
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.List;

/**
 * A dependency specification parsed from a PEP 508 string in pyproject.toml.
 * Used for declared dependencies ({@code dependencies}, {@code buildRequires},
 * {@code optionalDependencies}, {@code dependencyGroups}).
 * <p>
 * When a lock file is available, the {@code resolved} field links to the
 * corresponding {@link ResolvedDependency} entry.
 */
@Value
@With
public class Dependency implements RpcCodec<Dependency> {
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
