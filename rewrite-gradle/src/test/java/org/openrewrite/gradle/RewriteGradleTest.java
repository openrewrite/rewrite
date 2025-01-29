package org.openrewrite.gradle;

import org.junit.jupiter.api.BeforeAll;
import org.openrewrite.test.RewriteTest;

public class RewriteGradleTest implements RewriteTest {

    @BeforeAll
    public static void makeSureArtifactsAreDownloaded() {
        GradleSetup.makeSureArtifactsAreDownloaded();
    }
}
