package spike;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Claim 1b: every jar on the production (main) runtime classpath is compiled to Java 8 or lower
 * (classfile major version &le; 52), proving the chosen library versions honor the Java 8 floor.
 * Multi-release overlays ({@code META-INF/versions/**}) and {@code module-info.class} are excluded, as they are
 * inert on a Java 8 runtime.
 */
class T4_BytecodeVersionTest {

    private static final int JAVA_8_MAJOR = 52;

    @Test
    void everyRuntimeJarTargetsJava8OrLower() throws Exception {
        String classpath = System.getProperty("spike.runtimeClasspath");
        assertNotNull(classpath, "spike.runtimeClasspath system property must be set by the build");

        List<String> violations = new ArrayList<>();
        int jarsScanned = 0;
        int classesScanned = 0;
        int maxMajorSeen = 0;
        boolean sawResolver = false;

        for (String entry : classpath.split(File.pathSeparator)) {
            if (entry.isEmpty() || !entry.endsWith(".jar")) {
                continue;
            }
            File jar = new File(entry);
            if (!jar.isFile()) {
                continue;
            }
            jarsScanned++;
            if (jar.getName().contains("maven-resolver")) {
                sawResolver = true;
            }
            try (ZipFile zip = new ZipFile(jar)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry ze = entries.nextElement();
                    String name = ze.getName();
                    if (!name.endsWith(".class")
                            || name.equals("module-info.class")
                            || name.endsWith("/module-info.class")
                            || name.startsWith("META-INF/versions/")) {
                        continue;
                    }
                    int major = classfileMajor(zip, ze);
                    classesScanned++;
                    maxMajorSeen = Math.max(maxMajorSeen, major);
                    if (major > JAVA_8_MAJOR) {
                        violations.add(jar.getName() + "!" + name + " -> major " + major);
                    }
                }
            }
        }

        System.out.println("[T4] scanned " + jarsScanned + " jars / " + classesScanned
                + " base classes; max classfile major = " + maxMajorSeen + " (Java 8 == 52)");
        assertTrue(jarsScanned >= 10, "expected the resolver stack on the classpath, only scanned " + jarsScanned + " jars");
        assertTrue(sawResolver, "expected a maven-resolver jar on the runtime classpath");
        assertTrue(classesScanned > 0, "expected to scan some classes");
        assertTrue(violations.isEmpty(),
                "found classfiles newer than Java 8 (major " + JAVA_8_MAJOR + "):\n  "
                        + String.join("\n  ", violations));
    }

    private static int classfileMajor(ZipFile zip, ZipEntry entry) throws Exception {
        try (InputStream is = zip.getInputStream(entry); DataInputStream dis = new DataInputStream(is)) {
            int magic = dis.readInt();
            assertEquals(0xCAFEBABE, magic, "bad magic in " + entry.getName());
            dis.readUnsignedShort(); // minor
            return dis.readUnsignedShort(); // major
        }
    }
}
