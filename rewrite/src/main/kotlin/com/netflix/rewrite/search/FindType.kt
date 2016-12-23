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
package com.netflix.rewrite.search

import com.netflix.rewrite.ast.*
import com.netflix.rewrite.ast.visitor.AstVisitor

class FindType(val clazz: String): AstVisitor<List<NameTree>>(emptyList()) {
    // NOTE: a type is possible anywhere a Tr.FieldAccess or Tr.Ident is possible, but not every FieldAccess or Ident
    // represents a type (could represent a variable name, etc.)

    override fun visitAnnotation(annotation: Tr.Annotation): List<NameTree> {
        return super.visitAnnotation(annotation) + annotation.annotationType.typeMatches()
    }

    override fun visitArrayType(arrayType: Tr.ArrayType): List<NameTree> {
        return super.visitArrayType(arrayType) + arrayType.elementType.typeMatches()
    }

    override fun visitClassDecl(classDecl: Tr.ClassDecl): List<NameTree> {
        return super.visitClassDecl(classDecl) + classDecl.extends.typeMatches() +
                classDecl.implements.typeMatches()
    }

    override fun visitMethod(method: Tr.MethodDecl): List<NameTree> {
        return super.visitMethod(method) + method.returnTypeExpr.typeMatches() +
                method.throws?.exceptions.typeMatches()
    }

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<NameTree> {
        val staticMatch = if(meth.type?.hasFlags(Flag.Static) ?: false)
            meth.select.typeMatches()
        else emptyList()

        return super.visitMethodInvocation(meth) + meth.typeParameters?.params.typeMatches() + staticMatch
    }

    override fun visitMultiCatch(multiCatch: Tr.MultiCatch): List<NameTree> {
        return super.visitMultiCatch(multiCatch) + multiCatch.alternatives.typeMatches()
    }

    override fun visitMultiVariable(multiVariable: Tr.VariableDecls): List<NameTree> {
        if(multiVariable.typeExpr is Tr.MultiCatch)
            return super.visitMultiVariable(multiVariable)
        return super.visitMultiVariable(multiVariable) + multiVariable.typeExpr.typeMatches()
    }

    override fun visitNewArray(newArray: Tr.NewArray): List<NameTree> {
        return super.visitNewArray(newArray) + newArray.typeExpr.typeMatches()
    }

    override fun visitNewClass(newClass: Tr.NewClass): List<NameTree> {
        return super.visitNewClass(newClass) + newClass.clazz.typeMatches()
    }

    override fun visitParameterizedType(type: Tr.ParameterizedType): List<NameTree> {
        return super.visitParameterizedType(type) + type.clazz.typeMatches() +
                type.typeArguments?.args.typeMatches()
    }

    override fun visitTypeCast(typeCast: Tr.TypeCast): List<NameTree> {
        return super.visitTypeCast(typeCast) + typeCast.clazz.tree.typeMatches()
    }

    override fun visitTypeParameter(typeParameter: Tr.TypeParameter): List<NameTree> {
        return super.visitTypeParameter(typeParameter) + typeParameter.bounds?.types.typeMatches()
    }

    override fun visitWildcard(wildcard: Tr.Wildcard): List<NameTree> {
        return super.visitWildcard(wildcard) + wildcard.boundedType.typeMatches()
    }

    fun Iterable<Tree>?.typeMatches(): List<NameTree> =
            this?.flatMap { it.typeMatches() } ?: emptyList()

    fun Tree?.typeMatches(): List<NameTree> {
        return if (this is NameTree && this.type.asClass()?.fullyQualifiedName == clazz) {
            listOf(this)
        } else emptyList()
    }
}