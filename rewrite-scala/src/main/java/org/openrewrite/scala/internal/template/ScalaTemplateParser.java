/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.scala.internal.template;

import org.openrewrite.java.internal.template.JavaTemplateParser;
import org.openrewrite.scala.ScalaParser;

import java.util.Set;
import java.util.function.Consumer;

public class ScalaTemplateParser extends JavaTemplateParser {
    public ScalaTemplateParser(boolean contextSensitive, ScalaParser.Builder parser, Consumer<String> onAfterVariableSubstitution, Consumer<String> onBeforeParseTemplate, Set<String> imports) {
        super(
                parser,
                onAfterVariableSubstitution,
                onBeforeParseTemplate,
                imports,
                contextSensitive,
                new ScalaBlockStatementTemplateGenerator(imports, contextSensitive),
                new ScalaAnnotationTemplateGenerator(imports)
        );
    }
}
