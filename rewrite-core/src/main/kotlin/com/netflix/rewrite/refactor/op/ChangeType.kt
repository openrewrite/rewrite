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
package com.netflix.rewrite.refactor.op

import com.netflix.rewrite.ast.*
import com.netflix.rewrite.refactor.RefactorVisitor

/**
 * NOTE: Does not currently transform all possible type references, and accomplishing this would be non-trivial.
 * For example, a method invocation select might refer to field `A a` whose type has now changed to `A2`, and so the type
 * on the select should change as well. But how do we identify the set of all method selects which refer to `a`? Suppose
 * it were prefixed like `this.a`, or `MyClass.this.a`, or indirectly via a separate method call like `getA()` where `getA()`
 * is defined on the super class.
 */
data class ChangeType(val from: String,
                      val to: String,
                      override val ruleName: String = "change-type"): RefactorVisitor<Tree>() {
    // NOTE: a type change is possible anywhere a Tr.FieldAccess or Tr.Ident is possible, but not every FieldAccess or Ident
    // represents a type (could represent a variable name, etc.)

    private val toClassType by lazy { Type.Class.build(to) }

    override fun visitAnnotation(annotation: Tr.Annotation): List<AstTransform<Tree>> =
        super.visitAnnotation(annotation) + annotation.annotationType
                .transformName<Tr.Annotation> { name, node -> node.copy(annotationType = name) }

    override fun visitArrayType(arrayType: Tr.ArrayType): List<AstTransform<Tree>> =
            super.visitArrayType(arrayType) + arrayType.elementType
                    .transformName<Tr.ArrayType> { name, node -> node.copy(elementType = name) }

    override fun visitClassDecl(classDecl: Tr.ClassDecl): List<AstTransform<Tree>> {
        return super.visitClassDecl(classDecl) +
                classDecl.extends.transformName<Tr.ClassDecl> { name, node -> node.copy(extends = name) } +
                classDecl.implements.transformNames { names, node: Tr.ClassDecl -> node.copy(implements = names) }
    }

    override fun visitFieldAccess(field: Tr.FieldAccess): List<AstTransform<Tree>> {
        return super.visitFieldAccess(field) +
                field.asClassReference().transformName<Tr.FieldAccess> { name, node ->
                    node.copy(target = name)
                }
    }

    override fun visitMethod(method: Tr.MethodDecl): List<AstTransform<Tree>> {
        return super.visitMethod(method) +
                method.returnTypeExpr.transformName<Tr.MethodDecl> { name, node -> node.copy(returnTypeExpr = name) } +
                method.throws?.exceptions.transformNames { names, node: Tr.MethodDecl -> node.copy(throws = method.throws!!.copy(exceptions = names)) }
    }

    override fun visitMethodInvocation(meth: Tr.MethodInvocation): List<AstTransform<Tree>> {
        val staticTargetChange = if(meth.select is NameTree && meth.type?.hasFlags(Flag.Static) ?: false)
            meth.select.transformName<Tr.MethodInvocation> { name, node -> node.copy(select = name) }
        else emptyList()

        return super.visitMethodInvocation(meth) + staticTargetChange +
                meth.typeParameters?.params.transformNames { names, node: Tr.MethodInvocation -> node.copy(typeParameters = meth.typeParameters!!.copy(params = names)) }
    }

    override fun visitMultiCatch(multiCatch: Tr.MultiCatch): List<AstTransform<Tree>> {
        return super.visitMultiCatch(multiCatch) +
                multiCatch.alternatives.transformNames { names, node: Tr.MultiCatch -> node.copy(alternatives = names) }
    }

    override fun visitMultiVariable(multiVariable: Tr.VariableDecls): List<AstTransform<Tree>> {
        if(multiVariable.typeExpr is Tr.MultiCatch)
            return super.visitMultiVariable(multiVariable)

        return super.visitMultiVariable(multiVariable) +
                multiVariable.typeExpr.transformName<Tr.VariableDecls> { name, node -> node.copy(typeExpr = name) } +
                multiVariable.vars.mapIndexed { i, (name) ->
                    name.transformName<Tr.VariableDecls> { name, node ->
                        node.copy(vars = node.vars.mapIndexed { j, originalVar ->
                            if(i == j)
                                originalVar.copy(name = originalVar.name.copy(type = name.type))
                            else originalVar
                        })
                    }
                }.flatten()
    }

    override fun visitNewArray(newArray: Tr.NewArray): List<AstTransform<Tree>> {
        return super.visitNewArray(newArray) +
                newArray.typeExpr.transformName<Tr.NewArray> { name, node -> node.copy(typeExpr = name) }
    }

    override fun visitNewClass(newClass: Tr.NewClass): List<AstTransform<Tree>> {
        return super.visitNewClass(newClass) +
                newClass.clazz.transformName<Tr.NewClass> { name, node -> node.copy(clazz = name) }
    }

    override fun visitParameterizedType(type: Tr.ParameterizedType): List<AstTransform<Tree>> {
        return super.visitParameterizedType(type) +
                type.clazz.transformName<Tr.ParameterizedType> { name, node -> node.copy(clazz = name) } +
                type.typeArguments?.args.transformNames { names, node: Tr.ParameterizedType -> node.copy(typeArguments = type.typeArguments!!.copy(args = names)) }
    }

    override fun visitTypeCast(typeCast: Tr.TypeCast): List<AstTransform<Tree>> =
            super.visitTypeCast(typeCast) +
                typeCast.clazz.tree.transformName<Tr.TypeCast> { name, node -> node.copy(clazz = typeCast.clazz.copy(tree = name)) }

    override fun visitTypeParameter(typeParameter: Tr.TypeParameter): List<AstTransform<Tree>> {
        return super.visitTypeParameter(typeParameter) +
                typeParameter.bounds?.types.transformNames { names, node: Tr.TypeParameter -> node.copy(bounds = typeParameter.bounds!!.copy(types = names)) }
    }

    override fun visitWildcard(wildcard: Tr.Wildcard): List<AstTransform<Tree>> =
            super.visitWildcard(wildcard) +
                wildcard.boundedType.transformName<Tr.Wildcard> { name, node -> node.copy(boundedType = name) }

    fun <T: Tree> NameTree?.transformName(change: (Tr.Ident, T) -> Tree): List<AstTransform<Tree>> {
        return if (this != null && this.type.asClass()?.fullyQualifiedName == from) {
            val originalFormatting = formatting
            transform {
                @Suppress("UNCHECKED_CAST")
                change(Tr.Ident.build(toClassType.className(), toClassType, originalFormatting), this as T)
            }
        } else emptyList()
    }

    fun <T: Tree, U: Tree> Iterable<T>?.transformNames(change: (List<T>, U) -> Tree): List<AstTransform<Tree>> {
        if(this == null)
            return emptyList()

        var atLeastOneChanged = false
        val transformed = map {
            if (it is NameTree && it.type.asClass()?.fullyQualifiedName == from) {
                atLeastOneChanged = true
                val classType = Type.Class.build(to)
                Tr.Ident.build(classType.className(), classType, it.formatting)
            } else it
        }

        return if(atLeastOneChanged) {
            transform {
                @Suppress("UNCHECKED_CAST")
                change(transformed as List<T>, this as U)
            }
        } else emptyList()
    }
}