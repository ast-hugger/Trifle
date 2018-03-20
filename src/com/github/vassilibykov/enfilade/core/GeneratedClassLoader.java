package com.github.vassilibykov.enfilade.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GeneratedClassLoader extends ClassLoader {
    private final Map<String, Compiler.Result> compilerResultsByName = new HashMap<>();
    private final Map<String, Class<?>> loadedClassesByName = new HashMap<>();

    public GeneratedClassLoader(ClassLoader parent) {
        super(parent);
    }

    public void add(Compiler.Result result) {
        compilerResultsByName.put(result.className(), result);
        dumpClassFile("dumped", result.bytecode());
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> loadedClass = loadedClassesByName.get(name);
        if (loadedClass == null) {
            Compiler.Result bytecodeHolder = compilerResultsByName.get(name);
            if (bytecodeHolder == null) {
                throw new ClassNotFoundException(name);
            }
            byte[] bytecode = bytecodeHolder.bytecode();
            loadedClass = defineClass(name, bytecode, 0, bytecode.length);
            loadedClassesByName.put(name, loadedClass);
        }
        return loadedClass;
    }

    private void dumpClassFile(String name, byte[] bytecode) {
        File classFile = new File(name + ".class");
        try {
            FileOutputStream classStream = new FileOutputStream(classFile);
            classStream.write(bytecode);
            classStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
