package org.openrewrite.java.recipes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.TreeVisitingPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Objects;

public class CollectionsToImmutableRecipe extends Recipe {
	private String before;
	private String after;

	// All recipes must be serializable. This is verified by RewriteTest.rewriteRun() in your tests.
	@JsonCreator

	public CollectionsToImmutableRecipe(
			@JsonProperty("before") String before,
			@JsonProperty("after") String after) {
		this.before = before;
		this.after = after;
	}

	@Override
	public String getDisplayName() {
		return "RenameField";
	}

	@Override
	public String getDescription() {
		return "Rename class field.";
	}

	@Override
	public TreeVisitor<?, ExecutionContext> getVisitor() {
		return new RenameFieldVisitor();
	}

	private String getBefore() {
		return before;
	}

	private void setBefore(final String before) {
		this.before = before;
	}

	private String getAfter() {
		return after;
	}

	private void setAfter(final String after) {
		this.after = after;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		final CollectionsToImmutableRecipe that = (CollectionsToImmutableRecipe) o;
		return Objects.equals(before, that.before) && Objects.equals(after, that.after);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), before, after);
	}

	private class RenameFieldVisitor
			extends JavaIsoVisitor<ExecutionContext> {

//		@Override
//		public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
//			System.out.println(TreeVisitingPrinter.printTree(getCursor()));
//			return classDecl;
//		}

		final MethodMatcher rmaMatcher = new MethodMatcher("java.util.Arrays asList(..)");

		final MethodMatcher unmodifiableListMatcher = new MethodMatcher("java.util.Collections unmodifiableList(..)");

//		@Override
//		public J.VariableDeclarations visitVariableDeclarations(final J.VariableDeclarations multiVariable, final ExecutionContext executionContext) {
//
//			System.out.println(multiVariable);
//
//
//
//			return super.visitVariableDeclarations(multiVariable, executionContext);
//		}

		@Override
		public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method, final ExecutionContext executionContext) {
			if (rmaMatcher.matches(method, true)) {

				final J.MethodInvocation parentInvocation = getCursor().getParentOrThrow().firstEnclosing(J.MethodInvocation.class);
				if (unmodifiableListMatcher.matches(parentInvocation, true)) {
					return super.visitMethodInvocation(method, executionContext);
				}

				J.MethodInvocation result = JavaTemplate.builder("Collections.unmodifiableList(#{any()})")
						.imports("java.util.Collections", "java.util.Arrays")
						.contextSensitive().build().apply(getCursor(), method.getCoordinates().replace(), method);
				maybeAddImport("java.util.Arrays");
				maybeAddImport("java.util.Collections");

				return result;
			}
			return super.visitMethodInvocation(method, executionContext);
		}
	}
}
