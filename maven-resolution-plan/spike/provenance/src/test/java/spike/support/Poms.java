package spike.support;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/** Writes POM files into a flat directory and tracks a GAV -> File map for {@link FilesystemModelResolver}. */
public class Poms {
    public final File dir;
    public final Map<String, File> byGav = new LinkedHashMap<>();

    public Poms(File dir) {
        this.dir = dir;
    }

    public File write(String groupId, String artifactId, String version, String pomXml) {
        try {
            File f = new File(dir, artifactId + "-" + version + ".pom");
            Files.write(f.toPath(), pomXml.getBytes(StandardCharsets.UTF_8));
            byGav.put(groupId + ":" + artifactId + ":" + version, f);
            return f;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
