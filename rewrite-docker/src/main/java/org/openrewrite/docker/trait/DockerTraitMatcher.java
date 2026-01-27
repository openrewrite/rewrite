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

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

/**
 * Base class for Docker trait matchers providing shared utilities for working with
 * Docker AST elements. Similar to {@link org.openrewrite.gradle.trait.GradleTraitMatcher}.
 *
 * @param <U> The trait type this matcher produces
 */
public abstract class DockerTraitMatcher<U extends Trait<?>> extends SimpleTraitMatcher<U> {

    /**
     * Extracts text from a Docker argument, replacing environment variables with wildcards
     * for glob matching purposes.
     *
     * @param arg The argument to extract text from
     * @return Text with environment variables replaced by '*'
     */
    protected String extractTextForMatching(Docker.Argument arg) {
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
     * Extracts text from a Docker argument, returning null if the argument contains
     * any environment variables (since the actual value cannot be determined statically).
     *
     * @param arg The argument to extract text from, may be null
     * @return The literal text content, or null if the argument is null or contains environment variables
     */
    protected @Nullable String extractText(Docker.@Nullable Argument arg) {
        if (arg == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Docker.ArgumentContent content : arg.getContents()) {
            if (content instanceof Docker.Literal) {
                sb.append(((Docker.Literal) content).getText());
            } else if (content instanceof Docker.EnvironmentVariable) {
                return null;
            }
        }
        return sb.toString();
    }

    /**
     * Extracts text from a Docker argument, including environment variable references
     * in their original form (e.g., ${VAR} or $VAR).
     *
     * @param arg The argument to extract text from, may be null
     * @return The text content with environment variable references preserved, or null if arg is null
     */
    protected @Nullable String extractTextWithVariables(Docker.@Nullable Argument arg) {
        if (arg == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Docker.ArgumentContent content : arg.getContents()) {
            if (content instanceof Docker.Literal) {
                sb.append(((Docker.Literal) content).getText());
            } else if (content instanceof Docker.EnvironmentVariable) {
                Docker.EnvironmentVariable env = (Docker.EnvironmentVariable) content;
                if (env.isBraced()) {
                    sb.append("${").append(env.getName()).append("}");
                } else {
                    sb.append("$").append(env.getName());
                }
            }
        }
        return sb.toString();
    }

    /**
     * Checks if a Docker argument contains any environment variables.
     *
     * @param arg The argument to check, may be null
     * @return true if the argument contains environment variables, false otherwise
     */
    protected boolean hasEnvironmentVariables(Docker.@Nullable Argument arg) {
        if (arg == null) {
            return false;
        }
        for (Docker.ArgumentContent content : arg.getContents()) {
            if (content instanceof Docker.EnvironmentVariable) {
                return true;
            }
        }
        return false;
    }

    /**
     * Performs bidirectional glob matching when environment variables are present.
     * When the text contains wildcards (from env vars), we need to check if either
     * the pattern matches the text OR the text (as a pattern) matches the pattern.
     *
     * @param text The text to match (may contain wildcards from env vars)
     * @param pattern The glob pattern to match against
     * @param hasEnvVars Whether the original text contained environment variables
     * @return true if there's a match in either direction
     */
    protected boolean matchesBidirectional(String text, String pattern, boolean hasEnvVars) {
        if (hasEnvVars) {
            return StringUtils.matchesGlob(text, pattern) ||
                   StringUtils.matchesGlob(pattern, text);
        }
        return StringUtils.matchesGlob(text, pattern);
    }

    /**
     * Gets the quote style from a Docker argument, if any literal content is quoted.
     *
     * @param arg The argument to check
     * @return The quote style, or null if unquoted
     */
    protected Docker.Literal.@Nullable QuoteStyle getQuoteStyle(Docker.Argument arg) {
        for (Docker.ArgumentContent content : arg.getContents()) {
            if (content instanceof Docker.Literal) {
                Docker.Literal.QuoteStyle style = ((Docker.Literal) content).getQuoteStyle();
                if (style != null) {
                    return style;
                }
            }
        }
        return null;
    }

    /**
     * Gets the Docker.File from the cursor path.
     *
     * @param cursor The cursor to search from
     * @return The Docker.File, or null if not found
     */
    protected Docker.@Nullable File getDockerFile(Cursor cursor) {
        SourceFile sourceFile = cursor.firstEnclosing(SourceFile.class);
        if (sourceFile instanceof Docker.File) {
            return (Docker.File) sourceFile;
        }
        return null;
    }

    /**
     * Gets the Docker.Stage containing the current cursor position.
     *
     * @param cursor The cursor to search from
     * @return The containing Stage, or null if not found
     */
    protected Docker.@Nullable Stage getStage(Cursor cursor) {
        return cursor.firstEnclosing(Docker.Stage.class);
    }
}
