package org.openrewrite.javascript.rpc;

import io.moderne.jsonrpc.JsonRpc;
import io.moderne.jsonrpc.handler.HeaderDelimitedMessageHandler;
import io.moderne.jsonrpc.handler.TraceMessageHandler;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.config.Environment;
import org.openrewrite.rpc.RewriteRpc;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

public class JavaScriptRewriteRpc extends RewriteRpc {
    private final JavaScriptRewriteRpcProcess process;

    private JavaScriptRewriteRpc(JavaScriptRewriteRpcProcess process, Environment marketplace) {
        super(process.getRpcClient(), marketplace);
        this.process = process;
    }

    public static JavaScriptRewriteRpc start(Environment marketplace, String... command) {
        JavaScriptRewriteRpcProcess process = new JavaScriptRewriteRpcProcess(command);
        process.start();
        return new JavaScriptRewriteRpc(process, marketplace);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        process.interrupt();
        try {
            process.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public int installRecipes(File recipes) {
        return send(
                "InstallRecipes",
                new InstallRecipesByFile(recipes),
                InstallRecipesResponse.class
        ).getRecipesInstalled();
    }

    public int installRecipes(String packageName) {
        return installRecipes(packageName, null);
    }

    public int installRecipes(String packageName, @Nullable String version) {
        return send(
                "InstallRecipes",
                new InstallRecipesByPackage(
                        new InstallRecipesByPackage.Package(packageName, version)),
                InstallRecipesResponse.class
        ).getRecipesInstalled();
    }

    private static class JavaScriptRewriteRpcProcess extends Thread {
        private final String[] command;

        @Nullable
        private Process process;

        @Getter
        private JsonRpc rpcClient;

        public JavaScriptRewriteRpcProcess(String... command) {
            this.command = command;
            this.setDaemon(false);
        }

        @Override
        public void run() {
            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                process = pb.start();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public synchronized void start() {
            super.start();
            while (this.process == null) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            this.rpcClient = new JsonRpc(new TraceMessageHandler(
                    "client",
                    new HeaderDelimitedMessageHandler(this.process.getInputStream(),
                            this.process.getOutputStream())
            ));
        }
    }
}
