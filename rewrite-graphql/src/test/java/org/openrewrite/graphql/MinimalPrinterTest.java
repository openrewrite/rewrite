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

public class MinimalPrinterTest {
    
    @Test
    void testMinimalPrinter() throws Exception {
        java.io.PrintWriter log = new java.io.PrintWriter(new java.io.FileWriter("/tmp/graphql-printer-debug.log"));
        // Create a minimal GraphQL AST manually
        GraphQl.Name queryName = new GraphQl.Name(
            java.util.UUID.randomUUID(),
            org.openrewrite.graphql.tree.Space.EMPTY,
            org.openrewrite.marker.Markers.EMPTY,
            "query"
        );
        
        // Create a minimal printer that logs everything
        class LoggingPrinter<P> extends GraphQlVisitor<PrintOutputCapture<P>> {
            @Override
            public GraphQl visitName(GraphQl.Name name, PrintOutputCapture<P> p) {
                log.println("LoggingPrinter.visitName called with: " + name.getValue());
                p.append("[NAME:" + name.getValue() + "]");
                return name;
            }
        }
        
        // Test 1: Direct visitor
        log.println("=== Test 1: Direct visitor ===");
        PrintOutputCapture<Void> out1 = new PrintOutputCapture<>(null);
        LoggingPrinter<Void> printer1 = new LoggingPrinter<>();
        printer1.visit(queryName, out1);
        log.println("Output: " + out1.getOut());
        
        // Test 2: Using the real GraphQlPrinter on just a name
        log.println("\n=== Test 2: Real GraphQlPrinter on Name ===");
        PrintOutputCapture<Void> out2 = new PrintOutputCapture<>(null);
        GraphQlPrinter<Void> printer2 = new GraphQlPrinter<>();
        printer2.visit(queryName, out2);
        log.println("Output: " + out2.getOut());
        
        log.close();
    }
}