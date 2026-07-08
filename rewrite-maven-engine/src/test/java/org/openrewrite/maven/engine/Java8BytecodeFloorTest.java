/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven.engine;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every base class in the published (shaded) jar — the engine's own classes plus the whole bundled resolver/Maven
 * stack — is compiled to Java 8 or lower (classfile major &le; 52). Multi-release overlays and {@code module-info}
 * are inert on a Java 8 runtime and excluded. Ported from the transport spike (claim 1b).
 */
class Java8BytecodeFloorTest {

    private static final int JAVA_8_MAJOR = 52;

    @Test
    void everyClassInShadedJarTargetsJava8OrLower() throws Exception {
        List<String> violations = new ArrayList<>();
        int classesScanned = 0;
        int maxMajorSeen = 0;
        boolean sawRelocatedResolver = false;

        try (ZipFile zip = new ZipFile(RelocationJarScanTest.shadowJar())) {
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
                if (name.contains("shaded/org/eclipse/aether/")) {
                    sawRelocatedResolver = true;
                }
                int major = classfileMajor(zip, ze);
                classesScanned++;
                maxMajorSeen = Math.max(maxMajorSeen, major);
                if (major > JAVA_8_MAJOR) {
                    violations.add(name + " -> major " + major);
                }
            }
        }

        System.out.println("[bytecode] scanned " + classesScanned + " base classes; max classfile major = "
                + maxMajorSeen + " (Java 8 == 52)");
        assertTrue(classesScanned > 100, "expected the resolver stack bundled, only scanned " + classesScanned);
        assertTrue(sawRelocatedResolver, "expected relocated resolver classes in the shaded jar");
        assertTrue(violations.isEmpty(), "classfiles newer than Java 8 (major " + JAVA_8_MAJOR + "):\n  "
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
