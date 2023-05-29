package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.properties.Assertions.properties;

public class AddMavenWrapperChecksumValidationTest implements RewriteTest {
  @Test
  void changeOnlyMatchingFile() {
    rewriteRun(
        spec -> spec.recipe(new AddMavenWrapperChecksumValidation(
        )),
        properties(
            "distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.1/apache-maven-3.9.1-bin.zip",
            """
              distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.1/apache-maven-3.9.1-bin.zip
              distributionSha256Sum=10b13517951362d435a3256222efd8b71524d9335d6dca4e78648a67ef71da41
              """,
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
        )
    );
  }
}
