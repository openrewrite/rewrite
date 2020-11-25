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
package org.openrewrite.java;

import org.openrewrite.Cursor;
import org.openrewrite.Tree;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.IntStream.range;

/**
 * Records a list of every AST element visited, in what order it was visited, and how many levels deep it is in its tree
 */
public class VisitRecorder extends AbstractJavaSourceVisitor<List<VisitRecorder.VisitRecord>> {
    public static class VisitRecord {
        final int depth;
        final Tree tree;

        public VisitRecord(int depth, Tree tree) {
            this.depth = depth;
            this.tree = tree;
        }

        @Override
        public String toString() {
            String padding = range(0, depth + 1).mapToObj(it -> " ").collect(Collectors.joining());
            return padding + tree;
        }
    }

    public VisitRecorder() {
        setCursoringOn();
    }

    @Override
    public List<VisitRecorder.VisitRecord> defaultTo(Tree t) {
        return emptyList();
    }

    @Override
    public List<VisitRecord> visitTree(Tree tree) {
        int parentCount = countParents(getCursor());
        return singletonList(new VisitRecord(parentCount, tree));
    }

    public int countParents(Cursor cursor) {
        return (int) cursor.getPathAsStream().count();
    }

    public static String printVisitRecords(List<VisitRecord> records) {
        StringBuilder sb = new StringBuilder();
        for(VisitRecord record : records) {
            sb.append(record.toString())
                    .append("\n");
        }
        return sb.toString();
    }
}
