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
import org.jspecify.annotations.Nullable;

/**
 * Port of @yarnpkg/core sources/types.ts {@code Ident} (Yarn 4.10.3).
 */
@Getter
class Ident {
    final String identHash;
    final @Nullable String scope;
    final String name;

    Ident(String identHash, @Nullable String scope, String name) {
        this.identHash = identHash;
        this.scope = scope;
        this.name = name;
    }

    @Override
    public String toString() {
        return StructUtils.stringifyIdent(this);
    }
}
