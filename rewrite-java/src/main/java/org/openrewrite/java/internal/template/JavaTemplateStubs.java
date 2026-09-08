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
package org.openrewrite.java.internal.template;

import org.openrewrite.java.tree.*;

import java.util.List;

import static org.openrewrite.java.internal.template.TemplateStubs.Imports.NONE;
import static org.openrewrite.java.internal.template.TemplateStubs.Imports.TEMPLATE;
import static org.openrewrite.java.internal.template.TemplateStubs.Imports.TEMPLATE_AND_ENCLOSING;

/**
 * Java stubs. Each wraps the template in a synthetic {@code $Template} class, so every extractor reaches the
 * templated element through {@link JavaSourceFile#getClasses()}.
 */
public class JavaTemplateStubs implements TemplateStubs {

    @Override
    public Stub<Statement> parameters() {
        return Stub.of("abstract class $Template { abstract void $template(#{}); }", TEMPLATE_AND_ENCLOSING,
                cu -> ((J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0)).getParameters());
    }

    @Override
    public Stub<J.Lambda.Parameters> lambdaParameters() {
        return Stub.ofOne("class $Template { { Object o = (#{}) -> {}; } }", TEMPLATE, cu -> {
            J.Block b = (J.Block) cu.getClasses().get(0).getBody().getStatements().get(0);
            J.VariableDeclarations v = (J.VariableDeclarations) b.getStatements().get(0);
            J.Lambda l = (J.Lambda) v.getVariables().get(0).getInitializer();
            assert l != null;
            return l.getParameters();
        });
    }

    @Override
    public Stub<TypeTree> anExtends() {
        return Stub.ofOne("class $Template extends #{} {}", TEMPLATE, cu -> {
            TypeTree anExtends = cu.getClasses().get(0).getExtends();
            assert anExtends != null;
            return anExtends;
        });
    }

    @Override
    public Stub<TypeTree> anImplements() {
        return Stub.of("class $Template implements #{} {}", TEMPLATE, cu -> {
            List<TypeTree> anImplements = cu.getClasses().get(0).getImplements();
            assert anImplements != null;
            return anImplements;
        });
    }

    @Override
    public Stub<NameTree> checkedExceptions() {
        return Stub.of("abstract class $Template { abstract void $template() throws #{}; }", TEMPLATE, cu -> {
            J.MethodDeclaration m = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
            List<NameTree> aThrows = m.getThrows();
            assert aThrows != null;
            return aThrows;
        });
    }

    @Override
    public Stub<J.TypeParameter> typeParameters() {
        return Stub.of("class $Template<#{}> {}", TEMPLATE, cu -> {
            List<J.TypeParameter> tps = cu.getClasses().get(0).getTypeParameters();
            assert tps != null;
            return tps;
        });
    }

    @Override
    public Stub<Expression> packageDeclaration() {
        //noinspection ConstantConditions
        return Stub.ofOne("package #{}; class $Template {}", NONE,
                cu -> cu.getPackageDeclaration().getExpression());
    }
}
