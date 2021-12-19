package org.openrewrite.java;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.internal.JavaTypeSignatureBuilder;
import org.openrewrite.java.tree.JavaType;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.StringJoiner;

class Java11TypeSignatureBuilder implements JavaTypeSignatureBuilder {
    @Nullable
    private Set<Type> typeStack;

    @Override
    public String signature(@Nullable Object t) {
        typeStack = null;
        return signature((Type) t);
    }

    private String signature(@Nullable Type type) {
        if (type == null) {
            return "{undefined}";
        } else if (type instanceof Type.ClassType) {
            return type.isParameterized() ? parameterizedSignature(type) : classSignature(type);
        } else if (type instanceof Type.TypeVar) {
            genericSignature(type);
        } else if (type instanceof Type.JCPrimitiveType) {
            return primitiveSignature(type);
        } else if (type instanceof Type.JCVoidType) {
            return "void";
        } else if (type instanceof Type.ArrayType) {
            return arraySignature(type);
        } else if (type instanceof Type.WildcardType) {
            if (type.isUnbound()) {
                return "?";
            } else {
                // FIXME how to find contravariant wildcards?
                return "? extends " + signature(((Type.WildcardType) type).type);
            }
        } else if (Type.noType.equals(type)) {
            return "{none}";
        }
        throw new IllegalStateException("Unexpected type " + type.getClass().getName());
    }

    private void completeClassSymbol(Symbol.ClassSymbol classSymbol) {
        String packageName = classSymbol.packge().fullname.toString();
        if (packageName.startsWith("com.sun.") ||
                packageName.startsWith("sun.") ||
                packageName.startsWith("java.awt.") ||
                packageName.startsWith("java.applet.") ||
                packageName.startsWith("jdk.") ||
                packageName.startsWith("org.graalvm")) {
            return;
        }

        try {
            classSymbol.complete();
        } catch (Symbol.CompletionFailure ignore) {
        }
    }

    @Override
    public String arraySignature(Object type) {
        return signature(((Type.ArrayType) type).elemtype) + "[]";
    }

    @Override
    public String classSignature(Object type) {
        Symbol.ClassSymbol sym = (Symbol.ClassSymbol) ((Type.ClassType) type).tsym;
        if (!sym.completer.isTerminal()) {
            completeClassSymbol(sym);
        }
        return sym.className();
    }

    @Override
    public String genericSignature(Object type) {
        Type.TypeVar generic = (Type.TypeVar) type;

        StringBuilder s = new StringBuilder(generic.tsym.name.toString());

        // FIXME if COVARIANT only, how to determine if contravariant or invariant?
        s.append(" extends ");

        StringJoiner boundSigs = new StringJoiner(" & ");
        if (generic.getUpperBound() instanceof Type.IntersectionClassType) {
            Type.IntersectionClassType intersectionBound = (Type.IntersectionClassType) generic.getUpperBound();
            if (intersectionBound.supertype_field != null) {
                boundSigs.add(genericBound(intersectionBound.supertype_field));
            }
            for (Type bound : intersectionBound.interfaces_field) {
                boundSigs.add(genericBound(bound));
            }
        } else {
            boundSigs.add(genericBound(generic.getUpperBound()));
        }
        s.append(boundSigs);

        return s.toString();
    }

    private String genericBound(Type bound) {
        if (typeStack != null && typeStack.contains(bound)) {
            return "(*)";
        }

        if (typeStack == null) {
            typeStack = Collections.newSetFromMap(new IdentityHashMap<>());
        }
        typeStack.add(bound);
        return signature(bound);
    }

    @Override
    public String parameterizedSignature(Object type) {
        StringBuilder s = new StringBuilder(classSignature(type));
        StringJoiner joiner = new StringJoiner(",", "<", ">");
        for (Type tp : ((Type.ClassType) type).typarams_field) {
            String signature = signature(tp);
            joiner.add(signature);
        }
        s.append(joiner);
        return s.toString();
    }

    @Override
    public String primitiveSignature(Object type) {
        TypeTag tag = ((Type.JCPrimitiveType) type).getTag();
        switch (tag) {
            case BOOLEAN:
                return JavaType.Primitive.Boolean.getKeyword();
            case BYTE:
                return JavaType.Primitive.Byte.getKeyword();
            case CHAR:
                return JavaType.Primitive.Char.getKeyword();
            case DOUBLE:
                return JavaType.Primitive.Double.getKeyword();
            case FLOAT:
                return JavaType.Primitive.Float.getKeyword();
            case INT:
                return JavaType.Primitive.Int.getKeyword();
            case LONG:
                return JavaType.Primitive.Long.getKeyword();
            case SHORT:
                return JavaType.Primitive.Short.getKeyword();
            case VOID:
                return JavaType.Primitive.Void.getKeyword();
            case NONE:
                return JavaType.Primitive.None.getKeyword();
            case CLASS:
                return JavaType.Primitive.String.getKeyword();
            case BOT:
                return JavaType.Primitive.Null.getKeyword();
            default:
                throw new IllegalArgumentException("Unknown type tag " + tag);
        }
    }

    public String methodSignature(Type selectType, Symbol.MethodSymbol symbol) {
        Type genericType = symbol.type;

        // Formatted like com.MyThing{name=add,resolved=Thing(Integer),generic=Thing<?>(Integer)}
        return signature(symbol.owner.type) + "{name=" + symbol.getSimpleName().toString() +

                // resolved signature
                ",resolved=" +
                signature(selectType.getReturnType()) + '(' +
                methodArgumentSignature(selectType, new StringJoiner(",")) + ')' +

                // generic signature
                ",generic=" +
                signature(genericType.getReturnType()) + '(' +
                methodArgumentSignature(genericType, new StringJoiner(",")) + ')';
    }

    private StringJoiner methodArgumentSignature(Type selectType, StringJoiner resolvedArgumentTypes) {
        if (selectType instanceof Type.MethodType) {
            Type.MethodType mt = (Type.MethodType) selectType;
            if (!mt.argtypes.isEmpty()) {
                for (Type argtype : mt.argtypes) {
                    if (argtype != null) {
                        resolvedArgumentTypes.add(signature(argtype));
                    }
                }
            }
        } else if (selectType instanceof Type.ForAll) {
            methodArgumentSignature(((Type.ForAll) selectType).qtype, resolvedArgumentTypes);
        }
        return resolvedArgumentTypes;
    }

    public String variableSignature(Symbol symbol) {
        // Formatted like com.MyThing{name=MY_FIELD,type=java.lang.String}
        return signature(symbol.owner.type) + "{name=" + symbol.name.toString() + '}';
    }
}
