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
package org.openrewrite.javascript.internal.registry;

import lombok.Value;

import java.util.Map;
import java.util.Set;

/**
 * The abbreviated packument ({@code Accept: application/vnd.npm.install-v1+json}), read only to
 * pick a version over a range or resolve a dist-tag. The byte-exact per-version fields come from
 * the single-version manifest, not from here.
 */
@Value
public class AbbreviatedPackument {
    String name;

    /** {@code dist-tags} such as {@code latest}/{@code next}, for resolving a tag constraint to a concrete version. */
    Map<String, String> distTags;

    /** The published version keys, in the registry's document order. */
    Set<String> versions;
}
