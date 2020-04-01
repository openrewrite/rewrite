package org.openrewrite.java.search;

import lombok.RequiredArgsConstructor;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaSourceVisitor;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@RequiredArgsConstructor
public class FindType extends JavaSourceVisitor<Set<NameTree>> {
    private final String clazz;

    @Override
    public Set<NameTree> defaultTo(Tree t) {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    @Override
    public Set<NameTree> reduce(Set<NameTree> r1, Set<NameTree> r2) {
        r1.addAll(r2);
        return r1;
    }

    @Override
    public Set<NameTree> visitTypeName(NameTree name) {
        JavaType.Class asClass = TypeUtils.asClass(name.getType());
        if(asClass != null && asClass.getFullyQualifiedName().equals(clazz)) {
            Set<NameTree> names = defaultTo(name);
            names.add(name);
            return names;
        }

        return super.visitTypeName(name);
    }
}
