package inga.jvmdependencyloader;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ObjectMapper mapper = new ObjectMapper();
        try (DependencyLoader resolver = new DependencyLoader()) {
            while (scanner.hasNextLine()) {
                Input input = mapper.readValue(scanner.nextLine(), Input.class);
                Object result = null;
                switch (input.getType()) {
                    case CLASS_PATHS:
                        result = resolver.getClassPaths(Paths.get(input.getFrom()), Paths.get(input.getRoot()));
                        break;
                    case METHODS:
                        result = resolver.readMethods(input.getFqcn(), Paths.get(input.getFrom()), Paths.get(input.getRoot()));
                        break;
                    case CLASSES:
                        result = resolver.readClasses(input.getFqcn(), Paths.get(input.getFrom()), Paths.get(input.getRoot()));
                        break;
                    case HIERARCHY:
                        result = resolver.readHierarchy(input.getFqcn(), Paths.get(input.getFrom()), Paths.get(input.getRoot()));
                        break;
                }
                String json = mapper.writeValueAsString(result);
                System.out.println(json);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
