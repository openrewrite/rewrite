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
package org.openrewrite.python.internal.poetrylock;

import lombok.Value;
import lombok.With;

import java.util.List;

/**
 * One entry in a {@code [package.dependencies]} table: a dependency name mapped to one or more
 * {@link PoetryLockConstraint}s (multiple constraints are emitted as a multiline array of inline
 * tables). The name is the pretty (as-declared) name used as the TOML key.
 */
@Value
@With
public class PoetryLockDependency {
    String name;
    List<PoetryLockConstraint> constraints;
}
