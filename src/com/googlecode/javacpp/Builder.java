/*
 * Copyright (C) 2011,2012,2013 Samuel Audet
 *
 * This file is part of JavaCPP.
 *
 * JavaCPP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * JavaCPP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaCPP.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.googlecode.javacpp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * The Builder is responsible for coordinating efforts between the Generator
 * and the native compiler. It contains the main() method, and basically takes
 * care of the tasks one would expect from a command line build tool, but can
 * also be used programmatically by setting its properties and calling build().
 *
 * @author Samuel Audet
 */
public class Builder {

    /**
     * Tries to find automatically include paths for {@code jni.h} and {@code jni_md.h},
     * as well as the link and library paths for the {@code jvm} library.
     *
     * @param properties the Properties containing the paths to update
     */
    public static void includeJavaPaths(Properties properties) {
        String platformName  = Loader.getPlatformName();
        String pathSeparator = properties.getProperty("path.separator");
        final String jvmlink = properties.getProperty("compiler.link.prefix", "") +
                       "jvm" + properties.getProperty("compiler.link.suffix", "");
        final String jvmlib  = properties.getProperty("library.prefix", "") +
                       "jvm" + properties.getProperty("library.suffix", "");
        final String[] jnipath = new String[2];
        final String[] jvmpath = new String[2];
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (new File(dir, "jni.h").exists()) {
                    jnipath[0] = dir.getAbsolutePath();
                }
                if (new File(dir, "jni_md.h").exists()) {
                    jnipath[1] = dir.getAbsolutePath();
                }
                if (new File(dir, jvmlink).exists()) {
                    jvmpath[0] = dir.getAbsolutePath();
                }
                if (new File(dir, jvmlib).exists()) {
                    jvmpath[1] = dir.getAbsolutePath();
                }
                return new File(dir, name).isDirectory();
            }
        };
        File javaHome = new File(System.getProperty("java.home")).getParentFile();
        try {
            javaHome = javaHome.getCanonicalFile();
        } catch (IOException e) { }
        LinkedList<File> dirs = new LinkedList<File>(Arrays.asList(javaHome.listFiles(filter)));
        while (!dirs.isEmpty()) {
            File d = dirs.pop();
            String dpath = d.getPath();
            for (File f : d.listFiles(filter)) {
                try {
                    f = f.getCanonicalFile();
                } catch (IOException e) { }
                if (!dpath.startsWith(f.getPath())) {
                    dirs.add(f);
                }
            }
        }
        if (jnipath[0] != null && jnipath[0].equals(jnipath[1])) {
            jnipath[1] = null;
        } else if (jnipath[0] == null) {
            String macpath = "/System/Library/Frameworks/JavaVM.framework/Headers/";
            if (new File(macpath).isDirectory()) {
                jnipath[0] = macpath;
            }
        }
        if (jvmpath[0] != null && jvmpath[0].equals(jvmpath[1])) {
            jvmpath[1] = null;
        }
        Loader.appendProperty(properties, "compiler.includepath", pathSeparator, jnipath);
        if (platformName.equals(properties.getProperty("platform.name", platformName))) {
            Loader.appendProperty(properties, "compiler.link", pathSeparator, "jvm");
            Loader.appendProperty(properties, "compiler.linkpath", pathSeparator, jvmpath);
            if (platformName.startsWith("macosx")) {
                Loader.appendProperty(properties, "compiler.framework", pathSeparator, "JavaVM");
            }
        }
    }

    /**
     * A simple {@link Thread} that reads data as fast as possible from an {@link InputStream} and
     * writes to the {@link OutputStream}. Used by {@link #compile(String, String, Properties)}
     * to flush the streams of a {@link Process}.
     */
    public static class Piper extends Thread {
        public Piper(InputStream is, OutputStream os) {
            this.is = is;
            this.os = os;
        }

        InputStream is;
        OutputStream os;

        @Override public void run() {
            try {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) != -1) {
                    os.write(buffer, 0, length);
                }
            } catch (IOException e) {
                System.err.println("Could not pipe from the InputStream to the OutputStream: " + e.getMessage());
            }
        }
    }

    /**
     * Launches and waits for the native compiler to produce a native shared library.
     *
     * @param sourceFilename the C++ source filename
     * @param outputFilename the output filename of the shared library
     * @param properties the Properties detailing the compiler options to use
     * @return the result of {@link Process#waitFor()}
     * @throws IOException
     * @throws InterruptedException
     */
    public int compile(String sourceFilename, String outputFilename, Properties properties)
            throws IOException, InterruptedException {
        LinkedList<String> command = new LinkedList<String>();

        includeJavaPaths(properties);

        String platformName  = Loader.getPlatformName();
        String pathSeparator = properties.getProperty("path.separator");
        String platformRoot  = properties.getProperty("platform.root");
        if (platformRoot == null || platformRoot.length() == 0) {
            platformRoot = ".";
        }
        if (!platformRoot.endsWith(File.separator)) {
            platformRoot += File.separator;
        }

        String compilerPath = properties.getProperty("compiler.path");
        if (platformRoot != null && !new File(compilerPath).isAbsolute() &&
                new File(platformRoot + compilerPath).exists()) {
            compilerPath = platformRoot + compilerPath;
        }
        command.add(compilerPath);

        String sysroot = properties.getProperty("compiler.sysroot");
        if (sysroot != null && sysroot.length() > 0) {
            String p = properties.getProperty("compiler.sysroot.prefix", "");
            for (String s : sysroot.split(pathSeparator)) {
                if (platformRoot != null && !new File(s).isAbsolute()) {
                    s = platformRoot + s;
                }
                if (new File(s).isDirectory()) {
                    if (p.endsWith(" ")) {
                        command.add(p.trim()); command.add(s);
                    } else {
                        command.add(p + s);
                    }
                }
            }
        }

        String includepath = properties.getProperty("compiler.includepath");
        if (includepath != null && includepath.length() > 0) {
            String p = properties.getProperty("compiler.includepath.prefix", "");
            for (String s : includepath.split(pathSeparator)) {
                if (platformRoot != null && !new File(s).isAbsolute()) {
                    s = platformRoot + s;
                }
                if (new File(s).isDirectory()) {
                    if (p.endsWith(" ")) {
                        command.add(p.trim()); command.add(s);
                    } else {
                        command.add(p + s);
                    }
                }
            }
        }

        command.add(sourceFilename);

        String options = properties.getProperty("compiler.options");
        if (options != null && options.length() > 0) {
            command.addAll(Arrays.asList(options.split(" ")));
        }

        command.addAll(compilerOptions);

        String outputPrefix = properties.getProperty("compiler.output.prefix");
        if (outputPrefix != null && outputPrefix.length() > 0) {
            command.addAll(Arrays.asList(outputPrefix.split(" ")));
        }

        if (outputPrefix == null || outputPrefix.length() == 0 || outputPrefix.endsWith(" ")) {
            command.add(outputFilename);
        } else {
            command.add(command.removeLast() + outputFilename);
        }

        String linkpath = properties.getProperty("compiler.linkpath");
        if (linkpath != null && linkpath.length() > 0) {
            String p  = properties.getProperty("compiler.linkpath.prefix", "");
            String p2 = properties.getProperty("compiler.linkpath.prefix2");
            for (String s : linkpath.split(pathSeparator)) {
                if (platformRoot != null && !new File(s).isAbsolute()) {
                    s = platformRoot + s;
                }
                if (new File(s).isDirectory()) {
                    if (p.endsWith(" ")) {
                        command.add(p.trim()); command.add(s);
                    } else {
                        command.add(p + s);
                    }
                    if (p2 != null) {
                        if (p2.endsWith(" ")) {
                            command.add(p2.trim()); command.add(s);
                        } else {
                            command.add(p2 + s);
                        }
                    }
                }
            }
        }

        String link = properties.getProperty("compiler.link");
        if (link != null && link.length() > 0) {
            String p = properties.getProperty("compiler.link.prefix", "");
            String x = properties.getProperty("compiler.link.suffix", "");
            for (String s : link.split(pathSeparator)) {
                String[] libnameversion = s.split("@");
                if (libnameversion.length == 3 && libnameversion[1].length() == 0) {
                    // Only use the version number when the user gave us a double @
                    s = libnameversion[0] + libnameversion[2];
                } else {
                    s = libnameversion[0];
                }
                if (p.endsWith(" ") && x.startsWith(" ")) {
                    command.add(p.trim()); command.add(s); command.add(x.trim());
                } else if (p.endsWith(" ")) {
                    command.add(p.trim()); command.add(s + x);
                } else if (x.startsWith(" ")) {
                    command.add(p + s); command.add(x.trim());
                } else {
                    command.add(p + s + x);
                }
            }
        }

        String framework = properties.getProperty("compiler.framework");
        if (framework != null && framework.length() > 0) {
            String p = properties.getProperty("compiler.framework.prefix", "");
            String x = properties.getProperty("compiler.framework.suffix", "");
            for (String s : framework.split(pathSeparator)) {
                if (p.endsWith(" ") && x.startsWith(" ")) {
                    command.add(p.trim()); command.add(s); command.add(x.trim());
                } else if (p.endsWith(" ")) {
                    command.add(p.trim()); command.add(s + x);
                } else if (x.startsWith(" ")) {
                    command.add(p + s); command.add(x.trim());
                } else {
                    command.add(p + s + x);
                }
            }
        }

        boolean windows = platformName.startsWith("windows");
        for (String s : command) {
            boolean hasSpaces = s.indexOf(" ") > 0;
            if (hasSpaces) {
                System.out.print(windows ? "\"" : "'");
            }
            System.out.print(s);
            if (hasSpaces) {
                System.out.print(windows ? "\"" : "'");
            }
            System.out.print(" ");
        }
        System.out.println();

        ProcessBuilder pb = new ProcessBuilder(command);
        if (environmentVariables != null) {
            pb.environment().putAll(environmentVariables);
        }
        Process p = pb.start();
        new Piper(p.getErrorStream(), System.err).start();
        new Piper(p.getInputStream(), System.out).start();
        return p.waitFor();
    }

    /**
     * Generates a C++ source file for classes, and compiles everything in
     * one shared library when {@code compile == true}.
     *
     * @param classes the Class objects as input to Generator
     * @param outputName the output name of the shared library
     * @return the actual File generated, either the compiled library or its source
     * @throws IOException
     * @throws InterruptedException
     */
    public File generateAndCompile(Class[] classes, String outputName) throws IOException, InterruptedException {
        File outputFile = null;
        Properties p = (Properties)properties.clone();
        for (Class c : classes) {
            Loader.appendProperties(p, c);
        }
        String platformName = p.getProperty("platform.name"), sourcePrefix;
        String sourceSuffix = p.getProperty("source.suffix", ".cpp");
        String libraryName  = p.getProperty("library.prefix", "") + outputName + p.getProperty("library.suffix", "");
        File outputPath;
        if (outputDirectory == null) {
            try {
                URL resourceURL = classes[0].getResource('/' + classes[0].getName().replace('.', '/') + ".class");
                File packageDir = new File(resourceURL.toURI()).getParentFile();
                outputPath      = new File(packageDir, platformName);
                sourcePrefix    = packageDir.getPath() + File.separator + outputName;
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } else {
            outputPath = outputDirectory;
            sourcePrefix = outputPath.getPath() + File.separator + outputName;
        }
        if (!outputPath.exists()) {
            outputPath.mkdirs();
        }
        Generator generator = new Generator(p);
        String sourceFilename = sourcePrefix + sourceSuffix;
        String headerFilename = header ? sourcePrefix + ".h" : null;
        String classPath = System.getProperty("java.class.path");
        for (String s : classLoader.getPaths()) {
            classPath += File.pathSeparator + s;
        }
        System.out.println("Generating source file: " + sourceFilename);
        if (generator.generate(sourceFilename, headerFilename, classPath, classes)) {
            generator.close();
            if (compile) {
                String libraryFilename = outputPath.getPath() + File.separator + libraryName;
                System.out.println("Building library file: " + libraryFilename);
                int exitValue = compile(sourceFilename, libraryFilename, p);
                if (exitValue == 0) {
                    new File(sourceFilename).delete();
                    outputFile = new File(libraryFilename);
                } else {
                    System.exit(exitValue);
                }
            } else {
                outputFile = new File(sourceFilename);
            }
        } else {
            System.out.println("Source file not generated: " + sourceFilename);
        }
        return outputFile;
    }

    /**
     * Stores all the files in the given JAR file. Also attempts to root the paths
     * of the filenames to each element of a list of classpaths.
     *
     * @param jarFile the JAR file to create
     * @param classpath an array of classpaths to try to use as root
     * @param files a list of files to store in the JAR file
     * @throws IOException
     */
    public static void createJar(File jarFile, String[] classpath, LinkedList<File> files) throws IOException {
        System.out.println("Creating JAR file: " + jarFile);
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile));
        for (File f : files) {
            String name = f.getPath();
            if (classpath != null) {
                // Store only the path relative to the classpath so that
                // our Loader may use the package name of the associated
                // class to get the file as a resource from the ClassLoader.
                String[] names = new String[classpath.length];
                for (int i = 0; i < classpath.length; i++) {
                    String path = new File(classpath[i]).getCanonicalPath();
                    if (name.startsWith(path)) {
                        names[i] = name.substring(path.length() + 1);
                    }
                }
                // Retain only the shortest relative name.
                for (int i = 0; i < names.length; i++) {
                    if (names[i] != null && names[i].length() < name.length()) {
                        name = names[i];
                    }
                }
            }
            ZipEntry e = new ZipEntry(name.replace(File.separatorChar, '/'));
            e.setTime(f.lastModified());
            jos.putNextEntry(e);
            FileInputStream fis = new FileInputStream(f);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) != -1) {
                jos.write(buffer, 0, length);
            }
            fis.close();
            jos.closeEntry();
//            f.delete();
//            f.getParentFile().delete();
        }
        jos.close();
    }

    /**
     * An extension of {@link URLClassLoader} that keeps a list of paths in memory.
     * Adds {@code System.getProperty("user.dir")} as default path if none are added.
     */
    public static class UserClassLoader extends URLClassLoader {
        private LinkedList<String> paths = new LinkedList<String>();
        public UserClassLoader() {
            super(new URL[0]);
        }
        public UserClassLoader(ClassLoader parent) {
            super(new URL[0], parent);
        }
        public void addPaths(String ... paths) {
            if (paths == null) {
                return;
            }
            for (String path : paths) {
                this.paths.add(path);
                try {
                    addURL(new File(path).toURI().toURL());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        public String[] getPaths() {
            if (paths.isEmpty()) {
                addPaths(System.getProperty("user.dir"));
            }
            return paths.toArray(new String[paths.size()]);
        }
        @Override protected Class<?> findClass(String name)
                throws ClassNotFoundException {
            if (paths.isEmpty()) {
                addPaths(System.getProperty("user.dir"));
            }
            return super.findClass(name);
        }
    }

    /**
     * Given a {@link UserClassLoader}, attempts to match and fill in a {@link Collection}
     * of {@link Class}, in various ways in which users may wish to do so.
     */
    public static class ClassScanner {
        public ClassScanner(Collection<Class> classes, UserClassLoader loader) {
            this.classes = classes;
            this.loader  = loader;
        }

        private Collection<Class> classes;
        private UserClassLoader loader;

        public void addClass(String className) {
            if (className == null) {
                return;
            } else if (className.endsWith(".class")) {
                className = className.substring(0, className.length()-6);
            }
            try {
                Class c = Class.forName(className, false, loader);
                if (!classes.contains(c)) {
                    classes.add(c);
                }
            } catch (ClassNotFoundException e) {
                System.err.println("Warning: Could not find class " + className + ": " + e);
            } catch (NoClassDefFoundError e) {
                System.err.println("Warning: Could not load class " + className + ": " + e);
            }
        }

        public void addMatchingFile(String filename, String packagePath, boolean recursive) {
            if (filename != null && filename.endsWith(".class") &&
                    (packagePath == null || (recursive && filename.startsWith(packagePath)) ||
                    filename.regionMatches(0, packagePath, 0, Math.max(filename.lastIndexOf('/'), packagePath.lastIndexOf('/'))))) {
                addClass(filename.replace('/', '.'));
            }
        }

        public void addMatchingDir(String parentName, File dir, String packagePath, boolean recursive) {
            File[] files = dir.listFiles();
            Arrays.sort(files);
            for (File f : files) {
                String pathName = parentName == null ? f.getName() : parentName + f.getName();
                if (f.isDirectory()) {
                    addMatchingDir(pathName + "/", f, packagePath, recursive);
                } else {
                    addMatchingFile(pathName, packagePath, recursive);
                }
            }
        }

        public void addPackage(String packageName, boolean recursive) throws IOException {
            String[] paths = loader.getPaths();
            final String packagePath = packageName == null ? null : (packageName.replace('.', '/') + "/");
            int prevSize = classes.size();
            for (String p : paths) {
                File file = new File(p);
                if (file.isDirectory()) {
                    addMatchingDir(null, file, packagePath, recursive);
                } else {
                    JarInputStream jis = new JarInputStream(new FileInputStream(file));
                    ZipEntry e = jis.getNextEntry();
                    while (e != null) {
                        addMatchingFile(e.getName(), packagePath, recursive);
                        jis.closeEntry();
                        e = jis.getNextEntry();
                    }
                    jis.close();
                }
            }
            if (classes.size() == 0 && packageName == null) {
                System.err.println("Warning: No classes found in the unnamed package");
                printHelp();
            } else if (prevSize == classes.size() && packageName != null) {
                System.err.println("Warning: No classes found in package " + packageName);
            }
        }

        public void addClassOrPackage(String name) throws IOException {
            if (name == null) {
                return;
            }
            name = name.replace('/', '.');
            if (name.endsWith(".**")) {
                addPackage(name.substring(0, name.length()-3), true);
            } else if (name.endsWith(".*")) {
                addPackage(name.substring(0, name.length()-2), false);
            } else {
                addClass(name);
            }
        }
    }

    /**
     * Default constructor that simply initializes everything.
     */
    public Builder() {
        Loader.loadLibraries = false;
        this.classLoader = new UserClassLoader(Thread.currentThread().getContextClassLoader());
        this.properties = Loader.getProperties();
        this.classes = new LinkedList<Class>();
        this.classScanner = new ClassScanner(classes, classLoader);
        this.compilerOptions = new LinkedList<String>();
    }

    /** The {@link ClassLoader} used by the {@link ClassScanner}. */
    UserClassLoader classLoader = null;
    /** The directory where the generated files and compiled shared libraries get written to.
     *  By default they are placed in the same directory as the {@code .class} file. */
    File outputDirectory = null;
    /** The name of the output generated source file or shared library. This enables single-
     *  file output mode. By default, the top-level enclosing classes get one file each. */
    String outputName = null;
    /** The name of the JAR file to create, if not {@code null}. */
    String jarPrefix = null;
    /** If true, compiles the generated source file to a shared library and deletes source. */
    boolean compile = true;
    /** If true, also generates C++ header files containing declarations of callback functions. */
    boolean header = false;
    /** Accumulates the various properties loaded from resources, files, command line options, etc. */
    Properties properties = null;
    /** The {@link LinkedList} that the {@link ClassScanner} fills up with {@link Class} objects to process. */
    LinkedList<Class> classes = null;
    /** The instance of the {@link ClassScanner}. */
    ClassScanner classScanner = null;
    /** User specified environment variables to pass to the native compiler. */
    Map<String,String> environmentVariables = null;
    /** Contains additional command line options from the user for the native compiler. */
    LinkedList<String> compilerOptions = null;

    /** Splits argument with {@link File#pathSeparator} and appends result to paths of the {@link #classLoader}. */
    public Builder classPaths(String classPaths) {
        classPaths(classPaths == null ? null : classPaths.split(File.pathSeparator));
        return this;
    }
    /** Appends argument to the paths of the {@link #classLoader}. */
    public Builder classPaths(String ... classPaths) {
        classLoader.addPaths(classPaths);
        return this;
    }
    /** Sets the {@link #outputDirectory} field to the argument. */
    public Builder outputDirectory(String outputDirectory) {
        outputDirectory(outputDirectory == null ? null : new File(outputDirectory));
        return this;
    }
    /** Sets the {@link #outputDirectory} field to the argument. */
    public Builder outputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }
    /** Sets the {@link #compile} field to the argument. */
    public Builder compile(boolean compile) {
        this.compile = compile;
        return this;
    }
    /** Sets the {@link #header} field to the argument. */
    public Builder header(boolean header) {
        this.header = header;
        return this;
    }
    /** Sets the {@link #outputName} field to the argument. */
    public Builder outputName(String outputName) {
        this.outputName = outputName;
        return this;
    }
    /** Sets the {@link #jarPrefix} field to the argument. */
    public Builder jarPrefix(String jarPrefix) {
        this.jarPrefix = jarPrefix;
        return this;
    }
    /** Adds to the {@link #properties} field the ones loaded from resources for the specified platform. */
    public Builder properties(String platformName) {
        properties(platformName == null ? null : Loader.getProperties(platformName));
        return this;
    }
    /** Adds all the properties of the argument to the {@link #properties} field. */
    public Builder properties(Properties properties) {
        if (properties != null) {
            this.properties.putAll(properties);
        }
        return this;
    }
    /** Adds to the {@link #properties} field the ones loaded from the specified file. */
    public Builder propertyFile(String filename) throws IOException {
        propertyFile(filename == null ? null : new File(filename));
        return this;
    }
    /** Adds to the {@link #properties} field the ones loaded from the specified file. */
    public Builder propertyFile(File propertyFile) throws IOException {
        if (propertyFile == null) {
            return this;
        }
        FileInputStream fis = new FileInputStream(propertyFile);
        properties = new Properties(properties);
        try {
            properties.load(new InputStreamReader(fis));
        } catch (NoSuchMethodError e) {
            properties.load(fis);
        }
        fis.close();
        return this;
    }
    /** Sets a property of the {@link #properties} field, in either "key=value" or "key:value" format. */
    public Builder property(String keyValue) {
        int equalIndex = keyValue.indexOf('=');
        if (equalIndex < 0) {
            equalIndex = keyValue.indexOf(':');
        }
        property(keyValue.substring(2, equalIndex),
                 keyValue.substring(equalIndex+1));
        return this;
    }
    /** Sets a key/value pair property of the {@link #properties} field. */
    public Builder property(String key, String value) {
        if (key.length() > 0 && value.length() > 0) {
            properties.put(key, value);
        }
        return this;
    }
    /** Requests the {@link #classScanner} to add a class or all classes from a package.
     *  A {@code null} argument indicates the unnamed package. */
    public Builder classesOrPackages(String ... classesOrPackages) throws IOException {
        if (classesOrPackages == null) {
            classScanner.addPackage(null, true);
        } else for (String s : classesOrPackages) {
            classScanner.addClassOrPackage(s);
        }
        return this;
    }
    /** Sets the {@link #environmentVariables} field to the argument. */
    public Builder environmentVariables(Map<String,String> environmentVariables) {
        this.environmentVariables = environmentVariables;
        return this;
    }
    /** Appends arguments to the {@link #compilerOptions} field. */
    public Builder compilerOptions(String ... options) {
        if (options != null) {
            compilerOptions.addAll(Arrays.asList(options));
        }
        return this;
    }

    /**
     * Starts the build process and returns a {@link Collection} of {@link File} produced.
     *
     * @return the Collection of File produced
     * @throws IOException
     * @throws InterruptedException
     */
    public Collection<File> build() throws IOException, InterruptedException {
        if (classes.isEmpty()) {
            return null;
        }

        LinkedList<File> outputFiles;
        if (outputName == null) {
            outputFiles = new LinkedList<File>();
            Map<String, LinkedList<Class>> map = new LinkedHashMap<String, LinkedList<Class>>();
            for (Class c : classes) {
                Properties p = (Properties)properties.clone();
                if (Loader.appendProperties(p, c) != c) {
                    continue;
                }
                String libraryName = p.getProperty("loader.library", "");
                if (libraryName.length() == 0) {
                    continue;
                }
                LinkedList<Class> classList = map.get(libraryName);
                if (classList == null) {
                    map.put(libraryName, classList = new LinkedList<Class>());
                }
                classList.add(c);
            }
            for (String libraryName : map.keySet()) {
                LinkedList<Class> classList = map.get(libraryName);
                File f = generateAndCompile(classList.toArray(new Class[classList.size()]), libraryName);
                if (f != null) {
                    outputFiles.add(f);
                }
            }
        } else {
            outputFiles = new LinkedList<File>();
            File f = generateAndCompile(classes.toArray(new Class[classes.size()]), outputName);
            if (f != null) {
                outputFiles.add(f);
            }
        }

        if (jarPrefix != null && !outputFiles.isEmpty()) {
            File jarFile = new File(jarPrefix + "-" + properties.get("platform.name") + ".jar");
            File d = jarFile.getParentFile();
            if (d != null && !d.exists()) {
                d.mkdir();
            }
            createJar(jarFile, outputDirectory == null ? classLoader.getPaths() : null, outputFiles);
        }
        return outputFiles;
    }

    /**
     * Simply prints out to the display the command line usage.
     */
    public static void printHelp() {
        String version = Builder.class.getPackage().getImplementationVersion();
        if (version == null) {
            version = "unknown";
        }
        System.out.println(
            "JavaCPP version " + version + "\n" +
            "Copyright (C) 2011-2013 Samuel Audet <samuel.audet@gmail.com>\n" +
            "Project site: http://code.google.com/p/javacpp/\n\n" +

            "Licensed under the GNU General Public License version 2 (GPLv2) with Classpath exception.\n" +
            "Please refer to LICENSE.txt or http://www.gnu.org/licenses/ for details.");
        System.out.println();
        System.out.println("Usage: java -jar javacpp.jar [options] [class or package (suffixed with .* or .**)]");
        System.out.println();
        System.out.println("where options include:");
        System.out.println();
        System.out.println("    -classpath <path>      Load user classes from path");
        System.out.println("    -d <directory>         Output all generated files to directory");
        System.out.println("    -o <name>              Output everything in a file named after given name");
        System.out.println("    -nocompile             Do not compile or delete the generated source files");
        System.out.println("    -header                Generate header file with declarations of callbacks functions");
        System.out.println("    -jarprefix <prefix>    Also create a JAR file named \"<prefix>-<platform.name>.jar\"");
        System.out.println("    -properties <resource> Load all properties from resource");
        System.out.println("    -propertyfile <file>   Load all properties from file");
        System.out.println("    -D<property>=<value>   Set property to value");
        System.out.println("    -Xcompiler <option>    Pass option directly to compiler");
        System.out.println();
    }

    /**
     * The terminal shell interface to the Builder.
     *
     * @param args an array of arguments as described by {@link #printHelp()}
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        boolean addedClasses = false;
        Builder builder = new Builder();
        for (int i = 0; i < args.length; i++) {
            if ("-help".equals(args[i]) || "--help".equals(args[i])) {
                printHelp();
                System.exit(0);
            } else if ("-classpath".equals(args[i]) || "-cp".equals(args[i]) || "-lib".equals(args[i])) {
                builder.classPaths(args[++i]);
            } else if ("-d".equals(args[i])) {
                builder.outputDirectory(args[++i]);
            } else if ("-o".equals(args[i])) {
                builder.outputName(args[++i]);
            } else if ("-cpp".equals(args[i]) || "-nocompile".equals(args[i])) {
                builder.compile(false);
            } else if ("-header".equals(args[i])) {
                builder.header(true);
            } else if ("-jarprefix".equals(args[i])) {
                builder.jarPrefix(args[++i]);
            } else if ("-properties".equals(args[i])) {
                builder.properties(args[++i]);
            } else if ("-propertyfile".equals(args[i])) {
                builder.propertyFile(args[++i]);
            } else if (args[i].startsWith("-D")) {
                builder.property(args[i]);
            } else if ("-Xcompiler".equals(args[i])) {
                builder.compilerOptions(args[++i]);
            } else if (args[i].startsWith("-")) {
                System.err.println("Error: Invalid option \"" + args[i] + "\"");
                printHelp();
                System.exit(1);
            } else {
                builder.classesOrPackages(args[i]);
                addedClasses = true;
            }
        }
        if (!addedClasses) {
            builder.classesOrPackages(null);
        }
        builder.build();
    }
}
