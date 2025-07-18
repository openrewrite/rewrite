/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.graphql;

import org.junit.jupiter.api.Test;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.graphql.internal.GraphQlPrinter;
import org.openrewrite.graphql.tree.GraphQl;

import static org.junit.jupiter.api.Assertions.*;

class DebugArgumentTest {
    
    @Test
    void debugArgumentParsing() {
        String source = """
            query {
              user(
                id: "123", name: "John",
                active: true
              ) {
                id
              }
            }
            """;
        
        GraphQlParser parser = GraphQlParser.builder().build();
        var parsed = parser.parse(source).findFirst().orElseThrow();
        
        if (parsed instanceof org.openrewrite.tree.ParseError) {
            fail("Failed to parse: " + parsed);
        }
        
        GraphQl.Document doc = (GraphQl.Document) parsed;
        GraphQl.OperationDefinition op = (GraphQl.OperationDefinition) doc.getDefinitions().get(0);
        GraphQl.SelectionSet sel = op.getSelectionSet();
        GraphQl.Field field = (GraphQl.Field) sel.getSelections().get(0);
        GraphQl.Arguments args = field.getArguments();
        
        System.out.println("Number of arguments: " + args.getArguments().size());
        
        for (int i = 0; i < args.getArguments().size(); i++) {
            GraphQl.Argument arg = args.getArguments().get(i);
            String argName = arg.getName().getValue();
            String after = "";
            
            System.out.println("Argument " + i + " (" + argName + "):");
            System.out.println("  Prefix: '" + arg.getPrefix().getWhitespace().replace("\n", "\\n") + "'");
            System.out.println("  After: '" + after.replace("\n", "\\n") + "'");
            System.out.println("  After contains comma: " + after.contains(","));
        }
        
        // Print the parsed document
        PrintOutputCapture<Integer> output = new PrintOutputCapture<>(0);
        new GraphQlPrinter<Integer>().visit(doc, output);
        String printed = output.out.toString();
        
        System.out.println("\nPrinted output:");
        System.out.println(printed);
    }
}