/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.tree;

import org.openrewrite.Incubating;
import org.openrewrite.java.tree.JavaType;

@Incubating(since = "0.0")
public class TsType {
    public static final JavaType.ShallowClass Any = JavaType.ShallowClass.build("any");
    public static final JavaType.ShallowClass Number = JavaType.ShallowClass.build("number");
    public static final JavaType.ShallowClass Undefined = JavaType.ShallowClass.build("undefined");
    public static final JavaType.ShallowClass Unit = JavaType.ShallowClass.build("unit");
    public static final JavaType.ShallowClass Unknown = JavaType.ShallowClass.build("unknown");

    public static final JavaType.ShallowClass Anonymous = JavaType.ShallowClass.build("type.analysis.Anonymous");
    public static final JavaType.ShallowClass MergedInterface = JavaType.ShallowClass.build("type.analysis.MergedInterface");
    public static final JavaType.ShallowClass MissingSymbol = JavaType.ShallowClass.build("type.analysis.MissingSymbol");
    public static final JavaType.ShallowClass PrimitiveUnion = JavaType.ShallowClass.build("type.analysis.PrimitiveUnion");
    public static final JavaType.ShallowClass Union = JavaType.ShallowClass.build("type.analysis.Union");

    public static final JavaType.ShallowClass Enum = JavaType.ShallowClass.build("type.analysis.Enum");
    public static final JavaType.ShallowClass EnumLiteral = JavaType.ShallowClass.build("type.analysis.EnumLiteral");
    public static final JavaType.ShallowClass BigInt = JavaType.ShallowClass.build("type.analysis.BigInt");
    public static final JavaType.ShallowClass BigIntLiteral = JavaType.ShallowClass.build("type.analysis.BigIntLiteral");
    public static final JavaType.ShallowClass ESSymbol = JavaType.ShallowClass.build("type.analysis.ESSymbol");
    public static final JavaType.ShallowClass UniqueESSymbol = JavaType.ShallowClass.build("type.analysis.UniqueESSymbol");
    public static final JavaType.ShallowClass Never = JavaType.ShallowClass.build("type.analysis.Never");
    public static final JavaType.ShallowClass TypeParameter = JavaType.ShallowClass.build("type.analysis.TypeParameter");
    public static final JavaType.ShallowClass Intersection = JavaType.ShallowClass.build("type.analysis.Intersection");
    public static final JavaType.ShallowClass Index = JavaType.ShallowClass.build("type.analysis.Index");
    public static final JavaType.ShallowClass IndexedAccess = JavaType.ShallowClass.build("type.analysis.IndexedAccess");
    public static final JavaType.ShallowClass Conditional = JavaType.ShallowClass.build("type.analysis.Conditional");
    public static final JavaType.ShallowClass Substitution = JavaType.ShallowClass.build("type.analysis.Substitution");
    public static final JavaType.ShallowClass NonPrimitive = JavaType.ShallowClass.build("type.analysis.NonPrimitive");
    public static final JavaType.ShallowClass TemplateLiteral = JavaType.ShallowClass.build("type.analysis.TemplateLiteral");
    public static final JavaType.ShallowClass StringMapping = JavaType.ShallowClass.build("type.analysis.StringMapping");
    public static final JavaType.ShallowClass AnyOrUnknown = JavaType.ShallowClass.build("type.analysis.AnyOrUnknown");
    public static final JavaType.ShallowClass Nullable = JavaType.ShallowClass.build("type.analysis.Nullable");
    public static final JavaType.ShallowClass Literal = JavaType.ShallowClass.build("type.analysis.Literal");
    public static final JavaType.ShallowClass Freshable = JavaType.ShallowClass.build("type.analysis.Freshable");
    public static final JavaType.ShallowClass StringOrNumberLiteral = JavaType.ShallowClass.build("type.analysis.StringOrNumberLiteral");
    public static final JavaType.ShallowClass StringOrNumberLiteralOrUnique = JavaType.ShallowClass.build("type.analysis.StringOrNumberLiteralOrUnique");
    public static final JavaType.ShallowClass DefinitelyFalsy = JavaType.ShallowClass.build("type.analysis.DefinitelyFalsy");
    public static final JavaType.ShallowClass PossiblyFalsy = JavaType.ShallowClass.build("type.analysis.PossiblyFalsy");
    public static final JavaType.ShallowClass Intrinsic = JavaType.ShallowClass.build("type.analysis.Intrinsic");
    public static final JavaType.ShallowClass Primitive = JavaType.ShallowClass.build("type.analysis.Primitive");
    public static final JavaType.ShallowClass StringLike = JavaType.ShallowClass.build("type.analysis.StringLike");
    public static final JavaType.ShallowClass NumberLike = JavaType.ShallowClass.build("type.analysis.NumberLike");
    public static final JavaType.ShallowClass BigIntLike = JavaType.ShallowClass.build("type.analysis.BigIntLike");
    public static final JavaType.ShallowClass BooleanLike = JavaType.ShallowClass.build("type.analysis.BooleanLike");
    public static final JavaType.ShallowClass EnumLike = JavaType.ShallowClass.build("type.analysis.EnumLike");
    public static final JavaType.ShallowClass ESSymbolLike = JavaType.ShallowClass.build("type.analysis.ESSymbolLike");
    public static final JavaType.ShallowClass VoidLike = JavaType.ShallowClass.build("type.analysis.VoidLike");
    public static final JavaType.ShallowClass DefinitelyNonNullable = JavaType.ShallowClass.build("type.analysis.DefinitelyNonNullable");
    public static final JavaType.ShallowClass DisjointDomains = JavaType.ShallowClass.build("type.analysis.DisjointDomains");
    public static final JavaType.ShallowClass UnionOrIntersection = JavaType.ShallowClass.build("type.analysis.UnionOrIntersection");
    public static final JavaType.ShallowClass StructuredType = JavaType.ShallowClass.build("type.analysis.StructuredType");
    public static final JavaType.ShallowClass TypeVariable = JavaType.ShallowClass.build("type.analysis.TypeVariable");
    public static final JavaType.ShallowClass InstantiableNonPrimitive = JavaType.ShallowClass.build("type.analysis.InstantiableNonPrimitive");
    public static final JavaType.ShallowClass InstantiablePrimitive = JavaType.ShallowClass.build("type.analysis.InstantiablePrimitive");
    public static final JavaType.ShallowClass Instantiable = JavaType.ShallowClass.build("type.analysis.Instantiable");
    public static final JavaType.ShallowClass StructuredOrInstantiable = JavaType.ShallowClass.build("type.analysis.StructuredOrInstantiable");
    public static final JavaType.ShallowClass ObjectFlagsType = JavaType.ShallowClass.build("type.analysis.ObjectFlagsType");
    public static final JavaType.ShallowClass Simplifiable = JavaType.ShallowClass.build("type.analysis.Simplifiable");
    public static final JavaType.ShallowClass Singleton = JavaType.ShallowClass.build("type.analysis.Singleton");
    public static final JavaType.ShallowClass Narrowable = JavaType.ShallowClass.build("type.analysis.Narrowable");
    public static final JavaType.ShallowClass IncludesMask = JavaType.ShallowClass.build("type.analysis.IncludesMask");
    public static final JavaType.ShallowClass NotPrimitiveUnion = JavaType.ShallowClass.build("type.analysis.NotPrimitiveUnion");
}
