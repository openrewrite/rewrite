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
package org.openrewrite.java.dataflow.internal.csv;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CSVLoader {
    public static <R extends Mergable, E> R loadFromFile(String csvFileName, R emptyModel, Function<Iterable<E>, R> merger, Function<String[], E> csvMapper) {
        final Mergable[] models = new Mergable[]{emptyModel};
        try (ScanResult scanResult = new ClassGraph().acceptPaths("data-flow").enableMemoryMapping().scan()) {
            scanResult.getResourcesWithLeafName(csvFileName)
                    .forEachInputStreamIgnoringIOException((res, input) -> models[0] = models[0].merge(loadCvs(input, res.getURI(), merger, csvMapper)));
        }
        //noinspection unchecked
        return (R) models[0];
    }

    private static <R extends Mergable, E> R loadCvs(InputStream input, URI source, Function<Iterable<E>, R> merger, Function<String[], E> csvMapper) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        try {
            List<E> models = new ArrayList<E>();
            //noinspection UnusedAssignment skip the header line
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(";");
                models.add(csvMapper.apply(tokens));
            }
            return merger.apply(models);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read data-flow values from " + source, e);
        }
    }

}
