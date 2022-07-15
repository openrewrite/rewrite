/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrewrite.artifacts;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import org.openrewrite.internal.lang.Nullable;

import java.util.Objects;

import static org.openrewrite.internal.StringUtils.isNullOrEmpty;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@Data
@EqualsAndHashCode
public class ContainerImage {

    @With
    @Nullable
    String repository;
    @With
    @Nullable
    String image;
    @With
    @Nullable
    String tag;
    @Nullable
    String digest;

    public ContainerImage(@Nullable String repository,
                           @Nullable String image,
                           @Nullable String tag,
                           @Nullable String digest) {
        this.repository = repository;
        this.image = image;
        this.tag = tag;
        this.digest = digest;
    }

    public static ContainerImage parseImageName(String imageName) {
        String repository = null;
        String image = imageName;
        String tag = null;
        String digest = null;

        int idx = imageName.lastIndexOf('@');
        if (idx > -1) {
            digest = imageName.substring(idx + 1);
            imageName = imageName.substring(0, idx);
        }
        idx = imageName.lastIndexOf(':');
        if (idx > -1) {
            image = imageName.substring(0, idx);
            tag = imageName.substring(idx + 1);
            imageName = imageName.substring(0, idx);
        }
        idx = imageName.lastIndexOf('/');
        if (idx > -1) {
            image = imageName.substring(idx + 1);
            String s = imageName.substring(0, idx);
            if (!isNullOrEmpty(s)) {
                repository = s;
            }
        }
        return new ContainerImage(repository, image, tag, digest);
    }

    public boolean matches(ContainerImage otherName) {
        boolean matchesRepo =
                (this.getRepository() == null && otherName.getRepository() == null)
                        || Objects.equals(this.getRepository(), otherName.getRepository())
                        || matchesGlob(this.getRepository(), otherName.getRepository());
        boolean matchesImage =
                (this.getImage() == null && otherName.getImage() == null)
                        || Objects.equals(this.getImage(), otherName.getImage())
                        || matchesGlob(this.getImage(), otherName.getImage());
        boolean matchesTag =
                (this.getTag() == null && otherName.getTag() == null)
                        || Objects.equals(this.getTag(), otherName.getTag())
                        || matchesGlob(this.getTag(), otherName.getTag());
        boolean matchesDigest =
                (this.getDigest() == null && otherName.getDigest() == null)
                        || Objects.equals(this.getDigest(), otherName.getDigest())
                        || matchesGlob(this.getDigest(), otherName.getDigest());

        return matchesRepo && matchesImage && matchesTag && matchesDigest;
    }

    public boolean hasRepository() {
        return !isNullOrEmpty(repository);
    }

    public boolean hasImage() {
        return !isNullOrEmpty(image);
    }

    public boolean hasTag() {
        return !isNullOrEmpty(tag);
    }

    public boolean hasDigest() {
        return !isNullOrEmpty(digest);
    }

    @Override
    public String toString() {
        String s = "";
        if (!isNullOrEmpty(repository)) {
            s += repository + "/";
        }
        s += image;
        if (!isNullOrEmpty(tag)) {
            s += ":" + tag;
        }
        if (!isNullOrEmpty(digest) && !"*".equals(digest)) {
            s += "@" + digest;
        }
        return s;
    }

}
