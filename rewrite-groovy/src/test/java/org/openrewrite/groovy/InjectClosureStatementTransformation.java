/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.groovy;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.util.List;

/**
 * A stand-in for the kind of <em>global</em> Groovy AST transformation that real
 * libraries register via {@code META-INF/services/org.codehaus.groovy.transform.ASTTransformation}
 * (Spock, various Gradle plugins, instrumentation agents, ...).
 * <p>
 * Groovy discovers it from the compilation classpath and runs it on <em>every</em> compilation,
 * including the synthetic snippets OpenRewrite recipes parse at runtime. This particular one
 * injects a synthetic statement (with no real source coordinates) into the first closure it finds —
 * e.g. the body of {@code plugins { ... }} — which is exactly the situation that desynchronizes
 * the position-coupled {@link GroovyParserVisitor} from the original source text.
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class InjectClosureStatementTransformation implements ASTTransformation {

    /** Set whenever this transformation actually runs, so tests can assert whether it was suppressed. */
    public static volatile boolean executed = false;

    @Override
    public void visit(ASTNode[] nodes, SourceUnit source) {
        executed = true;
        if (source.getAST() == null) {
            return;
        }
        for (ClassNode classNode : source.getAST().getClasses()) {
            for (MethodNode method : classNode.getMethods()) {
                if (method.getCode() instanceof BlockStatement) {
                    injectIntoFirstClosure(((BlockStatement) method.getCode()).getStatements());
                }
            }
            // Script bodies live in the synthetic run() method too, but also scan the module statements
        }
        if (source.getAST().getStatementBlock() != null) {
            injectIntoFirstClosure(source.getAST().getStatementBlock().getStatements());
        }
    }

    private void injectIntoFirstClosure(List<Statement> statements) {
        for (Statement statement : statements) {
            if (statement instanceof ExpressionStatement &&
                ((ExpressionStatement) statement).getExpression() instanceof MethodCallExpression) {
                MethodCallExpression call = (MethodCallExpression) ((ExpressionStatement) statement).getExpression();
                if (call.getArguments() instanceof ArgumentListExpression) {
                    for (Expression arg : ((ArgumentListExpression) call.getArguments()).getExpressions()) {
                        if (arg instanceof ClosureExpression && ((ClosureExpression) arg).getCode() instanceof BlockStatement) {
                            BlockStatement body = (BlockStatement) ((ClosureExpression) arg).getCode();
                            // A synthetic statement with no source position, as transforms routinely emit.
                            ExpressionStatement injected = new ExpressionStatement(
                                    new MethodCallExpression(new VariableExpression("this"), "__injectedBySpockLikeTransform", new ArgumentListExpression()));
                            injected.setLineNumber(-1);
                            injected.setColumnNumber(-1);
                            injected.getExpression().setLineNumber(-1);
                            injected.getExpression().setColumnNumber(-1);
                            body.getStatements().add(0, injected);
                            return;
                        }
                    }
                }
            }
        }
    }
}
