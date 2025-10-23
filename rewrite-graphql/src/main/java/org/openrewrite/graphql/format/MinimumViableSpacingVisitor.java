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
package org.openrewrite.graphql.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.graphql.GraphQlIsoVisitor;
import org.openrewrite.graphql.tree.GraphQl;
import org.openrewrite.graphql.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.List;

public class MinimumViableSpacingVisitor<P> extends GraphQlIsoVisitor<P> {

    @Nullable
    private final Tree stopAfter;

    public MinimumViableSpacingVisitor() {
        this(null);
    }

    public MinimumViableSpacingVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public @Nullable GraphQl postVisit(GraphQl tree, P p) {
        if (stopAfter != null && stopAfter.isScope(tree)) {
            getCursor().putMessageOnFirstEnclosing(GraphQl.Document.class, "stop", true);
        }
        return super.postVisit(tree, p);
    }

    @Override
    public @Nullable GraphQl visit(@Nullable Tree tree, P p) {
        if (getCursor().getNearestMessage("stop") != null) {
            return (GraphQl) tree;
        }
        return super.visit(tree, p);
    }

    @Override
    public GraphQl.Field visitField(GraphQl.Field field, P p) {
        GraphQl.Field f = super.visitField(field, p);
        
        // Ensure minimum spacing after field name
        if (f.getArguments() != null && f.getArguments().getPrefix().getWhitespace().isEmpty()) {
            f = f.withArguments(f.getArguments().withPrefix(Space.EMPTY));
        }
        
        // Ensure minimum spacing before directives
        if (f.getDirectives() != null && !f.getDirectives().isEmpty()) {
            List<GraphQl.Directive> directives = f.getDirectives();
            if (!directives.isEmpty() && directives.get(0).getPrefix().getWhitespace().isEmpty()) {
                directives.set(0, directives.get(0).withPrefix(Space.build(" ", directives.get(0).getPrefix().getComments())));
                f = f.withDirectives(directives);
            }
        }
        
        return f;
    }

    @Override
    public GraphQl.NamedType visitNamedType(GraphQl.NamedType namedType, P p) {
        GraphQl.NamedType n = super.visitNamedType(namedType, p);
        
        // Ensure no spacing within type names
        if (!n.getPrefix().getWhitespace().isEmpty() && getCursor().getParent() != null) {
            n = n.withPrefix(Space.EMPTY);
        }
        
        return n;
    }

    // TODO: Add union support when GraphQl.Union is implemented
    // @Override
    // public GraphQl.Union visitUnion(GraphQl.Union union, P p) {
    //     GraphQl.Union u = super.visitUnion(union, p);
    //     
    //     // Ensure spacing around = and | operators
    //     if (u.getEqualPrefix() != null && u.getEqualPrefix().getWhitespace().isEmpty()) {
    //         u = u.withEqualPrefix(Space.SINGLE_SPACE);
    //     }
    //     
    //     if (u.getMembers() != null) {
    //         List<GraphQl.RightPadded<GraphQl.NamedType>> members = u.getMembers().getPadding().getElements();
    //         for (int i = 0; i < members.size(); i++) {
    //             GraphQl.RightPadded<GraphQl.NamedType> member = members.get(i);
    //             if (i > 0 && member.getElement().getPrefix().getWhitespace().isEmpty()) {
    //                 members.set(i, member.withElement(member.getElement().withPrefix(Space.SINGLE_SPACE)));
    //             }
    //         }
    //         u = u.withMembers(u.getMembers().getPadding().withElements(members));
    //     }
    //     
    //     return u;
    // }

    // TODO: Add TypeDefinition support when proper visitor method is available
    // @Override
    // public GraphQl.TypeDefinition visitTypeDefinition(GraphQl.TypeDefinition typeDefinition, P p) {
    //     GraphQl.TypeDefinition t = super.visitTypeDefinition(typeDefinition, p);
    //     
    //     // Ensure spacing around implements
    //     if (t.getImplementz() != null && !t.getImplementz().isEmpty()) {
    //         List<GraphQl.NamedType> implementz = t.getImplementz();
    //         if (!implementz.isEmpty() && implementz.get(0).getPrefix().getWhitespace().isEmpty()) {
    //             implementz.set(0, implementz.get(0).withPrefix(Space.build(" ", implementz.get(0).getPrefix().getComments())));
    //             t = t.withImplementz(implementz);
    //         }
    //     }
    //     
    //     return t;
    // }
}