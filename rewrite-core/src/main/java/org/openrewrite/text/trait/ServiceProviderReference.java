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
package org.openrewrite.text.trait;

import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.text.PlainText;
import org.openrewrite.trait.Reference;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Value
public class ServiceProviderReference implements Reference {
    Cursor cursor;
    Kind kind;
    String value;

    @Override
    public Tree getTree() {
        return cursor.getValue();
    }

    @Override
    public boolean supportsRename() {
        return true;
    }

    @Override
    public Tree rename(Renamer renamer, Cursor cursor, ExecutionContext ctx) {
        PlainText text = cursor.getValue();
        String newValue = renamer.rename(this);

        // Rename matching content lines
        String[] lines = text.getText().split("\n", -1);
        boolean contentChanged = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                sb.append("\n");
            }
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.equals(value)) {
                sb.append(line.replace(value, newValue));
                contentChanged = true;
            } else {
                sb.append(line);
            }
        }

        PlainText result = contentChanged ? text.withText(sb.toString()) : text;

        // Rename source path if the filename matches this reference's value
        String fileName = result.getSourcePath().getFileName().toString();
        if (fileName.equals(value)) {
            Path parent = result.getSourcePath().getParent();
            if (parent != null) {
                result = result.withSourcePath(parent.resolve(newValue));
            }
        }

        return result;
    }

    public static class Provider implements Reference.Provider {
        static final Predicate<String> JAVA_FQCN = Pattern.compile(
                "^\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*" +
                        "(?:\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)+$").asPredicate();

        @Override
        public boolean isAcceptable(SourceFile sourceFile) {
            if (!(sourceFile instanceof PlainText)) {
                return false;
            }
            String path = sourceFile.getSourcePath().toString().replace('\\', '/');
            if (!path.contains("META-INF/services/")) {
                return false;
            }
            String fileName = sourceFile.getSourcePath().getFileName().toString();
            return JAVA_FQCN.test(fileName);
        }

        @Override
        public Set<Reference> getReferences(SourceFile sourceFile) {
            PlainText plainText = (PlainText) sourceFile;
            Set<Reference> references = new HashSet<>();
            Cursor cursor = new Cursor(new Cursor(null, Cursor.ROOT_VALUE), plainText);

            // Reference for the filename (the service interface FQCN)
            String fileName = plainText.getSourcePath().getFileName().toString();
            if (JAVA_FQCN.test(fileName)) {
                references.add(new ServiceProviderReference(cursor, Reference.Kind.TYPE, fileName));
            }

            // References for each implementation class in the content
            for (String line : plainText.getText().split("\n", -1)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#") && JAVA_FQCN.test(trimmed)) {
                    references.add(new ServiceProviderReference(cursor, Reference.Kind.TYPE, trimmed));
                }
            }

            return references;
        }
    }
}
