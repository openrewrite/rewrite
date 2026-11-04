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

import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.formatter.JsonMessageFormatter;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import org.junit.jupiter.api.Test;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.rpc.RpcObjectData;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.text.PlainText;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

class PythonResolutionResultRpcTest {

    @Test
    void roundTripCyclicGraph() {
        ResolvedDependency a = new ResolvedDependency("a", "1.0.0", null, new ArrayList<>());
        ResolvedDependency b = new ResolvedDependency("b", "1.0.0", null, new ArrayList<>());
        a.getDependencies().add(b);
        b.getDependencies().add(a);

        PythonResolutionResult marker = new PythonResolutionResult(UUID.randomUUID(), "my-app", "1.0.0",
                null, null, "pyproject.toml", null, null,
                emptyList(),
                singletonList(new PythonResolutionResult.Dependency("a", "==1.0.0", null, null, a)),
                emptyMap(), emptyMap(), emptyList(), emptyList(),
                asList(a, b),
                null, null);

        ResolvedDependency received = sendAndReceive(marker).getDependencies().get(0).getResolved();
        assertThat(received).isNotNull();

        ResolvedDependency receivedB = received.getDependencies().get(0);
        assertThat(receivedB.getName()).isEqualTo("b");

        ResolvedDependency backEdge = receivedB.getDependencies().get(0);
        assertThat(backEdge.getName())
                .as("the dependency closing the cycle must carry its own state, not a blank placeholder")
                .isEqualTo("a");
        assertThat(backEdge)
                .as("the back-reference closing the cycle must resolve to the same instance")
                .isSameAs(received);
    }

    @Test
    void dependencyGroupsOverTheWire() throws IOException {
        ResolvedDependency pytest = new ResolvedDependency("pytest", "8.0.0", null, new ArrayList<>());
        ResolvedDependency pluggy = new ResolvedDependency("pluggy", "1.5.0", null, new ArrayList<>());
        pytest.getDependencies().add(pluggy);
        pluggy.getDependencies().add(pytest);
        Dependency declared = new Dependency("PyTest", ">=8", singletonList("cov"), "python_version > '3'", pytest);

        PythonResolutionResult marker = new PythonResolutionResult(UUID.randomUUID(), "my-app", "1.0.0",
                null, null, "pyproject.toml", null, null,
                emptyList(), emptyList(),
                singletonMap("dev", asList(declared, new Dependency("unlocked", null, null, null, null))),
                singletonMap("test", singletonList(declared)),
                emptyList(), emptyList(),
                asList(pytest, pluggy),
                null, null);
        PlainText sent = PlainText.builder().sourcePath(Paths.get("pyproject.toml")).text("").build();
        sent = sent.withMarkers(sent.getMarkers().add(marker));

        // two peers over pipes, so the marker crosses as the JSON a real peer would be sent
        PipedOutputStream serverOut = new PipedOutputStream();
        PipedOutputStream clientOut = new PipedOutputStream();
        RewriteRpc server = peer(new PipedInputStream(clientOut), serverOut);
        RewriteRpc client = peer(new PipedInputStream(serverOut), clientOut);
        try {
            server.print(sent);
            PlainText received = client.getObject(sent.getId().toString(), PlainText.class.getName());
            PythonResolutionResult read = received.getMarkers().findFirst(PythonResolutionResult.class).orElseThrow();

            Dependency dev = read.getOptionalDependencies().get("dev").get(0);
            assertThat(dev).isNotSameAs(declared);
            assertThat(dev)
                    .extracting(Dependency::getName, Dependency::getVersionConstraint, Dependency::getExtras, Dependency::getMarker)
                    .containsExactly("PyTest", ">=8", singletonList("cov"), "python_version > '3'");
            assertThat(dev.getResolved())
                    .as("a declared dependency points at the entry of the resolved list, not at a copy of it")
                    .isSameAs(read.getResolvedDependencies().get(0));
            assertThat(read.getOptionalDependencies().get("dev").get(1).getResolved()).isNull();
            assertThat(read.getDependencyGroups().get("test").get(0).getResolved()).isSameAs(dev.getResolved());
            assertThat(read.findDependencyInAnyScope("unlocked")).isNotNull();
            assertThat(read.getAllDeclaredDependencies()).hasSize(3);
        } finally {
            client.shutdown();
            server.shutdown();
        }
    }

    private static RewriteRpc peer(InputStream in, OutputStream out) {
        JsonMessageFormatter formatter = new JsonMessageFormatter(new ParameterNamesModule());
        return new RewriteRpc(new JsonRpc(new HeaderDelimitedMessageHandler(formatter, in, out)), new RecipeMarketplace());
    }

    private static PythonResolutionResult sendAndReceive(PythonResolutionResult marker) {
        Deque<List<RpcObjectData>> batches = new ArrayDeque<>();
        RpcSendQueue sq = new RpcSendQueue(1_000_000, batches::addLast, new IdentityHashMap<>(), null, false);
        sq.send(marker, null, null);
        sq.flush();

        // The wire carries a UUID as a string, as the transport's JSON encoding would.
        List<RpcObjectData> all = new ArrayList<>();
        while (!batches.isEmpty()) {
            for (RpcObjectData data : batches.removeFirst()) {
                all.add(data.getValue() instanceof UUID ?
                        new RpcObjectData(data.getState(), data.getValueType(), data.getValue().toString(), data.getRef(), false) :
                        data);
            }
        }
        Deque<List<RpcObjectData>> drain = new ArrayDeque<>();
        drain.add(all);

        return new RpcReceiveQueue(new HashMap<>(), drain::removeFirst, null, null).receive(null);
    }
}
