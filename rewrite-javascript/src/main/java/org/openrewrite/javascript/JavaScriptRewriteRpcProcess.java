/*
 * Copyright 2025 the original author or authors.
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
