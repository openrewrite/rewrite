/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;

enum JavaSourceRoot {

    Main,

    Test;

    public static @NonNull JavaSourceRoot fromName(@Nullable String sourceRoot) {
        if (sourceRoot == null) {
            return Main;
        }

        switch (sourceRoot.toLowerCase()) {
            case "main":
                return Main;
            case "test":
                return Test;
            default:
                throw new IllegalArgumentException("Invalid source root: " + sourceRoot);
        }
    }
}
