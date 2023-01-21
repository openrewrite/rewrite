package org.openrewrite.maven.tree;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Every model object that is transferred via the tooling API must implement {@link Serializable}, including
 * certain maven model objects that are shared by rewrite-gradle.
 */
public class OpenRewriteModelSerializableTest {

    @Test
    void mavenRepository() throws IOException {
        MavenRepository mavenRepository = new MavenRepository("id", "url", null, null, null, null);
        ObjectOutputStream os = new ObjectOutputStream(new ByteArrayOutputStream());
        os.writeObject(mavenRepository);
    }
}
