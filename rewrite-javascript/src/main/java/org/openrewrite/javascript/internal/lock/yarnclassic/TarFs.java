/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.PosixPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Port of tar-fs 1.16.3 {@code extract} on POSIX as yarn 1.22.22's tarball fetcher calls it ({@code strip: 1} and its
 * {@code map} of link targets), and of 1.16.0, which yarn bundles before 1.10.0: the directory an archive extracts
 * into.
 */
final class TarFs {

    /**
     * A file, a directory or a symbolic link; the names of a hard link share one file, whose bytes stay in the
     * archive until read.
     */
    static final class Node {
        final boolean directory;
        final @Nullable String symlink;
        private byte[] archive = new byte[0];
        private int offset;
        private int size;

        /**
         * The entry's name where the archive ends inside its content: its write runs on after the extract finishes, so
         * what a read finds there follows the timing of the disk.
         */
        private @Nullable String cut;

        private Node(boolean directory, @Nullable String symlink) {
            this.directory = directory;
            this.symlink = symlink;
        }

        byte[] content() {
            if (cut != null) {
                throw new CannotKnow("what yarn reads of " + cut + ", which the tarball's archive ends inside: the " +
                                     "extract finishes there, the file's write runs on");
            }
            return Arrays.copyOfRange(archive, offset, offset + size);
        }
    }

    private static final int MAX_LINKS = 40;

    private final Map<String, Node> tree = new HashMap<>();

    /**
     * The tar-fs yarn bundles: 1.16.3 from yarn 1.10.0; before, 1.16.0, which neither refuses an entry whose directory
     * is a link ({@code validate}) nor keeps a hard link's target inside the directory (1.15.2, before yarn 1.7.0,
     * extracts as 1.16.0 does on POSIX).
     */
    enum Release {
        V1_16_0,
        V1_16_3
    }

    /**
     * Whether yarn's map keeps every link inside the directory (from yarn 1.22.0).
     */
    private final boolean jail;

    private final Release release;

    private TarFs(boolean jail, Release release) {
        this.jail = jail;
        this.release = release;
        tree.put("", new Node(true, null));
    }

    /**
     * @throws JsError the error the extraction stops with: an entry type it does not write, a path through a file.
     */
    static TarFs extract(byte[] tar) {
        return extract(tar, true, Release.V1_16_3);
    }

    /**
     * {@code extract} of tar-fs {@code release} with yarn's map of link targets into the directory ({@code jail}), as
     * yarn 1.22.0 and later extract; before, a link out of the directory reaches the disk around yarn's cache.
     */
    static TarFs extract(byte[] tar, boolean jail, Release release) {
        return extract(tar, jail, release, TarStream.Release.V1_6_1, true);
    }

    /**
     * {@code extract} with tar-stream {@code streamRelease}, on a Writable that fails an archive that ends early or
     * not ({@link TarStream#extract(byte[], TarStream.OnEntry, TarStream.Release, boolean)}).
     */
    static TarFs extract(byte[] tar, boolean jail, Release release, TarStream.Release streamRelease,
                         boolean failsWhereTheDataEnds) {
        TarFs fs = new TarFs(jail, release);
        TarStream.extract(tar, fs::entry, streamRelease, failsWhereTheDataEnds);
        return fs;
    }

    private void entry(TarStream.Header header, byte[] archive, int offset, int size) {
        header.name = strip(header.name);
        String linkname = header.linkname;
        if (Js.truthy(linkname) && ("link".equals(header.type) || PosixPath.isAbsolute(linkname))) {
            header.linkname = strip(linkname);
        }
        if (jail && Js.truthy(header.linkname)) {
            String basePath = PosixPath.dirname(PosixPath.join("/", header.name));
            header.linkname = PosixPath.relative("/", PosixPath.join(basePath, header.linkname));
        }
        String name = PosixPath.join("/", header.name).substring(1);
        if ("directory".equals(header.type)) {
            mkdirp(name);
            return;
        }
        String dir = parent(name);
        if (release == Release.V1_16_3) {
            validate(dir);
        }
        mkdirp(dir);
        if ("file".equals(header.type)) {
            write(name, archive, offset, size, header.cut);
        } else if ("link".equals(header.type) || "symlink".equals(header.type)) {
            if (header.linkname == null) {
                throw Js.invalidArgType("The \"" + ("link".equals(header.type) ? "path" : "target") +
                                             "\" argument must be of type string", null);
            }
            unlink(name);
            if ("link".equals(header.type)) {
                link(hardLinkTarget(header.linkname), name);
            } else {
                create(name, new Node(false, header.linkname), "symlink");
            }
        } else {
            throw new JsError(JsError.ERROR, "unsupported type for " + name + " (" + header.type + ")");
        }
    }

    /**
     * A hard link's source: 1.16.3 joins its target to the directory as a path from its root; 1.16.0 resolves it from
     * the directory, out of it where it climbs out or is absolute.
     */
    private String hardLinkTarget(String linkname) {
        if (release == Release.V1_16_3) {
            return PosixPath.join("/", linkname).substring(1);
        }
        String target = PosixPath.normalize(linkname);
        if (PosixPath.isAbsolute(target) || "..".equals(target) || target.startsWith("../")) {
            throw outside(linkname);
        }
        return ".".equals(target) ? "" : target;
    }

    private static CannotKnow outside(String location) {
        return new CannotKnow("what the files outside yarn's cache directory hold that a link of the archive leads " +
                              "to (" + location + ")");
    }

    private static String strip(String name) {
        List<String> parts = Arrays.asList(name.split("/", -1));
        return String.join("/", parts.subList(1, parts.size()));
    }

    private static String parent(String location) {
        int slash = location.lastIndexOf('/');
        return slash < 0 ? "" : location.substring(0, slash);
    }

    /**
     * The path with every symbolic link on it followed, the last one only when {@code followLast}; null when a link
     * leads out of the directory or round in a loop.
     */
    private @Nullable String real(String location, boolean followLast) {
        LinkedList<String> todo = new LinkedList<>(segments(location));
        List<String> done = new ArrayList<>();
        int links = 0;
        while (!todo.isEmpty()) {
            String segment = todo.removeFirst();
            if ("..".equals(segment)) {
                if (done.isEmpty()) {
                    if (!jail && links > 0) {
                        throw outside(location);
                    }
                    return null;
                }
                done.remove(done.size() - 1);
                continue;
            }
            done.add(segment);
            Node node = tree.get(String.join("/", done));
            if (node != null && node.symlink != null && (followLast || !todo.isEmpty())) {
                if (++links > MAX_LINKS || PosixPath.isAbsolute(node.symlink)) {
                    return null;
                }
                done.remove(done.size() - 1);
                todo.addAll(0, segments(node.symlink));
            }
        }
        return String.join("/", done);
    }

    private static List<String> segments(String path) {
        List<String> segments = new ArrayList<>();
        for (String segment : path.split("/")) {
            if (!segment.isEmpty() && !".".equals(segment)) {
                segments.add(segment);
            }
        }
        return segments;
    }

    private @Nullable Node node(@Nullable String real) {
        return real == null ? null : tree.get(real);
    }

    // validate: an existing directory up to the root that lstat sees as anything but a directory is no valid path
    private void validate(String dir) {
        for (String p = dir; !p.isEmpty(); p = parent(p)) {
            Node node = node(real(p, false));
            if (node != null && !node.directory) {
                throw new JsError(JsError.ERROR, p + " is not a valid path");
            }
        }
    }

    private void mkdirp(String dir) {
        if (dir.isEmpty()) {
            return;
        }
        mkdirp(parent(dir));
        String real = real(dir, true);
        Node node = node(real);
        // without validate, 1.16.0 makes the directories a link leads to, an absolute one on the disk around the cache
        if (real == null && release == Release.V1_16_0) {
            throw outside(dir);
        }
        if (real == null || node != null && !node.directory) {
            throw error("EEXIST: file already exists, mkdir '" + dir + "'");
        }
        if (node == null) {
            tree.put(real, new Node(true, null));
        }
    }

    // createWriteStream opens through symbolic links; a directory is no file to write
    private void write(String name, byte[] archive, int offset, int size, boolean cut) {
        String real = real(name, true);
        Node node = node(real);
        if (node == null) {
            Node parent = real == null ? null : node(real(parent(real), true));
            if (parent == null || !parent.directory) {
                throw error("ENOENT: no such file or directory, open '" + name + "'");
            }
            node = new Node(false, null);
            tree.put(real, node);
        } else if (node.directory) {
            throw error("EISDIR: illegal operation on a directory, open '" + name + "'");
        }
        node.archive = archive;
        node.offset = offset;
        node.size = size;
        node.cut = cut ? name : null;
    }

    // tar-fs ignores what unlink says: a directory stays
    private void unlink(String name) {
        String real = real(name, false);
        Node node = node(real);
        if (node != null && !node.directory) {
            tree.remove(real);
        }
    }

    private void link(String source, String name) {
        Node node = node(real(source, false));
        if (node == null) {
            throw error("ENOENT: no such file or directory, link '" + source + "' -> '" + name + "'");
        }
        if (node.directory) {
            throw error("EPERM: operation not permitted, link '" + source + "' -> '" + name + "'");
        }
        create(name, node, "link");
    }

    private void create(String name, Node node, String syscall) {
        String real = real(name, false);
        if (real == null || tree.containsKey(real)) {
            throw error("EEXIST: file already exists, " + syscall + " '" + name + "'");
        }
        tree.put(real, node);
    }

    /**
     * What a path names with its links followed, as {@code fs.exists} and {@code fs.readFile} see it; null for
     * nothing.
     */
    @Nullable
    Node get(String location) {
        return node(real(location, true));
    }

    private static JsError error(String message) {
        return new JsError(JsError.ERROR, message);
    }
}
