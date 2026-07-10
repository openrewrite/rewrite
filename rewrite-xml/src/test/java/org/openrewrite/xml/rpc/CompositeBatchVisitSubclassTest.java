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
package org.openrewrite.xml.rpc;

import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.formatter.JsonMessageFormatter;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.RecipeScheduler;
import org.openrewrite.SourceFile;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.marketplace.RecipeBundle.runtimeClasspath;

/**
 * Regression test for the RPC edit-phase {@code BatchVisit} batching: when a composite
 * runs several same-RPC recipes against a source file whose runtime class is a
 * <em>subclass</em> of the canonical tree type (as produced by lazily-loaded LSTs such as
 * the Moderne CLI's V3 format), every batched recipe but the last was silently skipped.
 * <p>
 * The cause was {@code RecipeRunStage} keying off the raw {@code src.getClass().getName()}
 * (the subclass name) instead of {@link org.openrewrite.rpc.DynamicDispatchRpcCodec#canonicalSourceFileType}.
 * {@code getLanguages()} advertises canonical type names, so the batch language gate never
 * matched for subclasses and the batch was abandoned; only the trailing recipe (which falls
 * through to the non-batch path) ran.
 */
class CompositeBatchVisitSubclassTest {

    RewriteRpc server;
    RewriteRpc client;

    /**
     * A non-final {@link SourceFile} subtype whose runtime class name is not the canonical
     * type registered with the codec — mirrors a lazily-deserialized LST proxy.
     */
    static class LazyXmlDocument extends Xml.Document {
        LazyXmlDocument(Xml.Document d) {
            super(d.getId(), d.getSourcePath(), "", d.getMarkers(), null, d.isCharsetBomMarked(),
                    d.getChecksum(), d.getFileAttributes(), d.getProlog(), d.getRoot(), d.getEof());
        }
    }

    @BeforeEach
    void before() throws IOException {
        var serverOut = new PipedOutputStream();
        var clientOut = new PipedOutputStream();
        var serverIn = new PipedInputStream(clientOut);
        var clientIn = new PipedInputStream(serverOut);

        Environment env = Environment.builder().build();

        var serverFormatter = new JsonMessageFormatter(new ParameterNamesModule());
        var clientFormatter = new JsonMessageFormatter(new ParameterNamesModule());

        client = new RewriteRpc(new JsonRpc(new HeaderDelimitedMessageHandler(clientFormatter, clientIn, clientOut)), env.toMarketplace(runtimeClasspath()))
          .batchSize(1);
        server = new RewriteRpc(new JsonRpc(new HeaderDelimitedMessageHandler(serverFormatter, serverIn, serverOut)), env.toMarketplace(runtimeClasspath()))
          .batchSize(1);
    }

    @AfterEach
    void after() {
        client.shutdown();
        server.shutdown();
    }

    @Test
    void allBatchedRecipesRunWhenSourceIsCanonicalTypeSubclass() {
        Xml.Document parsed = (Xml.Document) XmlParser.builder().build()
          .parse("<root attr=\"oldA\">\n    <child>oldV</child>\n</root>\n")
          .findFirst()
          .orElseThrow();
        // Hand the scheduler a subclass instance, so getClass().getName() != canonical type.
        Xml.Document subclass = new LazyXmlDocument(parsed);

        // Two consecutive same-RPC recipes that edit different parts of the document.
        Recipe changeChild = client.prepareRecipe("org.openrewrite.xml.ChangeTagValue",
          Map.of("elementName", "/root/child", "oldValue", "oldV", "newValue", "newV"));
        Recipe changeAttr = client.prepareRecipe("org.openrewrite.xml.ChangeTagAttribute",
          Map.of("elementName", "root", "attributeName", "attr", "oldValue", "oldA", "newValue", "newA"));

        var ctx = new InMemoryExecutionContext(t -> {
            throw new IllegalStateException(t);
        });
        RecipeRun run = new RecipeScheduler().scheduleRun(
          new CompositeRecipe(List.of(changeChild, changeAttr)),
          new InMemoryLargeSourceSet(List.of(subclass)), ctx, 2);

        SourceFile after = run.getChangeset().getAllResults().get(0).getAfter();
        String printed = after.printAll();

        // The trailing recipe always ran (even with the bug).
        assertThat(printed).as("trailing batched recipe").contains("attr=\"newA\"");
        // The non-trailing recipe must also run — this is what the bug skipped.
        assertThat(printed).as("non-trailing batched recipe").contains("<child>newV</child>");
    }
}
