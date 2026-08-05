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
package org.openrewrite.python.service;

import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.formatter.JsonMessageFormatter;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.python.tree.Py;
import org.openrewrite.rpc.RewriteRpc;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Paths;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the Python-hosted topology in-process: the "host" peer plays the Python process that
 * owns the {@link Py.CompilationUnit} and requests a visit; the "server" peer plays the JVM it
 * spawned. The JVM then holds no {@link org.openrewrite.python.rpc.PythonRewriteRpc} handle, so
 * {@link PythonImportService} must dispatch import edits back over the serving connection.
 */
class PythonImportServiceRpcTest {
    RewriteRpc host;
    RewriteRpc server;

    @BeforeEach
    void before() throws IOException {
        var serverOut = new PipedOutputStream();
        var hostOut = new PipedOutputStream();
        var serverIn = new PipedInputStream(hostOut);
        var hostIn = new PipedInputStream(serverOut);

        host = new RewriteRpc(new JsonRpc(new HeaderDelimitedMessageHandler(
          new JsonMessageFormatter(new ParameterNamesModule()), hostIn, hostOut)), new RecipeMarketplace())
          .batchSize(1);
        server = new RewriteRpc(new JsonRpc(new HeaderDelimitedMessageHandler(
          new JsonMessageFormatter(new ParameterNamesModule()), serverIn, serverOut)), new RecipeMarketplace())
          .batchSize(1);
    }

    @AfterEach
    void after() {
        host.shutdown();
        server.shutdown();
    }

    @Test
    void dispatchesImportEditBackToRequestingPeer() {
        Py.CompilationUnit cu = new Py.CompilationUnit(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
          Paths.get("test.py"), null, null, false, null, emptyList(), emptyList(), Space.EMPTY);

        // The host asks the server to run a visitor that queues a maybeRemoveImport; the
        // resulting org.openrewrite.python.RemoveImport dispatch lands back on the host, whose
        // test stand-in marks the tree with the (module, name) pair it received.
        Tree after = host.visit(cu, RemovesTypingList.class.getName(), 0);

        assertThat(after).isInstanceOf(Py.CompilationUnit.class);
        assertThat(((SourceFile) after).getMarkers().findFirst(SearchResult.class))
          .hasValueSatisfying(found -> assertThat(found.getDescription()).isEqualTo("typing.List"));
    }

    public static class RemovesTypingList extends JavaVisitor<Object> {
        @Override
        public @Nullable J preVisit(J tree, Object o) {
            stopAfterPreVisit();
            maybeRemoveImport("typing.List");
            return tree;
        }
    }
}
