package org.openrewrite.maven;

import java.nio.file.Path;
import java.nio.file.Paths;

public class MavenHomeDirectory {
    public static Path get() {
        String m2Home = System.getenv("M2_HOME");
        if (m2Home != null) {
            return Paths.get(m2Home);
        }
        return Paths.get(System.getProperty("user.home"), ".m2");
    }

    public static Path getRepository() {
        return get().resolve("repository");
    }
}
