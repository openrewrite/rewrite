package org.openrewrite.java.tree;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;

public class TreeSerializer {
    private final TypeReference<List<J.CompilationUnit>> cuListType = new TypeReference<>() {
    };

    private final ObjectMapper mapper;

    public TreeSerializer() {
        var f = new SmileFactory();
        f.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true);
        this.mapper = new ObjectMapper(f).setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public String writePretty(J.CompilationUnit cu) {
        try {
            return new ObjectMapper()
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(cu);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public void write(List<J.CompilationUnit> cus, OutputStream out) {
        try {
            mapper.writeValue(out, cus);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] write(List<J.CompilationUnit> cus) {
        try {
            return mapper.writeValueAsBytes(cus);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void write(J.CompilationUnit cu, OutputStream out) {
        try {
            mapper.writeValue(out, cu);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] write(J.CompilationUnit cu) {
        try {
            return mapper.writeValueAsBytes(cu);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<J.CompilationUnit> readList(InputStream input) {
        try {
            return mapper.readValue(input, cuListType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<J.CompilationUnit> readList(byte[] bytes) {
        try {
            return mapper.readValue(bytes, cuListType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public J.CompilationUnit read(InputStream input) {
        try {
            return mapper.readValue(input, J.CompilationUnit.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public J.CompilationUnit read(byte[] bytes) {
        try {
            return mapper.readValue(bytes, J.CompilationUnit.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}