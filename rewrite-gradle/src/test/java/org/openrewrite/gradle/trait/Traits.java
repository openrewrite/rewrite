package org.openrewrite.gradle.trait;


public class Traits {
    private Traits() {
    }

    public static GradleDependency.Matcher gradleDependency() {
        return new GradleDependency.Matcher();
    }

}