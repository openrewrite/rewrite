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

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

/**
 * Records the build constraints a {@link org.openrewrite.golang.tree.Go.CompilationUnit}'s file declares:
 * the combined {@code //go:build} / {@code // +build} expression and any {@code GOOS}/{@code GOARCH} its
 * filename suffix implies. Its presence marks a file as platform-specific; an unconstrained file carries no
 * marker. A file outside the parser's primary build context still carries this marker but is left
 * un-type-checked (see {@link PartialTypeAttribution}), so mutually exclusive files never collide as
 * redeclarations. Each field is empty when the file states none.
 */
@Value
@With
public class BuildConstraint implements Marker, RpcCodec<BuildConstraint> {
    UUID id;
    String constraint;
    String goos;
    String goarch;

    @Override
    public void rpcSend(BuildConstraint after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, BuildConstraint::getConstraint);
        q.getAndSend(after, BuildConstraint::getGoos);
        q.getAndSend(after, BuildConstraint::getGoarch);
    }

    @Override
    public BuildConstraint rpcReceive(BuildConstraint before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withConstraint(q.receive(before.getConstraint()))
                .withGoos(q.receive(before.getGoos()))
                .withGoarch(q.receive(before.getGoarch()));
    }
}
