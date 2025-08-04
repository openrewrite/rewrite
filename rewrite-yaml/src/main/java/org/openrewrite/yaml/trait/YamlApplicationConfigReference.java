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
import org.openrewrite.SourceFile;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.yaml.tree.Yaml;

import java.util.function.Predicate;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class YamlApplicationConfigReference extends YamlReference {
    Cursor cursor;
    Kind kind;

    public static class Provider extends YamlProvider {
        private static final Predicate<String> applicationPropertiesMatcher = Pattern.compile("^application(-\\w+)?\\.(yaml|yml)$").asPredicate();
        private static final SimpleTraitMatcher<YamlReference> matcher = new SimpleTraitMatcher<YamlReference>() {
            private final Predicate<String> javaFullyQualifiedTypePattern = Pattern.compile(
                            "^\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*" +
                                    "\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*" +
                                    "(?:\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*$").asPredicate();

            @Override
            protected @Nullable YamlReference test(Cursor cursor) {
                Object value = cursor.getValue();
                if (value instanceof Yaml.Scalar && javaFullyQualifiedTypePattern.test(((Yaml.Scalar) value).getValue())) {
                    return new YamlApplicationConfigReference(cursor, determineKind(((Yaml.Scalar) value).getValue()));
                }
                return null;
            }

            private Kind determineKind(String value) {
                return Character.isUpperCase(value.charAt(value.lastIndexOf('.') + 1)) ? Kind.TYPE : Kind.PACKAGE;
            }
        };

        @Override
        public boolean isAcceptable(SourceFile sourceFile) {
            return super.isAcceptable(sourceFile) && applicationPropertiesMatcher.test(sourceFile.getSourcePath().getFileName().toString());
        }

        @Override
        public SimpleTraitMatcher<YamlReference> getMatcher() {
            return matcher;
        }
    }
}
