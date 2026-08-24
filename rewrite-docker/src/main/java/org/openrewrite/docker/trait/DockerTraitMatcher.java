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
 * Base class for Docker trait matchers, holding the glob matching they share.
 *
 * @param <U> The trait type this matcher produces
 */
abstract class DockerTraitMatcher<U extends Trait<?>> extends SimpleTraitMatcher<U> {

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

    /// A text holding an environment variable stands as a wildcard, so it is matched in both
    /// directions: either may be the pattern the other answers to.
    static boolean matchesBidirectional(String text, String pattern, boolean hasEnvVars) {
        if (hasEnvVars) {
            return StringUtils.matchesGlob(text, pattern) ||
                    StringUtils.matchesGlob(pattern, text);
        }
        return StringUtils.matchesGlob(text, pattern);
    }

    static boolean partMatches(Docker.Argument part, String pattern) {
        return matchesBidirectional(extractTextForMatching(part), pattern, ArgumentContents.containsVariable(part));
    }

    /// As [#partMatches], but a pattern also matches a name that spells the same image differently.
    /// Both are compared canonically; familiarizing them instead would widen `docker.io/*` to `*`.
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
