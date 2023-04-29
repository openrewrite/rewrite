/*
 * Copyright 2020 the original author or authors.
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

import org.openrewrite.Preconditions;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class PrimitiveWrapperClassConstructorToValueOf extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use primitive wrapper `valueOf` method";
    }

    @Override
    public String getDescription() {
        return "The constructor of all primitive types has been deprecated in favor of using the static factory method `valueOf` available for each of the primitive type wrappers.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2129");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> condition = Preconditions.or(
                new UsesType<>("java.lang.Boolean", false),
                new UsesType<>("java.lang.Byte", false),
                new UsesType<>("java.lang.Character", false),
                new UsesType<>("java.lang.Double", false),
                new UsesType<>("java.lang.Float", false),
                new UsesType<>("java.lang.Integer", false),
                new UsesType<>("java.lang.Long", false),
                new UsesType<>("java.lang.Short", false)
        );
        return Preconditions.check(condition, new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
                J.NewClass nc = (J.NewClass) super.visitNewClass(newClass, executionContext);
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(nc.getType());
                if (type != null && nc.getArguments().size() == 1) {
                    Expression arg = nc.getArguments().get(0);
                    JavaTemplate.Builder valueOf;
                    switch (type.getFullyQualifiedName()) {
                        case "java.lang.Boolean":
                            valueOf = JavaTemplate.builder(this::getCursor, "Boolean.valueOf(#{any(boolean)})");
                            break;
                        case "java.lang.Byte":
                            valueOf = JavaTemplate.builder(this::getCursor, "Byte.valueOf(#{any(byte)})");
                            break;
                        case "java.lang.Character":
                            valueOf = JavaTemplate.builder(this::getCursor, "Character.valueOf(#{any(char)})");
                            break;
                        case "java.lang.Double":
                            valueOf = JavaTemplate.builder(this::getCursor, "Double.valueOf(#{any(double)})");
                            break;
                        case "java.lang.Integer":
                            valueOf = JavaTemplate.builder(this::getCursor, "Integer.valueOf(#{any(int)})");
                            break;
                        case "java.lang.Long":
                            valueOf = JavaTemplate.builder(this::getCursor, "Long.valueOf(#{any(long)})");
                            break;
                        case "java.lang.Short":
                            valueOf = JavaTemplate.builder(this::getCursor, "Short.valueOf(#{any(short)})");
                            break;
                        case "java.lang.Float":
                            if (arg instanceof J.Literal && JavaType.Primitive.Double == ((J.Literal) arg).getType()) {
                                arg = ((J.Literal) arg).withType(JavaType.Primitive.String);
                                arg = ((J.Literal) arg).withValueSource("\"" + ((J.Literal) arg).getValue() + "\"");
                            }

                            JavaType argType = arg.getType();
                            if (TypeUtils.isOfClassType(argType, "java.lang.Double")) {
                                valueOf = JavaTemplate.builder(this::getCursor, "Float.valueOf(#{any(java.lang.Double)}.floatValue())");
                            } else {
                                valueOf = JavaTemplate.builder(this::getCursor, "Float.valueOf(#{any(float)})");
                            }
                            break;
                        default:
                            return nc;
                    }
                    return nc.withTemplate(valueOf.build(), nc.getCoordinates().replace(), arg);
                }
                return nc;
            }
        });
    }
}
