package org.openrewrite.java.cleanup;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.val;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.JavaType;

public class DeencapsulateObjects extends Recipe {
  @Override protected TreeVisitor<?, ExecutionContext> getVisitor() {
    return new Visitor();
  }

  public static class Visitor extends JavaIsoVisitor<ExecutionContext> {

    static String degetterify(String input) {
      String getterRemoved = input.replaceAll("^get([A-Z])", "$1");
      // Fastest way to lowercase the first character of a string
      // https://stackoverflow.com/a/31735171
      char[] c = getterRemoved.toCharArray();
      c[0] = Character.toLowerCase(c[0]);
      return new String(c);
    }

    @Override public J.MethodDeclaration visitMethodDeclaration(        J.MethodDeclaration method,        ExecutionContext notused    ) {
      AtomicReference<J.MethodDeclaration> modifiedMethod =
          new AtomicReference<>(super.visitMethodDeclaration(method, notused));
      AtomicBoolean wasModified = new AtomicBoolean(false);

      modifiedMethod.get().getParameters()
          .stream()
          .filter(i -> i instanceof J.VariableDeclarations)
          .map(i -> (J.VariableDeclarations) i)
          .forEach(it -> {

            Identifier parameter = it.getVariables().get(0).getName();
            FindParameterUsages usagesFinder = new FindParameterUsages(parameter);
            usagesFinder.visit(modifiedMethod.get(), modifiedMethod.get());
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
              ).visitMethodDeclaration(modifiedMethod.get(), null));

              // find and replace usages
              modifiedMethod.set((J.MethodDeclaration) new ReplaceInvocationsWithParameter(
                  replacedParameterName,
                  usage
              ).visitMethodDeclaration(modifiedMethod.get(), null));
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

  public static class ReplaceInvocationsWithParameter extends JavaVisitor<Void> {
    private final String newParameterName;
    private final Identifier methodIdentifier;

    ReplaceInvocationsWithParameter(String newParameterName, Identifier methodIdentifier) {
      this.newParameterName = newParameterName;
      this.methodIdentifier = methodIdentifier;
    }

    @Override public J visitMethodInvocation(J.MethodInvocation method, Void notused) {
      if (method.getName() == methodIdentifier) {
        return method.withTemplate(template("${}").build(), method.getCoordinates().replace(),
            newParameterName);
      }
      return super.visitMethodInvocation(method, notused);
    }
  }

  public static class ReplaceParameterWithCaller extends JavaIsoVisitor<Void> {
    private final Identifier parameter;
    private final String newParameterName;
    private final JavaType newType;

    ReplaceParameterWithCaller(Identifier parameter, String newParameterName, JavaType newtyppe) {
      this.parameter = parameter;
      this.newParameterName = newParameterName;
      this.newType = newtyppe;
    }

    @Override public J.VariableDeclarations visitVariableDeclarations(
        J.VariableDeclarations multiVariable, Void notused) {
      if (multiVariable.getVariables().get(0).getName() == parameter) {

        return super.visitVariableDeclarations(multiVariable, notused)
            .withTemplate(template("${} ${}").build(), multiVariable.getCoordinates().replace(),
                newType, newParameterName);
      }
      return super.visitVariableDeclarations(multiVariable, notused);
    }
  }

  public static class FindParameterUsages extends JavaVisitor<J> {
    private final Identifier parameter;

    FindParameterUsages(Identifier parameter) {

      this.parameter = parameter;
    }

    HashSet<Identifier> uniqueUsages = new HashSet<>();
    JavaType returnType = null;

    @Override public J visitIdentifier(Identifier ident, J notUsed) {
      if (ident == parameter) {
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
      return super.visitIdentifier(ident, notUsed);
    }
  }
}
