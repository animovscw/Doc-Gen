package io.docgen.core.analyzer;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves JavaDoc comments from source files in JAR.
 * Extracts method descriptions and parameter documentation.
 */
public class JavadocResolver {

    private static final Pattern JAVADOC_PATTERN = Pattern.compile(
            "/\\*\\*\\s*\\n((?:[\\s*]*.*\\n)*?)\\s*\\*/"
    );

    private static final Pattern PARAM_TAG_PATTERN = Pattern.compile(
            "@param\\s+(\\w+)\\s+(.+?)(?=@|$)"
    );

    private static final Pattern RETURN_TAG_PATTERN = Pattern.compile(
            "@return\\s+(.+?)(?=@|$)"
    );

    private final Map<String, String> sourceCache = new HashMap<>();
    private final File jarFile;

    public JavadocResolver(File jarFile) {
        this.jarFile = jarFile;
    }

    /**
     * Extracts method description from JavaDoc.
     */
    public String resolveMethodDescription(Method method) {
        try {
            String sourceCode = getSourceCode(method.getDeclaringClass());
            if (sourceCode == null) return null;

            String signature = method.getName() + "\\s*\\(";
            Pattern methodPattern = Pattern.compile(
                    JAVADOC_PATTERN.pattern() + "\\s*(?:public|private|protected|static|final|synchronized)*\\s+\\S+\\s+" + signature
            );

            Matcher matcher = methodPattern.matcher(sourceCode);
            if (matcher.find()) {
                String javadoc = matcher.group(1);
                return extractMainDescription(javadoc);
            }
        } catch (Exception e) {
            // Ignore errors, return null
        }
        return null;
    }

    /**
     * Extracts parameter description from JavaDoc.
     */
    public String resolveParameterDescription(Method method, String paramName) {
        try {
            String sourceCode = getSourceCode(method.getDeclaringClass());
            if (sourceCode == null) return null;

            String signature = method.getName() + "\\s*\\(";
            Pattern methodPattern = Pattern.compile(
                    JAVADOC_PATTERN.pattern() + "\\s*(?:public|private|protected|static|final|synchronized)*\\s+\\S+\\s+" + signature
            );

            Matcher matcher = methodPattern.matcher(sourceCode);
            if (matcher.find()) {
                String javadoc = matcher.group(1);
                return extractParameterDescription(javadoc, paramName);
            }
        } catch (Exception e) {
            // Ignore errors
        }
        return null;
    }

    /**
     * Extracts return value description from JavaDoc.
     */
    public String resolveReturnDescription(Method method) {
        try {
            String sourceCode = getSourceCode(method.getDeclaringClass());
            if (sourceCode == null) return null;

            String signature = method.getName() + "\\s*\\(";
            Pattern methodPattern = Pattern.compile(
                    JAVADOC_PATTERN.pattern() + "\\s*(?:public|private|protected|static|final|synchronized)*\\s+\\S+\\s+" + signature
            );

            Matcher matcher = methodPattern.matcher(sourceCode);
            if (matcher.find()) {
                String javadoc = matcher.group(1);
                return extractReturnDescription(javadoc);
            }
        } catch (Exception e) {
        }
        return null;
    }

    private String getSourceCode(Class<?> cls) throws Exception {
        String className = cls.getName();
        if (sourceCache.containsKey(className)) {
            return sourceCache.get(className);
        }

        String sourceFileName = cls.getSimpleName() + ".java";
        String sourceCode = null;

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(sourceFileName) && !entry.isDirectory()) {
                    try (var is = jar.getInputStream(entry)) {
                        sourceCode = new String(is.readAllBytes());
                        break;
                    }
                }
            }
        }

        sourceCache.put(className, sourceCode);
        return sourceCode;
    }

    private String extractMainDescription(String javadoc) {
        String[] lines = javadoc.split("\n");
        StringBuilder description = new StringBuilder();

        for (String line : lines) {
            line = line.replaceAll("^\\s*\\*\\s?", "").trim();
            if (line.startsWith("@")) break;
            if (!line.isEmpty()) {
                if (!description.isEmpty()) description.append(" ");
                description.append(line);
            }
        }

        return !description.isEmpty() ? description.toString() : null;
    }

    private String extractParameterDescription(String javadoc, String paramName) {
        Matcher matcher = PARAM_TAG_PATTERN.matcher(javadoc);
        while (matcher.find()) {
            if (matcher.group(1).equals(paramName)) {
                return matcher.group(2).replaceAll("\\s+", " ").trim();
            }
        }
        return null;
    }

    private String extractReturnDescription(String javadoc) {
        Matcher matcher = RETURN_TAG_PATTERN.matcher(javadoc);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("\\s+", " ").trim();
        }
        return null;
    }

    public void close() {
        sourceCache.clear();
    }
}

