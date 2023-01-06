/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.kotlin;

import org.jetbrains.kotlin.descriptors.ClassKind;
import org.jetbrains.kotlin.fir.FirElement;
import org.jetbrains.kotlin.fir.declarations.FirClass;
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus;
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol;
import org.jetbrains.kotlin.fir.types.ConeClassLikeType;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.StandardClassIds;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeMapping;
import org.openrewrite.java.internal.JavaReflectionTypeMapping;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.JavaType;

public class KotlinTypeMapping implements JavaTypeMapping<FirElement> {
    private final KotlinTypeSignatureBuilder signatureBuilder = new KotlinTypeSignatureBuilder();
    private final JavaTypeCache typeCache;
    private final JavaReflectionTypeMapping reflectionTypeMapping;


    public KotlinTypeMapping(JavaTypeCache typeCache) {
        this.typeCache = typeCache;
        this.reflectionTypeMapping = new JavaReflectionTypeMapping(typeCache);
    }

    public JavaType type(@Nullable FirElement type) {
        if (type == null) {
            return JavaType.Class.Unknown.getInstance();
        }

        String signature = signatureBuilder.signature(type);
        JavaType existing = typeCache.get(signature);
        if (existing != null) {
            return existing;
        }

        if (type instanceof FirClass) {
            return classType((FirClass) type, signature);
        }

        throw new UnsupportedOperationException("Unknown type " + type.getClass().getName());
    }

    private JavaType.FullyQualified classType(FirClass firClass, String signature) {
        FirClassSymbol<? extends FirClass> sym = firClass.getSymbol();

        String classFqn = sym.getClassId().asFqNameString();

        JavaType.FullyQualified fq = typeCache.get(classFqn);
        JavaType.Class clazz = (JavaType.Class) (fq instanceof JavaType.Parameterized ? ((JavaType.Parameterized) fq).getType() : fq);
        if (clazz == null) {
            clazz = new JavaType.Class(
                    null,
                    convertToFlagsBitMap(firClass.getStatus()),
                    classFqn,
                    convertToClassKind(firClass.getClassKind()),
                    null, null, null, null, null, null, null
            );
        }

        return clazz;
    }

    public JavaType.Primitive primitive(ConeClassLikeType type) {
        ClassId classId = type.getLookupTag().getClassId();
        if (StandardClassIds.INSTANCE.getByte().equals(classId)) {
            return JavaType.Primitive.Byte;
        } else if (StandardClassIds.INSTANCE.getBoolean().equals(classId)) {
            return JavaType.Primitive.Boolean;
        } else if (StandardClassIds.INSTANCE.getChar().equals(classId)) {
            return JavaType.Primitive.Char;
        } else if (StandardClassIds.INSTANCE.getDouble().equals(classId)) {
            return JavaType.Primitive.Double;
        } else if (StandardClassIds.INSTANCE.getFloat().equals(classId)) {
            return JavaType.Primitive.Float;
        } else if (StandardClassIds.INSTANCE.getInt().equals(classId)) {
            return JavaType.Primitive.Int;
        } else if (StandardClassIds.INSTANCE.getLong().equals(classId)) {
            return JavaType.Primitive.Long;
        } else if (StandardClassIds.INSTANCE.getShort().equals(classId)) {
            return JavaType.Primitive.Short;
        } else if (StandardClassIds.INSTANCE.getString().equals(classId)) {
            return JavaType.Primitive.String;
        } else if (StandardClassIds.INSTANCE.getUnit().equals(classId)) {
            return JavaType.Primitive.Void;
        }

        throw new UnsupportedOperationException("Unknown primitive type " + type);
    }

    private long convertToFlagsBitMap(FirDeclarationStatus status) {
        // TODO ... map status to eq flags.
        return 0;
    }

    private JavaType.FullyQualified.Kind convertToClassKind(ClassKind classKind) {
        if (ClassKind.CLASS == classKind) {
            return JavaType.FullyQualified.Kind.Class;
        } else if (ClassKind.ANNOTATION_CLASS == classKind) {
            return JavaType.FullyQualified.Kind.Annotation;
        } else if (ClassKind.ENUM_CLASS == classKind) {
            return JavaType.FullyQualified.Kind.Enum;
        }

        throw new UnsupportedOperationException("Unknown class kind" + classKind.name());
    }
}
