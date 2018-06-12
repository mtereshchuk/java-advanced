package ru.ifmo.rain.tereshchuk.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Provides implementation of interfaces {@link Impler} and {@link JarImpler}.
 *
 * @author Maxim Tereshchuk
 */
public class Implementor implements Impler, JarImpler {

    /**
     * Class to print implementation for.
     */
    private Class<?> token;

    /**
     * Prints implementation of class to file.
     */
    private PrintWriter printWriter;

    /**
     * New class name with "Impl".
     */
    private String className;

    /**
     * Produces code implementing class or interface specified by provided <tt>token</tt>.
     * <p>
     * Generated class full name should be same as full name of the type token with <tt>Impl</tt> suffix
     * added. Generated source code should be placed in the correct subdirectory of the specified
     * <tt>root</tt> directory and have correct file name. For example, the implementation of the
     * interface {@link java.util.List} should go to <tt>$root/java/util/ListImpl.java</tt>
     *
     *
     * @param token type token to create implementation for.
     * @param root root directory.
     * @throws ImplerException {@link info.kgeorgiy.java.advanced.implementor.ImplerException}
     * when implementation can't be generated.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Incorrect arguments");
        }
        if (!token.isInterface()) {
            throw new ImplerException("Token must be interface");
        }
        this.token = token;
        this.className = token.getSimpleName() + "Impl";
        try (PrintWriter printWriter =
                     new PrintWriter(
                             new OutputStreamWriter(
                                     new FileOutputStream(getFilePath(root)),
                                     StandardCharsets.UTF_8))) {
            this.printWriter = printWriter;
            printFile();
        } catch (IOException e) {
            throw new ImplerException(e);
        }
    }

    /**
     * Generates {@link java.lang.String} of path to {@link this.token} relative from root.
     *
     * @param root directory is treated as package's root.
     * @return string of path to class, starting with path to directory, where package is located.
     * @throws IOException if creation of directories failed.
     */
    private String getFilePath(Path root) throws IOException {
        root = root.resolve(token.getPackage().getName().replace('.', '/'));
        Files.createDirectories(root);
        return root.resolve(className + ".java").toString();
    }

    /**
     * Prints implementation of {@link this.token} to file.
     *
     * @throws ImplerException if not able to implement given class.
     */
    private void printFile() throws ImplerException {
        printPackage();
        printClassDeclaration();
        printMethods();
        printWriter.print("}");
    }

    /**
     * Prints package of {@link this.token}.
     */
    private void printPackage() {
        printWriter.print("package " + token.getPackage().getName() + ";\n\n");
    }

    /**
     * Prints declaration of {@link this.token}.
     */
    private void printClassDeclaration() {
        printWriter.print("public class " + className + " "
                + "implements " + token.getSimpleName() + " {\n\n");
    }

    /**
     * Prints all methods of {@link this.token}.
     * Prints declaration, parameters and exceptions for each method.
     *
     * @see Method
     */
    private void printMethods() {
        for (Method method : token.getMethods()) {
            printWriter.print("public "
                    + method.getReturnType().getCanonicalName() + " "
                    + method.getName()
                    + "(");

            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                printWriter.print((i == 0 ? "" : ", ")
                        + parameters[i].getType().getCanonicalName()
                        + " " + parameters[i].getName());
            }
            printWriter.print(")");

            Class<?> exceptions[] = method.getExceptionTypes();
            for (int i = 0; i < exceptions.length; i++) {
                printWriter.print((i == 0 ? " throws " : ", ") + exceptions[i].getName());
            }

            printWriter.print(" {\n" + "return"
                    + (method.getReturnType().equals(void.class) ? "" : " ")
                    + getDefaultValue(method.getReturnType())
                    + ";\n" + "}\n\n");
        }
    }

    /**
     * Generates string that representing default value of class with given type.
     * Empty string for {@link Void void},
     * true for {@link Boolean boolean},
     * 0 for other primitive types,
     * null for non-primitive types.
     *
     * @param type type to get default value for.
     * @return {@link java.lang.String} with default value.
     */
    private String getDefaultValue(Class<?> type) {
        if (type.equals(void.class)) {
            return "";
        } else if (type.equals(boolean.class)) {
            return "true";
        } else if (type.isPrimitive()) {
            return "0";
        }
        return "null";
    }

    /**
     * Produces <tt>.jar</tt> file implementing class or interface specified by provided <tt>token</tt>.
     * <p>
     * Generated class full name should be same as full name of the type token with <tt>Impl</tt> suffix
     * added.
     *
     * @param token type token to create implementation for.
     * @param jarFile target <tt>.jar</tt> file.
     * @throws ImplerException {@link info.kgeorgiy.java.advanced.implementor.ImplerException}
     * when implementation cannot be generated.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        String className = token.getName().replace('.', '/') + "Impl";
        String classPath = jarFile.getName(0) + "/" + className;
        compileClass(classPath + ".java");
        writeJarFile(className + ".class", classPath + ".class", jarFile);
    }

    /**
     * Compiles given file.
     * Compiles the file by using default java compiler, that provided by {@link ToolProvider#getSystemJavaCompiler()}.
     *
     * @param fileName string of path to a file to compile.
     * @throws ImplerException {@link info.kgeorgiy.java.advanced.implementor.ImplerException}
     * if default compiler is unavailable or compilation error occurred
     * (compiler returned non-zero exit code).
     */
    private void compileClass(String fileName) throws ImplerException {
        int returnNumber = ToolProvider.getSystemJavaCompiler().run(null, null, null, fileName);
        if (returnNumber != 0) {
            throw new ImplerException("Exception when compiling");
        }
    }

    /**
     * Creates jar archive at jarPath and copies className file to it.
     *
     * @param jarPath path for jar archive.
     * @param className name of class to be copied into archive.
     * @throws ImplerException {@link info.kgeorgiy.java.advanced.implementor.ImplerException}
     * if I/O error occurred.
     */
    private void writeJarFile(String className, String classPath, Path jarPath) throws ImplerException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream output = new JarOutputStream(new FileOutputStream(jarPath.toFile()), manifest)) {
            output.putNextEntry(new JarEntry(className));
            Files.copy(Paths.get(classPath), output);
        } catch (IOException e) {
            throw new ImplerException("Exception when creating jar file", e);
        }
    }

    /**
     * Entry point of program.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        Implementor implementor = new Implementor();
        try {
            if (args[0].equals("-jar")) {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            } else {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            }
        } catch (ClassNotFoundException | ImplerException e) {
            System.err.println(e.getMessage());
        }
    }
}
