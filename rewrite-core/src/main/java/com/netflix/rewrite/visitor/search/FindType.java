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
package com.netflix.rewrite.visitor.search;

import com.netflix.rewrite.tree.NameTree;
import com.netflix.rewrite.tree.Tree;
import com.netflix.rewrite.tree.Type;
import com.netflix.rewrite.tree.TypeUtils;
import com.netflix.rewrite.visitor.AstVisitor;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static java.util.Collections.*;

@RequiredArgsConstructor
public class FindType extends AstVisitor<List<NameTree>> {
    private final String clazz;

    @Override
    public List<NameTree> defaultTo(Tree t) {
        return emptyList();
    }

    @Override
    public List<NameTree> visitTypeName(NameTree name) {
        Type.Class asClass = TypeUtils.asClass(name.getType());
        return asClass == null ? emptyList() :
                asClass.getFullyQualifiedName().equals(clazz) ? singletonList(name) :
                        emptyList();
    }
}
