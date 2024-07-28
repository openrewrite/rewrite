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
package org.openrewrite.java;

import lombok.Getter;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.grammar.MethodSignatureLexer;
import org.openrewrite.java.internal.grammar.MethodSignatureParser;
import org.openrewrite.java.internal.grammar.MethodSignatureParserBaseVisitor;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.regex.Pattern;

import static org.openrewrite.java.tree.TypeUtils.fullyQualifiedNamesAreEqual;

@Getter
public class TypeMatcher {
    private static final String ASPECTJ_DOT_PATTERN = StringUtils.aspectjNameToPattern(".");

    @SuppressWarnings("NotNullFieldNotInitialized")
    @Getter
    private Pattern targetTypePattern;

    @Nullable
    private String targetType;

    private final String signature;

    /**
     * Whether to match on subclasses of {@link #targetTypePattern}.
     */
    @Getter
    private final boolean matchInherited;

    public TypeMatcher(@Nullable String fieldType) {
        this(fieldType, false);
    }

    public TypeMatcher(@Nullable String fieldType, boolean matchInherited) {
        this.signature = fieldType == null ? ".*" : fieldType;
        this.matchInherited = matchInherited;

        if (StringUtils.isBlank(fieldType)) {
            targetTypePattern = Pattern.compile(".*");
        } else {
            MethodSignatureParser parser = new MethodSignatureParser(new CommonTokenStream(new MethodSignatureLexer(
                    CharStreams.fromString(fieldType))));

            new MethodSignatureParserBaseVisitor<Void>() {
                @Override
                public Void visitTargetTypePattern(MethodSignatureParser.TargetTypePatternContext ctx) {
                    String pattern = new TypeVisitor().visitTargetTypePattern(ctx);
                    if (isPlainIdentifier(ctx)) {
                        targetType = pattern;
                    }
                    targetTypePattern = Pattern.compile(StringUtils.aspectjNameToPattern(pattern));
                    return null;
                }
            }.visitTargetTypePattern(parser.targetTypePattern());
        }
    }

    public boolean matches(@Nullable TypeTree tt) {
        return tt != null && matches(tt.getType());
    }

    public boolean matchesPackage(String packageName) {
        return targetTypePattern.matcher(packageName).matches() ||
               targetTypePattern.matcher(packageName.replaceAll("\\.\\*$",
                       "." + signature.substring(signature.lastIndexOf('.') + 1))).matches();
    }

    public boolean matches(@Nullable JavaType type) {
        return TypeUtils.isOfTypeWithName(
                TypeUtils.asFullyQualified(type),
                matchInherited,
                this::matchesTargetTypeName
        );
    }

    private boolean matchesTargetTypeName(String fullyQualifiedTypeName) {
        return this.targetType != null && fullyQualifiedNamesAreEqual(this.targetType, fullyQualifiedTypeName) ||
               this.targetTypePattern.matcher(fullyQualifiedTypeName).matches();
    }

    private static boolean isPlainIdentifier(MethodSignatureParser.TargetTypePatternContext context) {
        return context.BANG() == null &&
               context.AND() == null &&
               context.OR() == null &&
               context.classNameOrInterface().DOTDOT().isEmpty() &&
               context.classNameOrInterface().WILDCARD().isEmpty();
    }
}
