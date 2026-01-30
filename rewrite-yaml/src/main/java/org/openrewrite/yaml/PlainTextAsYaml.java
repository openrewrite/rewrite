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
package org.openrewrite.yaml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Option;
import org.openrewrite.Parser;
import org.openrewrite.text.PlainTextAs;
import org.openrewrite.yaml.tree.Yaml;

/**
 * Parse plain text files matching a specified pattern as YAML.
 * <p>
 * This recipe is intended for files with non-standard extensions or names that contain YAML content
 * but are not automatically recognized by OpenRewrite's YAML parser. For files with standard YAML
 * extensions ({@code *.yaml}, {@code *.yml}), configure the YAML parser at LST build time instead.
 * <p>
 * Common use cases include proprietary configuration files or data formats that use YAML syntax
 * but have custom file extensions specific to an organization or application.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class PlainTextAsYaml extends PlainTextAs<Yaml.Documents> {

    @Option(displayName = "File pattern",
            description = "A glob pattern to match files that should be parsed as YAML. " +
                          "This pattern should match files with non-standard extensions that contain YAML content.",
            example = "**/*.myconfig")
    String filePattern;

    @Override
    public String getDisplayName() {
        return "Parse plain text as YAML";
    }

    @Override
    public String getDescription() {
        return "Parse plain text files matching the specified pattern as YAML, so that YAML-specific recipes " +
               "can be applied to them. This is intended for files with non-standard extensions that contain " +
               "YAML content but are not automatically parsed as YAML. For standard YAML file extensions, " +
               "configure the YAML parser during LST build time instead.";
    }

    @Override
    protected String getFilePattern() {
        return filePattern;
    }

    @Override
    protected Parser.Builder getParserBuilder() {
        return YamlParser.builder();
    }
}
