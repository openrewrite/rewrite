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
package org.openrewrite.yaml.trait;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.yaml.JsonPathMatcher;
import org.openrewrite.yaml.tree.Yaml;

@AllArgsConstructor
public class YamlValue implements Trait<Yaml.Mapping.Entry> {
    @Getter
    Cursor cursor;

    public String getKey() {
        return getTree().getKey().getValue();
    }

    public Yaml.Scalar getKeyAsScalar() {
        return (Yaml.Scalar) (getTree().getKey());
    }

    public YamlValue withKey(String newKey) {
        Yaml.Scalar key = getKeyAsScalar().withValue(newKey);
        cursor = new Cursor(cursor.getParent(), getTree().withKey(key));
        return this;
    }

    public String getValue() {
        return getValueAsScalar().getValue();
    }

    public Yaml.Scalar getValueAsScalar() {
        return (Yaml.Scalar) (getTree().getValue());
    }

    public YamlValue withValue(String newValue) {
        Yaml.Scalar value = getValueAsScalar().withValue(newValue);
        cursor = new Cursor(cursor.getParent(), getTree().withValue(value));
        return this;
    }

    public static class Matcher extends SimpleTraitMatcher<YamlValue> {
        private final JsonPathMatcher jsonPathMatcher;

        public Matcher(String jsonPath) {
            this.jsonPathMatcher = new JsonPathMatcher(jsonPath);
        }

        @Override
        protected @Nullable YamlValue test(Cursor cursor) {
            if (jsonPathMatcher.matches(cursor) &&
                cursor.getValue() instanceof Yaml.Mapping.Entry &&
                ((Yaml.Mapping.Entry) cursor.getValue()).getValue() instanceof Yaml.Scalar) {
                return new YamlValue(cursor);
            }
            return null;
        }
    }
}
