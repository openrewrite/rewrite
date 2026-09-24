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
package org.openrewrite.python.internal.index;

import lombok.Getter;
import org.openrewrite.python.PythonPackageIndex;

import java.util.List;

/**
 * A failure talking to a package index, carrying enough structure for the lock
 * engine to map it to a per-package recipe failure.
 */
@Getter
public class PythonIndexException extends RuntimeException {

    public enum Reason {
        UNREACHABLE,
        AUTH_FAILED,
        NOT_FOUND
    }

    private final Reason reason;
    private final String indexUrl;

    public PythonIndexException(Reason reason, String indexUrl, String message) {
        super(message);
        this.reason = reason;
        this.indexUrl = indexUrl;
    }

    public PythonIndexException(Reason reason, String indexUrl, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.indexUrl = indexUrl;
    }

    /**
     * An HTTP 401/403 from {@code url}, naming any source URL credentials that were not sent
     * because their variables were unset where the lock was regenerated.
     */
    static PythonIndexException authFailed(PythonPackageIndex index, int code, String url) {
        String message = "HTTP " + code + " from " + url;
        List<String> placeholders = index.getUnresolvedCredentialPlaceholders();
        if (!placeholders.isEmpty()) {
            message += "; credentials in the index URL were not sent because " + String.join(", ", placeholders) +
                    (placeholders.size() == 1 ? " is" : " are") + " not set in the environment the recipe runs in";
        }
        return new PythonIndexException(Reason.AUTH_FAILED, index.getUrl(), message);
    }
}
