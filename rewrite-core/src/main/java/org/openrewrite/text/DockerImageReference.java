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
package org.openrewrite.text;

import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.trait.Reference;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Value
public class DockerImageReference implements Reference {
    Cursor cursor;
    String value;

    @Override
    public Kind getKind() {
        return Kind.IMAGE;
    }

    public static class Provider implements Reference.Provider {
        private static final Pattern FROM = Pattern.compile("FROM\\s+([\\w:.-]*)", Pattern.CASE_INSENSITIVE);

        @Override
        public boolean isAcceptable(SourceFile sourceFile) {
            if (sourceFile instanceof PlainText) {
                PlainText text = (PlainText) sourceFile;
                String fileName = text.getSourcePath().toFile().getName();
                return (fileName.endsWith("Dockerfile") || fileName.equals("Containerfile")) && FROM.matcher(text.getText()).find();
            }
            return false;
        }

        @Override
        public Set<Reference> getReferences(SourceFile sourceFile) {
            Set<Reference> references = new HashSet<>();
            java.util.regex.Matcher m = FROM.matcher(((PlainText) sourceFile).getText());
            Cursor c = new Cursor(new Cursor(null, Cursor.ROOT_VALUE), sourceFile);

            while (m.find()) {
                references.add(new DockerImageReference(c, m.group(1)));
            }

            return references;
        }
    }
}
