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
package org.openrewrite.javascript.internal.lock.bun;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Port of bun's src/install/lockfile.zig {@code default_trusted_dependencies} (bun 1.3.10), from bun's
 * {@code default-trusted-dependencies.txt}, by truncated name hash, with the names other releases add or drop.
 */
final class DefaultTrustedDependencies {

    static final Set<Integer> HASHES = load();

    private DefaultTrustedDependencies() {
    }

    /**
     * The list as {@code release} has it: 1.3.10's, but {@code opencode-ai} only from 1.2.16
     * ({@link BunRelease#trustsOpencodeAiByDefault()}), {@code yarn} before 1.2.19
     * ({@link BunRelease#trustsYarnByDefault()}) and {@code @anthropic-ai/claude-code} from 1.3.12
     * ({@link BunRelease#trustsClaudeCodeByDefault()}).
     */
    static Set<Integer> of(BunRelease release) {
        Set<Integer> hashes = new LinkedHashSet<>(HASHES);
        listed(hashes, "opencode-ai", release.trustsOpencodeAiByDefault());
        listed(hashes, "yarn", release.trustsYarnByDefault());
        listed(hashes, "@anthropic-ai/claude-code", release.trustsClaudeCodeByDefault());
        return hashes;
    }

    private static void listed(Set<Integer> hashes, String name, boolean listed) {
        int hash = (int) Wyhash11.stringHash(name);
        if (listed) {
            hashes.add(hash);
        } else {
            hashes.remove(hash);
        }
    }

    private static Set<Integer> load() {
        String resource = "default-trusted-dependencies.txt";
        try (InputStream in = DefaultTrustedDependencies.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException(resource + " is missing");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
            Set<Integer> hashes = new LinkedHashSet<>();
            for (String name : new String(out.toByteArray(), StandardCharsets.UTF_8).split("[ \r\n\t]+")) {
                if (!name.isEmpty()) {
                    hashes.add((int) Wyhash11.stringHash(name));
                }
            }
            return Collections.unmodifiableSet(hashes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
