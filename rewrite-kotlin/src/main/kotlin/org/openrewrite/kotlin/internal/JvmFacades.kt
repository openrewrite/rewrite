/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.kotlin.internal

import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name

internal val FirBasedSymbol<*>.isFromLibrary: Boolean
    get() = origin == FirDeclarationOrigin.Library || origin == FirDeclarationOrigin.BuiltIns

internal val FirDeclaration.jvmName: String?
    get() = annotations
        .getAnnotationByClassId(JvmStandardClassIds.Annotations.JvmName, moduleData.session)
        ?.getStringArgument(Name.identifier("name"))

internal val JvmPackagePartSource.facadeFqn: String
    get() = (facadeClassName ?: className).fqNameForTopLevelClassMaybeWithDollars.asString()

/**
 * The class this file's top-level functions and properties belong to.
 * For a file that is not a script, that is the JVM class kotlinc compiles them into.
 * A `@file:JvmMultifileClass` file maps to the facade that callers bind to, not its part class.
 */
@OptIn(DirectDeclarationsAccess::class)
internal val FirFile.facadeFqn: String
    get() {
        if (declarations.any { it is FirScript }) {
            // Every script compiles on its own into a class named after its file, so all `build.gradle.kts`
            // files would share one type. The path in this name keeps the scripts of one parse apart.
            val path = name.replace("/", ".").replace("\\", ".").replace(".kt", "Kt")
            return if (packageFqName.isRoot) path else "${packageFqName.asString()}.$path"
        }
        // `name` is the input path the parser was given, directories included.
        val simpleName = jvmName
            ?: PackagePartClassUtils.getFilePartShortName(name.substringAfterLast('/').substringAfterLast('\\'))
        return packageFqName.child(Name.identifier(simpleName)).asString()
    }
