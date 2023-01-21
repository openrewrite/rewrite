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
package org.openrewrite.maven.tree;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Every model object that is transferred via the tooling API must implement {@link Serializable}, including
 * certain maven model objects that are shared by rewrite-gradle.
 */
public class OpenRewriteModelSerializableTest {

    @Test
    void mavenRepository() throws IOException {
        MavenRepository mavenRepository = new MavenRepository("id", "url", null, null, null, null);
        ObjectOutputStream os = new ObjectOutputStream(new ByteArrayOutputStream());
        os.writeObject(mavenRepository);
    }
}
