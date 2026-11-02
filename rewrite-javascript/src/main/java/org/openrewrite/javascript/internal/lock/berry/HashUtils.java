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
package org.openrewrite.javascript.internal.lock.berry;

import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Port of @yarnpkg/core sources/hashUtils.ts {@code makeHash} (Yarn 4.10.3) for string arguments.
 */
final class HashUtils {

    private HashUtils() {
    }

    /**
     * The sha512 hex of the arguments concatenated, nulls skipped.
     */
    static String makeHash(@Nullable String... args) {
        StringBuilder acc = new StringBuilder();
        for (String arg : args) {
            if (arg != null) {
                acc.append(arg);
            }
        }
        return sha512Hex(acc.toString().getBytes(StandardCharsets.UTF_8));
    }

    static String sha512Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-512").digest(bytes);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
