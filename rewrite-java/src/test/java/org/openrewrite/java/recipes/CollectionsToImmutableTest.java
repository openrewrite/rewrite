package org.openrewrite.java.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class CollectionsToImmutableTest implements RewriteTest {
	@Test
	void renameFieldRenamesFoundField() {
		rewriteRun(
				recipeSpec -> recipeSpec.recipe(new CollectionsToImmutableRecipe("Arrays.asList(\"A\",\"B\");", "y")),
				java(
						"""
								package my.test;
										
								import java.util.Arrays;
											
								class A
								{
								   public static final List<String> entries = Arrays.asList("A", "B");
								}
								""",
						"""
								package my.test;
											
								import java.util.Arrays;
								import java.util.Collections;
           								   
								class A
								{
								   public static final List<String> entries = Collections.unmodifiableList(Arrays.asList("A", "B"));
								}
								"""
				)
		);
	}
}