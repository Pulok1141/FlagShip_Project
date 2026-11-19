package io.flagship.server.command;

/**
 * Command pattern interface for encapsulating all feature flag mutations (FR7).
 */
public interface FlagChangeCommand {

    /**
     * Execute the command mutation, compute diff, record version history, and trigger SSE broadcast.
     *
     * @return the command result
     */
    CommandResult execute();

    /**
     * Rollback the mutation to the previous state.
     */
    void rollback();
}
