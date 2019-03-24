package ru.ifmo.rain.balahnin.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Implementation for {@link JarImpler}
 */
public class Implementor implements JarImpler {
    /**
     * {@link String} constant for line separator
     **/
    private final String lineSeparator = System.lineSeparator();
    /**
     * {@link String} constant for tab
     **/
    private final String tab = "    ";
    /**
     * {@link Set} set of methods for escape collisions
     **/
    private Set<MethodWithHash> methods;
    /**
     * {@link StringBuilder} ans of implementing
     **/
    private StringBuilder result;
    /**
     * {@link Class} inputted token
     **/
    private Class<?> token;

    /**
     * Create Implementor, without token
     */
    public Implementor(){}

    /**
     * depends on parameters run {@code implement} or {@code implementJar}
     * <p>
     * If arguments is {@code [realizable class] [path to save file] }
     * create java-file with implementation of realizable class on the path.
     * If arguments is {@code [-jar] [realizable class] [path to save file] }
     * create jar-file with implementation of realizable class on the path
     *
     * @param args strings with input - Should contains
     *             *             {@code [realizable class] [path to save file] }
     *             *             or {@code [-jar] [realizable class] [path to save jar-file] }
     * @throws ImplerException if has impler exceptions while implementing.
     */
    public static void main(String[] args) throws ImplerException {
        if (args == null) {
            System.err.println("need 2 arguments - input and output files names, now - 0");
            return;
        } else if (args.length < 2) {
            System.err.println("need 2 or 3 arguments" + args.length);
            return;
        }
        Implementor implementor = new Implementor();
        try {
            if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else if (args.length == 3) {
                if (args[0].equals("jar")) {
                    implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
                }
            }
        } catch (ClassNotFoundException e) {
            throw new ImplerException("couldn't find class " + e.getMessage());
        } catch (InvalidPathException e) {
            throw new ImplerException("Invalid path: " + e.getMessage());
        }

    }


    /**
     * implemented token to path
     *
     * @param token token with implementing class
     * @param path  path to place where needed implementing file
     * @throws ImplerException if couldn't create directory or
     *                         {@code setConstructors()} throw Exception
     *                         or couldn't write to path
     */
    @Override
    public void implement(Class<?> token, Path path) throws ImplerException {
        result = new StringBuilder();
        methods = new HashSet<>();
        result = new StringBuilder();
        this.token = token;
        if (token == null || token.equals(Enum.class) || token.isPrimitive() || token.isArray() || Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("token class can't be a primitive, Enum, Array, or final");
        }
        try {
            path = path.resolve(token.getPackageName()
                    .replace('.', File.separatorChar))
                    .resolve(token.getSimpleName() + "Impl.java");
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
        StringBuilder unicodeResult = new StringBuilder();
        for (int i = 0; i < result.length(); ++i) {
            int codeChar = (int) result.charAt(i);
            unicodeResult.append((codeChar >= 128) ? (String.format("\\u%04x", codeChar)) : (char) codeChar);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(unicodeResult.toString());
        } catch (IOException e) {
            throw new ImplerException(e.getMessage());
        }
    }

    /**
     * create jar-file in {@code path} and put them implemented {@code token}
     *
     * @param token token with implementing class
     * @param path  path to place where needed implementing file
     * @throws ImplerException if couldn't create directory or
     *                         {@code implement} throw ImplerException
     *                         or couldn't compile class
     */
    @Override
    public void implementJar(Class<?> token, Path path) throws ImplerException {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory(path.toAbsolutePath().getParent(), "temp");
        } catch (IOException e) {
            throw new ImplerException("can't create temp dir: " + e.getMessage());
        }

        implement(token, tempDir);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("System compiler is null");
        }
        String filePath = tempDir.resolve(token.getCanonicalName().replace('.', File.separatorChar) + "Impl").toString();
        String[] args = {
                "-encoding", "UTF-8",
                tempDir.resolve(filePath + ".java").toString(),
                "-cp",
                tempDir.toString() + File.pathSeparator + System.getProperty("java.class.path")
        };

        if (compiler.run(InputStream.nullInputStream(), OutputStream.nullOutputStream(),
                OutputStream.nullOutputStream(), args) != 0) {
            throw new ImplerException("can't compile class: " + token.getSimpleName());
        }

        try (JarOutputStream writer = new JarOutputStream(Files.newOutputStream(path), getManifest())) {
            writer.putNextEntry(new ZipEntry(token.getName().replace('.', '/') + "Impl.class"));
            Files.copy(tempDir.resolve(filePath + ".class"), writer);
        } catch (IOException e) {
            System.err.println("Error while write in jar: " + e.getMessage());
        }
    }

    /**
     * create manifest with my name
     *
     * @return Manifest
     */
    private Manifest getManifest() {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR, "Balahnin Sergey");
        return manifest;
    }

    /**
     * take all parents and all interfaces and their parents and call
     * {@code setMethods()} from them
     *
     * @param token implementing class(or interface) (because token can be changed by recursive)
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

    /**
     * take all non-private constructors and add to result implementation of them
     *
     * @param token implementing class
     * @throws ImplerException if all constructors are private
     */
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

    /**
     * take all methods from token and add to result implementation of them
     *
     * @param token implementring class
     */
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

    /**
     * common method for {@code setMethods} and {@code setConstructors}
     * <p>
     * first set modifiers from executable (except abstract, transient, and native)
     * then set return type and name
     * then set arguments
     * then set body
     *
     * @param executable    - method or constructor
     * @param returnType    - return type
     * @param body          - text with neediest body
     * @param isConstructor true if executable is constructor, false otherwise
     */
    private void setExecutable(Executable executable, String returnType, StringBuilder body, boolean isConstructor) {
        int modifier = executable.getModifiers();
        //убили и не пишем abstract, transient, native
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

    /**
     * add package of {@code token} to result
     */
    private void setPackage() {
        String packageName = token.getPackageName();
        if (!packageName.isEmpty()) {
            result.append("package ").append(packageName).append(';').append(lineSeparator).append(lineSeparator);
        }
    }

    /**
     * add name of {@code token} to result
     */
    private void setName() {
        result.append("public class ")
                .append(token.getSimpleName()).append("Impl ")
                .append((token.isInterface()) ? "implements " : "extends ")
                .append(token.getSimpleName()).append(" {").append(lineSeparator);
    }


    /**
     * add throwable Exceptions of {@code executable} to result
     *
     * @param executable method or constructor which can throw exception
     */
    private void setExceptions(Executable executable) {
        Class<?>[] exceptions = executable.getExceptionTypes();
        if (exceptions.length > 0) {
            result.append("throws ");
            for (Class<?> exception : exceptions) {
                result.append(exception.getCanonicalName()).append(" ");
            }
        }
    }

    /**
     * generate string with new line and {@code countTabs} of tabs
     *
     * @param countTabs neediest count tabs for result
     * @return string consisting of new line and {@code countTabs} of tabs
     */
    private StringBuilder separatorWithTabs(int countTabs) {
        StringBuilder result = new StringBuilder(lineSeparator);
        for (int i = 0; i < countTabs; i++) {
            result.append(tab);
        }
        return result;
    }

    /**
     * class-Wrapper for good hash of methods
     */
    class MethodWithHash {
        /**
         * Wrapped method
         */
        private Method realMethod;
        /**
         * good hash for method
         */
        private String hash;

        /**
         * set realMethod to {@code method} and calc hash - hash from string of method
         *
         * @param method set realMethod
         */
        MethodWithHash(Method method) {
            realMethod = method;
            hash = realMethod.getName() + Arrays.toString(realMethod.getParameters());
        }

        /**
         * check equals of {@code MethodWithHash}
         *
         * @param other other {@code MethodWithHash}
         * @return true if {@code other instance of MethodWithHash} and {@code this} and {@code other} has equal hash
         */
        public boolean equals(Object other) {
            if (other instanceof MethodWithHash) {
                return hash.equals(((MethodWithHash) other).hash);
            }
            return false;
        }

        /**
         * return good hash of method
         *
         * @return generated with constructor hash
         */
        public int hashCode() {
            return hash.hashCode();
        }
    }
}