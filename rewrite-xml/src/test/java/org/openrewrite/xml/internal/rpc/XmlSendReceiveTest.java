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
package org.openrewrite.xml.internal.rpc;

import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.formatter.JsonMessageFormatter;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import io.moderne.jsonrpc.handler.TraceMessageHandler;
import lombok.SneakyThrows;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.config.Environment;
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static org.openrewrite.marketplace.RecipeBundle.runtimeClasspath;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.xml.Assertions.xml;

class XmlSendReceiveTest implements RewriteTest {
    RewriteRpc server;
    RewriteRpc client;

    @BeforeEach
    void before() throws IOException {
        PipedOutputStream serverOut = new PipedOutputStream();
        PipedOutputStream clientOut = new PipedOutputStream();
        PipedInputStream serverIn = new PipedInputStream(clientOut);
        PipedInputStream clientIn = new PipedInputStream(serverOut);

        Environment env = Environment.builder().build();

        JsonMessageFormatter serverFormatter = new JsonMessageFormatter(new ParameterNamesModule());
        JsonMessageFormatter clientFormatter = new JsonMessageFormatter(new ParameterNamesModule());

        server = new RewriteRpc(new JsonRpc(new TraceMessageHandler("server", new HeaderDelimitedMessageHandler(serverFormatter, serverIn, serverOut))), env.toMarketplace(runtimeClasspath()))
                .batchSize(1);
        client = new RewriteRpc(new JsonRpc(new TraceMessageHandler("client", new HeaderDelimitedMessageHandler(clientFormatter, clientIn, clientOut))), env.toMarketplace(runtimeClasspath()))
                .batchSize(1);
    }

    @AfterEach
    void after() {
        server.shutdown();
        client.shutdown();
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new TreeVisitor<>() {
            @Override
            @SneakyThrows
            public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                Tree t = server.visit((SourceFile) tree, ChangeTagValue.class.getName(), 0);
                stopAfterPreVisit();
                return t;
            }
        }));
    }

    @DocumentExample
    @Test
    void sendReceiveSimpleTag() {
        rewriteRun(
                xml(
                        """
                                <?xml version="1.0" encoding="UTF-8"?>
                                <root>
                                    <child>original</child>
                                </root>
                                """,
                        """
                                <?xml version="1.0" encoding="UTF-8"?>
                                <root>
                                    <child>changed</child>
                                </root>
                                """
                )
        );
    }

    @Test
    void sendReceiveAttributes() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new TreeVisitor<>() {
                    @Override
                    @SneakyThrows
                    public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                        Tree t = server.visit((SourceFile) tree, ChangeAttributeValue.class.getName(), 0);
                        stopAfterPreVisit();
                        return t;
                    }
                })),
                xml(
                        """
                                <root attr="old">
                                    <child key="value"/>
                                </root>
                                """,
                        """
                                <root attr="new">
                                    <child key="value"/>
                                </root>
                                """
                )
        );
    }

    @Test
    void sendReceiveComment() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new TreeVisitor<>() {
                    @Override
                    @SneakyThrows
                    public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                        Tree t = server.visit((SourceFile) tree, ChangeComment.class.getName(), 0);
                        stopAfterPreVisit();
                        return t;
                    }
                })),
                xml(
                        """
                                <root>
                                    <!-- old comment -->
                                    <child/>
                                </root>
                                """,
                        """
                                <root>
                                    <!-- new comment -->
                                    <child/>
                                </root>
                                """
                )
        );
    }

    @Test
    void sendReceiveIdempotent() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new TreeVisitor<>() {
                    @Override
                    @SneakyThrows
                    public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                        // Send and receive without modification (identity visitor)
                        Tree t = server.visit((SourceFile) tree, NoOp.class.getName(), 0);
                        stopAfterPreVisit();
                        return t;
                    }
                })),
                xml(
                        """
                                <?xml version="1.0" encoding="UTF-8"?>
                                <root attr="value">
                                    <!-- a comment -->
                                    <child>text</child>
                                    <empty/>
                                </root>
                                """
                )
        );
    }

    @Test
    void sendReceiveProcessingInstruction() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new TreeVisitor<>() {
                    @Override
                    @SneakyThrows
                    public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                        Tree t = server.visit((SourceFile) tree, NoOp.class.getName(), 0);
                        stopAfterPreVisit();
                        return t;
                    }
                })),
                xml(
                        """
                                <?xml version="1.0"?>
                                <?xml-stylesheet type="text/xsl" href="style.xsl"?>
                                <root/>
                                """
                )
        );
    }

    @Test
    void sendReceiveCdata() {
        rewriteRun(
                spec -> spec.recipe(toRecipe(() -> new TreeVisitor<>() {
                    @Override
                    @SneakyThrows
                    public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                        Tree t = server.visit((SourceFile) tree, NoOp.class.getName(), 0);
                        stopAfterPreVisit();
                        return t;
                    }
                })),
                xml(
                        """
                                <root><![CDATA[some <special> data]]></root>
                                """
                )
        );
    }

    public static class ChangeTagValue extends XmlVisitor<Integer> {
        @Override
        public Xml visitCharData(Xml.CharData charData, Integer p) {
            if ("original".equals(charData.getText())) {
                return charData.withText("changed");
            }
            return charData;
        }
    }

    public static class ChangeAttributeValue extends XmlVisitor<Integer> {
        @Override
        public Xml visitAttribute(Xml.Attribute attribute, Integer p) {
            if ("attr".equals(attribute.getKey().getName())) {
                return attribute.withValue(
                        attribute.getValue().withValue("new")
                );
            }
            return super.visitAttribute(attribute, p);
        }
    }

    public static class ChangeComment extends XmlVisitor<Integer> {
        @Override
        public Xml visitComment(Xml.Comment comment, Integer p) {
            if (comment.getText().contains("old comment")) {
                return comment.withText(" new comment ");
            }
            return comment;
        }
    }

    public static class NoOp extends XmlVisitor<Integer> {
        // Identity visitor - no changes
    }
}
