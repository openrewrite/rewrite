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
import java.util.List;

@Value
@With
public class CSharpLanguageVersion implements Marker, RpcCodec<CSharpLanguageVersion> {
    UUID id;
    String version;
    List<TargetFrameworkVersion> targetFrameworkVersions;

    @Override
    public void rpcSend(CSharpLanguageVersion after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, CSharpLanguageVersion::getVersion);
        q.getAndSendList(after, CSharpLanguageVersion::getTargetFrameworkVersions, TargetFrameworkVersion::getTargetFramework, null);
    }

    @Override
    public CSharpLanguageVersion rpcReceive(CSharpLanguageVersion before, RpcReceiveQueue q) {
        return before.withId(q.receiveAndGet(before.id, UUID::fromString))
                .withVersion(q.receive(before.version))
                .withTargetFrameworkVersions(q.receiveList(before.targetFrameworkVersions, null));
    }
    @Value
    public static class TargetFrameworkVersion implements RpcCodec<TargetFrameworkVersion> {
        String targetFramework;
        String version;

        @Override
        public void rpcSend(TargetFrameworkVersion after, RpcSendQueue q) {
            q.getAndSend(after, TargetFrameworkVersion::getTargetFramework, null);
            q.getAndSend(after, TargetFrameworkVersion::getVersion);
        }

        @Override
        public TargetFrameworkVersion rpcReceive(TargetFrameworkVersion before, RpcReceiveQueue q) {
            return new TargetFrameworkVersion(q.receive(before.targetFramework), q.receive(before.version));
        }
    }

}
