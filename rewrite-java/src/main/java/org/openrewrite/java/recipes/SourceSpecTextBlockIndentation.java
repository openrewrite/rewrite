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
package org.openrewrite.java.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.regex.Pattern;

public class SourceSpecTextBlockIndentation extends Recipe {
    @Override
    public String getDisplayName() {
        return "Minimal indentation for `SourceSpecs` text blocks";
    }

    @Override
    public String getDescription() {
        return "Text blocks that assert before and after source code should have minimal indentation.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            final Pattern endTextBlockOnOwnLine = Pattern.compile("\\s+\"\"\"\\s*$");

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (method.getMethodType() != null && TypeUtils.isOfClassType(method.getMethodType().getReturnType(),
                        "org.openrewrite.test.SourceSpecs")) {
                    return method.withArguments(ListUtils.map(method.getArguments(), argument -> {
                        if (TypeUtils.isString(argument.getType()) && argument instanceof J.Literal) {
                            J.Literal source = (J.Literal) argument;
                            if (source.getValueSource() != null && source.getValueSource().startsWith("\"\"\"") &&
                                endTextBlockOnOwnLine.matcher(source.getValueSource()).find()) {

                                String[] lines = source.getValueSource().split("\n");
                                int[] indentations = new int[lines.length - 1];
                                boolean[] nonSpaceCharacter = new boolean[lines.length - 1];
                                Arrays.fill(indentations, 0);
                                Arrays.fill(nonSpaceCharacter, false);

                                nextLine:
                                for (int i = 1; i < lines.length; i++) {
                                    String line = lines[i];
                                    for (int j = 0; j < line.length(); j++) {
                                        if (line.charAt(j) == ' ') {
                                            indentations[i - 1]++;
                                        } else {
                                            nonSpaceCharacter[i - 1] = true;
                                            continue nextLine;
                                        }
                                    }
                                }

                                int expectedIndent = indentations[indentations.length - 1];
                                if (indentations.length >= 2 &&
                                    nonSpaceCharacter[0] &&
                                    nonSpaceCharacter[indentations.length - 2] &&
                                    indentations[0] == indentations[indentations.length - 2] &&
                                    indentations[0] >= expectedIndent) {

                                    for (int i = 0; i < indentations.length - 1; i++) {
                                        if (nonSpaceCharacter[i] && indentations[i] < indentations[0]) {
                                            // the first and last lines of the source code are further
                                            // right that some other block of code in the middle
                                            return argument;
                                        }
                                    }

                                    int marginTrim = indentations[0] - expectedIndent;
                                    StringJoiner fixedSource = new StringJoiner("\n");
                                    for (int i = 0; i < lines.length; i++) {
                                        String line = lines[i];
                                        if (i == 0 || i == lines.length - 1 || indentations[i - 1] < expectedIndent) {
                                            fixedSource.add(line);
                                        } else {
                                            fixedSource.add(line.substring(marginTrim));
                                        }
                                    }

                                    J.Literal withFixedSource = source.withValueSource(fixedSource.toString());
                                    if (withFixedSource.getPrefix().getComments().isEmpty()
                                        && withFixedSource.getPrefix().getWhitespace().isEmpty()) {
                                        return maybeAutoFormat(withFixedSource, withFixedSource.withPrefix(Space.format("\n")), ctx);
                                    }
                                    return withFixedSource;
                                }

                            }
                        }
                        return argument;
                    }));
                }
                return super.visitMethodInvocation(method, ctx);
            }
        };
    }
}
