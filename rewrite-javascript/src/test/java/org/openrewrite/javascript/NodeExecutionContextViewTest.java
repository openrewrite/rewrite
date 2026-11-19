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
package org.openrewrite.javascript;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.javascript.internal.registry.NpmRegistryClient;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NodeExecutionContextViewTest {

    @Test
    void suppliedRegistryClientIsTheOneReturned() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        NpmRegistryClient client = new NpmRegistryClient(HttpSenderExecutionContextView.view(ctx).getHttpSender());

        NodeExecutionContextView.view(ctx).setRegistryClient(client);

        assertThat(NodeExecutionContextView.view(ctx).getRegistryClient()).isSameAs(client);
    }

    @Test
    void registryClientIsNotStoredWhenNoneIsSupplied() {
        ExecutionContext ctx = new InMemoryExecutionContext();
        Map<String, Object> before = new HashMap<>(ctx.getMessages());

        NodeExecutionContextView.view(ctx).getRegistryClient();

        assertThat(ctx.getMessages()).isEqualTo(before);
    }
}
