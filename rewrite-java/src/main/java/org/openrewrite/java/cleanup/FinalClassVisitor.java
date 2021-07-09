package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.cleanup.ModifierOrder.sortModifiers;

public class FinalClassVisitor extends JavaIsoVisitor<ExecutionContext> {
    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDeclaration, ExecutionContext executionContext) {
        J.ClassDeclaration cd = super.visitClassDeclaration(classDeclaration, executionContext);
        getCursor().dropParentUntil(it -> it instanceof J.ClassDeclaration && ((J.ClassDeclaration)it))
        if(cd.hasModifier(J.Modifier.Type.Final) || cd.getKind() != J.ClassDeclaration.Kind.Type.Class) {
            return cd;
        }

        boolean allPrivate = true;
        List<J.MethodDeclaration> constructors = new ArrayList<>();
        for(Statement s : cd.getBody().getStatements()) {
            if(s instanceof J.MethodDeclaration && ((J.MethodDeclaration)s).isConstructor()) {
                J.MethodDeclaration constructor = (J.MethodDeclaration)s;
                constructors.add(constructor);
                if(!constructor.hasModifier(J.Modifier.Type.Private)) {
                    allPrivate = false;
                }
            }
        }

        if(constructors.size() > 0 && allPrivate) {
            List<J.Modifier> modifiers = new ArrayList<>(cd.getModifiers());
            modifiers.add(new J.Modifier(randomId(), Space.EMPTY, Markers.EMPTY,  J.Modifier.Type.Final, emptyList()));
            modifiers = sortModifiers(modifiers);
            cd = cd.withModifiers(modifiers);

            assert getCursor().getParent() != null;
            cd = autoFormat(cd, cd.getName(), executionContext, getCursor().getParent());
        }
        return cd;
    }
}
