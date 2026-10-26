/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.gradle;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Gives each test JVM its own registry for the Gradle daemons that tooling API tests build with, so its daemon stays
 * warm. In the registry that concurrent test JVMs otherwise share, Gradle stops a daemon that has been idle for ten
 * seconds while another is idle too, and the next test waits on a new daemon whose first build takes ten seconds or
 * more. Auto-registered like {@link MavenSettingsAutoLoadingExtension}.
 */
public class GradleDaemonSlotExtension implements BeforeAllCallback {

    private static final String REGISTRY_BASE = "org.gradle.daemon.registry.base";
    private static final String IDLE_TIMEOUT = "org.gradle.daemon.idletimeout";

    // Held until the JVM exits, which frees the slot for a later test run to reuse its daemon
    private static @Nullable FileLock slot;

    @Override
    public void beforeAll(ExtensionContext context) {
        claimSlot();
    }

    private static synchronized void claimSlot() {
        if (slot != null || System.getProperty(REGISTRY_BASE) != null) {
            return;
        }
        Path slots = Paths.get(System.getProperty("java.io.tmpdir"), "openrewrite-gradle-test-daemons");
        for (int i = 0; i < 64; i++) {
            try {
                Path registry = Files.createDirectories(slots.resolve(Integer.toString(i)));
                FileChannel channel = FileChannel.open(registry.resolve("slot.lock"), CREATE, WRITE);
                FileLock lock = channel.tryLock();
                if (lock == null) {
                    channel.close();
                    continue;
                }
                slot = lock;
                System.setProperty(REGISTRY_BASE, registry.toString());
                // Alone in its registry, an idle daemon would otherwise outlive the tests by Gradle's default 3 hours
                if (System.getProperty(IDLE_TIMEOUT) == null) {
                    System.setProperty(IDLE_TIMEOUT, Long.toString(Duration.ofMinutes(10).toMillis()));
                }
                return;
            } catch (IOException ignored) {
                // Try the next slot, and failing all of them, the shared registry
            }
        }
    }
}
