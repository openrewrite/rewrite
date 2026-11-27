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
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.UUID;

/**
 * The {@code generated_code} option that .editorconfig sets for a C# source file.
 * Absent where no option is set.
 */
@Value
@With
public class GeneratedCodeOption implements Marker, RpcCodec<GeneratedCodeOption> {
    UUID id;
    boolean generated;

    @Override
    public void rpcSend(GeneratedCodeOption after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, GeneratedCodeOption::isGenerated);
    }

    @Override
    public GeneratedCodeOption rpcReceive(GeneratedCodeOption before, RpcReceiveQueue q) {
        return before.withId(q.receiveAndGet(before.id, UUID::fromString))
                .withGenerated(q.receive(before.generated));
    }
}
