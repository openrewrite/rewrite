/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.docker.trait;

import org.openrewrite.docker.internal.ArgumentContents;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

/**
 * Base class for Docker trait matchers providing shared utilities for working with
 * Docker AST elements.
 *
 * @param <U> The trait type this matcher produces
 */
abstract class DockerTraitMatcher<U extends Trait<?>> extends SimpleTraitMatcher<U> {

    /**
     * Extracts text from a Docker argument, replacing environment variables with wildcards
     * for glob matching purposes.
     *
     * @param arg The argument to extract text from
     * @return Text with environment variables replaced by '*'
     */
    static String extractTextForMatching(Docker.Argument arg) {
        StringBuilder sb = new StringBuilder();
        for (Docker.ArgumentContent content : arg.getContents()) {
            if (content instanceof Docker.Literal) {
                sb.append(((Docker.Literal) content).getText());
            } else if (content instanceof Docker.EnvironmentVariable) {
                sb.append("*");
            }
        }
        return sb.toString();
    }

    /**
     * Performs bidirectional glob matching when environment variables are present.
     * When the text contains wildcards (from env vars), we need to check if either
     * the pattern matches the text OR the text (as a pattern) matches the pattern.
     *
     * @param text       The text to match (may contain wildcards from env vars)
     * @param pattern    The glob pattern to match against
     * @param hasEnvVars Whether the original text contained environment variables
     * @return true if there's a match in either direction
     */
    static boolean matchesBidirectional(String text, String pattern, boolean hasEnvVars) {
        if (hasEnvVars) {
            return StringUtils.matchesGlob(text, pattern) ||
                    StringUtils.matchesGlob(pattern, text);
        }
        return StringUtils.matchesGlob(text, pattern);
    }

    /**
     * Checks whether one part of an image reference matches a glob pattern, treating any
     * environment variable it contains as a wildcard.
     *
     * @param part    The image name, tag or digest to match
     * @param pattern The glob pattern to match against
     * @return true if the part matches
     */
    static boolean partMatches(Docker.Argument part, String pattern) {
        return matchesBidirectional(extractTextForMatching(part), pattern, ArgumentContents.containsVariable(part));
    }

    /// As [#partMatches], but for an image name, where a pattern also matches a name that spells
    /// the same image differently: `ubuntu` matches `docker.io/library/ubuntu`, and the other way
    /// round. Both are compared canonically, which fills in the registry and namespace a name
    /// leaves out rather than dropping the ones a pattern writes; familiarizing them instead would
    /// widen `docker.io/*` to `*`, matching every image on every registry.
    static boolean imageNameMatches(Docker.Argument imageName, String pattern) {
        if (partMatches(imageName, pattern)) {
            return true;
        }
        String text = extractTextForMatching(imageName);
        ImageName name = ImageName.parse(text);
        ImageName patternName = ImageName.parse(pattern);
        return matchesBidirectional(name.getCanonical(), patternName.getCanonical(), ArgumentContents.containsVariable(imageName));
    }
}
