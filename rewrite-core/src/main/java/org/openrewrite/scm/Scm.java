package org.openrewrite.scm;

import org.openrewrite.internal.lang.Nullable;

public interface Scm extends Comparable<Scm> {
    String getOrigin();

    String cleanHostAndPath(String cloneUrl);

    default boolean belongsToScm(String cloneUrl) {
        return cleanHostAndPath(cloneUrl).startsWith(getOrigin());
    }

    @Nullable
    default String determineOrganization(String path) {
        return path.substring(0, path.indexOf("/"));
    }

    @Nullable
    default String determineGroupPath(String path) {
        return null;
    }

    @Nullable
    default String determineProject(String path) {
        return null;
    }

    default String determineRepositoryName(String path) {
        return path.substring(path.indexOf("/") + 1);
    }

    default ScmUrlComponents determineScmUrlComponents(String cloneUrl) {
        String path = cleanHostAndPath(cloneUrl).substring(getOrigin().length() + 1);
        return new ScmUrlComponents(getOrigin(), determineOrganization(path), determineGroupPath(path), determineProject(path), determineRepositoryName(path));
    }

    @Override
    default int compareTo(Scm o) {
        return getOrigin().compareTo(o.getOrigin());
    }

}
