package inga.jvmdependencyloader;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        var mapper = new ObjectMapper();
        try (var resolver = new DependencyLoader()) {
            while (scanner.hasNextLine()) {
                var input = mapper.readValue(scanner.nextLine(), Input.class);
                var result = switch (input.type()) {
                    case METHODS -> resolver.readMethods(input.fqcn(), Path.of(input.from()), Path.of(input.root()));
                    case CLASSES -> resolver.readClasses(input.fqcn(), Path.of(input.from()), Path.of(input.root()));
                    case HIERARCHY -> resolver.readHierarchy(input.fqcn(), Path.of(input.from()), Path.of(input.root()));
                };
                var json = mapper.writeValueAsString(result);
                System.out.println(json);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
