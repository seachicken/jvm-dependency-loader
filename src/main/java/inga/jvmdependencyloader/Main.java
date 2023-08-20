package inga.jvmdependencyloader;

import com.fasterxml.jackson.core.JsonProcessingException;
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
                var methods = resolver.readMethods(input.fqcn(), Path.of(input.from()));
                try {
                    var json = mapper.writeValueAsString(methods);
                    System.out.println(json);
                } catch (JsonProcessingException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
