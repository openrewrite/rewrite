package org.openrewrite.gradle.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GradleWrapperScriptLoaderTest {

    @Test
    void nearestVersion() {
        GradleWrapperScriptLoader.Nearest nearest = new GradleWrapperScriptLoader().findNearest("6.9.100");
        String v = nearest.getResolved().getVersion();
        assertThat(v).startsWith("6.9.");

        // this version doesn't exist, so we should get the nearest instead
        assertThat(v).isNotEqualTo("6.9.100");
    }

    @Test
    void lastVersion() {
        GradleWrapperScriptLoader.Nearest nearest = new GradleWrapperScriptLoader().findNearest(null);
        String v = nearest.getResolved().getVersion();
        assertThat(Integer.parseInt(v.split("\\.")[0])).isGreaterThanOrEqualTo(8);
    }
}
