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

/**
 * Port of yarn src/reporters/base-reporter.js {@code lang} and {@code stringifyLangArgs} (yarn 1.22.22): a message of
 * reporters/lang/en.js with its {@code $0}, {@code $1}, ... filled by each argument's {@code JSON.stringify}.
 */
final class Lang {

    private Lang() {
    }

    static String format(String message, @Nullable Object... args) {
        String out = message;
        for (int i = 0; i < args.length; i++) {
            out = out.replace("$" + i, stringify(args[i]));
        }
        return out;
    }

    // JSON.stringify(val) || val + ''
    private static String stringify(@Nullable Object val) {
        String json = val == null ? null : Js.jsonStringify(val);
        return json == null ? Js.string(val) : json;
    }
}
