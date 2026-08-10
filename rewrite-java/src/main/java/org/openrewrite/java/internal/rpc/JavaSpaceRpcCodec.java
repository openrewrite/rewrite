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
package org.openrewrite.java.internal.rpc;

import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.rpc.DynamicDispatchRpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

public class JavaSpaceRpcCodec extends DynamicDispatchRpcCodec<Space> {

    @Override
    public String getSourceFileType() {
        return J.CompilationUnit.class.getName();
    }

    @Override
    public Class<? extends Space> getType() {
        return Space.class;
    }

    @Override
    public void rpcSend(Space after, RpcSendQueue q) {
        sendSpace(after, q);
    }

    @Override
    public Space rpcReceive(Space before, RpcReceiveQueue q) {
        return receiveSpace(before, q);
    }

    /**
     * Serializes a {@link Space} (its comments and whitespace) onto the send queue. Shared by the Java
     * visitor and by markers that carry a {@code Space}, so none of them need to allocate a {@code JavaSender}
     * just to reach the queue.
     */
    public static void sendSpace(Space space, RpcSendQueue q) {
        q.getAndSendList(space, Space::getComments,
                c -> {
                    if (c instanceof TextComment) {
                        return ((TextComment) c).getText() + c.getSuffix();
                    } else if (c instanceof Javadoc.DocComment) {
                        return ((Javadoc.DocComment) c).getId();
                    }
                    throw new IllegalArgumentException("Unexpected comment type " + c.getClass().getName());
                },
                c -> {
                    if (c instanceof TextComment) {
                        TextComment tc = (TextComment) c;
                        q.getAndSend(tc, TextComment::isMultiline);
                        q.getAndSend(tc, TextComment::getText);
                    } else {
                        throw new IllegalArgumentException("Unexpected comment type " + c.getClass().getName());
                    }
                    q.getAndSend(c, Comment::getSuffix);
                    q.getAndSend(c, Comment::getMarkers);
                });
        q.getAndSend(space, Space::getWhitespace);
    }

    /**
     * Reconstructs a {@link Space} from the receive queue, the inverse of {@link #sendSpace}.
     */
    public static Space receiveSpace(Space before, RpcReceiveQueue q) {
        return before
                .withComments(q.receiveList(before.getComments(), c -> {
                    if (c instanceof TextComment) {
                        return ((TextComment) c).withMultiline(q.receive(c.isMultiline()))
                                .withText(q.receive(((TextComment) c).getText()))
                                .withSuffix(q.receive(c.getSuffix()))
                                .withMarkers(q.receive(c.getMarkers()));
                    }
                    return c;
                }))
                .withWhitespace(q.receive(before.getWhitespace()));
    }
}
