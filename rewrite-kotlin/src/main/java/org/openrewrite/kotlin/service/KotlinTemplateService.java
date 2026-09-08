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
package org.openrewrite.kotlin.service;

import org.openrewrite.java.internal.template.JavaTemplateLanguageExtension;
import org.openrewrite.java.internal.template.JavaTemplateParser;
import org.openrewrite.java.internal.template.Substitutions;
import org.openrewrite.java.service.TemplateService;
import org.openrewrite.java.tree.JavaCoordinates;
import org.openrewrite.kotlin.internal.template.KotlinTemplateJavaExtension;

public class KotlinTemplateService extends TemplateService {

    @Override
    public JavaTemplateLanguageExtension languageExtension(JavaTemplateParser templateParser,
                                                           Substitutions substitutions,
                                                           String substitutedTemplate,
                                                           JavaCoordinates coordinates,
                                                           boolean autoFormat) {
        return new KotlinTemplateJavaExtension(templateParser, substitutions, substitutedTemplate, coordinates, autoFormat);
    }
}
