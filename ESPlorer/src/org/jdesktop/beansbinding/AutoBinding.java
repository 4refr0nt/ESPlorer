/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.beansbinding;

/**
 * An implementation of {@code Binding} that automatically syncs the source
 * and target by refreshing and saving according to one of three update
 * strategies. The update strategy is specified for an {@code AutoBinding}
 * on creation, and is one of:
 * <p>
 * <ul>
 *     <li>{@code AutoBinding.UpdateStrategy.READ_ONCE}</li>
 *     <li>{@code AutoBinding.UpdateStrategy.READ}</li>
 *     <li>{@code AutoBinding.UpdateStrategy.READ_WRITE}</li>
 * </ul>
 * <p>
 * <a name="STRATEGY_BEHAVIOR">The behavior</a> of {@code AutoBinding} for each
 * of the update strategies is defined as follows:
 * <p>
 * <table border="0">
 * <tr valign="baseline">
 *   <td><b><font size="+1">{@code READ_ONCE}</font></b></td>
 *   <td>&nbsp;&nbsp;&nbsp;</td>
 *   <td>
 *     <b>Summary:</b><br>
 *     Tries to sync the target from the source only once, at bind time.
 *     <p>
 *     <b>Details:</b><br>
 *     At bind time, tries to sync the target from the source, by calling
 *     {@code refreshAndNotify}. No further automatic syncing is done.
 *   </td>
 * </tr>
 * <tr><td colspan="3"><br></td></tr>
 * <tr valign="baseline">
 *   <td><b><font size="+1">{@code READ}</font></b></td>
 *   <td>&nbsp;&nbsp;&nbsp;</td>
 *   <td>
 *     <b>Summary:</b><br>
 *     Tries to keep the target in sync with the source.
 *     <p>
 *     <b>Details:</b><br>
 *     At bind time, tries to sync the target from the source, by calling
 *     {@code refreshAndNotify}. Then automatically tries to sync the target
 *     from the source by calling {@code refreshAndNotify} when either the source
 *     changes value, or the target changes from unwriteable to writeable.
 *   </td>
 * </tr>
 * <tr><td colspan="3"><br></td></tr>
 * <tr valign="baseline">
 *   <td><b><font size="+1">{@code READ_WRITE}</font></b></td>
 *   <td>&nbsp;&nbsp;&nbsp;</td>
 *   <td>
 *     <b>Summary:</b><br>
 *     Tries to keep both the source and target in sync with each other.
 *     <p>
 *     <b>Details:</b><br>
 *     At bind time, first tries to sync the target from the source, by calling
 *     {@code refresh}. If the call succeeds, notifies the binding listeners
 *     of a successful sync. If the call returns failure, then tries to instead sync the
 *     source from the target by calling {@code save}. If this second call succeeds,
 *     notifies the binding listeners of a succesful sync. If it returns failure, notifies
 *     the binding listeners of a failed sync, indicating the reason for the original
 *     refresh failure.
 *     <p>
 *     Automatically responds to changes in the state of the source as follows:
 *     If the change represents a value change, use the try-refresh-then-save
 *     procedure mentioned above. Otherwise, if the change represents the
 *     source becoming writeable, tries to update the source from the target
 *     by calling {@code saveAndNotify}.
 *     <p>
 *     Automatically responds to changes in the state of the target as follows:
 *     If the change represents the target simply becoming writeable, try to
 *     sync the target from the source by calling {@code refreshAndNotify}. If
 *     the change represents the target becoming writeable and the value changing
 *     together, use the try-refresh-then-save procedure mentioned above. Finally
 *     if the change represents the target's value changing alone, first try to
 *     sync the source from the target by calling {@code save}.
 *     If that succeeds, notify the listeners of a successful sync. If it
 *     returns failure due to conversion or validation, notify the listeners of a sync
 *     failure, providing the conversion or validation failure. If it fails for
 *     any other reason, then instead try to sync the target from the source by
 *     calling {@code refresh}. If this succeeds, notify the listeners of successful
 *     sync. Otherwise notify them of failure with the reasons for the original
 *     save failure.
 *   </td>
 * </tr>
 * </table>
 *
 * @param <SS> the type of source object
 * @param <SV> the type of value that the source property represents
 * @param <TS> the type of target object
 * @param <TV> the type of value that the target property represents
 *
 * @author Shannon Hickey
 */
public class AutoBinding<SS, SV, TS, TV> extends Binding<SS, SV, TS, TV> {

    private UpdateStrategy strategy;

    /**
     * An enumeration representing the possible update strategies of an
     * {@code AutoBinding}. See {@code AutoBinding's} class level
     * <a href="AutoBinding.html#STRATEGY_BEHAVIOR">documentation</a> for complete
     * details on the sync behavior for each possible update strategy.
     */
    public enum UpdateStrategy {

        /**
         * An update strategy where the {@code AutoBinding} tries to sync the
         * target from the source only once, at bind time.
         */
        READ_ONCE,

        /**
         * An update strategy where the {@code AutoBinding} tries to keep the target
         * in sync with the source.
         */
        READ,

        /**
         * An update strategy where the {@code AutoBinding} tries to keep both the
         * source and target in sync with each other.
         */
        READ_WRITE
    }

    /**
     * Create an instance of {@code AutoBinding} between two properties of two objects,
     * with the given update strategy.
     *
     * @param strategy the update strategy
     * @param sourceObject the source object
     * @param sourceProperty a property on the source object
     * @param targetObject the target object
     * @param targetProperty a property on the target object
     * @param name a name for the {@code Binding}
     * @throws IllegalArgumentException if the source property or target property is {@code null}
     */
    protected AutoBinding(UpdateStrategy strategy, SS sourceObject, Property<SS, SV> sourceProperty, TS targetObject, Property<TS, TV> targetProperty, String name) {
        super(sourceObject, sourceProperty, targetObject, targetProperty, name);

        if (strategy == null) {
            throw new IllegalArgumentException("must provide update strategy");
        }

        this.strategy = strategy;
    }

    /**
     * Returns the {@code AutoBinding's} update strategy.
     *
     * @return the update strategy
     */
    public final UpdateStrategy getUpdateStrategy() {
        return strategy;
    }

    private final void tryRefreshThenSave() {
        SyncFailure refreshFailure = refresh();
        if (refreshFailure == null) {
            notifySynced();
        } else {
            SyncFailure saveFailure = save();
            if (saveFailure == null) {
                notifySynced();
            } else {
                notifySyncFailed(refreshFailure);
            }
        }
    }

    private final void trySaveThenRefresh() {
        SyncFailure saveFailure = save();
        if (saveFailure == null) {
            notifySynced();
        } else if (saveFailure.getType() == SyncFailureType.CONVERSION_FAILED || saveFailure.getType() == SyncFailureType.VALIDATION_FAILED) {
            notifySyncFailed(saveFailure);
        } else {
            SyncFailure refreshFailure = refresh();
            if (refreshFailure == null) {
                notifySynced();
            } else {
                notifySyncFailed(saveFailure);
            }
        }
    }

    protected void bindImpl() {
        UpdateStrategy strat = getUpdateStrategy();

        if (strat == UpdateStrategy.READ_ONCE) {
            refreshAndNotify();
        } else if (strat == UpdateStrategy.READ) {
            refreshAndNotify();
        } else {
            tryRefreshThenSave();
        }
    }

    protected void unbindImpl() {}

    /**
     * Returns a string representing the internal state of the {@code Binding}.
     * This method is intended to be used for debugging purposes only,
     * and the content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be {@code null}.
     *
     * @return a string representing the state of the {@code Binding}.
     */
    protected String paramString() {
        return super.paramString() + ", updateStrategy=" + getUpdateStrategy();
    }

    protected void sourceChangedImpl(PropertyStateEvent pse) {
        if (strategy == UpdateStrategy.READ_ONCE) {
            // nothing to do
        } else if (strategy == UpdateStrategy.READ) {
            if (pse.getValueChanged()) {
                refreshAndNotify();
            }
        } else if (strategy == UpdateStrategy.READ_WRITE) {
            if (pse.getValueChanged()) {
                tryRefreshThenSave();
            } else if (pse.isWriteable()) {
                saveAndNotify();
            }
        }
    }

    protected void targetChangedImpl(PropertyStateEvent pse) {
        if (strategy == UpdateStrategy.READ_ONCE) {
            // nothing to do
        } else if (strategy == UpdateStrategy.READ) {
            if (pse.getWriteableChanged() && pse.isWriteable()) {
                refreshAndNotify();
            }
        } else if (strategy == UpdateStrategy.READ_WRITE) {
            if (pse.getWriteableChanged() && pse.isWriteable()) {
                if (pse.getValueChanged()) {
                    tryRefreshThenSave();
                } else {
                    refreshAndNotify();
                }
            } else if (pse.getValueChanged()) {
                trySaveThenRefresh();
            }
        }
    }

}
