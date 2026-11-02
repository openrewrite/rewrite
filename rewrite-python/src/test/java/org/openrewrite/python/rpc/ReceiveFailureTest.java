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
package org.openrewrite.python.rpc;

import lombok.Value;
import lombok.With;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Marker;
import org.openrewrite.python.PythonParser;
import org.openrewrite.python.tree.Py;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;
import org.openrewrite.text.PlainText;
import org.openrewrite.tree.ParseError;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.Tree.randomId;

/**
 * The sender of a tree counts it, and every ref it assigned while sending it, as received. When the
 * receiver fails part-way it says so, and both sides forget the transfer, or the next one arrives as a
 * change to a tree, and as bare refs to objects, that the receiver never built.
 */
class ReceiveFailureTest {

    @Test
    void treeThePythonProcessFailsToTake() {
        PythonRewriteRpc rpc = PythonRewriteRpc.getOrStart();
        Py.CompilationUnit cu = (Py.CompilationUnit) PythonParser.builder().build()
          .parse("x = 1\ny = 2\n").collect(toList()).get(0);

        // on the last statement, so that the transfer is well under way when it fails
        List<Statement> statements = new ArrayList<>(cu.getStatements());
        Statement last = statements.get(statements.size() - 1);
        statements.set(statements.size() - 1, last.withMarkers(last.getMarkers().add(new Unreceivable(randomId()))));
        Py.CompilationUnit unreceivable = cu.withStatements(statements);

        assertThatThrownBy(() -> rpc.print(unreceivable)).hasMessageContaining(Unreceivable.class.getName());

        assertThat(rpc.print(cu)).isEqualTo("x = 1\ny = 2\n");
        assertThat(rpc.print(cu.withStatements(singletonList(cu.getStatements().get(1))))).isEqualTo("\ny = 2\n");
    }

    @Test
    void treeThisSideFailsToTake() {
        PythonRewriteRpc rpc = PythonRewriteRpc.getOrStart();
        Parser.Input input = Parser.Input.fromString(Paths.get("a.py"), "x = 1\n");

        // asked for as a type under which this side has no codec for a Python tree, so it stops reading
        // part-way through a transfer that the Python process has by then sent in full
        List<SourceFile> unreceived = rpc.parse(singletonList(input), null, PythonParser.builder().build(),
          PlainText.class.getName(), new InMemoryExecutionContext()).collect(toList());
        assertThat(unreceived).singleElement().isInstanceOf(ParseError.class);

        // the next tree shares objects with that one, which this side never recorded the refs of
        assertThat(PythonParser.builder().build().parse("x = 1\n").collect(toList())).singleElement()
          .isInstanceOfSatisfying(Py.CompilationUnit.class, cu -> assertThat(cu.printAll()).isEqualTo("x = 1\n"));
    }

    /**
     * A marker this side sends by its codec, which the Python process has no codec to read.
     */
    @Value
    @With
    static class Unreceivable implements Marker, RpcCodec<Unreceivable> {
        UUID id;

        @Override
        public void rpcSend(Unreceivable after, RpcSendQueue q) {
            q.getAndSend(after, Marker::getId);
        }

        @Override
        public Unreceivable rpcReceive(Unreceivable before, RpcReceiveQueue q) {
            return before.withId(q.receiveAndGet(before.getId(), UUID::fromString));
        }
    }
}
