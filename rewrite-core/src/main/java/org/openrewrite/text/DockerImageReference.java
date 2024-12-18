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
            Cursor c = new Cursor(null, Cursor.ROOT_VALUE);

            while (m.find()) {
                references.add(new DockerImageReference(c, m.group(1)));
            }

            return references;
        }
    }
}
