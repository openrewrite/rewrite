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
package org.openrewrite.python.internal.index;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The pieces of the OS environment that index discovery reads, injectable so tests
 * control environment variables, the home directory, and the platform.
 */
public interface Environment {

    Environment SYSTEM = new Environment() {
        @Override
        public @Nullable String getenv(String name) {
            return System.getenv(name);
        }

        @Override
        public Path userHome() {
            return Paths.get(System.getProperty("user.home"));
        }

        @Override
        public String osName() {
            return System.getProperty("os.name");
        }
    };

    @Nullable
    String getenv(String name);

    Path userHome();

    String osName();
}
