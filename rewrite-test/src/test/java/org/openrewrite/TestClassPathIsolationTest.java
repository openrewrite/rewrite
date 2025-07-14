package org.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.tree.ParseError;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for JavaParser classpath isolation functionality.
 *
 * <p>This test suite demonstrates a bug in OpenRewrite's classpath isolation mechanism.
 * The {@link JavaParser#classpathFromResources} method is intended to create isolated parsing
 * environments where only explicitly specified dependencies are available on the classpath.
 * However, the current implementation fails to provide true isolation because it still includes
 * runtime classpath dependencies alongside the explicitly specified ones.</p>
 *
 * <p><strong>The Problem:</strong> In {@code JavaParser.Builder.resolvedClasspath()}, when
 * {@code artifactNames} is not empty, the method adds runtime classpath dependencies via
 * {@code JavaParser.dependenciesFromClasspath()}. This contamination means that both
 * {@code javax.servlet} and {@code jakarta.servlet} APIs are available regardless of the
 * isolated classpath configuration.</p>
 *
 * <p><strong>Impact:</strong> This broken isolation affects recipe detection logic for
 * migration scenarios, particularly the javax→jakarta servlet migration. Recipes cannot
 * reliably determine which API version is available in the target environment, leading
 * to incorrect transformation decisions.</p>
 *
 * <p><strong>Expected Behavior:</strong> When using
 * {@code classpathFromResources(ctx, "jakarta.servlet-api")}, the parser
 * should only be able to resolve jakarta.servlet imports and should fail to resolve
 * javax.servlet imports.</p>
 *
 * <p><strong>Current Behavior:</strong> Both javax.servlet and jakarta.servlet imports
 * resolve successfully due to runtime classpath contamination, making isolation ineffective.</p>
 *
 * <p><strong>NOTE:</strong> the type table containts both dependencies
 * javax.servlet:javax.servlet-api:4.+ and jakarta.servlet:jakarta.servlet-api:6.+</p>
 */
class TestClassPathIsolationTest {

    private final JavaParser JAKARTA_CP_PARSER = JavaParser.fromJavaVersion()
      .classpathFromResources(new InMemoryExecutionContext(), "jakarta.servlet-api")
      .build();
    private final JavaParser JAVAX_CP_PARSER = JavaParser.fromJavaVersion()
      .classpathFromResources(new InMemoryExecutionContext(), "javax.servlet-api")
      .build();

    /**
     * Tests that jakarta-only classpath rejects javax imports (isolation failure).
     *
     * <p><strong>Current Failure:</strong> The test currently passes (javax imports
     * work) due to runtime classpath contamination, documenting the broken behavior.
     * When classpath isolation is fixed, this assertion should be changed to
     * {@code isFalse()}.
     */
    @Test
    void rejectUnexpectedType() {
        assertThat(JAKARTA_CP_PARSER.parse("import javax.servlet.ServletContext; class Test {}"))
          .as("javax.servlet should NOT be available in Jakarta-only classpath")
          .hasOnlyElementsOfType(ParseError.class);
    }


    /**
     * Tests that jakarta-only classpath accepts jakarta imports (positive case).
     *
     * <p><strong>Expected Behavior:</strong> This test should pass both before and
     * after fixing classpath isolation, as it tests the intended functionality.
     */
    @Test
    void parseExpectedType() {
         assertThat(JAKARTA_CP_PARSER.parse("import jakarta.servlet.ServletContext; class Test {}"))
          .as("jakarta.servlet should be available in Jakarta classpath")
          .doesNotHaveAnyElementsOfTypes(ParseError.class);
    }

    /**
     * Tests that recipe detection differs based on classpath configuration (isolation failure).
     *
     * <p>This test simulates how migration recipes could detect which servlet API version is
     * available in the target environment. It creates two parsers with different classpath
     * configurations and checks whether they produce different detection results.
     *
     * <p><strong>Current Failure:</strong> Because both parsers can resolve both API
     * versions due to runtime classpath contamination, they produce identical detection
     * results. This breaks recipe logic that depends on classpath-based API detection.
     */
    @Test
    void resultShouldDifferByClasspath() {
        assertThat(JAKARTA_CP_PARSER.parse("import javax.servlet.ServletContext; class Test {}"))
          .as("javax.servlet should be available in javax classpath")
          .hasOnlyElementsOfType(ParseError.class);

        assertThat(JAVAX_CP_PARSER.parse("import javax.servlet.ServletContext; class Test {}"))
          .as("javax.servlet should be available in javax classpath")
          .doesNotHaveAnyElementsOfTypes(ParseError.class);
    }
}
