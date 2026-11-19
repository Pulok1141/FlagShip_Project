package io.flagship.core.observer;

/**
 * Observer pattern interface for listening to feature flag updates.
 */
@FunctionalInterface
public interface FlagChangeListener {

    /**
     * Called when a flag is added, updated, toggled, or deleted.
     *
     * @param event the change event
     */
    void onFlagChanged(FlagChangeEvent event);
}
