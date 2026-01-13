package org.openrewrite.kotlin.internal.template;

import lombok.RequiredArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.java.internal.template.AnnotationTemplateGenerator;
import org.openrewrite.java.tree.JavaSourceFile;

import java.util.Set;

@RequiredArgsConstructor
public class KotlinAnnotationTemplateGenerator extends AnnotationTemplateGenerator {

    public KotlinAnnotationTemplateGenerator(Set<String> imports) {
        super(imports);
    }

    @Override
    public void addDummyClass(Cursor cursor, StringBuilder after) {
        if (cursor.getParentOrThrow().getValue() instanceof JavaSourceFile) {
            after.insert(0, "class Clazz {}");
        } else {
            after.insert(0, "static class Clazz {}");
        }
    }

    @Override
    public void addDummyInterface(StringBuilder after) {
        after.append("\nannotation class Placeholder {}");
    }

}
