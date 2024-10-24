package org.openrewrite.xml.internal;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openrewrite.xml.trait.JavaTypeReference;
import org.openrewrite.xml.tree.Xml;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class JavaTypeReferences {
    private final Xml.Document cu;
    private final Set<JavaTypeReference> typeReferences;

    public static JavaTypeReferences build(Xml.Document doc) {
        Set<JavaTypeReference> typeReferences = new HashSet<>();
        new JavaTypeReference.Matcher().asVisitor(reference -> {
            typeReferences.add(reference);
            return reference.getTree();
        }).visit(doc, null);
        return new JavaTypeReferences(doc, typeReferences);
    }
}
