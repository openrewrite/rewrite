/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.marker;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32Util;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.With;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

/**
 * Detection logic from <a href="Gradle">https://github.com/gradle/gradle/blob/master/subprojects/base-services/src/main/java/org/gradle/internal/os/c.java</a>
 */
@SuppressWarnings("StaticInitializerReferencesSubClass")
public abstract class OperatingSystemProvenance implements Marker {
    public static final Windows WINDOWS = new Windows();
    public static final MacOs MAC_OS = new MacOs();
    public static final Solaris SOLARIS = new Solaris();
    public static final Linux LINUX = new Linux();
    public static final FreeBSD FREE_BSD = new FreeBSD();
    public static final Unix UNIX = new Unix();

    private static OperatingSystemProvenance currentOs;
    private final String toStringValue;
    private final String osName;
    private final String osVersion;

    OperatingSystemProvenance() {
        osName = System.getProperty("os.name");
        osVersion = System.getProperty("os.version");
        toStringValue = getName() + " " + getVersion() + " " + System.getProperty("os.arch");
    }

    private interface C extends Library {
        @SuppressWarnings("UnusedReturnValue")
        int gethostname(byte[] name, int size_t) throws LastErrorException;
    }

    public static String hostname() {
        OperatingSystemProvenance currentOs = current();
        if (currentOs.isWindows()) {
            try {
                if (System.getenv("COMPUTERNAME") != null) {
                    return System.getenv("COMPUTERNAME");
                }
                return Kernel32Util.getComputerName();
            } catch (Throwable t) {
                // unable to determine the host name on this Windows instance.
                // with variations in build tool versions and JVMs, this can sometimes break
            }
        } else if (currentOs.isMacOsX()) {
            try {
                if (System.getenv("HOSTNAME") != null) {
                    return System.getenv("HOSTNAME");
                }

                Path plist = Paths.get("/Library/Preferences/SystemConfiguration/preferences.plist");
                if (Files.exists(plist)) {
                    try {
                        /*
                         <dict>
                             <key>LocalHostName</key>
                             <string>MacBook-Pro-4</string>
                         </dict>
                         */
                        String hostname = new String(Files.readAllBytes(plist));
                        int localHostName = hostname.indexOf("<key>LocalHostName</key>");
                        if (localHostName > 0) {
                            int valueTag = hostname.indexOf("<string>", localHostName);
                            if (valueTag > 0) {
                                int valueStart = valueTag + "<string>".length();
                                return hostname.substring(valueStart, hostname.indexOf("</string>", valueStart)).trim() + ".local";
                            }
                        }
                    } catch (Throwable t) {
                        // fall through to the next option
                    }
                }

                C c = Native.load("c", C.class);
                byte[] hostname = new byte[256];
                c.gethostname(hostname, hostname.length);
                return Native.toString(hostname);
            } catch (Throwable t) {
                // unable to determine the host name on this instance.
            }
        } else if (currentOs.isUnix()) {
            try {
                if (System.getenv("HOSTNAME") != null) {
                    return System.getenv("HOSTNAME");
                }

                Path etcHostname = Paths.get("/etc/hostname");
                if (Files.exists(etcHostname)) {
                    try {
                        String hostname = new String(Files.readAllBytes(etcHostname));
                        return hostname.trim();
                    } catch (Throwable t) {
                        // fall through to the next option
                    }
                }

                C c = Native.load("c", C.class);
                byte[] hostname = new byte[256];
                c.gethostname(hostname, hostname.length);
                return Native.toString(hostname);
            } catch (Throwable t) {
                // unable to determine the host name on this instance.
            }
        }
        return "localhost";
    }

    public static OperatingSystemProvenance current() {
        if (currentOs == null) {
            currentOs = forName(System.getProperty("os.name"));
        }
        return currentOs;
    }

    // for testing current()
    static void resetCurrent() {
        currentOs = null;
    }

    public static OperatingSystemProvenance forName(String os) {
        String osName = os.toLowerCase();
        if (osName.contains("windows")) {
            return WINDOWS;
        } else if (osName.contains("mac os x") || osName.contains("darwin") || osName.contains("osx")) {
            return MAC_OS;
        } else if (osName.contains("sunos") || osName.contains("solaris")) {
            return SOLARIS;
        } else if (osName.contains("linux")) {
            return LINUX;
        } else if (osName.contains("freebsd")) {
            return FREE_BSD;
        } else {
            // Not strictly true
            return UNIX;
        }
    }

    @Override
    public String toString() {
        return toStringValue;
    }

    public String getName() {
        return osName;
    }

    public String getVersion() {
        return osVersion;
    }

    public boolean isWindows() {
        return false;
    }

    public boolean isUnix() {
        return false;
    }

    public boolean isMacOsX() {
        return false;
    }

    public boolean isLinux() {
        return false;
    }

    public abstract String getNativePrefix();

    public abstract String getScriptName(String scriptPath);

    public abstract String getExecutableName(String executablePath);

    public abstract String getExecutableSuffix();

    public abstract String getSharedLibraryName(String libraryName);

    public abstract String getSharedLibrarySuffix();

    public abstract String getStaticLibraryName(String libraryName);

    public abstract String getStaticLibrarySuffix();

    public abstract String getLinkLibrarySuffix();

    public abstract String getLinkLibraryName(String libraryPath);

    public abstract String getFamilyName();

    public abstract EOL getEOL();

    public enum EOL {
        CRLF,
        LF
    }

    /**
     * Locates the given executable in the system path. Returns null if not found.
     */
    public @Nullable File findInPath(String name) {
        String exeName = getExecutableName(name);
        if (exeName.contains(File.separator)) {
            File candidate = new File(exeName);
            if (candidate.isFile()) {
                return candidate;
            }
            return null;
        }
        for (File dir : getPath()) {
            File candidate = new File(dir, exeName);
            if (candidate.isFile()) {
                return candidate;
            }
        }

        return null;
    }

    public List<File> findAllInPath(String name) {
        List<File> all = new ArrayList<>();
        for (File dir : getPath()) {
            File candidate = new File(dir, name);
            if (candidate.isFile()) {
                all.add(candidate);
            }
        }
        return all;
    }

    public List<File> getPath() {
        String path = System.getenv(getPathVar());
        if (path == null) {
            return emptyList();
        }
        List<File> entries = new ArrayList<>();
        for (String entry : path.split(Pattern.quote(File.pathSeparator))) {
            entries.add(new File(entry));
        }
        return entries;
    }

    public String getPathVar() {
        return "PATH";
    }

    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
    static class Windows extends OperatingSystemProvenance {
        String nativePrefix;

        @With
        @EqualsAndHashCode.Include
        UUID id;

        Windows() {
            nativePrefix = resolveNativePrefix();
            id = Tree.randomId();
        }

        @Override
        public EOL getEOL() {
            return EOL.CRLF;
        }

        @Override
        public boolean isWindows() {
            return true;
        }

        @Override
        public String getFamilyName() {
            return "windows";
        }

        @Override
        public String getScriptName(String scriptPath) {
            return withExtension(scriptPath, ".bat");
        }

        @Override
        public String getExecutableSuffix() {
            return ".exe";
        }

        @Override
        public String getExecutableName(String executablePath) {
            return withExtension(executablePath, ".exe");
        }

        @Override
        public String getSharedLibrarySuffix() {
            return ".dll";
        }

        @Override
        public String getSharedLibraryName(String libraryPath) {
            return withExtension(libraryPath, ".dll");
        }

        @Override
        public String getLinkLibrarySuffix() {
            return ".lib";
        }

        @Override
        public String getLinkLibraryName(String libraryPath) {
            return withExtension(libraryPath, ".lib");
        }

        @Override
        public String getStaticLibrarySuffix() {
            return ".lib";
        }

        @Override
        public String getStaticLibraryName(String libraryName) {
            return withExtension(libraryName, ".lib");
        }

        @Override
        public String getNativePrefix() {
            return nativePrefix;
        }

        private String resolveNativePrefix() {
            String arch = System.getProperty("os.arch");
            if ("i386".equals(arch)) {
                arch = "x86";
            }
            return "win32-" + arch;
        }

        @Override
        public String getPathVar() {
            return "Path";
        }

        @Override
        @NonNull
        public UUID getId() {
            return id;
        }
    }

    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
    static class Unix extends OperatingSystemProvenance {
        String nativePrefix;

        @With
        @EqualsAndHashCode.Include
        UUID id;

        Unix() {
            nativePrefix = resolveNativePrefix();
            id = Tree.randomId();
        }

        @Override
        public EOL getEOL() {
            return EOL.LF;
        }

        @Override
        public String getScriptName(String scriptPath) {
            return scriptPath;
        }

        @Override
        public String getFamilyName() {
            return "unknown";
        }

        @Override
        public String getExecutableSuffix() {
            return "";
        }

        @Override
        public String getExecutableName(String executablePath) {
            return executablePath;
        }

        @Override
        public String getSharedLibraryName(String libraryName) {
            return getLibraryName(libraryName, getSharedLibrarySuffix());
        }

        private String getLibraryName(String libraryName, String suffix) {
            if (libraryName.endsWith(suffix)) {
                return libraryName;
            }
            int pos = libraryName.lastIndexOf('/');
            if (pos >= 0) {
                return libraryName.substring(0, pos + 1) + "lib" + libraryName.substring(pos + 1) + suffix;
            } else {
                return "lib" + libraryName + suffix;
            }
        }

        @Override
        public String getSharedLibrarySuffix() {
            return ".so";
        }

        @Override
        public String getLinkLibrarySuffix() {
            return getSharedLibrarySuffix();
        }

        @Override
        public String getLinkLibraryName(String libraryPath) {
            return getSharedLibraryName(libraryPath);
        }

        @Override
        public String getStaticLibrarySuffix() {
            return ".a";
        }

        @Override
        public String getStaticLibraryName(String libraryName) {
            return getLibraryName(libraryName, ".a");
        }

        @Override
        public boolean isUnix() {
            return true;
        }

        @Override
        public String getNativePrefix() {
            return nativePrefix;
        }

        private String resolveNativePrefix() {
            String arch = getArch();
            String osPrefix = getOsPrefix();
            osPrefix += "-" + arch;
            return osPrefix;
        }

        protected String getArch() {
            String arch = System.getProperty("os.arch");
            if ("x86".equals(arch)) {
                arch = "i386";
            }
            if ("x86_64".equals(arch)) {
                arch = "amd64";
            }
            if ("powerpc".equals(arch)) {
                arch = "ppc";
            }
            return arch;
        }

        protected String getOsPrefix() {
            String osPrefix = getName().toLowerCase();
            int space = osPrefix.indexOf(" ");
            if (space != -1) {
                osPrefix = osPrefix.substring(0, space);
            }
            return osPrefix;
        }

        @Override
        @NonNull
        public UUID getId() {
            return id;
        }
    }

    static class MacOs extends Unix {
        @Override
        public boolean isMacOsX() {
            return true;
        }

        @Override
        public String getFamilyName() {
            return "os x";
        }

        @Override
        public String getSharedLibrarySuffix() {
            return ".dylib";
        }

        @Override
        public String getNativePrefix() {
            return "darwin";
        }
    }

    static class Linux extends Unix {
        @Override
        public boolean isLinux() {
            return true;
        }

        @Override
        public String getFamilyName() {
            return "linux";
        }
    }

    static class FreeBSD extends Unix {
    }

    static class Solaris extends Unix {
        @Override
        public String getFamilyName() {
            return "solaris";
        }

        @Override
        protected String getOsPrefix() {
            return "sunos";
        }

        @Override
        protected String getArch() {
            String arch = System.getProperty("os.arch");
            if (arch.equals("i386") || arch.equals("x86")) {
                return "x86";
            }
            return super.getArch();
        }
    }

    private static String withExtension(String filePath, String extension) {
        if (filePath.toLowerCase().endsWith(extension)) {
            return filePath;
        }
        return removeExtension(filePath) + extension;
    }

    private static String removeExtension(String filePath) {
        int fileNameStart = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        int extensionPos = filePath.lastIndexOf('.');

        if (extensionPos > fileNameStart) {
            return filePath.substring(0, extensionPos);
        }
        return filePath;
    }
}
