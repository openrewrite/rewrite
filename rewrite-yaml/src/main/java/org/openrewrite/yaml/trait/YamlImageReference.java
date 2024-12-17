/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.yaml.trait;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.yaml.tree.Yaml;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class YamlImageReference extends YamlReference {
    Cursor cursor;

    @Override
    public Kind getKind() {
        return Kind.IMAGE;
    }

    public static class Provider extends YamlProvider {
        @Override
        SimpleTraitMatcher<YamlReference> getMatcher() {
            return new SimpleTraitMatcher<YamlReference>() {
                private final Predicate<String> image = Pattern.compile("image").asPredicate();
                private final AtomicBoolean found = new AtomicBoolean(false);

                @Override
                protected @Nullable YamlReference test(Cursor cursor) {
                    Object value = cursor.getValue();
                    if (value instanceof Yaml.Scalar) {
                        if (image.test(((Yaml.Scalar) value).getValue())) {
                            found.set(true);
                        } else if (found.get()) {
                            found.set(false);
                            return new YamlImageReference(cursor);
                        }
                    }
                    return null;
                }
            };
        }
    }
}
