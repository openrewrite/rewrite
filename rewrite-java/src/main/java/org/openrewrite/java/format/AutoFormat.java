/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.format;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.style.NamedStyles;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Properties;

@Value
@EqualsAndHashCode(callSuper = false)
public class AutoFormat extends Recipe {

    @Option(displayName = "Style",
            description = "See https://docs.openrewrite.org/concepts-and-explanations/styles for a description on styles.",
            example = "type: specs.openrewrite.org/v1beta/style\n" +
                    "name: com.yourorg.YesTabsNoStarImports\n" +
                    "styleConfigs:\n" +
                    "  - org.openrewrite.java.style.TabsAndIndentsStyle:\n" +
                    "      useTabCharacter: true",
            required = false)
    @Nullable
    String style;

    @Override
    public String getDisplayName() {
        return "Format Java code";
    }

    @Override
    public String getDescription() {
        return "Format Java code using a standard comprehensive set of Java formatting recipes.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AutoFormatVisitor<>(null, computeNamedStyles());
    }

    private NamedStyles[] computeNamedStyles() {
        if (style == null) {
            return new NamedStyles[0];
        }

        return new YamlResourceLoader(new ByteArrayInputStream(style.getBytes()), URI.create("AutoFormat$style"), new Properties()).listStyles().toArray(new NamedStyles[0]);
    }
}
