/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.tree.Properties;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.internal.NameCaseConvention.LOWER_CAMEL;

@Value
@EqualsAndHashCode(callSuper = false)
public class DeleteProperty extends Recipe {

    @Override
    public String getDisplayName() {
        return "Delete Property";
    }

    @Override
    public String getDescription() {
        return "Deletes key/value pairs from properties files.";
    }

    @Option(displayName = "Property key matcher",
            description = "The key(s) to be deleted. This is a glob expression.",
            example = "management.metrics.binders.files.enabled or management.metrics.*")
    String propertyKey;

    @Option(displayName = "Use relaxed binding",
            description = "Whether to match the `propertyKey` using [relaxed binding](https://docs.spring.io/spring-boot/docs/2.5.6/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding) " +
                    "rules. Default is `true`. Set to `false`  to use exact matching.",
            required = false)
    @Nullable
    Boolean relaxedBinding;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PropertiesVisitor<ExecutionContext>() {
            @Override
            public Properties visitFile(Properties.File file, ExecutionContext executionContext) {
                Properties.File f = (Properties.File) super.visitFile(file, executionContext);

                String prefix = null;
                List<Properties.Content> contents = f.getContent();
                List<Properties.Content> newContents = new ArrayList<>();
                for (int i = 0; i < contents.size(); i++) {
                    Properties.Content content = contents.get(i);
                    if (content instanceof Properties.Entry && isMatch(((Properties.Entry) content).getKey())) {
                        if (i == 0) {
                            prefix = ((Properties.Entry) content).getPrefix();
                        }
                    } else {
                        if (prefix != null) {
                            content = (Properties.Content) content.withPrefix(prefix);
                            prefix = null;
                        }
                        newContents.add(content);
                    }
                }

                return contents.size() == newContents.size() ? f : f.withContent(newContents);
            }

            private boolean isMatch(String key) {
                if (!Boolean.FALSE.equals(relaxedBinding)) {
                    return StringUtils.matchesGlob(LOWER_CAMEL.format(key), LOWER_CAMEL.format(propertyKey));
                } else {
                    return StringUtils.matchesGlob(key, propertyKey);
                }
            }
        };
    }
}
