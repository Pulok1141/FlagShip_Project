package io.flagship.server.command;

import io.flagship.server.domain.FlagEnvConfig;

public record CommandResult(
        boolean success,
        long version,
        String diffJson,
        String message,
        FlagEnvConfig config
) {
    public static CommandResult success(long version, String diffJson, FlagEnvConfig config) {
        return new CommandResult(true, version, diffJson, "Command executed successfully", config);
    }

    public static CommandResult error(String message) {
        return new CommandResult(false, 0L, null, message, null);
    }
}
