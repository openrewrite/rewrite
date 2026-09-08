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
package org.openrewrite.java.service;

import org.openrewrite.Incubating;
import org.openrewrite.java.internal.template.*;
import org.openrewrite.java.tree.JavaCoordinates;

/**
 * Supplies the {@link JavaTemplateLanguageExtension} that performs the tree surgery for a template application.
 * <p>
 * This is resolved from the <em>source file being modified</em>, not from the {@code JavaTemplate} subclass the
 * recipe author instantiated. The two are independent: a recipe may legitimately apply a Java snippet via
 * {@code JavaTemplate} to a Kotlin source file, and that still needs Kotlin-shaped surgery — Kotlin renders
 * supertypes after a single {@code :}, and holds top-level declarations directly on the compilation unit rather
 * than in a {@code J.Block}. The snippet's own language is handled separately by {@link TemplateStubs}.
 */
@Incubating(since = "8.92.0")
public class TemplateService {

    public JavaTemplateLanguageExtension languageExtension(JavaTemplateParser templateParser,
                                                           Substitutions substitutions,
                                                           String substitutedTemplate,
                                                           JavaCoordinates coordinates,
                                                           boolean autoFormat) {
        return new JavaTemplateJavaExtension(templateParser, substitutions, substitutedTemplate, coordinates, autoFormat);
    }
}
