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
package org.openrewrite.java

import org.openrewrite.java.tree.J

/**
 * Produces a report about missing type attributions within a CompilationUnit.
 */
class TypeValidator {

    data class ValidationOptions(
        val classDeclarations: Boolean = true,
        val identifiers: Boolean = true,
        val methodDeclarations: Boolean = true,
        val methodInvocations: Boolean = true,
    ) {
        companion object {
            class Builder(
                var classDeclarations: Boolean = true,
                var identifiers: Boolean = true,
                var methodDeclarations: Boolean = true,
                var methodInvocations: Boolean = true,
            )

            fun builder(init: Builder.() -> Unit): ValidationOptions {
                val builder = Builder()
                init.invoke(builder)
                return ValidationOptions(
                    classDeclarations = builder.classDeclarations,
                    identifiers = builder.identifiers,
                    methodDeclarations = builder.methodDeclarations,
                    methodInvocations = builder.methodInvocations
                )
            }
        }
    }

    companion object {
        private val defaultOptions = ValidationOptions()

        @JvmStatic
        fun assertTypesValid(
            cu: J.CompilationUnit,
            options: ValidationOptions = defaultOptions
        ) {
            TypeValidation().identifiers(options.identifiers).methodInvocations(options.methodInvocations)
                .methodDeclarations(options.methodDeclarations).classDeclarations(options.classDeclarations)
                .assertValidTypes(cu)
        }
    }

}
