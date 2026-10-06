/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.internal.RemoveStatementsVisitor;
import org.openrewrite.gradle.trait.ExtraProperty;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveExtraProperty extends Recipe {

    String displayName = "Remove Extra Property";

    String description = "Gradle's [ExtraPropertiesExtension](https://docs.gradle.org/current/dsl/org.gradle.api.plugins.ExtraPropertiesExtension.html) " +
            "is a commonly used mechanism for setting arbitrary key/value pairs on a project. " +
            "This recipe removes every assignment of the property with the given key, whatever its value, " +
            "such as `ext.foo = 'bar'`, `ext['foo'] = 'bar'`, `ext.set('foo', 'bar')`, an assignment in an `ext { }` block, " +
            "or Kotlin's `extra[\"foo\"] = \"bar\"`. An `ext { }` block left empty is removed as well. " +
            "Reads of the property are left as they are.";

    @Option(displayName = "Key",
            description = "The key of the property to remove.",
            example = "foo")
    String key;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new RemoveStatementsVisitor<ExecutionContext>() {
            @Override
            public Statement visitStatement(Statement statement, ExecutionContext ctx) {
                if ((statement instanceof J.Assignment || statement instanceof J.MethodInvocation) &&
                    key.equals(ExtraProperty.Matcher.assignedProperty(getCursor()))) {
                    remove(statement);
                    return statement;
                }
                return super.visitStatement(statement, ctx);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (isEmptyExtBlock(m) && !isEmptyExtBlock(method)) {
                    remove(m);
                }
                return m;
            }

            private boolean isEmptyExtBlock(J.MethodInvocation method) {
                return "ext".equals(method.getSimpleName()) && method.getArguments().size() == 1 &&
                       method.getArguments().get(0) instanceof J.Lambda &&
                       ((J.Lambda) method.getArguments().get(0)).getBody() instanceof J.Block &&
                       ((J.Block) ((J.Lambda) method.getArguments().get(0)).getBody()).getStatements().isEmpty();
            }
        });
    }
}
