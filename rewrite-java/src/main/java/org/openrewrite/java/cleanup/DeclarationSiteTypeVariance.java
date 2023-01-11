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
package org.openrewrite.java.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.openrewrite.java.tree.J.Wildcard.Bound.Extends;
import static org.openrewrite.java.tree.J.Wildcard.Bound.Super;

@Value
@EqualsAndHashCode(callSuper = true)
public class DeclarationSiteTypeVariance extends Recipe {

    @Option(displayName = "Variant types",
            description = "A list of well-known classes that have in/out type variance.",
            example = "java.util.function.Function<IN, OUT>")
    List<String> variantTypes;

    @Option(displayName = "Excluded bounds",
            description = "A list of bounds that should not receive explicit variance. Globs supported.",
            example = "java.lang.*",
            required = false)
    @Nullable
    List<String> excludedBounds;

    @Option(displayName = "Exclude final classes",
            description = "If true, do not add `? extends` variance to final classes. " +
                          "`? super` variance will be added regardless of finality.",
            required = false)
    @Nullable
    Boolean excludeFinalClasses;

    @Override
    public String getDisplayName() {
        return "Properly use declaration-site type variance";
    }

    @Override
    public String getDescription() {
        return "Currently, Java requires use-site type variance, so if someone has `Function<IN, OUT>` method parameter, it should rather be `Function<? super IN, ? extends OUT>`. " +
               "Unfortunately, it is not easy to notice that `? super` and `? extends` is missing, so this recipe adds it where that would improve the situation.";
    }

    @Override
    public Validated validate() {
        Validated v = super.validate();
        for (String variantType : variantTypes) {
            v = v.and(Validated.test("variantTypes", "Must be a valid variant type", variantType, vt -> {
                try {
                    VariantTypeSpec.build(vt);
                    return true;
                } catch (Throwable ignored) {
                    return false;
                }
            }));
        }
        return v;
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        List<VariantTypeSpec> variantTypeSpecs = variantTypes.stream().map(VariantTypeSpec::build).collect(Collectors.toList());
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                if (m.getMethodType() != null && m.getMethodType().isOverride()) {
                    return m;
                }
                return m.withParameters(ListUtils.map(m.getParameters(), param -> {
                    if (param instanceof J.VariableDeclarations) {
                        J.VariableDeclarations varParam = (J.VariableDeclarations) param;
                        if (varParam.getTypeExpression() instanceof J.ParameterizedType) {
                            J.ParameterizedType pt = (J.ParameterizedType) varParam.getTypeExpression();
                            for (VariantTypeSpec variantTypeSpec : variantTypeSpecs) {
                                if (variantTypeSpec.hasType(pt)) {
                                    return varParam.withTypeExpression(useDeclarationSiteVariance(pt, variantTypeSpec));
                                }
                            }

                        }
                    }
                    return param;
                }));
            }

            private J.ParameterizedType useDeclarationSiteVariance(J.ParameterizedType pt, VariantTypeSpec spec) {
                return pt.withTypeParameters(ListUtils.map(pt.getTypeParameters(), (i, tp) -> {
                    VariantTypeSpec.Variance variance = spec.getVariances().get(i);
                    if (tp instanceof J.Wildcard ||
                        !(tp instanceof NameTree) ||
                        variance == VariantTypeSpec.Variance.INVARIANT) {
                        return tp;
                    }

                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(tp.getType());
                    if (fq != null) {
                        if (excludedBounds != null) {
                            for (String excludedBound : excludedBounds) {
                                if (StringUtils.matchesGlob(fq.getFullyQualifiedName(), excludedBound)) {
                                    return tp;
                                }
                            }
                        }
                        if (Boolean.TRUE.equals(excludeFinalClasses) && fq.getFlags().contains(Flag.Final) &&
                            variance == VariantTypeSpec.Variance.OUT) {
                            return tp;
                        }
                    }

                    return new J.Wildcard(
                            Tree.randomId(),
                            tp.getPrefix(),
                            Markers.EMPTY,
                            JLeftPadded.build(variance == VariantTypeSpec.Variance.OUT ? Extends : Super)
                                    .withBefore(Space.format(" ")),
                            tp.withPrefix(Space.format(" "))
                    );
                }));
            }
        };
    }

    @Value
    private static class VariantTypeSpec {
        String fullyQualifiedName;
        List<Variance> variances;

        enum Variance {
            IN,
            OUT,
            INVARIANT
        }

        public boolean hasType(J.ParameterizedType pt) {
            return TypeUtils.isOfClassType(pt.getType(), fullyQualifiedName);
        }

        public static VariantTypeSpec build(String pattern) {
            String fqn = pattern.substring(0, pattern.indexOf('<'));
            String variancesStr = pattern.substring(pattern.indexOf('<') + 1, pattern.lastIndexOf('>'));
            return new VariantTypeSpec(fqn, Arrays.stream(variancesStr.split(","))
                    .map(String::trim)
                    .map(Variance::valueOf)
                    .collect(Collectors.toList()));
        }
    }
}
