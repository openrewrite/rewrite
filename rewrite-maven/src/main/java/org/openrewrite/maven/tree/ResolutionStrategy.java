package org.openrewrite.maven.tree;

public enum ResolutionStrategy {
    NEAREST_WINS, //Maven style, where the nearest dependency in the tree is used
    NEWEST_WINS   //Gradle style, where the latest version is used
}
