package org.openrewrite.javascript;

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class InstallRecipeFromFileIntegTest {

    @Test
    void installRecipeFromFile() {
        JavaScriptRewriteRpc.setFactory(JavaScriptRewriteRpc.builder());

        File nodeUtilUpgrade = new File("recipes-nodejs/dist/index.js");
        assertThat(nodeUtilUpgrade).exists();
        assertThat(JavaScriptRewriteRpc.getOrStart().installRecipes(nodeUtilUpgrade)).isGreaterThan(0);
    }
}
