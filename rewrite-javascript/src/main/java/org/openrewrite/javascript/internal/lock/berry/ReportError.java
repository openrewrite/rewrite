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

import lombok.Getter;

/**
 * Port of @yarnpkg/core sources/Report.ts {@code ReportError} (Yarn 4.10.3).
 */
@Getter
final class ReportError extends RuntimeException {

    // a plain JavaScript error thrown during resolution
    static final int EXCEPTION = 1;
    static final int RESOLVER_NOT_FOUND = 10;
    static final int FETCHER_NOT_FOUND = 11;
    static final int REMOTE_INVALID = 15;
    static final int REMOTE_NOT_FOUND = 16;
    static final int AUTHENTICATION_NOT_FOUND = 33;
    static final int NETWORK_ERROR = 35;
    static final int STACK_OVERFLOW_RESOLUTION = 45;
    static final int RESOLUTION_MISMATCH = 78;
    static final int RESOLUTION_FAILED = 82;

    private final int code;

    ReportError(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * {@code YN0082}: Yarn's code as it prints it.
     */
    String getName() {
        return String.format("YN%04d", code);
    }
}
