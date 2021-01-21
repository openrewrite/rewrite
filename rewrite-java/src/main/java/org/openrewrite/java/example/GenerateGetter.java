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
package org.openrewrite.java.example;

import lombok.Data;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeProcessor;
import org.openrewrite.Validated;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoProcessor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;

import static org.openrewrite.Validated.required;

@Data
public class GenerateGetter extends Recipe {
    private final String fieldName;

    @Override
    protected TreeProcessor<?, ExecutionContext> getProcessor() {
        return new GenerateGetterProcessor<>();
    }

    @Override
    public Validated validate() {
        return required("fieldName", fieldName);
    }

    private class GenerateGetterProcessor<P> extends JavaIsoProcessor<P> {
        private final JavaTemplate GETTER = JavaTemplate
                .builder("" +
                        "public #{} get#{}() {\n" +
                        "    return #{};" +
                        "}")
                .build();

        public GenerateGetterProcessor() {
            setCursoringOn();
        }

        @Override
        public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable, P p) {
            if (variable.isField(getCursor()) && variable.getSimpleName().equals(fieldName)) {
                getCursor().putMessageOnFirstEnclosing(J.ClassDecl.class, "var", variable);
            }
            return super.visitVariable(variable, p);
        }

        @Override
        public J.ClassDecl visitClassDecl(J.ClassDecl classDecl, P p) {
            J.ClassDecl c = super.visitClassDecl(classDecl, p);
            J.VariableDecls.NamedVar var = getCursor().pollMessage("var");
            if (var != null) {
                J.Block body = c.getBody();
//                c = c.withBody(body.withStatements(
//                        ListUtils.concat(
//                                body.getStatements(),
//                                new JRightPadded<>(
//                                        GETTER.generateBefore(body.getEnd(),
//                                                TypeUtils.asClass(var.getType()).getClassName(),
//                                                var.getSimpleName() /* upper case */,
//                                                var.getSimpleName()).iterator().next(),
//                                        Space.EMPTY
//                                )
//                        )));
            }
            return c;
        }
    }
}
