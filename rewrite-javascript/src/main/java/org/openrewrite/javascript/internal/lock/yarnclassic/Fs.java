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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.StripBom;

/**
 * Port of yarn src/util/fs.js (yarn 1.22.22): {@code readJson} and {@code writeFilePreservingEol}.
 */
final class Fs {

    private Fs() {
    }

    /**
     * {@code readJsonAndFile}'s object: JSON.parse of the file without its byte order mark.
     * @throws JsError the {@code SyntaxError} of JSON that does not parse, its message naming the file.
     */
    static @Nullable Object readJson(String file, String text) {
        try {
            return Js.jsonParse(StripBom.stripBom(text));
        } catch (JsError e) {
            throw new JsError(e.getName(), file + ": " + e.getMessage());
        }
    }

    /**
     * The line ending of the file it replaces, the first CR or LF in it, else the host's {@code os.EOL} ({@code os} is
     * {@code process.platform}, null when unknown).
     */
    static String writeFilePreservingEol(@Nullable String existing, String data, @Nullable String os) {
        String eol = null;
        if (existing != null) {
            for (int i = 0; i < existing.length(); i++) {
                char c = existing.charAt(i);
                if (c == '\r') {
                    eol = "\r\n";
                    break;
                }
                if (c == '\n') {
                    eol = "\n";
                    break;
                }
            }
        }
        if (eol == null) {
            if (os == null) {
                throw new CannotKnow("the line ending yarn writes (os.EOL of the host), as the lock holds none");
            }
            eol = "win32".equals(os) ? "\r\n" : "\n";
        }
        return "\n".equals(eol) ? data : data.replace("\n", eol);
    }
}
