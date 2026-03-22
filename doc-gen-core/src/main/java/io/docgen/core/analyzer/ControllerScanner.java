package io.docgen.core.analyzer;

import io.docgen.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class ControllerScanner {

    private static final Logger logger = LoggerFactory.getLogger(ControllerScanner.class);

    private static final Set<String> CONTROLLER_ANNOTATIONS = Set.of(
            "Controller", "RestController"
    );

    private static final Set<String> HTTP_METHOD_ANNOTATIONS = Set.of(
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping", "RequestMapping"
    );

    private static final Set<String> SKIP_PARAMETER_TYPES = Set.of(
            "HttpServletRequest", "HttpServletResponse", "HttpSession",
            "BindingResult", "Model", "ModelAndView", "Principal",
            "Locale", "TimeZone", "ZoneId", "OutputStream", "Writer"
    );

    private final Map<String, Schema> schemaRegistry;
    private final TypeResolver typeResolver;
    private final JavadocResolver javadocResolver;

    public ControllerScanner(Map<String, Schema> schemaRegistry) {
        this(schemaRegistry, null);
    }

    public ControllerScanner(Map<String, Schema> schemaRegistry, JavadocResolver javadocResolver) {
        this.schemaRegistry = schemaRegistry;
        this.typeResolver = new TypeResolver(schemaRegistry);
        this.javadocResolver = javadocResolver;
    }

    public Map<String, PathItem> scan(List<Class<?>> classes) {
        Map<String, PathItem> paths = new LinkedHashMap<>();

        for (Class<?> cls : classes) {
            if (!isController(cls)) continue;
            scanController(cls, paths);
        }

        return paths;
    }


    private void scanController(Class<?> cls, Map<String, PathItem> paths) {
        List<String> classPaths = extractPaths(cls.getAnnotations());
        if (classPaths.isEmpty()) classPaths = List.of("");

        String tag = cls.getSimpleName()
                .replaceAll("Controller$", "")
                .replaceAll("RestController$", "");

        for (Method method : cls.getDeclaredMethods()) {
            List<String> httpMethods = new ArrayList<>();
            List<String> methodPaths = new ArrayList<>();

            for (Annotation ann : method.getAnnotations()) {
                String annName = ann.annotationType().getSimpleName();
                if (!HTTP_METHOD_ANNOTATIONS.contains(annName)) continue;

                httpMethods.addAll(resolveHttpMethods(ann));
                List<String> mp = extractPaths(new Annotation[]{ann});
                if (mp.isEmpty()) mp = List.of("");
                methodPaths.addAll(mp);
            }

            if (httpMethods.isEmpty()) continue;
            if (methodPaths.isEmpty()) methodPaths = List.of("");

            Operation op;
            try {
                op = buildOperation(method, tag);
            } catch (Throwable t) {
                logger.warn("Skipping method {}: {}", method.getName(), t.getMessage());
                continue;
            }

            for (String classPath : classPaths) {
                for (String methodPath : methodPaths) {
                    String fullPath = normalizePath(classPath + methodPath);
                    for (String httpMethod : httpMethods) {
                        PathItem pathItem = paths.computeIfAbsent(fullPath, k -> new PathItem());
                        pathItem.setOperation(httpMethod, op);
                    }
                }
            }
        }
    }


    private Operation buildOperation(Method method, String tag) {
        Operation op = new Operation();
        op.setOperationId(method.getName());
        op.setSummary(humanize(method.getName()));
        op.setTags(List.of(tag));

        List<io.docgen.core.model.Parameter> params = new ArrayList<>();
        for (Parameter param : method.getParameters()) {
            io.docgen.core.model.Parameter p = buildParameter(param, method);
            if (p != null) params.add(p);
        }
        if (!params.isEmpty()) op.setParameters(params);

        RequestBody requestBody = buildRequestBody(method);
        if (requestBody != null) op.setRequestBody(requestBody);

        op.setResponses(buildResponses(method));

        return op;
    }

    private io.docgen.core.model.Parameter buildParameter(Parameter param, Method method) {
        String typeName = param.getType().getSimpleName();
        if (SKIP_PARAMETER_TYPES.contains(typeName)) return null;

        if (hasAnnotation(param, "RequestBody")) return null;

        io.docgen.core.model.Parameter p = new io.docgen.core.model.Parameter();

        if (hasAnnotation(param, "PathVariable")) {
            p.setIn(io.docgen.core.model.Parameter.In.path);
            p.setRequired(true);
            p.setName(getAnnotationValue(param, "PathVariable", param.getName()));
        } else if (hasAnnotation(param, "RequestParam")) {
            p.setIn(io.docgen.core.model.Parameter.In.query);
            p.setRequired(isAnnotationRequired(param, "RequestParam"));
            p.setName(getAnnotationValue(param, "RequestParam", param.getName()));
        } else if (hasAnnotation(param, "RequestHeader")) {
            p.setIn(io.docgen.core.model.Parameter.In.header);
            p.setRequired(isAnnotationRequired(param, "RequestHeader"));
            p.setName(getAnnotationValue(param, "RequestHeader", param.getName()));
        } else {
            return null;
        }

        Schema schema = typeResolver.resolve(param.getParameterizedType());
        if (schema != null) p.setSchema(schema);

        return p;
    }

    private RequestBody buildRequestBody(Method method) {
        for (Parameter param : method.getParameters()) {
            if (!hasAnnotation(param, "RequestBody")) continue;

            Schema schema = typeResolver.resolve(param.getParameterizedType());
            if (schema == null) schema = new Schema();

            RequestBody.MediaType mediaType = new RequestBody.MediaType(schema);
            RequestBody rb = new RequestBody();
            rb.setRequired(true);
            rb.setContent(Map.of("application/json", mediaType));
            return rb;
        }
        return null;
    }

    private Map<String, Response> buildResponses(Method method) {
        Map<String, Response> responses = new LinkedHashMap<>();

        String statusCode = determineSuccessStatus(method);

        Schema returnSchema = null;
        try {
            java.lang.reflect.Type returnType = method.getGenericReturnType();
            returnSchema = typeResolver.resolve(returnType);
        } catch (Throwable t) {
            System.err.println("[WARN] Could not resolve return type of " + method.getName() + ": " + t.getMessage());
        }

        Response response = new Response();
        response.setDescription("Successful operation");

        if (returnSchema != null) {
            RequestBody.MediaType mediaType = new RequestBody.MediaType(returnSchema);
            response.setContent(Map.of("application/json", mediaType));
        }

        responses.put(statusCode, response);
        return responses;
    }


    private boolean isController(Class<?> cls) {
        for (Annotation ann : cls.getAnnotations()) {
            String name = ann.annotationType().getSimpleName();
            if (CONTROLLER_ANNOTATIONS.contains(name)) return true;
            for (Annotation metaAnn : ann.annotationType().getAnnotations()) {
                if (CONTROLLER_ANNOTATIONS.contains(metaAnn.annotationType().getSimpleName())) return true;
            }
        }
        return false;
    }

    private List<String> extractPaths(Annotation[] annotations) {
        List<String> paths = new ArrayList<>();
        for (Annotation ann : annotations) {
            String name = ann.annotationType().getSimpleName();
            if (!HTTP_METHOD_ANNOTATIONS.contains(name)) continue;
            try {
                String[] value = getStringArray(ann, "value");
                if (value == null || value.length == 0) {
                    value = getStringArray(ann, "path");
                }
                if (value != null) paths.addAll(Arrays.asList(value));
            } catch (Exception ignored) {}
        }
        return paths;
    }

    private List<String> resolveHttpMethods(Annotation ann) {
        String name = ann.annotationType().getSimpleName();
        return switch (name) {
            case "GetMapping"    -> List.of("GET");
            case "PostMapping"   -> List.of("POST");
            case "PutMapping"    -> List.of("PUT");
            case "DeleteMapping" -> List.of("DELETE");
            case "PatchMapping"  -> List.of("PATCH");
            case "RequestMapping" -> {
                try {
                    Object[] methods = (Object[]) ann.annotationType()
                            .getMethod("method").invoke(ann);
                    if (methods == null || methods.length == 0) {
                        yield List.of("GET");
                    }
                    List<String> result = new ArrayList<>();
                    for (Object m : methods) result.add(m.toString());
                    yield result;
                } catch (Exception e) {
                    yield List.of("GET");
                }
            }
            default -> List.of("GET");
        };
    }

    private String[] getStringArray(Annotation ann, String attribute) {
        try {
            Object val = ann.annotationType().getMethod(attribute).invoke(ann);
            if (val instanceof String[] arr) return arr;
            if (val instanceof String s) return new String[]{s};
        } catch (Exception ignored) {}
        return null;
    }

    private boolean hasAnnotation(Parameter param, String simpleName) {
        for (Annotation ann : param.getAnnotations()) {
            if (ann.annotationType().getSimpleName().equals(simpleName)) return true;
        }
        return false;
    }

    private String getAnnotationValue(Parameter param, String annotationName, String defaultValue) {
        for (Annotation ann : param.getAnnotations()) {
            if (!ann.annotationType().getSimpleName().equals(annotationName)) continue;
            try {
                String val = (String) ann.annotationType().getMethod("value").invoke(ann);
                if (val != null && !val.isEmpty()) return val;
                val = (String) ann.annotationType().getMethod("name").invoke(ann);
                if (val != null && !val.isEmpty()) return val;
            } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    private boolean isAnnotationRequired(Parameter param, String annotationName) {
        for (Annotation ann : param.getAnnotations()) {
            if (!ann.annotationType().getSimpleName().equals(annotationName)) continue;
            try {
                return (boolean) ann.annotationType().getMethod("required").invoke(ann);
            } catch (Exception ignored) {}
        }
        return false;
    }

    private String determineSuccessStatus(Method method) {
        for (Annotation ann : method.getAnnotations()) {
            if (ann.annotationType().getSimpleName().equals("ResponseStatus")) {
                try {
                    Object code = ann.annotationType().getMethod("value").invoke(ann);
                    Object numeric = code.getClass().getMethod("value").invoke(code);
                    return String.valueOf(numeric);
                } catch (Exception ignored) {}
            }
        }
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class || returnType == Void.class) return "204";
        return "200";
    }

    private String normalizePath(String path) {
        if (path.isEmpty()) return "/";
        if (!path.startsWith("/")) path = "/" + path;
        path = path.replaceAll("//+", "/");
        return path;
    }

    private String humanize(String methodName) {
        return methodName
                .replaceAll("([A-Z])", " $1")
                .trim()
                .substring(0, 1).toUpperCase()
                + methodName.replaceAll("([A-Z])", " $1").trim().substring(1);
    }
}

