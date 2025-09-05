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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.properties.tree.Properties;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.openrewrite.internal.NameCaseConvention.LOWER_CAMEL;

@Value
@EqualsAndHashCode(callSuper = false)
public class DeleteProperty extends Recipe {

    @Override
    public String getDisplayName() {
        return "Delete property by key";
    }

    @Override
    public String getDescription() {
        return "Deletes key/value pairs from properties files, as well as any comments that immediately precede the key/value pair. " +
                "Comments separated by two or more newlines from the deleted key/value pair are preserved." ;
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
            public Properties visitFile(Properties.File file, ExecutionContext ctx) {
                Properties.File f1 = (Properties.File) super.visitFile(file, ctx);
                AtomicReference<@Nullable String> prefixOnNextEntry = new AtomicReference<>(null);
                AtomicBoolean deleted = new AtomicBoolean(false);
                Properties.File mapped = f1.withContent(ListUtils.map(f1.getContent(), (index, current) -> {
                    if (current instanceof Properties.Comment && nextEntryMatches(f1.getContent(), index)) {
                        prefixOnNextEntry.compareAndSet(null, current.getPrefix());
                        return null;
                    }
                    if (isMatch(current)) {
                        deleted.set(true);
                        return null;
                    }
                    if (deleted.getAndSet(false)) {
                        String prefix = prefixOnNextEntry.getAndSet(null);
                        if (prefix != null) {
                            return (Properties.Content) current.withPrefix(prefix);
                        }
                    }
                    return current;
                }));
                if (f1 != mapped) {
                    return mapped.withContent(ListUtils.mapFirst(mapped.getContent(), c -> (Properties.Content) c.withPrefix("")));
                }
                return mapped;
            }

            private boolean isMatch(Properties.Content current) {
                if (current instanceof Properties.Entry) {
                    String key = ((Properties.Entry) current).getKey();
                    if (Boolean.FALSE.equals(relaxedBinding)) {
                        return StringUtils.matchesGlob(key, propertyKey);
                    }
                    return StringUtils.matchesGlob(LOWER_CAMEL.format(key), LOWER_CAMEL.format(propertyKey));
                }
                return false;
            }

            /**
             * @return true if the next entry not separated by two or more newlines matches the property key.
             */
            private boolean nextEntryMatches(List<Properties.Content> contents, int index) {
                while (++index < contents.size()) {
                    Properties.Content next = contents.get(index);
                    if (next.getPrefix().matches("\\R{2,}")) {
                        return false; // Two or more newlines, stop checking.
                    }
                    if (isMatch(next)) {
                        return true;
                    }
                    if (next instanceof Properties.Entry) {
                        return false; // Unrelated entry, stop checking.
                    }
                }
                return false;
            }
        };
    }
}
