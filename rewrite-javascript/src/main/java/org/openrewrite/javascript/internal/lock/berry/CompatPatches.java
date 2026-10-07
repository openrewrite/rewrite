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

/**
 * Port of @yarnpkg/plugin-compat sources/index.ts {@code reduceDependency} (Yarn 4.10.3).
 */
final class CompatPatches {

    private CompatPatches() {
    }

    static Descriptor reduceDependency(Descriptor dependency, YarnRelease release) {
        for (String name : release.getPatches()) {
            if (StructUtils.makeIdent(null, name).identHash.equals(dependency.identHash)) {
                return StructUtils.makeDescriptor(dependency, StructUtils.makeRange("patch:",
                        StructUtils.stringifyDescriptor(dependency),
                        "optional!builtin<compat/" + StructUtils.stringifyIdent(dependency) + ">", null));
            }
        }
        return dependency;
    }
}
