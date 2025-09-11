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

import org.openrewrite.graphql.GraphQlIsoVisitor;
import org.openrewrite.graphql.tree.GraphQl;

/**
 * Normalizes formatting in GraphQL documents by ensuring consistent whitespace
 * and removing unnecessary formatting variations.
 */
public class NormalizeFormatVisitor<P> extends GraphQlIsoVisitor<P> {
    
    @Override
    public GraphQl.Field visitField(GraphQl.Field field, P p) {
        GraphQl.Field f = super.visitField(field, p);
        // TODO: Implement format normalization for fields
        return f;
    }
    
    @Override
    public GraphQl.FieldDefinition visitFieldDefinition(GraphQl.FieldDefinition fieldDefinition, P p) {
        GraphQl.FieldDefinition f = super.visitFieldDefinition(fieldDefinition, p);
        // TODO: Implement format normalization for field definitions
        return f;
    }
}