/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
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
import org.openrewrite.groovy.GroovyTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class UsePropertyAssignmentSyntax extends Recipe {

    @Option(displayName = "Property name",
            description = "The name of the property whose method-call or space-separated syntax should be converted to assignment syntax using `=`.",
            example = "description")
    String propertyName;

    @Override
    public String getDisplayName() {
        return "Use `=` assignment syntax for Gradle properties";
    }

    @Override
    public String getDescription() {
        return "Converts deprecated Groovy DSL property assignment syntax from space/method-call form " +
               "(e.g., `description 'text'` or `description('text')`) to assignment form (`description = 'text'`). " +
               "Addresses Gradle 8.14 deprecation: " +
               "\"Properties should be assigned using the 'propName = value' syntax.\".";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s`", propertyName);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles("**/*.gradle"), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                if (m.getArguments().size() != 1 || m.getArguments().get(0) instanceof J.Empty) {
                    return m;
                }

                if (!propertyName.equals(m.getSimpleName())) {
                    return m;
                }

                if (m.getSelect() != null) {
                    return GroovyTemplate.apply("#{any()}." + propertyName + " = #{any()}",
                            getCursor(), m.getCoordinates().replace(),
                            m.getSelect(), m.getArguments().get(0));
                }
                return GroovyTemplate.apply(propertyName + " = #{any()}",
                        getCursor(), m.getCoordinates().replace(),
                        m.getArguments().get(0));
            }
        });
    }
}
