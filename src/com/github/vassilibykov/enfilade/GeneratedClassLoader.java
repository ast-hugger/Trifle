package com.github.vassilibykov.enfilade;

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
}
