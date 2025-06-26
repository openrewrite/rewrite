/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.internal.parser.data;

public class StaticConstantsClass {
    // Primitive compile-time constants - these SHOULD be stored
    public static final int INT_CONSTANT = 42;
    public static final String STRING_CONSTANT = "Hello World";
    public static final boolean BOOLEAN_CONSTANT = true;
    public static final long LONG_CONSTANT = 123456789L;
    public static final float FLOAT_CONSTANT = 3.14f;
    public static final double DOUBLE_CONSTANT = 2.718281828;
    public static final char CHAR_CONSTANT = 'A';
    public static final byte BYTE_CONSTANT = 127;
    public static final short SHORT_CONSTANT = 32767;
    
    // String constants with special characters
    public static final String STRING_WITH_QUOTES = "He said \"Hello\"";
    public static final String STRING_WITH_PIPES = "value1|value2|value3";
    public static final String EMPTY_STRING = "";
    
    // Null constant - this SHOULD be stored as null
    public static final String NULL_STRING = null;
    public static final Object NULL_OBJECT = null;
    
    // Class constants - these should NOT be stored (not compile-time constants in Java)
    public static final Class<?> CLASS_CONSTANT = String.class;
    
    // Enum constants - these should NOT be stored (not compile-time constants)
    public static final TestEnum ENUM_CONSTANT = TestEnum.VALUE1;
    
    // Expression constants - these SHOULD be stored (compile-time constant expressions)
    public static final int EXPRESSION_CONSTANT = 10 + 20 * 2;
    public static final String CONCAT_CONSTANT = "Hello" + " " + "World";
    
    // Array constants - these should NOT be stored (arrays are not compile-time constants)
    public static final int[] ARRAY_CONSTANT = {1, 2, 3};
    
    // These should NOT be stored as they're not compile-time constants
    public static final String METHOD_RESULT = getString(); // Method call
    public static final long CURRENT_TIME = System.currentTimeMillis(); // Method call
    
    // These should NOT be stored for other reasons
    public final int INSTANCE_FINAL = 100; // Not static
    public static int STATIC_NOT_FINAL = 200; // Not final
    private static final int PRIVATE_CONSTANT = 300; // Private (should still work but test separately)
    
    public static String getString() {
        return "dynamic";
    }
    
    public enum TestEnum {
        VALUE1, VALUE2
    }
}