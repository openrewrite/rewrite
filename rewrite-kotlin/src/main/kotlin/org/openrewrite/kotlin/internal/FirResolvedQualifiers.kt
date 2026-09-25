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

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.coneType

// A qualifier's `resolvedType` is its companion object's type (or `kotlin.Unit`), not the class it names.
internal fun FirResolvedQualifier.namedClassSymbol(session: FirSession): FirRegularClassSymbol? =
    when (val symbol = qualifierSymbol) {
        is FirRegularClassSymbol -> symbol
        is FirTypeAliasSymbol -> symbol.resolvedExpandedTypeRef.coneType.toRegularClassSymbol(session)
        else -> null
    }
