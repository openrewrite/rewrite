package spike.support;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Writes POMs into a Maven2 on-disk repository layout, served to the resolver as a {@code file://} RemoteRepository. */
public class Repo {
    public final File root;

    public Repo(File root) {
        this.root = root;
    }

    public void pom(String groupId, String artifactId, String version, String xml) {
        try {
            File dir = new File(root, groupId.replace('.', '/') + "/" + artifactId + "/" + version);
            if (!dir.mkdirs() && !dir.isDirectory()) {
                throw new IOException("could not create " + dir);
            }
            Files.write(new File(dir, artifactId + "-" + version + ".pom").toPath(), xml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String uri() {
        return root.toURI().toString();
    }
}
