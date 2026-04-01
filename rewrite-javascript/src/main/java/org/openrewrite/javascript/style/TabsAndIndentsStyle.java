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
public class TabsAndIndentsStyle implements JavaScriptStyle, RpcCodec<TabsAndIndentsStyle> {
    Boolean useTabCharacter;
    Integer tabSize;
    Integer indentSize;
    Integer continuationIndent;
    Boolean keepIndentsOnEmptyLines;
    Boolean indentChainedMethods;
    Boolean indentAllChainedCallsInAGroup;

    @Override
    public void rpcSend(TabsAndIndentsStyle after, RpcSendQueue q) {
        q.getAndSend(after, TabsAndIndentsStyle::getUseTabCharacter);
        q.getAndSend(after, TabsAndIndentsStyle::getTabSize);
        q.getAndSend(after, TabsAndIndentsStyle::getIndentSize);
        q.getAndSend(after, TabsAndIndentsStyle::getContinuationIndent);
        q.getAndSend(after, TabsAndIndentsStyle::getKeepIndentsOnEmptyLines);
        q.getAndSend(after, TabsAndIndentsStyle::getIndentChainedMethods);
        q.getAndSend(after, TabsAndIndentsStyle::getIndentAllChainedCallsInAGroup);
    }

    @Override
    public TabsAndIndentsStyle rpcReceive(TabsAndIndentsStyle before, RpcReceiveQueue q) {
        return before
                .withUseTabCharacter(q.receive(before.getUseTabCharacter()))
                .withTabSize(q.receive(before.getTabSize()))
                .withIndentSize(q.receive(before.getIndentSize()))
                .withContinuationIndent(q.receive(before.getContinuationIndent()))
                .withKeepIndentsOnEmptyLines(q.receive(before.getKeepIndentsOnEmptyLines()))
                .withIndentChainedMethods(q.receive(before.getIndentChainedMethods()))
                .withIndentAllChainedCallsInAGroup(q.receive(before.getIndentAllChainedCallsInAGroup()));
    }
}
