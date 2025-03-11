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
package org.openrewrite.kotlin.internal.template;

import org.openrewrite.java.internal.template.AnnotationTemplateGenerator;
import org.openrewrite.java.internal.template.JavaTemplateParser;
import org.openrewrite.kotlin.KotlinParser;

import java.util.Set;
import java.util.function.Consumer;

public class KotlinTemplateParser extends JavaTemplateParser {
    public KotlinTemplateParser(boolean contextSensitive, KotlinParser.Builder parser, Consumer<String> onAfterVariableSubstitution, Consumer<String> onBeforeParseTemplate, Set<String> imports) {
        super(
                parser,
                onAfterVariableSubstitution,
                onBeforeParseTemplate,
                imports,
                contextSensitive,
                new KotlinBlockStatementTemplateGenerator(imports, contextSensitive),
                new AnnotationTemplateGenerator(imports)
        );
    }
}
