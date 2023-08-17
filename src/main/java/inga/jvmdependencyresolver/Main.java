package inga.jvmdependencyresolver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        var mapper = new ObjectMapper();
        try (var resolver = new DependencyResolver()) {
            while (scanner.hasNextLine()) {
                var input = mapper.readValue(scanner.nextLine(), Input.class);
                switch (input.command()) {
                    case LOAD_PROJECT -> {
                        resolver.loadProject(input.path());
                        System.out.println();
                    }
                    case READ_METHODS -> {
                        var methods = resolver.readMethods(input.path());
                        try {
                            var json = mapper.writeValueAsString(methods);
                            System.out.println(json);
                        } catch (JsonProcessingException e) {
                            throw new IllegalArgumentException(e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
