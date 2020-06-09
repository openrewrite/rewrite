package org.openrewrite.java;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.fail;

public class JavaParserResolver implements ParameterResolver {
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(JavaParser.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        @SuppressWarnings("OptionalGetWithoutIsPresent") Object o = extensionContext.getTestInstance().get();
        try {
            // support arbitrarily nested tests in the TCK
            Class<?> clazz = o.getClass();
            Object target = o;
            do {
                try {
                    Method javaParser = clazz.getMethod("javaParser");
                    javaParser.setAccessible(true); // because JUnit 5 test classes don't have to be public
                    return javaParser.invoke(target);
                } catch (NoSuchMethodException ignored) {
                }

                try {
                    target = o.getClass().getDeclaredField("this$0").get(target);
                } catch (NoSuchFieldException e) {
                    break;
                }
            } while ((clazz = clazz.getEnclosingClass()) != null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        fail("This should never happen -- an implementation of javaParser() was not found");
        return null;
    }
}
