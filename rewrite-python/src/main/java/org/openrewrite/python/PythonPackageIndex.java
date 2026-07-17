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
package org.openrewrite.python;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

/**
 * A Python package index (a pipenv {@code [[source]]}), after environment
 * variable expansion and credential resolution.
 */
@Value
@With
public class PythonPackageIndex {
    String name;
    String url;
    boolean verifySsl;

    @Nullable
    String username;

    @Nullable
    String password;

    /**
     * True when the URL still contains {@code ${VAR}} placeholders whose variables were
     * unset at discovery time; using such an index is a configuration failure.
     */
    boolean unresolvedPlaceholders;
}
