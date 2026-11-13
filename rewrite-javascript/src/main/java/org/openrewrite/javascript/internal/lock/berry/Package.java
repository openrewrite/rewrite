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
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Port of @yarnpkg/core sources/types.ts {@code Package} (Yarn 4.10.3).
 */
@Getter
@Setter
final class Package extends Locator {

    static final String HARD = "HARD";
    static final String SOFT = "SOFT";

    @Nullable
    String version;

    @Nullable
    String languageName;

    /**
     * {@link #HARD} or {@link #SOFT}, or whatever upper-cased value a lockfile gave.
     */
    String linkType;

    @Nullable
    Object conditions;

    Map<String, Descriptor> dependencies;
    Map<String, Descriptor> peerDependencies;
    Map<String, Map<@Nullable String, Map<String, Object>>> dependenciesMeta;
    Map<String, Map<String, Object>> peerDependenciesMeta;
    Map<String, String> bin;

    Package(Locator locator, @Nullable String version, @Nullable String languageName, String linkType,
            @Nullable Object conditions, Map<String, Descriptor> dependencies,
            Map<String, Descriptor> peerDependencies,
            Map<String, Map<@Nullable String, Map<String, Object>>> dependenciesMeta,
            Map<String, Map<String, Object>> peerDependenciesMeta, Map<String, String> bin) {
        super(locator.identHash, locator.scope, locator.name, locator.locatorHash, locator.reference);
        this.version = version;
        this.languageName = languageName;
        this.linkType = linkType;
        this.conditions = conditions;
        this.dependencies = dependencies;
        this.peerDependencies = peerDependencies;
        this.dependenciesMeta = dependenciesMeta;
        this.peerDependenciesMeta = peerDependenciesMeta;
        this.bin = bin;
    }
}
