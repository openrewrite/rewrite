package org.openrewrite.java;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

/**
 * Extract recipe examples from a test file which are annotated with @DocumentExample
 * Output is the content of the yaml file to present examples
 * Format is like:
 * <pre>
 *               type: specs.openrewrite.org/v1beta/example
 *               recipeName: test.ChangeTextToHello
 *               examples:
 *                 - description: "Change World to Hello in a text file"
 *                   before: "World"
 *                   after: "Hello!"
 *                   language: "text"
 *                 - description: "Change World to Hello in a java file"
 *                   before: |
 *                     public class A {
 *                         void method() {
 *                             System.out.println("World");
 *                         }
 *                     }
 *                   after: |
 *                     public class A {
 *                         void method() {
 *                             System.out.println("Hello!");
 *                         }
 *                     }
 *                   language: "java"
 * </pre>
 */
public class ExamplesExtractor extends JavaIsoVisitor<ExecutionContext> {

    private final StringBuilder outputSb;


    protected ExamplesExtractor() {
        outputSb = new StringBuilder();
    }

    /**
     * print the recipe example yaml.
     */
    public String printRecipeExampleYaml() {
        return outputSb.toString();
    }

    /*
    Yaml format:
              type: specs.openrewrite.org/v1beta/example
              recipeName: test.ChangeTextToHello
              examples:
                - description: "Change World to Hello in a text file"
                  before: "World"
                  after: "Hello!"
                  language: "text"
                - description: "Change World to Hello in a java file"
                  before: |
                    public class A {
                        void method() {
                            System.out.println("World");
                        }
                    }
                  after: |
                    public class A {
                        void method() {
                            System.out.println("Hello!");
                        }
                    }
                  language: "java"
     */
    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
        return super.visitCompilationUnit(cu, executionContext);
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {



        return super.visitMethodInvocation(method, executionContext);
    }

    @Override
    public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
        return super.visitNewClass(newClass, executionContext);
    }
}
