package io.docgen.core.analyzer;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Opens a JAR file and loads all classes from it into a child URLClassLoader.
 * The parent ClassLoader provides Spring / Jakarta / Jackson types so that
 * annotation metadata can be inspected via reflection.
 */
public class JarClassLoader implements AutoCloseable {

    private final URLClassLoader classLoader;
    private final JarFile jarFile;

    public JarClassLoader(File jar) throws Exception {
        this.jarFile = new JarFile(jar);
        // Use thread context classloader as parent so that annotation classes
        // from the CLI fat JAR are visible when resolving annotations via reflection.
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        if (parent == null) parent = JarClassLoader.class.getClassLoader();
        this.classLoader = new URLClassLoader(
                new URL[]{jar.toURI().toURL()},
                parent
        );
    }

    /**
     * Returns all classes found in the JAR (excluding inner anonymous classes that
     * cannot be loaded by simple name mapping).
     */
    public List<Class<?>> loadAllClasses() {
        List<Class<?>> classes = new ArrayList<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.endsWith(".class") && !name.contains("$")) {
                String className = name.replace('/', '.').replace(".class", "");
                try {
                    Class<?> cls = classLoader.loadClass(className);
                    classes.add(cls);
                } catch (Throwable t) {
                    // Skip unloadable classes (e.g. missing transitive deps)
                    System.err.println("[WARN] Could not load class: " + className + " — " + t.getMessage());
                }
            }
        }
        return classes;
    }

    public URLClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public void close() throws Exception {
        classLoader.close();
        jarFile.close();
    }
}

