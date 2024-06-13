/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.gradle.search.FindGradleProject;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.Objects;


@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeExtraProperty extends Recipe {

    @Override
    public String getDisplayName() {
        return "Change Extra Property";
    }

    @Override
    public String getDescription() {
        return "Gradle's [ExtraPropertiesExtension](https://docs.gradle.org/current/dsl/org.gradle.api.plugins.ExtraPropertiesExtension.html) " +
               "is a commonly used mechanism for setting arbitrary key/value pairs on a project. " +
               "This recipe will change the value of a property with the given key name if that key can be found. " +
               "It assumes that the value being set is a String literal. " +
               "Does not add the value if it does not already exist.";
    }

    @Option(displayName = "Key",
            description = "The key of the property to change.",
            example = "foo")
    String key;

    @Option(displayName = "Value",
            description = "The new value to set. The value will be treated the contents of a string literal.",
            example = "bar")
    String value;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindGradleProject(FindGradleProject.SearchCriteria.File).getVisitor(), new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public J.Assignment visitAssignment(J.Assignment as, ExecutionContext ctx) {
                if(!(as.getAssignment() instanceof J.Literal)) {
                    return as;
                }
                if(as.getVariable() instanceof J.Identifier) {
                    if(!Objects.equals(key, ((J.Identifier) as.getVariable()).getSimpleName())) {
                        return as;
                    }
                    J.MethodInvocation m = getCursor().firstEnclosing(J.MethodInvocation.class);
                    if(m == null || !m.getSimpleName().equals("ext")) {
                        return as;
                    }
                    as = updateAssignment(as);
                } else if(as.getVariable() instanceof J.FieldAccess) {
                    J.FieldAccess var = (J.FieldAccess) as.getVariable();
                    if(!Objects.equals(key, var.getSimpleName())) {
                        return as;
                    }
                    if((var.getTarget() instanceof J.Identifier && ((J.Identifier) var.getTarget()).getSimpleName().equals("ext"))
                       ||  (var.getTarget() instanceof J.FieldAccess && ((J.FieldAccess) var.getTarget()).getSimpleName().equals("ext")) ) {
                        as = updateAssignment(as);
                    }
                }

                return as;
            }
        });
    }

    private J.Assignment updateAssignment(J.Assignment as) {
        if(!(as.getAssignment() instanceof J.Literal)) {
            return as;
        }
        J.Literal asVal = (J.Literal) as.getAssignment();
        if(Objects.equals(value, asVal.getValue())) {
            return as;
        }
        String quote = "\"";
        if(asVal.getValueSource() != null && asVal.getValueSource().trim().startsWith("'")) {
            quote = "'";
        }
        return as.withAssignment(asVal.withValue(value)
                .withValueSource(quote + value + quote));
    }
}
