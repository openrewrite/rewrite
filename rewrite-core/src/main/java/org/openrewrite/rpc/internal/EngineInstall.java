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
package org.openrewrite.rpc.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Installs the engine an RPC server runs from, a package or a tool, into a directory of its own, once, however many
 * threads and processes ask for it at the same time: as parallel test forks do, one JVM each, when they first need a
 * version that is not installed yet.
 * <ul>
 * <li>An install is complete once its marker, {@value #MARKER}, names its version. The marker is written last, so a
 * directory without it, which a crashed, killed or raced install leaves, is never taken for installed: it is removed
 * and installed again.</li>
 * <li>An install is made in a private temporary directory beside its target and moved into place whole, so that no
 * process ever sees a directory while it is being written, and two installs never mix their files in one.</li>
 * <li>Installs of one target are serialized across processes by a {@link FileLock} on a lock file beside it, and within
 * a JVM by a monitor, since a JVM holds a file lock for all of its threads. A process that waited finds the install the
 * one before it made, and uses it.</li>
 * </ul>
 */
public final class EngineInstall {

    /**
     * The file in an installed engine's directory that marks its install complete, holding the version installed.
     */
    public static final String MARKER = ".openrewrite-install-complete";

    private static final String TEMPORARY_INFIX = ".installing-";

    private static final int MOVE_ATTEMPTS = 20;

    private static final ConcurrentMap<String, Object> MONITORS = new ConcurrentHashMap<>();

    private EngineInstall() {
    }

    /**
     * Makes an install into a directory that is empty, and that is the install's alone until it is moved into place.
     */
    @FunctionalInterface
    public interface Installer {
        void install(Path directory) throws Exception;
    }

    /**
     * Whether {@code directory} holds a complete install of {@code version}.
     */
    public static boolean isInstalled(Path directory, String version) {
        Path marker = directory.resolve(MARKER);
        if (!Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        try {
            return version.equals(new String(Files.readAllBytes(marker), UTF_8).trim());
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * {@code target} once it holds a complete install of {@code version}, which {@code installer} makes unless it already
     * does. A directory there without the marker, a partial install, is removed first, as are the temporary
     * directories of installs that never finished.
     *
     * @throws RuntimeException what the installer threw, or an {@link UncheckedIOException} when the install could not
     *                          be put in place
     */
    public static Path ensureInstalled(Path target, String version, Installer installer) {
        Path directory = target.toAbsolutePath().normalize();
        if (isInstalled(directory, version)) {
            return directory;
        }
        Path parent = directory.getParent();
        String name = directory.getFileName().toString();
        return exclusively(parent.resolve(name + ".lock"), () -> {
            if (isInstalled(directory, version)) {
                // Installed by the process that held the lock before this one.
                return directory;
            }
            deleteRecursively(directory);
            deleteTemporaries(parent, name);
            // Not Files.createTempDirectory, which makes the directory its owner's alone: the install keeps the
            // permissions of what it is moved as, and an install made as one user, as in an image, is read as another.
            Path temporary = Files.createDirectory(parent.resolve("." + name + TEMPORARY_INFIX + UUID.randomUUID()));
            try {
                installer.install(temporary);
                Files.write(temporary.resolve(MARKER), version.getBytes(UTF_8));
                moveIntoPlace(temporary, directory, version);
            } finally {
                // Nothing is left there once the install has moved; what a failed install left goes, and if it cannot,
                // the next install removes it rather than this one hiding why it failed.
                try {
                    deleteRecursively(temporary);
                } catch (IOException ignored) {
                }
            }
            return directory;
        });
    }

    /**
     * Runs {@code action} holding the lock {@code lockFile} names, which no other process or thread holds meanwhile.
     *
     * @throws RuntimeException what the action threw, an {@link UncheckedIOException} wrapping the {@link IOException}
     *                          it threw, or one when the lock could not be taken
     */
    public static <T> T exclusively(Path lockFile, Callable<T> action) {
        Path lock = lockFile.toAbsolutePath().normalize();
        Object monitor = MONITORS.computeIfAbsent(lock.toString(), key -> new Object());
        synchronized (monitor) {
            try {
                Files.createDirectories(lock.getParent());
                try (FileChannel channel = FileChannel.open(lock, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                     FileLock ignored = channel.lock()) {
                    return action.call();
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while installing under " + lock, e);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Moves the finished install into place as a whole, by a rename when the file system allows one. A target that
     * appeared meanwhile, which only a process that takes no lock could have put there, is used when it is complete.
     * On Windows a virus scanner holds the files just written open for a moment, and a directory cannot be renamed
     * while a file in it is open, so a move that is refused is tried again for a few seconds.
     */
    private static void moveIntoPlace(Path temporary, Path directory, String version)
            throws IOException, InterruptedException {
        for (int attempt = 1; ; attempt++) {
            try {
                try {
                    Files.move(temporary, directory, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, directory);
                }
                return;
            } catch (FileAlreadyExistsException | DirectoryNotEmptyException e) {
                if (isInstalled(directory, version)) {
                    return;
                }
                throw e;
            } catch (AccessDeniedException e) {
                // Also how Windows refuses to rename a directory over one that exists.
                if (isInstalled(directory, version)) {
                    return;
                }
                if (attempt == MOVE_ATTEMPTS) {
                    throw e;
                }
                Thread.sleep(250);
            }
        }
    }

    /**
     * Removes what installs of {@code name} that never finished left beside it: their temporary directories. Run under
     * the lock, so no install of it is under way.
     */
    private static void deleteTemporaries(Path parent, String name) throws IOException {
        String prefix = "." + name + TEMPORARY_INFIX;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(parent, entry ->
                entry.getFileName().toString().startsWith(prefix))) {
            for (Path entry : entries) {
                deleteRecursively(entry);
            }
        }
    }

    /**
     * Deletes {@code path} and everything beneath it, following no link out of it; nothing when there is nothing there.
     */
    static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
                if (e instanceof NoSuchFileException) {
                    return FileVisitResult.CONTINUE;
                }
                throw e;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                if (e != null && !(e instanceof NoSuchFileException)) {
                    throw e;
                }
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
