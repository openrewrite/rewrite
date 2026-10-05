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

import com.fasterxml.jackson.databind.json.JsonMapper;
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
import org.openrewrite.marker.SearchResult;
import org.openrewrite.marketplace.RecipeMarketplace;
import org.openrewrite.python.tree.Py;
import org.openrewrite.rpc.RewriteRpc;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the Python-hosted topology in-process: the "host" peer plays the Python process that
 * owns the tree, the "server" peer the spawned JVM, which has no
 * {@link org.openrewrite.python.rpc.PythonRewriteRpc} handle.
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
        // The predicate only dispatches when an import could actually satisfy the request, so the
        // tree needs one for the RemovesTypingList visitor to have anything to remove.
        Py.CompilationUnit cu = PythonImports.cu(PythonImports.fromImport("typing", PythonImports.member("List")));

        // The maybeRemoveImport queued on the server dispatches org.openrewrite.python.RemoveImport
        // back to the host, where the test stand-in receives it.
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

    @Test
    void addVisitorIsConstructibleFromAnOptionsMap() {
        var mapper = JsonMapper.builder().addModule(new ParameterNamesModule()).build();
        var options = new HashMap<String, Object>();
        options.put("module", "typing");
        options.put("name", "List");
        options.put("alias", null);
        options.put("onlyIfReferenced", true);

        PythonAddImportVisitor<?> visitor = mapper.convertValue(options, PythonAddImportVisitor.class);

        assertThat(visitor.getModule()).isEqualTo("typing");
        assertThat(visitor.getName()).isEqualTo("List");
        assertThat(visitor.getAlias()).isNull();
        assertThat(visitor.isOnlyIfReferenced()).isTrue();
    }

    @Test
    void removeVisitorIsConstructibleFromAnOptionsMap() {
        var mapper = JsonMapper.builder().addModule(new ParameterNamesModule()).build();
        var options = new HashMap<String, Object>();
        options.put("module", "typing");
        options.put("name", "List");
        options.put("onlyIfUnused", false);

        PythonRemoveImportVisitor<?> visitor = mapper.convertValue(options, PythonRemoveImportVisitor.class);

        assertThat(visitor.getModule()).isEqualTo("typing");
        assertThat(visitor.isOnlyIfUnused()).isFalse();
    }
}
