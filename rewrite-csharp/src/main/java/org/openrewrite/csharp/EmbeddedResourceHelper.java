/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.csharp;

import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class EmbeddedResourceHelper {
    public static final URL REWRITE_SERVER_DLL_RESOURCE =
            Objects.requireNonNull(EmbeddedResourceHelper.class.getClassLoader()
//                    .getContextClassLoader()
                    .getResource("DotnetServer.zip"));


    public static void installDotnetServer(File destDir) {
        installExecutable(REWRITE_SERVER_DLL_RESOURCE, destDir);
    }

    @SneakyThrows
    public static void installExecutable(URL executable, File destDir) {
        // Create lock file for synchronization
        File lockFile = new File(destDir, ".extract.lock");
        destDir.mkdirs();

        // Keep lock file around - don't delete it to avoid race conditions
        RandomAccessFile raf = null;
        FileChannel channel = null;
        FileLock lock = null;

        try {
            raf = new RandomAccessFile(lockFile, "rw");
            channel = raf.getChannel();

            // Try to acquire lock with retries
            int maxRetries = 100;
            int retryCount = 0;
            while (lock == null && retryCount < maxRetries) {
                try {
                    lock = channel.tryLock();
                    if (lock == null) {
                        // Another process has the lock, wait and retry
                        Thread.sleep(100);
                        retryCount++;
                    }
                } catch (OverlappingFileLockException e) {
                    // This thread already has a lock on this file
                    Thread.sleep(100);
                    retryCount++;
                }
            }

            if (lock == null) {
                throw new IOException("Could not acquire lock after " + maxRetries + " retries");
            }

            // Double-check if extraction is needed after acquiring lock
            Map<String, String> zipChecksums = computeZipChecksums(executable);

            boolean needsExtraction = false;
            for (Map.Entry<String, String> entry : zipChecksums.entrySet()) {
                File targetFile = new File(destDir, entry.getKey());
                if (!targetFile.exists() || !entry.getValue().equals(computeFileChecksum(targetFile))) {
                    needsExtraction = true;
                    break;
                }
            }

            if (needsExtraction) {
                System.out.println("DotnetServer extraction needed - proceeding with extraction to: " + destDir);
                extractZipWithRetry(executable, destDir);
                System.out.println("DotnetServer extraction completed successfully");
            } else {
                System.out.println("DotnetServer already up-to-date in: " + destDir);
            }

        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    @SneakyThrows
    private static void extractZipWithRetry(URL executable, File destDir) {
        int maxRetries = 3;
        IOException lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                extractZip(executable, destDir);
                return; // Success
            } catch (IOException e) {
                lastException = e;
                if (e.getMessage() != null && e.getMessage().contains("being used by another process")) {
                    System.out.println("File in use, waiting before retry " + (i + 1) + "/" + maxRetries);
                    Thread.sleep(1000); // Wait 1 second before retry
                } else {
                    throw e; // Don't retry for other types of errors
                }
            }
        }

        if (lastException != null) {
            throw new IOException("Failed to extract after " + maxRetries + " retries", lastException);
        }
    }

    @SneakyThrows
    private static Map<String, String> computeZipChecksums(URL zipUrl) {
        Map<String, String> checksums = new HashMap<>();
        byte[] buffer = new byte[8192];

        try (ZipInputStream zis = new ZipInputStream(zipUrl.openStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        md.update(buffer, 0, len);
                    }
                    checksums.put(entry.getName(), bytesToHex(md.digest()));
                }
                zis.closeEntry();
            }
        }
        return checksums;
    }

    @SneakyThrows
    private static String computeFileChecksum(File file) {
        if (!file.exists()) {
            return "";
        }
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        return bytesToHex(md.digest(fileBytes));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }


    private static void extractZip(URL executable, File destDir) throws IOException {
        byte[] buffer = new byte[8192];
        try (ZipInputStream zis = new ZipInputStream(executable.openStream())) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(destDir, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        if (!newFile.isDirectory()) {
                            throw new IOException("Failed to create directory " + newFile);
                        }
                    }
                } else {
                    // fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    // write file content
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
        }
    }
    static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
