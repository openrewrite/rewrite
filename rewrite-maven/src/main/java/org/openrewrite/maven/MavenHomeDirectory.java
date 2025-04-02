/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.maven;

import java.nio.file.Path;
import java.nio.file.Paths;

public class MavenHomeDirectory {
    public static Path get() {
        String m2Home = System.getenv("M2_HOME");
        if (m2Home != null) {
            return Paths.get(m2Home);
        }
        return Paths.get(System.getProperty("user.home"), ".m2");
    }

    public static Path getRepository() {
        return get().resolve("repository");
    }
}
