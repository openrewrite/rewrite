/*
 * Copyright 2024 the original author or authors.
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

import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import io.moderne.jsonrpc.handler.TraceMessageHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.test.RewriteTest;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
public class RewriteRpcTest implements RewriteTest {
    RewriteRpc client;
    JavaScriptRewriteRpcProcess server;

    @BeforeEach
    void before() throws InterruptedException {
        this.server = new JavaScriptRewriteRpcProcess(
                "node",
//          "--inspect-brk",
                "./dist/src/main/javascript/rpc/server.js"
        );
        server.start();
        InputStream serverIn = server.getInputStream();
        OutputStream serverOut = server.getOutputStream();

        Thread.sleep(3000);
        System.out.println("Sending message");

        this.client = new RewriteRpc(
                new JsonRpc(new TraceMessageHandler("client",
                        new HeaderDelimitedMessageHandler(serverIn, serverOut))),
                Environment.builder().build()
        ).batchSize(1).timeout(Duration.ofMinutes(10));
    }

    @AfterEach
    void after() throws InterruptedException {
        client.shutdown();
        server.interrupt();
        server.join();
    }

    @Test
    void getRecipes() {
        assertThat(client.getRecipes()).isNotEmpty();
    }

//    @Test
//    void rpcToJavascript() {
//        rewriteRun(
//          json(
//            """
//              { "key": "value" }
//              """
//          )
//        );
//    }
}
