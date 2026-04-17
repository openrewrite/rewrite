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
package org.openrewrite.javascript.style;

import lombok.Value;
import lombok.With;
import org.openrewrite.javascript.JavaScriptStyle;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

@Value
@With
public class WrappingAndBracesStyle implements JavaScriptStyle, RpcCodec<WrappingAndBracesStyle> {

    IfStatement ifStatement;
    KeepWhenReformatting keepWhenReformatting;

    @Override
    public void rpcSend(WrappingAndBracesStyle after, RpcSendQueue q) {
        q.getAndSend(after, WrappingAndBracesStyle::getIfStatement);
        q.getAndSend(after, WrappingAndBracesStyle::getKeepWhenReformatting);
    }

    @Override
    public WrappingAndBracesStyle rpcReceive(WrappingAndBracesStyle before, RpcReceiveQueue q) {
        return before
                .withIfStatement(q.receive(before.getIfStatement()))
                .withKeepWhenReformatting(q.receive(before.getKeepWhenReformatting()));
    }

    @Value
    @With
    public static class IfStatement implements RpcCodec<IfStatement> {
        Boolean elseOnNewLine;

        @Override
        public void rpcSend(IfStatement after, RpcSendQueue q) {
            q.getAndSend(after, IfStatement::getElseOnNewLine);
        }

        @Override
        public IfStatement rpcReceive(IfStatement before, RpcReceiveQueue q) {
            return before.withElseOnNewLine(q.receive(before.getElseOnNewLine()));
        }
    }

    @Value
    @With
    public static class KeepWhenReformatting implements RpcCodec<KeepWhenReformatting> {
        Boolean simpleBlocksInOneLine;
        Boolean simpleMethodsInOneLine;

        @Override
        public void rpcSend(KeepWhenReformatting after, RpcSendQueue q) {
            q.getAndSend(after, KeepWhenReformatting::getSimpleBlocksInOneLine);
            q.getAndSend(after, KeepWhenReformatting::getSimpleMethodsInOneLine);
        }

        @Override
        public KeepWhenReformatting rpcReceive(KeepWhenReformatting before, RpcReceiveQueue q) {
            return before
                    .withSimpleBlocksInOneLine(q.receive(before.getSimpleBlocksInOneLine()))
                    .withSimpleMethodsInOneLine(q.receive(before.getSimpleMethodsInOneLine()));
        }
    }
}
