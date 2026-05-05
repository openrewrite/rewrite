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
package org.openrewrite.golang.marker;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

/**
 * Identifies the Go project a source file belongs to. Mirrors
 * {@link org.openrewrite.java.marker.JavaProject}.
 * <p>
 * Recipes that need module-level dependency information look up the
 * sibling {@code go.mod} source by path and read its
 * {@link GoResolutionResult} marker — the same connector-by-path pattern
 * that Maven uses for {@code pom.xml} beside {@code src/main/java}.
 */
@Value
@With
public class GoProject implements Marker, RpcCodec<GoProject> {
    @EqualsAndHashCode.Exclude
    UUID id;

    String projectName;

    @Override
    public void rpcSend(GoProject after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, GoProject::getProjectName);
    }

    @Override
    public GoProject rpcReceive(GoProject before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withProjectName(q.receive(before.getProjectName()));
    }
}
