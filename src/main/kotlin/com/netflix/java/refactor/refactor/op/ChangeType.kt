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
package com.netflix.java.refactor.refactor.op

import com.netflix.java.refactor.ast.*
import com.netflix.java.refactor.refactor.RefactorVisitor

data class ChangeType(val from: String, val to: String): RefactorVisitor() {
    // NOTE: a type change is possible anywhere a Tr.FieldAccess or Tr.Ident is possible, but not every FieldAccess or Ident
    // represents a type (could represent a variable name, etc.)

    override fun visitAnnotation(annotation: Tr.Annotation): List<AstTransform<*>> {
        val changes = annotation.annotationType.changeName()?.let { change ->
            AstTransform<Tr.Annotation>(cursor()) { copy(annotationType = change) }
        }

        return super.visitAnnotation(annotation) + listOf(changes).filterNotNull()
    }

    override fun visitArrayType(arrayType: Tr.ArrayType): List<AstTransform<*>> {
        val changes = if (arrayType.elementType is NameTree) {
            arrayType.elementType.changeName()
                    ?.let { change -> AstTransform<Tr.ArrayType>(cursor()) { copy(elementType = change) } }
        } else null

        return super.visitArrayType(arrayType) + listOf(changes).filterNotNull()
    }

    override fun visitClassDecl(classDecl: Tr.ClassDecl): List<AstTransform<*>> {
        val extendsChange = if(classDecl.extends is NameTree) {
            classDecl.extends.changeName()
        } else null

        var atLeastOneImplementsChanged = false
        val implementsChanges = classDecl.implements.map {
            it.changeName()?.let { atLeastOneImplementsChanged = true; it } ?: it
        }

        val changes = if(extendsChange != null || atLeastOneImplementsChanged) {
            AstTransform<Tr.ClassDecl>(cursor()) {
                copy(extends = extendsChange ?: extends, implements = if(atLeastOneImplementsChanged) {
                    implementsChanges
                } else implements)
            }
        } else null

        return super.visitClassDecl(classDecl) + listOf(changes).filterNotNull()
    }

    override fun visitMethod(method: Tr.MethodDecl): List<AstTransform<*>> {
        val returnChange = if(method.returnTypeExpr is NameTree) {
            method.returnTypeExpr.changeName()
        } else null

        var atLeastOneThrowsChanged = false
        val exceptionChanges = method.throws?.exceptions?.map {
            it.changeName()?.let { atLeastOneThrowsChanged = true; it } ?: it
        }

        val changes = if(returnChange != null || atLeastOneThrowsChanged) {
            AstTransform<Tr.MethodDecl>(cursor()) {
                copy(returnTypeExpr = returnChange ?: returnTypeExpr, throws = if(atLeastOneThrowsChanged) {
                    throws!!.copy(exceptions = exceptionChanges!!)
                } else throws)
            }
        } else null

        return super.visitMethod(method) + listOf(changes).filterNotNull()
    }

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<*>> {
        val staticTargetChange = if(meth.select is NameTree && meth.type?.hasFlags(Flag.Static) ?: false)
            meth.select.changeName()
        else null

        var atLeastOneTypeParamChanged = false
        val typeParamChanges = meth.typeParameters?.params?.map {
            it.changeName()?.let { atLeastOneTypeParamChanged = true; it } ?: it
        }

        val changes = if(staticTargetChange != null || atLeastOneTypeParamChanged) {
            AstTransform<Tr.MethodInvocation>(cursor()) {
                copy(select = staticTargetChange ?: select, typeParameters = if(atLeastOneTypeParamChanged) {
                    typeParameters!!.copy(params = typeParamChanges!!)
                } else typeParameters)
            }
        } else null

        return super.visitMethodInvocation(meth) + listOf(changes).filterNotNull()
    }

    override fun visitMultiCatch(multiCatch: Tr.MultiCatch): List<AstTransform<*>> {
        var atLeastOneAlternativeChanged = false
        val alternativeChanges = multiCatch.alternatives.map {
            it.changeName()?.let { atLeastOneAlternativeChanged = true; it } ?: it
        }

        val changes = if(atLeastOneAlternativeChanged) {
            AstTransform<Tr.MultiCatch>(cursor()) {
                copy(alternatives = alternativeChanges)
            }
        } else null

        return super.visitMultiCatch(multiCatch) + listOf(changes).filterNotNull()
    }

    override fun visitMultiVariable(multiVariable: Tr.VariableDecls): List<AstTransform<*>> {
        if(multiVariable.typeExpr is Tr.MultiCatch)
            return super.visitMultiVariable(multiVariable)

        val changes = if (multiVariable.typeExpr is NameTree) {
            multiVariable.typeExpr.changeName()
                    ?.let { change -> AstTransform<Tr.VariableDecls>(cursor()) { copy(typeExpr = change) } }
        } else null

        return super.visitMultiVariable(multiVariable) + listOf(changes).filterNotNull()
    }

    override fun visitNewArray(newArray: Tr.NewArray): List<AstTransform<*>> {
        val changes = if (newArray.typeExpr is NameTree) {
            newArray.typeExpr.changeName()
                    ?.let { change -> AstTransform<Tr.NewArray>(cursor()) { copy(typeExpr = change) } }
        } else null

        return super.visitNewArray(newArray) + listOf(changes).filterNotNull()
    }

    override fun visitNewClass(newClass: Tr.NewClass): List<AstTransform<*>> {
        val changes = if (newClass.clazz is NameTree) {
            newClass.clazz.changeName()
                    ?.let { change -> AstTransform<Tr.NewClass>(cursor()) { copy(clazz = change) } }
        } else null

        return super.visitNewClass(newClass) + listOf(changes).filterNotNull()
    }

    override fun visitParameterizedType(type: Tr.ParameterizedType): List<AstTransform<*>> {
        val clazzChange = type.clazz.changeName()

        var atLeastOneParamChanged = false
        val paramChanges = type.typeArguments?.args?.map<Expression, Expression> {
            if(it is NameTree) {
                it.changeName()?.let { atLeastOneParamChanged = true; it } ?: it
            } else it
        }

        val changes = if(clazzChange != null || atLeastOneParamChanged) {
            AstTransform<Tr.ParameterizedType>(cursor()) {
                copy(clazz = clazzChange ?: clazz, typeArguments = if(atLeastOneParamChanged) {
                    typeArguments!!.copy(args = paramChanges!!)
                } else typeArguments)
            }
        } else null

        return super.visitParameterizedType(type) + listOf(changes).filterNotNull()
    }

    override fun visitTypeCast(typeCast: Tr.TypeCast): List<AstTransform<*>> {
        val changes = if (typeCast.clazz.tree is NameTree) {
            typeCast.clazz.tree.changeName()
                    ?.let { change -> AstTransform<Tr.Parentheses<TypeTree>>(cursor().plus(typeCast.clazz)) { copy(tree = change) } }
        } else null

        return super.visitTypeCast(typeCast) + listOf(changes).filterNotNull()
    }

    override fun visitTypeParameter(typeParameter: Tr.TypeParameter): List<AstTransform<*>> {
        var atLeastOneBoundChanged = false
        val boundChanges = typeParameter.bounds.map {
            it.changeName()?.let { atLeastOneBoundChanged = true; it } ?: it
        }

        val changes = if(atLeastOneBoundChanged) {
            AstTransform<Tr.TypeParameter>(cursor()) { copy(bounds = boundChanges) }
        } else null

        return super.visitTypeParameter(typeParameter) + listOf(changes).filterNotNull()
    }

    override fun visitWildcard(wildcard: Tr.Wildcard): List<AstTransform<*>> {
        val changes = wildcard.boundedType?.changeName()?.let { change ->
            AstTransform<Tr.Wildcard>(cursor()) { copy(boundedType = change) }
        }

        return super.visitWildcard(wildcard) + listOf(changes).filterNotNull()
    }

    fun NameTree?.changeName(): Tr.Ident? {
        return if (this?.type.asClass()?.fullyQualifiedName == from) {
            val toType = Type.Class.build(cu.typeCache(), to)
            Tr.Ident(toType.className(), toType, this!!.formatting)
        } else null
    }
}