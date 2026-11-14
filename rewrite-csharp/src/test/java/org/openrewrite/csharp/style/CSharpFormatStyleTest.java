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
package org.openrewrite.csharp.style;

import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.moderne.jsonrpc.JsonRpcSuccess;
import io.moderne.jsonrpc.RawJson;
import io.moderne.jsonrpc.formatter.JsonMessageFormatter;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.Tree;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.rpc.request.GetObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CSharpFormatStyleTest {

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/9417")
    void flagsThatFitInAnIntRoundTripThroughJson() throws IOException {
        // Clearing both wrapping bits (45, 46) leaves the Roslyn defaults below 2^31, so the
        // flags arrive from JSON as an Integer rather than a Long.
        long flags = CSharpFormatStyle.DEFAULT_FLAGS & ~(0b11L << 45);
        assertThat(flags).isLessThanOrEqualTo(Integer.MAX_VALUE);
        CSharpFormatStyle style = new CSharpFormatStyle(Tree.randomId(), flags, 4, 4, "\n", 1, 0);

        List<RpcObjectData> sent = new ArrayList<>();
        RpcSendQueue sq = new RpcSendQueue(1, sent::addAll, new IdentityHashMap<>(), null, false);
        sq.send(style, null, null);
        sq.flush();

        GetObjectResponse decoded = throughJson(sent);
        RpcReceiveQueue rq = new RpcReceiveQueue(new HashMap<>(), () -> decoded, null, null);
        CSharpFormatStyle received = rq.receive(null);

        assertThat(received).isEqualTo(style);
        assertThat(received.isWrappingPreserveSingleLine()).isFalse();
    }

    private static GetObjectResponse throughJson(List<RpcObjectData> batch) throws IOException {
        JsonMessageFormatter formatter = new JsonMessageFormatter(new ParameterNamesModule());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        formatter.serialize(new JsonRpcSuccess(1, RawJson.of(batch)), out);
        return ((JsonRpcSuccess) formatter.deserialize(new ByteArrayInputStream(out.toByteArray())))
          .getResult(GetObjectResponse.class);
    }
}
