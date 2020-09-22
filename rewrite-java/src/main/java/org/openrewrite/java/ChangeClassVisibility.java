/*
 * Copyright 2020 the original author or authors.
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

import org.openrewrite.Formatting;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Set the specified class the specified visibility.
 * Class name should be fully qualified.
 * Visibilities are public/private/protected/package
 */
public class ChangeClassVisibility {
    public static class Scoped extends JavaRefactorVisitor {
        final J.ClassDecl clazz;
        final String visibility;

        public Scoped(J.ClassDecl clazz, String modifier) {
            this.clazz = clazz;
            this.visibility = modifier;
        }

        @Override
        public J visitClassDecl(J.ClassDecl classDecl) {
            J.ClassDecl c = refactor(classDecl, super::visitClassDecl);

            if (clazz.isScope(c)) {
                List<J.Modifier> modifiers = new ArrayList<>(classDecl.getModifiers());

                if(!visibility.equals("protected")) {
                    if(modifiers.isEmpty()) {
                        c = c.withKind(c.getKind().withPrefix(" "));
                    }

                    J.Modifier desiredModifier = J.Modifier.buildModifier(visibility, Formatting.EMPTY);
                    J.Modifier actualModifier = classDecl.getModifiers().stream()
                            .filter(modifier -> modifier.getClass().equals(desiredModifier.getClass()))
                            .findAny()
                            .orElse(desiredModifier);

                    modifiers.add(actualModifier);
                }

                modifiers = Stream.concat(
                    Stream.of(actualModifier),
                    modifiers.stream()
                        .filter(mod -> !(mod instanceof J.Modifier.Protected || mod instanceof J.Modifier.Private || mod instanceof J.Modifier.Public))
                )
                    .collect(toList())

                c = c.withModifiers(modifiers);

                if(modifiers.isEmpty()) {
                    c = c.withKind(c.getKind().withFormatting(Formatting.EMPTY));
                }
            }

            return c;
        }
    }
}
