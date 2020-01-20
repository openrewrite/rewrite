/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.tree.visitor.refactor.op;

import com.netflix.rewrite.tree.Expression;
import com.netflix.rewrite.tree.Formatting;
import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.visitor.refactor.AstTransform;
import com.netflix.rewrite.tree.visitor.refactor.RefactorVisitor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.netflix.rewrite.tree.Tr.randomId;

@AllArgsConstructor
public class DeleteMethodArgument extends RefactorVisitor<Tr.MethodInvocation> {
    int pos;

    @Override
    protected String getRuleName() {
        return "delete-method-argument";
    }

    @Override
    public List<AstTransform<Tr.MethodInvocation>> visitMethodInvocation(Tr.MethodInvocation method) {
        if(method.getArgs().getArgs().stream().filter(arg -> !(arg instanceof Tr.Empty)).count() > pos) {
            return transform(m -> {
                List<Expression> args = new ArrayList<>(method.getArgs().getArgs());
                args.remove(pos);
                if(args.isEmpty()) {
                    args = Collections.singletonList(new Tr.Empty(randomId(), Formatting.EMPTY));
                }
                return m.withArgs(m.getArgs().withArgs(args));
            });
        }
        return super.visitMethodInvocation(method);
    }
}
