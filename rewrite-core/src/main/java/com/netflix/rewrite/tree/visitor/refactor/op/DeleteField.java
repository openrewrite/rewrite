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

import com.netflix.rewrite.tree.Tr;
import com.netflix.rewrite.tree.visitor.refactor.AstTransform;
import com.netflix.rewrite.tree.visitor.refactor.RefactorVisitor;
import lombok.AllArgsConstructor;

import java.util.List;

import static java.util.stream.Collectors.toList;

@AllArgsConstructor
public class DeleteField extends RefactorVisitor<Tr.ClassDecl> {
    Iterable<Tr.VariableDecls> deletes;

    @Override
    protected String getRuleName() {
        return "delete-field";
    }

    @Override
    public List<AstTransform<Tr.ClassDecl>> visitClassDecl(Tr.ClassDecl classDecl) {
        return transform(cd -> cd.withBody(cd.getBody().withStatements(cd.getBody().getStatements().stream()
                .filter(s -> {
                    for (Tr.VariableDecls delete : deletes) {
                        if (delete == s) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(toList())))
        );
    }
}
