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
 * Port of @yarnpkg/core sources/types.ts {@code Descriptor} (Yarn 4.10.3); equal when their hashes are.
 */
@Getter
final class Descriptor extends Ident {
    final String descriptorHash;
    final String range;

    Descriptor(String identHash, @Nullable String scope, String name, String descriptorHash, String range) {
        super(identHash, scope, name);
        this.descriptorHash = descriptorHash;
        this.range = range;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Descriptor && ((Descriptor) o).descriptorHash.equals(descriptorHash);
    }

    @Override
    public int hashCode() {
        return descriptorHash.hashCode();
    }

    @Override
    public String toString() {
        return StructUtils.stringifyDescriptor(this);
    }
}
