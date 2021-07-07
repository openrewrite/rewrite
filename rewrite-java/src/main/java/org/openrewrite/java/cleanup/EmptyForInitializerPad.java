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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.style.PadPolicy;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;

public class EmptyForInitializerPad extends Recipe {
    @Override
    public String getDisplayName() {
        return "Empty for initializer padding";
    }

    @Override
    public String getDescription() {
        return "Fixes the whitespace of an empty for initializer. Adds or removes this whitespace according to `CheckstyleStyle.getEmptyForInitializer()`";
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private PadPolicy option = null;

            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                EmptyForInitializerStyle style = cu.getStyle(EmptyForInitializerStyle.class);
                if(style == null) {
                    return cu;
                }
                option = style.getOption();
                return super.visitCompilationUnit(cu, executionContext);
            }

            @Override
            public J.ForLoop visitForLoop(J.ForLoop f, ExecutionContext executionContext) {
                J.ForLoop.Control control = f.getControl();
                Statement init = control.getInit();
                Space prefix = init.getPrefix();
                String whitespace = prefix.getWhitespace();
                if(!whitespace.contains("\n") && init instanceof J.Empty) {
                    if(option == PadPolicy.NOSPACE && !whitespace.isEmpty()) {
                        f = f.withControl(control.withInit(init.withPrefix(prefix.withWhitespace(""))));
                    } else if(option == PadPolicy.SPACE && !whitespace.equals(" ")) {
                        f = f.withControl(control.withInit(init.withPrefix(prefix.withWhitespace(" "))));
                    }
                }
                return f;
            }
        };
    }
}
