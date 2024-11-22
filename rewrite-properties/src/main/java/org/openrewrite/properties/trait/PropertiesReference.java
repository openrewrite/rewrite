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
package org.openrewrite.properties.trait;

import lombok.Value;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.SourceFile;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.trait.Reference;
import org.openrewrite.trait.SimpleTraitMatcher;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Incubating(since = "8.40.3")
@Value
public class PropertiesReference implements Reference {
    Cursor cursor;
    Kind kind;

    @Override
    public @NonNull Kind getKind() {
        return kind;
    }

    @Override
    public @NonNull String getValue() {
        if (getTree() instanceof Properties.Entry) {
            return ((Properties.Entry) getTree()).getValue().getText();
        }
        throw new IllegalArgumentException("getTree() must be an Properties.Entry: " + getTree().getClass());
    }

    @Override
    public boolean supportsRename() {
        return true;
    }

    public static class Matcher extends SimpleTraitMatcher<PropertiesReference> {
        private static final Pattern javaFullyQualifiedTypePattern = Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(?:\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");

        @Override
        protected @Nullable PropertiesReference test(Cursor cursor) {
            Object value = cursor.getValue();
            if (value instanceof Properties.Entry && javaFullyQualifiedTypePattern.matcher(((Properties.Entry) value).getValue().getText()).matches()) {
                return new PropertiesReference(cursor, determineKind(((Properties.Entry) value).getValue().getText()));
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
            return sourceFile instanceof Properties.File;
        }
    }
}
