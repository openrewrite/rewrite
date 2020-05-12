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
package org.openrewrite.java.refactor;

import org.openrewrite.java.tree.J;

public class ChangeMethodName extends ScopedJavaRefactorVisitor {
    private final String name;

    public ChangeMethodName(J.MethodInvocation scope, String name) {
        super(scope.getId());
        this.name = name;
    }

    @Override
    public String getName() {
        return "core.ChangeMethodName{to=" + name + "}";
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method) {
        return isScope() ? method.withName(method.getName().withName(name)) :
                super.visitMethodInvocation(method);
    }
}
