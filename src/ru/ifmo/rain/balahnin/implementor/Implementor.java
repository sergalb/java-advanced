package ru.ifmo.rain.balahnin.implementor;


import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;

//public class Implementor implements JarImpler
public class Implementor implements Impler {
    private final String lineSeparator = System.lineSeparator();
    private final String tab = "    ";
    private Set<MethodWithHash> methods;
    private StringBuilder result;
    private Class<?> token;


    @Override
    public void implement(Class<?> token, Path path) throws ImplerException {
        result = new StringBuilder();
        methods = new HashSet<>();
        result = new StringBuilder();
        this.token = token;
        if (token == null || token.equals(Enum.class) || token.isPrimitive() || token.isArray() || Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Parent class can't be a primitive, Enum, Array, or final");
        }
        try {
            path = path.resolve(token.getPackageName().
                    replace('.', File.separatorChar)).
                    resolve(token.getSimpleName() + "Impl.java");
        } catch (InvalidPathException e) {
            throw new ImplerException(e.getMessage());
        }
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new ImplerException("can't create directory: " + path.getFileName());
            }
        }
        setPackage();
        setName();
        setConstructors(token);
        recursiveSetMethods(token);
        result.append("}");
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(result.toString());
        } catch (IOException e) {
            throw new ImplerException(e.getMessage());
        }
    }

    /*@Override
    public void implementJar(Class<?> token, Path path) throws ImplerException {

        createTempDir;
        implement(token, path + TempDirName);
        Java compiler: compile(createdFile, classPath - путь до TempDir + старое) и говорим run
        есть ClassNameImp.class - нужно запихать в джарник -try with resource и еще внутри джарника:
        try(stream - writer + manifest) [
            writer.putNextEntry(путь);
            Files.copy(writer, path до ClassNameImp.class);
        }
        манифест: кажется хуйня

        вернуть jar
        и удалить TempDir вместе со всем содержимым

    }

    /*
    psvm(String[] args) {
        если 2 аргумента - то нужно вызвать implement
        если 3 аргумента - то первый вероятно -jar и нужно вызвать implementJar
    }
     */

    private void recursiveSetMethods(Class<?> token) {
        if (token == null || Modifier.isFinal(token.getModifiers())) {
            return;
        }
        setMethods(token);
        for (Class<?> curInterface : token.getInterfaces()) {
            recursiveSetMethods(curInterface);
        }
        recursiveSetMethods(token.getSuperclass());
    }

    private void setConstructors(Class<?> token) throws ImplerException {
        Constructor<?>[] constructors = token.getDeclaredConstructors();
        if (!token.isInterface() &&
                Arrays.stream(constructors)
                        .allMatch(constructor -> Modifier.isPrivate(constructor.getModifiers()))) {
            throw new ImplerException("token hasn't non-private constructor");
        }
        for (Constructor<?> constructor : constructors) {
            setExecutable(constructor, "", new StringBuilder("super("), true);
        }
    }

    private void setMethods(Class<?> token) {
        List<Method> newMethods = new ArrayList<>();
        for (Method method : token.getDeclaredMethods()) {
            if (methods.add(new MethodWithHash(method)) && !method.isDefault()) {
                newMethods.add(method);
            }
        }
        for (Method method : newMethods) {
            Type returnType = method.getReturnType();
            StringBuilder body = new StringBuilder("return");
            if (((Class) returnType).isPrimitive()) {
                if (returnType.equals(boolean.class)) {
                    body.append(" false");
                } else if (!returnType.equals(void.class)) {
                    body.append(" 0");
                }
            } else {
                body.append(" null");
            }
            body.append(";");
            setExecutable(method, ((Class) returnType).getCanonicalName(), body, false);
        }
    }

    private void setExecutable(Executable executable, String returnType, StringBuilder body, boolean isConstructor) {
        int modifier = executable.getModifiers();
        //убили и е пишем abstract, transient, native
        modifier &= ~Modifier.ABSTRACT;
        modifier &= ~Modifier.TRANSIENT;
        modifier &= ~Modifier.NATIVE;

        if (Modifier.isPrivate(modifier) || Modifier.isFinal(modifier)
                || Modifier.isSynchronized(modifier) || Modifier.isVolatile(modifier)
                || Modifier.isStatic(modifier)) {
            return;
        }

        StringBuilder name = new StringBuilder(executable.getName());
        Parameter[] parameters = executable.getParameters();
        StringBuilder parametersStr = Arrays.stream(parameters).
                map(parameter -> new StringBuilder(parameter.getType().getCanonicalName() + " " + parameter.getName()))
                .reduce((paramNames, newName) -> new StringBuilder(paramNames + ", " + newName)).orElse(new StringBuilder());
        if (isConstructor) {
            name = (new StringBuilder(token.getSimpleName())).append("Impl");
            String parametersNames = Arrays.stream(parameters).map(Parameter::getName)
                    .reduce((paramNames, newName) -> paramNames + ", " + newName).orElse("");
            body.append(parametersNames).append(");");
        }

        result.append(tab).append(Modifier.toString(modifier)).append(" ")
                .append(returnType).append(" ")
                .append(name).append("(")
                .append(parametersStr).append(") ");
        setExceptions(executable);
        result.append("{").append(separatorWithTabs(2))
                .append(body).append(separatorWithTabs(1))
                .append("}").append(lineSeparator).append(lineSeparator);
    }

    private void setPackage() {
        String packageName = token.getPackageName();
        if (!packageName.isEmpty()) {
            result.append("package ").append(packageName).append(';').append(lineSeparator).append(lineSeparator);
        }
    }

    private void setName() {
        result.append("public class ")
                .append(token.getSimpleName()).append("Impl ")
                .append((token.isInterface()) ? "implements " : "extends ")
                .append(token.getSimpleName()).append(" {").append(lineSeparator);
    }

    private void setExceptions(Executable executable) {
        Class<?>[] exceptions = executable.getExceptionTypes();
        if (exceptions.length > 0) {
            result.append("throws ");
            for (Class<?> exception : exceptions) {
                result.append(exception.getCanonicalName()).append(" ");
            }
        }
    }

    private StringBuilder separatorWithTabs(int countTabs) {
        StringBuilder result = new StringBuilder(lineSeparator);
        for (int i = 0; i < countTabs; i++) {
            result.append(tab);
        }
        return result;
    }

    class MethodWithHash {

        private Method realMethod;
        private String hash;

        MethodWithHash(Method method) {
            realMethod = method;
            hash = realMethod.getName() + Arrays.toString(realMethod.getParameters());
        }

        public boolean equals(Object other) {
            if (other instanceof MethodWithHash) {
                return hash.equals(((MethodWithHash) other).hash);
            }
            return false;
        }

        public int hashCode() {
            return hash.hashCode();
        }
    }
}