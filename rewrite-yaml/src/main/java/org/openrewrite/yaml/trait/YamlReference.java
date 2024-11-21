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

import lombok.Value;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.trait.Reference;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.yaml.tree.Yaml;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Value
class YamlReference implements Reference {
    Cursor cursor;
    Kind kind;

    @Override
    public @NonNull Kind getKind() {
        return kind;
    }

    @Override
    public @NonNull String getValue() {
        if (getTree() instanceof Yaml.Scalar) {
            return ((Yaml.Scalar) getTree()).getValue();
        }
        throw new IllegalArgumentException("getTree() must be an Yaml.Document: " + getTree().getClass());
    }

    @Override
    public boolean supportsRename() {
        return true;
    }

    public static class Matcher extends SimpleTraitMatcher<YamlReference> {
        private static final Pattern javaFullyQualifiedTypePattern = Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(?:\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");

        @Override
        protected @Nullable YamlReference test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof Yaml.Scalar && javaFullyQualifiedTypePattern.matcher(((Yaml.Scalar) value).getValue()).matches()) {
                return new YamlReference(cursor, determineKind(((Yaml.Scalar) value).getValue()));
            }
            return null;
        }

        private Kind determineKind(String value) {
            return Character.isUpperCase(value.charAt(value.lastIndexOf('.') + 1)) ? Kind.TYPE : Kind.PACKAGE;
        }
    }

    @SuppressWarnings("unused")
    public static class Provider implements Reference.Provider {

        @Override
        public @NonNull Set<Reference> getReferences(SourceFile sourceFile) {
            Set<Reference> references = new HashSet<>();
            new Matcher().asVisitor(reference -> {
                references.add(reference);
                return reference.getTree();
            }).visit(sourceFile, 0);
            return references;
        }

        @Override
        public boolean isAcceptable(SourceFile sourceFile) {
            return sourceFile instanceof Yaml.Documents;
        }
    }
}
