package org.openrewrite.text;

import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.trait.Reference;
import org.openrewrite.trait.SimpleTraitMatcher;

@Value
public class DockerImageReference implements Reference {
    Cursor cursor;
    String value;

    @Override
    public Kind getKind() {
        return Kind.IMAGE;
    }

    // TODO:
    // - casing: https://docs.docker.com/reference/build-checks/from-as-casing/
    // - multi-stage: https://stackoverflow.com/questions/33322103/multiple-froms-what-it-means
    public static class Provider extends AbstractProvider<DockerImageReference> {
        @Override
        public boolean isAcceptable(SourceFile sourceFile) {
            if (sourceFile instanceof PlainText) {
                PlainText text = (PlainText) sourceFile;
                String fileName = text.getSourcePath().toFile().getName();
                return (fileName.endsWith("Dockerfile") || fileName.equals("Containerfile")) && text.getText().contains("FROM");
            }
            return false;
        }

        @Override
        public SimpleTraitMatcher<DockerImageReference> getMatcher() {
            return new SimpleTraitMatcher<DockerImageReference>() {
                @Override
                protected DockerImageReference test(Cursor cursor) {
                    String text = ((PlainText) cursor.getValue()).getText();
                    int index = StringUtils.indexOfNextNonWhitespace(text.indexOf("FROM") + 4, text);
                    StringBuilder image = new StringBuilder();
                    for (char c : text.substring(index).toCharArray()) {
                        if (Character.isWhitespace(c)) {
                            break;
                        }
                        image.append(c);
                    }
                    return new DockerImageReference(cursor, image.toString());
                }
            };
        }
    }
}
