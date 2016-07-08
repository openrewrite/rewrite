/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.java.refactor.compiler;

import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Options;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;

import javax.annotation.processing.Processor;
import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaCompilerHelper {
    List<JavaFileObject> sources = new ArrayList<>();
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
	InMemoryClassFileManager inMemoryClassFileManager = new InMemoryClassFileManager();
	List<Processor> processors;

	public JavaCompilerHelper(Processor... processors) {
		this.processors = Arrays.asList(processors);
	}

	private boolean addSource(final String sourceStr) {
		String className = fullyQualifiedName(sourceStr);
		if(className != null) {
			return sources.add(new SimpleJavaFileObject(URI.create("string:///" + className.replaceAll("\\.", "/") + ".java"), JavaFileObject.Kind.SOURCE) {
				@Override
				public CharSequence getCharContent(boolean ignoreEncodingErrors) {
					return sourceStr.trim();
				}
			});
		}
		return false;
	}
	
	public void compileAll(String... sourceStr) {
		for (String s : sourceStr) {
			addSource(s);
		}
		compileInternal();
	}
	
    public byte[] compile(final String sourceStr) {
		if(addSource(sourceStr)) {
			compileInternal();
			return inMemoryClassFileManager.classBytes(fullyQualifiedName(sourceStr));
        }
        return null;
    }

	public File jar(File f, String classSource) {
		return jar(f, Collections.singletonList(classSource));
	}
	
	public File jar(File f, Collection<String> classSources) {
		f.getParentFile().mkdirs();
		try(FileOutputStream fos = new FileOutputStream(f);
			JarOutputStream jos = new JarOutputStream(fos)) {

			for (String classSource : classSources) {
				String fqn = fullyQualifiedName(classSource);
				jos.putNextEntry(new JarEntry(fqn.replaceAll("\\.", "/") + ".class"));
				jos.write(compile(classSource));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return f;
	}
	
	private void compileInternal() {
		Context context = new Context();
		context.put(JavaFileManager.class, inMemoryClassFileManager);

		Options options = Options.instance(context);
		options.put("-g", "");
//		options.put("-verbose", "");
		options.put("verboseCompilePolicy", "");
		options.put("dev", "");
		options.put("-Xdiags:verbose", "");
//		options.isSet(Option.XDIAGS, "verbose");
		
		com.sun.tools.javac.main.JavaCompiler compiler = new com.sun.tools.javac.main.JavaCompiler(context);
		compiler.initProcessAnnotations(this.processors);

		try {
			compiler.compile(com.sun.tools.javac.util.List.from(sources.toArray(new JavaFileObject[sources.size()])));
		} catch (Throwable e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

    static String fullyQualifiedName(String sourceStr) {
		Matcher pkgMatcher = Pattern.compile("\\s*package\\s+([\\w\\.]+)").matcher(sourceStr);
		String pkg = pkgMatcher.find() ? pkgMatcher.group(1) + "." : "";

		Matcher classMatcher = Pattern.compile("\\s*(class|interface|enum)\\s+(\\w+)").matcher(sourceStr);
		return classMatcher.find() ? pkg + classMatcher.group(2) : null;
    }

    public void traceClass(String className) {
		PrintWriter pw = new PrintWriter(System.out);
		new ClassReader(inMemoryClassFileManager.classBytes(className)).accept(
                new TraceClassVisitor(pw), ClassReader.SKIP_FRAMES);
    }

	public void traceMethod(String className, final String method) {
		new ClassReader(inMemoryClassFileManager.classBytes(className)).accept(
				new ClassVisitor(Opcodes.ASM5) {
					PrintWriter pw = new PrintWriter(System.out);
					Textifier p = new Textifier();

					@Override
					public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
						if(name.equals(method)) {
							p.visitMethod(access, name, desc, signature, exceptions);
							return new TraceMethodVisitor(p);
						}
						return null;
					}

					@Override
					public void visitEnd() {
						p.visitClassEnd();
						if (pw != null) {
							p.print(pw);
							pw.flush();
						}
					}
				},
				ClassReader.SKIP_FRAMES
		);

	}

	public Object newInstance(String className) {
		try {
			return classLoader.loadClass(className).newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	ClassLoader classLoader = new ClassLoader() {
		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			byte[] b = inMemoryClassFileManager.classBytes(name);
			if(b != null) {
				return defineClass(name, b, 0, b.length);
			} else throw new ClassNotFoundException(name);
		}
	};

	class InMemoryClassFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
		Map<String, JavaClassObject> classesByName = new HashMap<>();

		protected InMemoryClassFileManager() {
			super(compiler.getStandardFileManager(diagnostics, null, null));
		}

		byte[] classBytes(String className) {
			JavaClassObject clazz = classesByName.get(className);
			return clazz == null ? null : clazz.bos.toByteArray();
		}

		@Override
		public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
			JavaClassObject clazz = new JavaClassObject(className, kind);
			classesByName.put(className, clazz);
			return clazz;
		}

		private class JavaClassObject extends SimpleJavaFileObject {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			JavaClassObject(String name, Kind kind) {
				super(URI.create("string:///" + name + ".class"), kind);
			}

			@Override
			public OutputStream openOutputStream() {
				return bos;
			}
		}
	}
}