package org.openrewrite.java.search;

import org.openrewrite.Tree;
import org.openrewrite.java.JavaSourceVisitor;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;

public class FindReferencedTypes extends JavaSourceVisitor<Set<JavaType.Class>> {
    @Override
    public Set<JavaType.Class> defaultTo(Tree t) {
        return emptySet();
    }

    @Override
    public Set<JavaType.Class> visitTypeName(NameTree name) {
        Set<JavaType.Class> referenced = new HashSet<>(super.visitTypeName(name));
        JavaType.Class asClass = TypeUtils.asClass(name.getType());
        if (asClass != null) {
            referenced.add(asClass);
        }
        return referenced;
    }
}
