package org.openrewrite.java.search;

import lombok.RequiredArgsConstructor;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaSourceVisitor;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

@RequiredArgsConstructor
public class FindInheritedFields extends JavaSourceVisitor<List<JavaType.Var>> {
    private final String fullyQualifiedClassName;

    @Override
    public List<JavaType.Var> defaultTo(Tree t) {
        return emptyList();
    }

    private List<JavaType.Var> superFields(@Nullable JavaType.Class type) {
        if(type == null || type.getSupertype() == null) {
            return emptyList();
        }
        List<JavaType.Var> types = new ArrayList<>();
        type.getMembers().stream()
                .filter(m -> !m.hasFlags(Flag.Private) && TypeUtils.hasElementType(m.getType(), fullyQualifiedClassName))
                .forEach(types::add);
        types.addAll(superFields(type.getSupertype()));
        return types;
    }

    @Override
    public List<JavaType.Var> visitClassDecl(J.ClassDecl classDecl) {
        JavaType.Class asClass = TypeUtils.asClass(classDecl.getType());
        return superFields(asClass == null ? null : asClass.getSupertype());
    }
}
