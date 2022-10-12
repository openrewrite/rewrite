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
package org.openrewrite.properties;

import org.openrewrite.*;
import org.openrewrite.marker.Range;
import org.openrewrite.properties.internal.PropertiesPrinter;
import org.openrewrite.properties.tree.Properties;

import java.util.IdentityHashMap;
import java.util.Map;

import static org.openrewrite.Tree.randomId;

public class UpdateSourcePositions extends Recipe {

    @Override
    public String getDisplayName() {
        return "Update Properties source positions";
    }

    @Override
    public String getDescription() {
        return "Calculate start position and length for every Properties AST element.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        Map<Tree, Range> positionMap = new IdentityHashMap<>();
        PositionPrintOutputCapture ppoc = new PositionPrintOutputCapture(new InMemoryExecutionContext());

        PropertiesPrinter<ExecutionContext> printer = new PropertiesPrinter<ExecutionContext>() {

            @Override
            public void printEntry(Properties.Entry entry, PrintOutputCapture<ExecutionContext> p) {
                Range.Position startPosition = ppoc.getPosition();
                super.printEntry(entry, p);
                Range.Position endPosition = ppoc.getPosition();
                positionMap.put(entry, new Range(randomId(), startPosition, endPosition));
            }

            @Override
            public void printComment(Properties.Comment comment, PrintOutputCapture<ExecutionContext> p) {
                Range.Position startPosition = ppoc.getPosition();
                super.printComment(comment, p);
                Range.Position endPosition = ppoc.getPosition();
                positionMap.put(comment, new Range(randomId(), startPosition, endPosition));
            }


            @Override
            protected void printValue(Properties.Value value, PrintOutputCapture<ExecutionContext> p) {
                Range.Position startPosition = ppoc.getPosition();
                super.printValue(value, p);
                Range.Position endPosition = ppoc.getPosition();
                positionMap.put(value, new Range(randomId(), startPosition, endPosition));
            }
        };

        return new PropertiesIsoVisitor<ExecutionContext>() {

            boolean firstVisit = true;

            @Override
            public Properties visit(Tree tree, ExecutionContext ctx) {
                if (tree == null) {
                    return null;
                }
                if (firstVisit) {
                    tree = printer.visit(tree, ppoc);
                    firstVisit = false;
                }

                Range range = positionMap.get(tree);
                if (range != null) {
                    Tree t = tree.withMarkers(tree.getMarkers().add(range));
                    return super.visit(t, ctx);
                }
                return super.visit(tree, ctx);
            }
        };
    }

}
