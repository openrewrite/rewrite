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
package org.openrewrite.javascript.internal.lock.pnpm;

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Port of @pnpm/crypto.shasums-file src/index.ts (pnpm 11.2.2): a release's SHASUMS256.txt as integrities.
 */
final class ShasumsFile {

    private ShasumsFile() {
    }

    @Value
    static class Item {
        String integrity;

        /**
         * The file the line names; null on a line with no second field.
         */
        @Nullable
        String fileName;
    }

    static List<Item> fetchShasumsFile(RegistryMetadata fetch, String shasumsUrl) {
        String shasumsFileContent = fetchShasumsFileRaw(fetch, shasumsUrl);
        List<Item> items = new ArrayList<>();
        for (String line : shasumsFileContent.split("\n", -1)) {
            if (line.isEmpty()) {
                continue;
            }
            String[] fields = Js.trim(line).split("\\s+");
            items.add(new Item("sha256-" + Base64.getEncoder().encodeToString(hex(fields[0])),
                    fields.length > 1 ? fields[1] : null));
        }
        return items;
    }

    static String fetchShasumsFileRaw(RegistryMetadata fetch, String shasumsUrl) {
        String body = fetch.document(shasumsUrl);
        if (body == null) {
            throw new PnpmError("FAILED_DOWNLOAD_SHASUM_FILE", "Failed to fetch integrity file: " + shasumsUrl);
        }
        return body;
    }

    /**
     * {@code Buffer.from(hex, 'hex')}: stops at the first pair that is not hex.
     */
    static byte[] hex(String hex) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i + 1 < hex.length(); i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) {
                break;
            }
            bytes.write(hi << 4 | lo);
        }
        return bytes.toByteArray();
    }
}
