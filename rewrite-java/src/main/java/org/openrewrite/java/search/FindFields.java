/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindFields extends Recipe {
    @Option(displayName = "Fully-qualified type name",
            description = "A fully-qualified Java type name, that is used to find matching fields.",
            example = "com.fasterxml.jackson.core.json.JsonWriteFeature")
    String fullyQualifiedTypeName;

    @Option(displayName = "Match inherited",
            description = "When enabled, find types that inherit from a deprecated type.",
            required = false)
    @Nullable
    Boolean matchInherited;

    @Option(displayName = "Field name",
            description = "The name of a field on the type.",
            example = "QUOTE_FIELD_NAMES")
    String fieldName;

    @Override
    public String getDisplayName() {
        return "Find fields";
    }

    @Override
    public String getInstanceNameSuffix() {
        return "on types `" + fullyQualifiedTypeName + "`";
    }

    @Override
    public String getDescription() {
        return "Find uses of a field.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesField<>(fullyQualifiedTypeName, fieldName), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                JavaType.Variable varType = fieldAccess.getName().getFieldType();
                if (varType != null && new TypeMatcher(fullyQualifiedTypeName, Boolean.TRUE.equals(matchInherited)).matches(varType.getOwner()) &&
                    StringUtils.matchesGlob(varType.getName(), fieldName)) {
                    return SearchResult.found(fieldAccess);
                }
                return super.visitFieldAccess(fieldAccess, ctx);
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                J.Identifier i = super.visitIdentifier(identifier, ctx);
                JavaType.Variable varType = identifier.getFieldType();
                if (varType != null && new TypeMatcher(fullyQualifiedTypeName, Boolean.TRUE.equals(matchInherited)).matches(varType.getOwner()) &&
                    StringUtils.matchesGlob(varType.getName(), fieldName)) {
                    i = SearchResult.found(i);
                }
                return i;
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
                J.MemberReference m = super.visitMemberReference(memberRef, ctx);
                JavaType.Variable varType = memberRef.getVariableType();
                if (varType != null && new TypeMatcher(fullyQualifiedTypeName, Boolean.TRUE.equals(matchInherited)).matches(varType.getOwner()) &&
                    StringUtils.matchesGlob(varType.getName(), fieldName)) {
                    m = m.withReference(SearchResult.found(m.getReference()));
                }
                return m;
            }
        });
    }

    public static Set<J> find(J j, String fullyQualifiedTypeName, String fieldName) {
        JavaVisitor<Set<J>> findVisitor = new JavaIsoVisitor<Set<J>>() {
            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Set<J> vs) {
                J.FieldAccess f = super.visitFieldAccess(fieldAccess, vs);
                JavaType.Variable varType = fieldAccess.getName().getFieldType();
                if (varType != null && new TypeMatcher(fullyQualifiedTypeName, true).matches(varType.getOwner()) &&
                    StringUtils.matchesGlob(varType.getName(), fieldName)) {
                    vs.add(f);
                }
                return f;
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Set<J> vs) {
                J.Identifier i = super.visitIdentifier(identifier, vs);
                JavaType.Variable varType = identifier.getFieldType();
                if (varType != null && new TypeMatcher(fullyQualifiedTypeName, true).matches(varType.getOwner()) &&
                    StringUtils.matchesGlob(varType.getName(), fieldName)) {
                    vs.add(i);
                }
                return i;
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, Set<J> vs) {
                J.MemberReference m = super.visitMemberReference(memberRef, vs);
                JavaType.Variable varType = memberRef.getVariableType();
                if (varType != null && new TypeMatcher(fullyQualifiedTypeName, true).matches(varType.getOwner()) &&
                    StringUtils.matchesGlob(varType.getName(), fieldName)) {
                    vs.add(m);
                }
                return m;
            }
        };

        Set<J> vs = new HashSet<>();
        findVisitor.visit(j, vs);
        return vs;
    }
}
