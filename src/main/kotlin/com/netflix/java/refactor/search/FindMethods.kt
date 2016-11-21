/**
 * Copyright 2016 Netflix, Inc.
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
package com.netflix.java.refactor.search

import com.netflix.java.refactor.ast.visitor.AstVisitor
import com.netflix.java.refactor.ast.Cursor
import com.netflix.java.refactor.ast.Tr

class FindMethods(signature: String): AstVisitor<List<Tr.MethodInvocation>>(emptyList()) {
    val matcher = MethodMatcher(signature)
    
    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<Tr.MethodInvocation> =
        if(matcher.matches(meth))
            listOf(meth)
        else super.visitMethodInvocation(meth)
}