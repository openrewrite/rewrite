/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.openrewrite.java.search.FindMissingTypes;
import org.openrewrite.java.tree.J;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
public class TypeValidation {

    private boolean classDeclarations = true;
    private boolean identifiers = true;
    private boolean methodDeclarations = true;
    private boolean methodInvocations = true;

    public static TypeValidation none() {
        return new TypeValidation(false, false, false, false);
    }

    private boolean enabled() {
        return identifiers || methodInvocations || methodDeclarations || classDeclarations;
    }

    public void assertValidTypes(J sf) {
        if (enabled()) {
            List<FindMissingTypes.MissingTypeResult> missingTypeResults = FindMissingTypes.findMissingTypes(sf);
            missingTypeResults = missingTypeResults.stream()
                    .filter(missingType -> {
                        if (identifiers && missingType.getJ() instanceof J.Identifier) {
                            return true;
                        } else if (classDeclarations && missingType.getJ() instanceof J.ClassDeclaration) {
                            return true;
                        } else if (methodInvocations && missingType.getJ() instanceof J.MethodInvocation) {
                            return true;
                        } else return methodDeclarations && missingType.getJ() instanceof J.MethodDeclaration;
                    })
                    .collect(Collectors.toList());
            if (!missingTypeResults.isEmpty()) {
                throw new IllegalStateException("AST contains missing or invalid type information\n" + missingTypeResults.stream().map(v -> v.getPath() + "\n" + v.getPrintedTree())
                        .collect(Collectors.joining("\n\n")));
            }
        }
    }
}
