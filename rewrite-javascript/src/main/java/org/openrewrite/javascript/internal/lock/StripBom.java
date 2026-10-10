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
package org.openrewrite.javascript.internal.lock;

/**
 * Port of strip-bom 3.0.0 (yarn 1.22.22) and 5.0.0 (pnpm 11.2.2), which answer alike for every string: only the
 * TypeError for a value that is not one differs.
 */
public final class StripBom {

    private StripBom() {
    }

    public static String stripBom(String string) {
        return !string.isEmpty() && string.charAt(0) == '\uFEFF' ? string.substring(1) : string;
    }
}
