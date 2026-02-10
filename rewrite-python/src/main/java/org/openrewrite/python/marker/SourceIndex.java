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

import lombok.Value;
import lombok.With;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

@Value
@With
public class SourceIndex implements RpcCodec<SourceIndex> {
    String name;
    String url;
    boolean defaultIndex;

    @Override
    public void rpcSend(SourceIndex after, RpcSendQueue q) {
        q.getAndSend(after, SourceIndex::getName);
        q.getAndSend(after, SourceIndex::getUrl);
        q.getAndSend(after, SourceIndex::isDefaultIndex);
    }

    @Override
    public SourceIndex rpcReceive(SourceIndex before, RpcReceiveQueue q) {
        return before
                .withName(q.receive(before.name))
                .withUrl(q.receive(before.url))
                .withDefaultIndex(q.receive(before.defaultIndex));
    }
}
