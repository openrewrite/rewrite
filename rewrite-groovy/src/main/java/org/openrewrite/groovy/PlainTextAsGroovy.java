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
package org.openrewrite.groovy;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Option;
import org.openrewrite.Parser;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.text.PlainTextAs;

/**
 * Parse plain text files matching a specified pattern as Groovy.
 * <p>
 * This recipe is intended for files with non-standard extensions or names that contain Groovy code
 * but are not automatically recognized by OpenRewrite's Groovy parser. For files with standard Groovy
 * extensions ({@code *.groovy}, {@code *.gradle}), configure the Groovy parser at LST build time instead.
 * <p>
 * Common use cases include:
 * <ul>
 *   <li>Jenkinsfiles (which are Groovy scripts without a {@code .groovy} extension)</li>
 *   <li>Proprietary script files that use Groovy syntax but have custom extensions</li>
 * </ul>
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class PlainTextAsGroovy extends PlainTextAs<G.CompilationUnit> {

    @Option(displayName = "File pattern",
            description = "A glob pattern to match files that should be parsed as Groovy. " +
                          "This pattern should match files with non-standard extensions that contain Groovy code.",
            example = "**/Jenkinsfile")
    String filePattern;

    @Override
    public String getDisplayName() {
        return "Parse plain text as Groovy";
    }

    @Override
    public String getDescription() {
        return "Parse plain text files matching the specified pattern as Groovy, so that Groovy-specific recipes " +
               "can be applied to them. This is intended for files with non-standard extensions that contain " +
               "Groovy code but are not automatically parsed as Groovy (e.g., Jenkinsfiles). For standard Groovy " +
               "file extensions, configure the Groovy parser during LST build time instead.";
    }

    @Override
    protected String getFilePattern() {
        return filePattern;
    }

    @Override
    protected Parser.Builder getParserBuilder() {
        return GroovyParser.builder();
    }
}
