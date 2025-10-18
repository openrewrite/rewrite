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
import org.openrewrite.graphql.style.IndentsStyle;
import org.openrewrite.graphql.tree.GraphQl;

/**
 * Manages indentation in GraphQL documents according to the specified style.
 */
public class IndentsVisitor<P> extends GraphQlIsoVisitor<P> {
    private final IndentsStyle style;
    
    @Nullable
    private final Tree stopAfter;

    public IndentsVisitor(IndentsStyle style) {
        this(style, null);
    }

    public IndentsVisitor(IndentsStyle style, @Nullable Tree stopAfter) {
        this.style = style;
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
        // TODO: Implement indentation logic for fields
        return f;
    }
    
    @Override
    public GraphQl.FieldDefinition visitFieldDefinition(GraphQl.FieldDefinition fieldDefinition, P p) {
        GraphQl.FieldDefinition f = super.visitFieldDefinition(fieldDefinition, p);
        // TODO: Implement indentation logic for field definitions
        return f;
    }
}