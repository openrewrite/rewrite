/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.Map;
import java.util.UUID;

/**
 * Marker containing the resolved Prettier configuration for a source file.
 * <p>
 * This marker is added by the parser when a Prettier config is detected in the project.
 * The config is resolved per-file (with overrides applied), so files with different
 * override rules will have different marker instances.
 * <p>
 * When this marker is present, AutoformatVisitor will use Prettier for formatting
 * instead of the built-in formatting visitors.
 */
@Value
@With
public class PrettierStyle implements Marker, RpcCodec<PrettierStyle> {
    UUID id;

    /**
     * The resolved Prettier options for this file (with overrides applied).
     */
    Map<String, Object> config;

    /**
     * The Prettier version from the project's package.json.
     * At formatting time, this version of Prettier will be loaded dynamically
     * (similar to npx) to ensure consistent formatting.
     */
    String prettierVersion;

    @Override
    public void rpcSend(PrettierStyle after, RpcSendQueue q) {
        q.getAndSend(after, Marker::getId);
        q.getAndSend(after, PrettierStyle::getConfig);
        q.getAndSend(after, PrettierStyle::getPrettierVersion);
    }

    @SuppressWarnings("unchecked")
    @Override
    public PrettierStyle rpcReceive(PrettierStyle before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.getId(), UUID::fromString))
                .withConfig(q.receive(before.getConfig()))
                .withPrettierVersion(q.receive(before.getPrettierVersion()));
    }
}
