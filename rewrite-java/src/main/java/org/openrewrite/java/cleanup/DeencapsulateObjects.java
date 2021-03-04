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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.JavaType;

@Incubating(since = "7.0.0")
public class DeencapsulateObjects extends Recipe {

  @Override
  public String getDisplayName() {
    return "De-encapsulate DTO parameters";
  }

  @Override
  public String getDescription() {
    return "Find DTO method parameters and replace with their underlying ID equivalent when method only uses ID.";
  }

  @Override protected TreeVisitor<?, ExecutionContext> getVisitor() {
    return new Visitor();
  }

  private static class Visitor extends JavaIsoVisitor<ExecutionContext> {

    static String degetterify(String input) {
      String getterRemoved = input.replaceAll("^get([A-Z])", "$1");
      // Fastest way to lowercase the first character of a string
      // https://stackoverflow.com/a/31735171
      char[] c = getterRemoved.toCharArray();
      c[0] = Character.toLowerCase(c[0]);
      return new String(c);
    }

    @Override public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext context) {
      AtomicReference<J.MethodDeclaration> modifiedMethod =
          new AtomicReference<>(super.visitMethodDeclaration(method, context));
      AtomicBoolean wasModified = new AtomicBoolean(false);

      modifiedMethod.get().getParameters()
          .stream()
          .filter(i -> i instanceof J.VariableDeclarations)
          .map(i -> (J.VariableDeclarations) i)
          .forEach(it -> {

            Identifier parameter = it.getVariables().get(0).getName();
            FindParameterUsages usagesFinder = new FindParameterUsages(parameter);
            usagesFinder.visit(modifiedMethod.get(), context);
            Set<Identifier> uniqueUsages = usagesFinder.uniqueUsages;
            if (uniqueUsages.size() == 1) {
              wasModified.set(true);
              // fix param
              Identifier usage = uniqueUsages.stream().findFirst().get();

              String replacedParameterName = degetterify(usage.getSimpleName());
              modifiedMethod.set(new ReplaceParameterWithCaller(
                  parameter,
                  replacedParameterName,
                  usagesFinder.returnType
              ).visitMethodDeclaration(modifiedMethod.get(), context));

              // find and replace usages
              modifiedMethod.set((J.MethodDeclaration) new ReplaceInvocationsWithParameter(
                  replacedParameterName,
                  usage
              ).visitMethodDeclaration(modifiedMethod.get(), context));
            }
          });
      if (wasModified.get()) {
        // find invocations of this method and change to the new signature
        //      val classname = cursor.firstEnclosingRequired()
        //      val params = method.params.params
        //      val signature = classname + "." + method.name.printTrimmed() + "(" + params + ")"
        //      println(signature)
        // andThen()
      }
      return modifiedMethod.get();
    }
  }

  private static class ReplaceInvocationsWithParameter extends JavaVisitor<ExecutionContext> {
    private final String newParameterName;
    private final Identifier methodIdentifier;

    ReplaceInvocationsWithParameter(String newParameterName, Identifier methodIdentifier) {
      this.newParameterName = newParameterName;
      this.methodIdentifier = methodIdentifier;
    }

    @Override public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext context) {
      if (method.getName() == methodIdentifier) {
        return method.withTemplate(template("${}").build(), method.getCoordinates().replace(),
            newParameterName);
      }
      return super.visitMethodInvocation(method, context);
    }
  }

  private static class ReplaceParameterWithCaller extends JavaIsoVisitor<ExecutionContext> {
    private final Identifier parameter;
    private final String newParameterName;
    private final JavaType newType;

    ReplaceParameterWithCaller(Identifier parameter, String newParameterName, JavaType newtype) {
      this.parameter = parameter;
      this.newParameterName = newParameterName;
      this.newType = newtype;
    }

    @Override public J.VariableDeclarations visitVariableDeclarations(
        J.VariableDeclarations multiVariable, ExecutionContext context) {
      if (multiVariable.getVariables().get(0).getName() == parameter) {

        return super.visitVariableDeclarations(multiVariable, context)
            .withTemplate(template("${} ${}").build(), multiVariable.getCoordinates().replace(),
                newType, newParameterName);
      }
      return super.visitVariableDeclarations(multiVariable, context);
    }
  }

  private static class FindParameterUsages extends JavaVisitor<ExecutionContext> {
    private final Identifier parameter;

    FindParameterUsages(Identifier parameter) {

      this.parameter = parameter;
    }

    HashSet<Identifier> uniqueUsages = new HashSet<>();
    JavaType returnType = null;

    @Override public J visitIdentifier(Identifier identifer, ExecutionContext context) {
      if (identifer == parameter) {
        J enclosingStatement = getCursor().getParentOrThrow().getValue();
        if (enclosingStatement instanceof J.MethodInvocation) {
          J.MethodInvocation methodInvocation = (J.MethodInvocation) enclosingStatement;
          uniqueUsages.add(methodInvocation.getName());
          returnType = methodInvocation.getReturnType();
        } else if (enclosingStatement instanceof J.VariableDeclarations.NamedVariable) {
          // Skipping, this is the parameter declaration
        } else {
          throw new RuntimeException(
              "enclosingStatement was ${enclosingStatement.javaClass.kotlin.qualifiedName}");
        }
      }
      return super.visitIdentifier(identifer, context);
    }
  }
}
