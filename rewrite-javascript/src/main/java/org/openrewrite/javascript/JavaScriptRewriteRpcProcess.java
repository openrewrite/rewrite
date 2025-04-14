package org.openrewrite.javascript;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

public class JavaScriptRewriteRpcProcess extends Thread {
    private final String[] command;

    @Nullable
    private Process process;

    public JavaScriptRewriteRpcProcess(String... command) {
        this.command = command;
    }

    @Override
    public void run() {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
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
    }

    public InputStream getInputStream() {
        if (process == null) {
            throw new IllegalStateException("Process not started");
        }
        return process.getInputStream();
    }

    public OutputStream getOutputStream() {
        if (process == null) {
            throw new IllegalStateException("Process not started");
        }
        return process.getOutputStream();
    }
}
