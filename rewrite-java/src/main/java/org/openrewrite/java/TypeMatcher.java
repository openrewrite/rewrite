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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.grammar.MethodSignatureLexer;
import org.openrewrite.java.internal.grammar.MethodSignatureParser;
import org.openrewrite.java.internal.grammar.MethodSignatureParserBaseVisitor;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.regex.Pattern;

@SuppressWarnings("NotNullFieldNotInitialized")
@Getter
public class TypeMatcher {
    private Pattern targetTypePattern;

    private final String signature;

    /**
     * Whether to match overridden forms of the method on subclasses of {@link #targetTypePattern}.
     */
    private final boolean matchInherited;

    public TypeMatcher(String signature, boolean matchInherited) {
        this.signature = signature;
        this.matchInherited = matchInherited;
        MethodSignatureParser parser = new MethodSignatureParser(new CommonTokenStream(new MethodSignatureLexer(
                CharStreams.fromString(signature + " *(..)"))));

        new MethodSignatureParserBaseVisitor<Void>() {
            @Override
            public Void visitMethodPattern(MethodSignatureParser.MethodPatternContext ctx) {
                targetTypePattern = Pattern.compile(new TypeVisitor().visitTargetTypePattern(ctx.targetTypePattern()));
                return null;
            }
        }.visit(parser.methodPattern());
    }

    public TypeMatcher(String signature) {
        this(signature, false);
    }

    public boolean matches(@Nullable JavaType type) {
        return matchesTargetType(TypeUtils.asFullyQualified(type));
    }

    public boolean matches(@Nullable TypeTree tt) {
        return tt != null && matches(tt.getType());
    }

    public boolean matchesPackage(String packageName) {
        return targetTypePattern.matcher(packageName).matches() ||
               targetTypePattern.matcher(packageName.replaceAll("\\.\\*$",
                       "." + signature.substring(signature.lastIndexOf('.') + 1))).matches();
    }

    boolean matchesTargetType(@Nullable JavaType.FullyQualified type) {
        if (type == null) {
            return false;
        }

        if (targetTypePattern.matcher(type.getFullyQualifiedName()).matches()) {
            return true;
        } else if (!"java.lang.Object".equals(type.getFullyQualifiedName()) && matchesTargetType(type.getSupertype() == null ? JavaType.ShallowClass.build("java.lang.Object") : type.getSupertype())) {
            return true;
        }

        if (matchInherited) {
            if (matchesTargetType(type.getSupertype())) {
                return true;
            }

            for (JavaType.FullyQualified anInterface : type.getInterfaces()) {
                if (matchesTargetType(anInterface)) {
                    return true;
                }
            }
        }

        return false;
    }
}
