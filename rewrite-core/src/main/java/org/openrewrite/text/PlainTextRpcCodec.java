/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.text;

import org.openrewrite.Tree;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class PlainTextRpcCodec extends DynamicDispatchRpcCodec<PlainText> {
    @Override
    public String getSourceFileType() {
        return "org.openrewrite.text.PlainText";
    }

    @Override
    public Class<? extends PlainText> getType() {
        return PlainText.class;
    }

    @Override
    public void rpcSend(PlainText after, RpcSendQueue q) {
        q.getAndSend(after, Tree::getId);
        q.getAndSend(after, Tree::getMarkers);
        q.getAndSend(after, (PlainText d) -> d.getSourcePath().toString());
        q.getAndSend(after, (PlainText d) -> d.getCharset().name());
        q.getAndSend(after, PlainText::isCharsetBomMarked);
        q.getAndSend(after, PlainText::getChecksum);
        q.getAndSend(after, PlainText::getFileAttributes);
        q.getAndSend(after, PlainText::getText);
        q.getAndSendList(after, PlainText::getSnippets, Tree::getId, snippet -> {
            q.getAndSend(snippet, Tree::getId);
            q.getAndSend(snippet, Tree::getMarkers);
            q.getAndSend(snippet, PlainText.Snippet::getText);
        });
    }

    @Override
    public PlainText rpcReceive(PlainText t, RpcReceiveQueue q) {
        PlainText p = t.withId(q.receiveAndGet(t.getId(), UUID::fromString))
                .withMarkers(q.receive(t.getMarkers()))
                .withSourcePath(q.<Path, String>receiveAndGet(t.getSourcePath(), Paths::get))
                .withCharsetName(q.receiveAndGet(t.getCharsetName(), String::toString))
                .withCharsetBomMarked(q.receive(t.isCharsetBomMarked()))
                .withChecksum(q.receive(t.getChecksum()))
                .withFileAttributes(q.receive(t.getFileAttributes()))
                .withText(q.receive(t.getText()));
        return p.withSnippets(q.receiveList(t.getSnippets(), s -> s
                    .withId(q.receiveAndGet(s.getId(), UUID::fromString))
                    .withMarkers(q.receive(s.getMarkers()))
                    .withText(q.receive(s.getText()))));
    }
}
