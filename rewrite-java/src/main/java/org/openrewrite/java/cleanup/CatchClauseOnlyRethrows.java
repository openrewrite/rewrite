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
package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Try.Catch;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singleton;

public class CatchClauseOnlyRethrows extends Recipe {
    @Override
    public String getDisplayName() {
        return "Catch clause shouldn't only rethrow";
    }

    @Override
    public String getDescription() {
        return "A `catch` clause that only rethrows the caught exception is unnecessary. " +
                "Letting the exception bubble up as normal achieves the same result with less code.";
    }

    @Override
    public Set<String> getTags() {
        return singleton("RSPEC-2737");
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext executionContext) {
                J.Block b = super.visitBlock(block, executionContext);
                List<Statement> statements = new ArrayList<>(b.getStatements().size());
                boolean statementsModified = false;
                for(Statement statement : b.getStatements()) {
                    if(!(statement instanceof J.Try)) {
                        statements.add(statement);
                        continue;
                    }
                    J.Try aTry = (J.Try) statement;
                    // If a try has no catches, no finally, and no resources get rid of it and merge its statements into the current block
                    if(aTry.getCatches().size() != 0 || aTry.getResources() != null || aTry.getFinally() != null) {
                        statements.add(statement);
                        continue;
                    }

                    // Adjust indentation to match the block the statements will be inserted into
                    for(Statement tryStatement : aTry.getBody().getStatements()) {
                        statements.add(autoFormat(tryStatement, executionContext, getCursor()));
                    }

                    statementsModified = true;
                }
                if(statementsModified) {
                    return b.withStatements(statements);
                }
                return b;
            }

            @Override
            public J.Try visitTry(J.Try tryable, ExecutionContext executionContext) {
                J.Try t = super.visitTry(tryable, executionContext);

                List<Catch> catches = new ArrayList<>(t.getCatches().size());
                for(Catch aCatch : t.getCatches()) {
                    if(aCatch.getBody().getStatements().size() != 1) {
                        catches.add(aCatch);
                        continue;
                    }
                    Statement statement = aCatch.getBody().getStatements().get(0);
                    if(!(statement instanceof J.Throw)) {
                        catches.add(aCatch);
                        continue;
                    }
                    J.Throw aThrow = (J.Throw) statement;
                    if(!(aThrow.getException() instanceof J.Identifier)) {
                        catches.add(aCatch);
                        continue;
                    }
                    J.Identifier caughtIdent = aCatch.getParameter().getTree().getVariables().get(0).getName();
                    J.Identifier identToThrow = (J.Identifier) aThrow.getException();
                    if(!caughtIdent.getSimpleName().equals(identToThrow.getSimpleName())) {
                        catches.add(aCatch);
                        continue;
                    }
                }
                if(catches.size() != t.getCatches().size()) {
                    t = t.withCatches(catches);
                }

                return t;
            }
        };
    }
}
