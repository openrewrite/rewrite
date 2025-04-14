package org.openrewrite.javascript;

import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import io.moderne.jsonrpc.handler.TraceMessageHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.rpc.RewriteRpc;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class RewriteRpcTest implements RewriteTest {
    RewriteRpc client;
    JavaScriptRewriteRpcProcess server;

    @BeforeEach
    void before() throws IOException {
        this.server = new JavaScriptRewriteRpcProcess(
          "node",
          "--inspect-brk",
          "./dist/src/main/javascript/rpc/server.js"
        );
        server.start();
        InputStream serverIn = server.getInputStream();
        OutputStream serverOut = server.getOutputStream();

        waitUntilReady(serverIn);

        this.client = new RewriteRpc(
          new JsonRpc(new TraceMessageHandler("client",
            new HeaderDelimitedMessageHandler(serverIn, serverOut))),
          Environment.builder().build()
        ).batchSize(1).timeout(Duration.ofMinutes(10));
    }

    private static void waitUntilReady(InputStream serverIn) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        StringBuilder message = new StringBuilder();
        while ((bytesRead = serverIn.read(buffer)) != -1) {
            message.append(new String(buffer, 0, bytesRead));
            if (message.toString().contains("Debugger attached.")) {
                break;
            }
        }
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
