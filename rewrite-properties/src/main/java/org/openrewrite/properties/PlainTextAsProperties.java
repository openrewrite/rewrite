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
package org.openrewrite.properties;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Option;
import org.openrewrite.Parser;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.text.PlainTextAs;

/**
 * Parse plain text files matching a specified pattern as Java Properties.
 * <p>
 * This recipe is intended for files with non-standard extensions or names that contain properties content
 * but are not automatically recognized by OpenRewrite's Properties parser. For files with standard
 * properties extensions ({@code *.properties}), configure the Properties parser at LST build time instead.
 * <p>
 * Common use cases include proprietary configuration files or data formats that use properties syntax
 * but have custom file extensions specific to an organization or application.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class PlainTextAsProperties extends PlainTextAs<Properties.File> {

    @Option(displayName = "File pattern",
            description = "A glob pattern to match files that should be parsed as Properties. " +
                          "This pattern should match files with non-standard extensions that contain properties content.",
            example = "**/*.myconfig")
    String filePattern;

    @Override
    public String getDisplayName() {
        return "Parse plain text as Properties";
    }

    @Override
    public String getDescription() {
        return "Parse plain text files matching the specified pattern as Java Properties, so that Properties-specific " +
               "recipes can be applied to them. This is intended for files with non-standard extensions that contain " +
               "properties content but are not automatically parsed as Properties. For standard properties file " +
               "extensions, configure the Properties parser during LST build time instead.";
    }

    @Override
    protected String getFilePattern() {
        return filePattern;
    }

    @Override
    protected Parser.Builder getParserBuilder() {
        return PropertiesParser.builder();
    }
}
