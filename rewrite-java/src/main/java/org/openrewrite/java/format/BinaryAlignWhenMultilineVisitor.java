/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.format;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.tree.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BinaryAlignWhenMultilineVisitor<P> extends TreeVisitor<Tree, P> {

    @Override
    public @Nullable Tree visit(@Nullable Tree tree, P p) {
        if (tree instanceof JavaSourceFile) {
            return new BinaryAlignWhenMultilineJavaVisitor<>().visit(tree, p);
        }
        return tree;
    }

    private static class BinaryAlignWhenMultilineJavaVisitor<P> extends JavaIsoVisitor<P> {
        final Map<J.Binary, Integer> binaryOffsetMap = new HashMap<>();

        @Override
        public @Nullable J visit(@Nullable Tree tree, P p) {
            if (tree instanceof JavaSourceFile) {
                new CollectBinaryOffset().collect((JavaSourceFile) tree, binaryOffsetMap);
            }
            return super.visit(tree, p);
        }

        @Override
        public J.Binary visitBinary(J.Binary binary, P p) {
            if (!binaryOffsetMap.containsKey(binary)) {
                return binary;
            }
            int offset = binaryOffsetMap.get(binary);

            // align operator
            JLeftPadded<J.Binary.Type> op = binary.getPadding().getOperator();
            Space opPrefix = op.getBefore();
            if (!opPrefix.getComments().isEmpty()) {
                String suffix = opPrefix.getComments().get(opPrefix.getComments().size() - 1).getSuffix();
                if (suffix.contains("\n")) {
                    String newSuffix = replaceLeadingSpaces(suffix, offset);
                    if (!newSuffix.equals(suffix)) {
                        List<Comment> newComments = ListUtils.mapLast(opPrefix.getComments(), c -> c.withSuffix(newSuffix));
                        opPrefix = opPrefix.withComments(newComments);
                        binary = binary.getPadding().withOperator(op.withBefore(opPrefix));
                    }
                }
            }

            if (opPrefix.getWhitespace().contains("\n")) {
                opPrefix = opPrefix.withWhitespace(replaceLeadingSpaces(opPrefix.getWhitespace(), offset));
                binary = binary.getPadding().withOperator(op.withBefore(opPrefix));
            }

            // align RHS
            Expression rhs = binary.getRight();
            Space rhsPrefix = rhs.getPrefix();
            if (rhsPrefix.getWhitespace().contains("\n")) {
                rhsPrefix = rhsPrefix.withWhitespace(replaceLeadingSpaces(rhsPrefix.getWhitespace(), offset));
                rhs = rhs.withPrefix(rhsPrefix);
            }
            binary = binary.withRight(rhs);
            return super.visitBinary(binary, p);
        }
    }

    private static String generateSpaces(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0 ; i < n; i ++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    private static String replaceLeadingSpaces(String input, int fixedSpaces) {
        Pattern pattern =  Pattern.compile("(?<=\\n)(\\s+)");
        Matcher matcher = pattern.matcher(input);
        String replacement = generateSpaces(fixedSpaces);
        return matcher.replaceFirst(replacement);
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class CollectBinaryOffset extends JavaPrinter<Map<J.Binary, Integer>> {
        public void collect(J j, Map<J.Binary, Integer> binaryOffsetMap) {
            new CollectBinaryOffset().visit(j, new PrintOutputCapture<>(binaryOffsetMap));
        }

        @Override
        public J visitBinary(J.Binary binary, PrintOutputCapture<Map<J.Binary, Integer>> p) {
            int offset;
            int prefixNewLineOffset = binary.getPrefix().getWhitespace().indexOf('\n');
            if (prefixNewLineOffset == -1) {
                offset = findLastNewlineDistance(p.out) + binary.getPrefix().getWhitespace().length();
            } else {
                offset = binary.getPrefix().getWhitespace().length() - 1 - prefixNewLineOffset;
            }

            Map<J.Binary, Integer> offsetMap = p.getContext();
            offsetMap.put(binary, offset);
            return super.visitBinary(binary, p);
        }

        private static int findLastNewlineDistance(StringBuilder sb) {
            int lastIndex = sb.lastIndexOf("\n");
            if (lastIndex != -1) {
                return sb.length() - 1 - lastIndex;
            } else {
                return sb.length();
            }
        }
    }
}
