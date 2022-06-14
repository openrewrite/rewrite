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
package org.openrewrite;

import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {
    private PathUtils() {}

    /**
     * Compare two paths, returning true if they indicate the same relative or absolute path, regardless of separators.
     *
     * "foo/a.txt" is considered to be equal to "foo\a.txt"
     */
    public static boolean equalIgnoringSeparators(Path a, Path b) {
        Path aNorm = Paths.get(FilenameUtils.separatorsToSystem(a.toString())).normalize();
        Path bNorm = Paths.get(FilenameUtils.separatorsToSystem(b.toString())).normalize();

        if(aNorm.getNameCount() != bNorm.getNameCount()) {
            return false;
        }

        for(int i = 0; i < aNorm.getNameCount(); i++) {
            if(!aNorm.getName(i).equals(bNorm.getName(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compare two strings representing file paths, returning tre if they indicate the same path regardless of separators
     */
    public static boolean equalIgnoringSeparators(String a , String b) {
        String[] aSegments = a.split("[\\\\/]+");
        String[] bSegments = b.split("[\\\\/]+");

        if(aSegments.length != bSegments.length) {
            return false;
        }
        for(int i = 0; i < aSegments.length; i++) {
            if(!aSegments[i].equals(bSegments[i])) {
                return false;
            }
        }

        return true;
    }
}
