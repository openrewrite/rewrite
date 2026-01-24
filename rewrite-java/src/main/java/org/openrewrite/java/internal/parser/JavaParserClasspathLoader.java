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
package org.openrewrite.java.internal.parser;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.JavaParser;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

/**
 * Prepares classpath resources for use by {@link JavaParser}.
 */
public interface JavaParserClasspathLoader {

    /**
     * Load a classpath resource.
     *
     * @param artifactName A descriptor for the classpath resource to load.
     * @return The path a JAR or classes directory that is suitable for use
     * as a classpath entry in a compilation step.
     */
    @Nullable
    Path load(String artifactName);

    /**
     * @return The artifact identifiers available from this loader, for use in diagnostic messages.
     */
    default Collection<String> availableArtifacts() {
        return Collections.emptyList();
    }
}
