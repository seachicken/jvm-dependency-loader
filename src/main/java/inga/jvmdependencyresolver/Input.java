package inga.jvmdependencyresolver;

import com.fasterxml.jackson.annotation.JsonCreator;

public record Input(
        Command command,
        String path
) {
    public enum Command {
        LOAD_PROJECT("load"),
        READ_METHODS("read")
        ;

        private final String code;

        Command(String code) {
            this.code = code;
        }

        @JsonCreator
        public static Command fromCode(String code) {
            for (Command command : values()) {
                if (command.code.equals(code)) {
                    return command;
                }
            }
            throw new IllegalArgumentException("unknown command: " + code);
        }
    }
}
