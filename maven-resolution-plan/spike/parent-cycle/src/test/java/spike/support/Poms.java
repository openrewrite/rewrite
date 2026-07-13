package spike.support;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/** Writes POM files and tracks a GAV -> File map for {@link FilesystemModelResolver}. */
public class Poms {
    public final File dir;
    public final Map<String, File> byGav = new LinkedHashMap<>();

    public Poms(File dir) {
        this.dir = dir;
    }

    /** Write into the flat root dir (used for repo-lookup cycles). */
    public File write(String groupId, String artifactId, String version, String pomXml) {
        return writeAt(new File(dir, artifactId + "-" + version + ".pom"), groupId, artifactId, version, pomXml);
    }

    /** Write into an explicit file (used for relativePath cycles where directory layout matters). */
    public File writeAt(File f, String groupId, String artifactId, String version, String pomXml) {
        try {
            File parent = f.getParentFile();
            if (parent != null) {
                Files.createDirectories(parent.toPath());
            }
            Files.write(f.toPath(), pomXml.getBytes(StandardCharsets.UTF_8));
            byGav.put(groupId + ":" + artifactId + ":" + version, f);
            return f;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
