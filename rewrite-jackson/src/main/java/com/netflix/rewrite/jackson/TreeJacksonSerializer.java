package com.netflix.rewrite.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import com.netflix.rewrite.tree.Tr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;

public class TreeJacksonSerializer {
    private final TypeReference<List<Tr.CompilationUnit>> cuListType = new TypeReference<>() {
    };

    private final ObjectMapper mapper;

    public TreeJacksonSerializer() {
        var f = new SmileFactory();
        f.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true);
        this.mapper = new ObjectMapper(f).setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public String writePretty(Tr.CompilationUnit cu) {
        try {
            return new ObjectMapper()
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public void write(List<Tr.CompilationUnit> cus, OutputStream out) {
        try {
            mapper.writeValue(out, cus);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] write(List<Tr.CompilationUnit> cus) {
        try {
            return mapper.writeValueAsBytes(cus);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void write(Tr.CompilationUnit cu, OutputStream out) {
        try {
            mapper.writeValue(out, cu);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] write(Tr.CompilationUnit cu) {
        try {
            return mapper.writeValueAsBytes(cu);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<Tr.CompilationUnit> readList(InputStream input) {
        try {
            return mapper.readValue(input, cuListType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<Tr.CompilationUnit> readList(byte[] bytes) {
        try {
            return mapper.readValue(bytes, cuListType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Tr.CompilationUnit read(InputStream input) {
        try {
            return mapper.readValue(input, Tr.CompilationUnit.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Tr.CompilationUnit read(byte[] bytes) {
        try {
            return mapper.readValue(bytes, Tr.CompilationUnit.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}