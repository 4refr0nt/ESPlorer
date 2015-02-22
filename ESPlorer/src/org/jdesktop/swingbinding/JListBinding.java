/*
 * Copyright (C) 2007 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */

package org.jdesktop.swingbinding;

import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.ObjectProperty;
import org.jdesktop.beansbinding.Property;
import org.jdesktop.beansbinding.PropertyStateEvent;
import org.jdesktop.beansbinding.PropertyStateListener;
import org.jdesktop.swingbinding.impl.AbstractColumnBinding;
import org.jdesktop.swingbinding.impl.ListBindingManager;
import static org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.*;

/**
 * Binds a {@code List} of objects to act as the elements of a {@code JList}.
 * Each object in the source {@code List} provides one element in the {@code JList}.
 * By setting a {@link org.jdesktop.swingbinding.JListBinding.DetailBinding DetailBinding}
 * you can specify the property to use to derive each list element from its
 * corresponding object in the source {@code List}. The default {@code DetailBinding} uses
 * the objects directly. Instances of {@code JListBinding} are obtained by
 * calling one of the {@code createJListBinding} methods in the {@code SwingBindings}
 * class.
 * <p>
 * Here is an example of creating a binding from a {@code List} of {@code Person}
 * objects to a {@code JList}:
 * <p>
 * <pre><code>
 *    // create the person list
 *    List<Person> people = createPersonList();
 *
 *    // create the binding from List to JList
 *    JListBinding lb = SwingBindings.createJListBinding(READ, people, jList);
 *
 *    // define the property to be used to derive list elements
 *    ELProperty fullNameP = ELProperty.create("${firstName} ${lastName}");
 *
 *    // add the detail binding
 *    lb.setDetailBinding(fullNameP);
 *
 *    // realize the binding
 *    lb.bind();
 * </code></pre>
 * <p>
 * The {@code JList} target of a {@code JListBinding} acts as a live view of
 * the objects in the source {@code List}, regardless of the update strategy (the
 * meaning of the update strategy is <a href="#CLARIFICATION">clarified later</a>
 * in this document). {@code JListBinding} listens to the property specified for
 * any {@code DetailBinding}, for all objects in the {@code List}, and updates
 * the values displayed in the {@code JList} in response to change. If the
 * {@code List} is an instance of {@code ObservableList}, then changes to the
 * {@code List} contents (such as adding, removing or replacing an object) are
 * also reflected in the {@code JList}. <b>Important:</b> Changing the contents
 * of a non-observable {@code List} while it is participating in a
 * {@code JListBinding} is unsupported, resulting in undefined behavior and
 * possible exceptions.
 * <p>
 * <a name="CLARIFICATION">{@code JListBinding} requires</a>
 * extra clarification on the operation of the
 * {@code refresh} and {@code save} methods and the meaning of the update
 * strategy. The target property of a {@code JListBinding} is not the
 * target {@code JList} property provided in the constructor, but rather a
 * private synthetic property representing the {@code List} of objects to show
 * in the target {@code JList}. This synthetic property is readable/writeable
 * only when the {@code JListBinding} is bound and the target {@code JList}
 * property is readable with a {@code non-null} value.
 * <p>
 * It is this private synthetic property on which the {@code refresh} and
 * {@code save} methods operate; meaning that these methods simply cause syncing
 * between the value of the source {@code List} property and the value of the
 * synthetic target property (representing the {@code List} to be shown in the
 * target {@code JList}). These methods do not, therefore, have anything to do
 * with refreshing <i>values</i> in the {@code JList}. Likewise, the update
 * strategy, which simply controls when {@code refresh} and {@code save} are
 * automatically called, also has nothing to do with refreshing <i>values</i>
 * in the {@code JList}.
 * <p>
 * <b>Note:</b> At the current time, the {@code READ_WRITE} update strategy
 * is not useful for {@code JListBinding}. To prevent unwanted confusion,
 * {@code READ_WRITE} is translated to {@code READ} by {@code JListBinding's}
 * constructor.
 * <p>
 * {@code JListBinding} works by installing a custom model on the target
 * {@code JList}, as appropriate, to represent the source {@code List}. The
 * model is installed on a target {@code JList} with the first succesful call
 * to {@code refresh} with that {@code JList} as the target. Subsequent calls
 * to {@code refresh} update the elements in this already-installed model.
 * The model is uninstalled from a target {@code JList} when either the
 * {@code JListBinding} is unbound or when the target {@code JList} property
 * changes to no longer represent that {@code JList}. Note: When the model is
 * uninstalled from a {@code JList}, it is replaced with a {@code DefaultListModel},
 * in order to leave the {@code JList} functional.
 * <p>
 * Some of the above is easier to understand with an example. Let's consider
 * a {@code JListBinding} ({@code binding}), with update strategy
 * {@code READ}, between a property representing a {@code List} ({@code listP})
 * and a property representing a {@code JList} ({@code jListP}). {@code listP}
 * and {@code jListP} both start off readable, referring to a {@code non-null}
 * {@code List} and {@code non-null} {@code JList} respectively. Let's look at
 * what happens for each of a sequence of events:
 * <p>
 * <table border=1>
 *   <tr><th>Sequence</th><th>Event</th><th>Result</th></tr>
 *   <tr valign="baseline">
 *     <td align="center">1</td>
 *     <td>explicit call to {@code binding.bind()}</td>
 *     <td>
 *         - synthetic target property becomes readable/writeable
 *         <br>
 *         - {@code refresh()} is called
 *         <br>
 *         - model is installed on target {@code JList}, representing list of objects
 *     </td>
 *   </tr>
 *   <tr valign="baseline">
 *     <td align="center">2</td>
 *     <td>{@code listP} changes to a new {@code List}</td>
 *     <td>
 *         - {@code refresh()} is called
 *         <br>
 *         - model is updated with new list of objects
 *     </td>
 *   </tr>
 *   <tr valign="baseline">
 *     <td align="center"><a name="STEP3" href="#NOTICE">3</a></td>
 *     <td>{@code jListP} changes to a new {@code JList}</td>
 *     <td>
 *         - model is uninstalled from old {@code JList}
 *     </td>
 *   </tr>
 *   <tr valign="baseline">
 *     <td align="center">4</td>
 *     <td>explicit call to {@code binding.refresh()}</td>
 *     <td>
 *         - model is installed on target {@code JList}, representing list of objects
 *     </td>
 *   </tr>
 *   <tr valign="baseline">
 *     <td align="center">5</td>
 *     <td>{@code listP} changes to a new {@code List}</td>
 *     <td>
 *         - {@code refresh()} is called
 *         <br>
 *         - model is updated with new list of objects
 *     </td>
 *   </tr>
 *   <tr valign="baseline">
 *     <td align="center">6</td>
 *     <td>explicit call to {@code binding.unbind()}</td>
 *     <td>
 *         - model is uninstalled from target {@code JList}
 *     </td>
 *   </tr>
 * </table>
 * <p>
 * <a name="NOTICE">Notice</a> that in <a href="#STEP3">step 3</a>, when the value
 * of the {@code JList} property changed, the new {@code JList} did not
 * automatically get the model with the elements applied to it. A change to the
 * target value should not cause an {@code AutoBinding} to sync the target from
 * the source. Step 4 forces a sync by explicitly calling {@code refresh}.
 * Alternatively, it could be caused by any other action that results
 * in a {@code refresh} (for example, the source property changing value, or an
 * explicit call to {@code unbind} followed by {@code bind}).
 * <p>
 * {@code DetailBindings} are managed by the {@code JList}. They are not
 * to be explicitly bound, unbound, added to a {@code BindingGroup}, or accessed
 * in a way that is not allowed for a managed binding.
 * <p>
 * In addition to binding the elements of a {@code JList}, it is possible to
 * bind to the selection of a {@code JList}. When binding to the selection of a {@code JList}
 * backed by a {@code JListBinding}, the selection is always in terms of elements
 * from the source {@code List}, regardless of any {@code DetailBinding} specified.
 * See the list of <a href="package-summary.html#SWING_PROPS">
 * interesting swing properties</a> in the package summary for more details.
 *
 * @param <E> the type of elements in the source {@code List}
 * @param <SS> the type of source object (on which the source property resolves to {@code List})
 * @param <TS> the type of target object (on which the target property resolves to {@code JList})
 *
 * @author Shannon Hickey
 */
public final class JListBinding<E, SS, TS> extends AutoBinding<SS, List<E>, TS, List> {

    private Property<TS, ? extends JList> listP;
    private ElementsProperty<TS> elementsP;
    private Handler handler = new Handler();
    private JList list;
    private BindingListModel model;
    private DetailBinding detailBinding;

    /**
     * Constructs an instance of {@code JListBinding}.
     *
     * @param strategy the update strategy
     * @param sourceObject the source object
     * @param sourceListProperty a property on the source object that resolves to the {@code List} of elements
     * @param targetObject the target object
     * @param targetJListProperty a property on the target object that resolves to a {@code JList}
     * @param name a name for the {@code JListBinding}
     * @throws IllegalArgumentException if the source property or target property is {@code null}
     */
    protected JListBinding(UpdateStrategy strategy, SS sourceObject, Property<SS, List<E>> sourceListProperty, TS targetObject, Property<TS, ? extends JList> targetJListProperty, String name) {
        super(strategy == READ_WRITE ? READ : strategy,
              sourceObject, sourceListProperty, targetObject, new ElementsProperty<TS>(), name);

        if (targetJListProperty == null) {
            throw new IllegalArgumentException("target JList property can't be null");
        }

        listP = targetJListProperty;
        elementsP = (ElementsProperty<TS>)getTargetProperty();
        setDetailBinding(null);
    }

    protected void bindImpl() {
        elementsP.setAccessible(isListAccessible());
        listP.addPropertyStateListener(getTargetObject(), handler);
        elementsP.addPropertyStateListener(null, handler);
        super.bindImpl();
    }

    protected void unbindImpl() {
        elementsP.removePropertyStateListener(null, handler);
        listP.removePropertyStateListener(getTargetObject(), handler);
        elementsP.setAccessible(false);
        cleanupForLast();
        super.unbindImpl();
    }

    private boolean isListAccessible() {
        return listP.isReadable(getTargetObject()) && listP.getValue(getTargetObject()) != null;
    }

    private boolean isListAccessible(Object value) {
        return value != null && value != PropertyStateEvent.UNREADABLE;
    }

    private void cleanupForLast() {
        if (list == null) {
            return;
        }

        resetListSelection();
        list.setModel(new DefaultListModel());
        list = null;
        model.setElements(null, true);
        model = null;
    }
    
    /**
     * Creates a {@code DetailBinding} and sets it as the {@code DetailBinding}
     * for this {@code JListBinding}. A {@code DetailBinding} specifies the property
     * of the objects in the source {@code List} to be used as the elements of the
     * {@code JList}. If the {@code detailProperty} parameter is {@code null}, the
     * {@code DetailBinding} specifies that the objects themselves be used.
     *
     * @param detailProperty the property with which to derive each list value
     *        from its corresponding object in the source {@code List}
     * @return the {@code DetailBinding}
     */
    public DetailBinding setDetailBinding(Property<E, ?> detailProperty) {
        return setDetailBinding(detailProperty, null);
    }

    /**
     * Creates a named {@code DetailBinding} and sets it as the {@code DetailBinding}
     * for this {@code JListBinding}. A {@code DetailBinding} specifies the property
     * of the objects in the source {@code List} to be used as the elements of the
     * {@code JList}. If the {@code detailProperty} parameter is {@code null}, the
     * {@code DetailBinding} specifies that the objects themselves be used.
     *
     * @param detailProperty the property with which to derive each list value
     *        from its corresponding object in the source {@code List}
     * @return the {@code DetailBinding}
     */
    public DetailBinding setDetailBinding(Property<E, ?> detailProperty, String name) {
        throwIfBound();

        if (name == null && JListBinding.this.getName() != null) {
            name = JListBinding.this.getName() + ".DETAIL_BINDING";
        }

        detailBinding = detailProperty == null ?
                        new DetailBinding(ObjectProperty.<E>create(), name) :
                        new DetailBinding(detailProperty, name);
        return detailBinding;
    }

    /**
     * Returns the {@code DetailBinding} for this {@code JListBinding}.
     * A {@code DetailBinding} specifies the property of the source {@code List} elements
     * to be used as the elements of the {@code JList}.
     *
     * @return the {@code DetailBinding}
     * @see #setDetailBinding(Property, String)
     */
    public DetailBinding getDetailBinding() {
        return detailBinding;
    }

    private final Property DETAIL_PROPERTY = new Property() {
        public Class<Object> getWriteType(Object source) {
            return Object.class;
        }

        public Object getValue(Object source) {
            throw new UnsupportedOperationException();
        }

        public void setValue(Object source, Object value) {
            throw new UnsupportedOperationException();
        }

        public boolean isReadable(Object source) {
            throw new UnsupportedOperationException();
        }

        public boolean isWriteable(Object source) {
            return true;
        }

        public void addPropertyStateListener(Object source, PropertyStateListener listener) {
            throw new UnsupportedOperationException();
        }

        public void removePropertyStateListener(Object source, PropertyStateListener listener) {
            throw new UnsupportedOperationException();
        }

        public PropertyStateListener[] getPropertyStateListeners(Object source) {
            throw new UnsupportedOperationException();
        }
    };

    /**
     * {@code DetailBinding} represents a binding between a property of the elements
     * in the {@code JListBinding's} source {@code List}, and the values shown in
     * the {@code JList}. Values in the {@code JList} are aquired by fetching the
     * value of the {@code DetailBinding's} source property for the associated object
     * in the source {@code List}.
     * <p>
     * A {@code Converter} may be specified on a {@code DetailBinding}. Specifying a
     * {@code Validator} is also possible, but doesn't make sense since {@code JList}
     * values aren't editable.
     * <p>
     * {@code DetailBindings} are managed by their {@code JListBinding}. They are not
     * to be explicitly bound, unbound, added to a {@code BindingGroup}, or accessed
     * in a way that is not allowed for a managed binding.
     *
     * @see org.jdesktop.swingbinding.JListBinding#setDetailBinding(Property, String)
     */
    public final class DetailBinding extends AbstractColumnBinding {

        private DetailBinding(Property<E, ?> detailProperty, String name) {
            super(0, detailProperty, DETAIL_PROPERTY, name);
        }

    }

    private class Handler implements PropertyStateListener {
        public void propertyStateChanged(PropertyStateEvent pse) {
            if (!pse.getValueChanged()) {
                return;
            }

            if (pse.getSourceProperty() == listP) {
                cleanupForLast();
                
                boolean wasAccessible = isListAccessible(pse.getOldValue());
                boolean isAccessible = isListAccessible(pse.getNewValue());

                if (wasAccessible != isAccessible) {
                    elementsP.setAccessible(isAccessible);
                } else if (elementsP.isAccessible()) {
                    elementsP.setValueAndIgnore(null, null);
                }
            } else {
                if (((ElementsProperty.ElementsPropertyStateEvent)pse).shouldIgnore()) {
                    return;
                }

                if (list == null) {
                    list = listP.getValue(getTargetObject());
                    resetListSelection();
                    model = new BindingListModel();
                    list.setModel(model);
                } else {
                    resetListSelection();
                }

                model.setElements((List)pse.getNewValue(), true);
            }
        }
    }

    private void resetListSelection() {
        ListSelectionModel selectionModel = list.getSelectionModel();
        selectionModel.setValueIsAdjusting(true);
        selectionModel.clearSelection();
        selectionModel.setAnchorSelectionIndex(-1);
        selectionModel.setLeadSelectionIndex(-1);
        selectionModel.setValueIsAdjusting(false);
    }
    
    private final class BindingListModel extends ListBindingManager implements ListModel  {
        private final List<ListDataListener> listeners;

        public BindingListModel() {
            listeners = new CopyOnWriteArrayList<ListDataListener>();
        }

        protected AbstractColumnBinding[] getColBindings() {
            return new AbstractColumnBinding[] {getDetailBinding()};
        }

        protected void allChanged() {
            contentsChanged(0, size());
        }

        protected void valueChanged(int row, int column) {
            contentsChanged(row, row);
        }

        protected void added(int index, int length) {
            assert length > 0; // enforced by ListBindingManager

            ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index, index + length - 1);
            for (ListDataListener listener : listeners) {
                listener.intervalAdded(e);
            }
        }

        protected void removed(int index, int length) {
            assert length > 0; // enforced by ListBindingManager

            ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index + length - 1);
            for (ListDataListener listener : listeners) {
                listener.intervalRemoved(e);
            }
        }

        protected void changed(int row) {
            contentsChanged(row, row);
        }

        private void contentsChanged(int row0, int row1) {
            ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, row0, row1);
            for (ListDataListener listener : listeners) {
                listener.contentsChanged(e);
            }
        }

        public Object getElementAt(int index) {
            return valueAt(index, 0);
        }

        public void addListDataListener(ListDataListener l) {
            listeners.add(l);
        }

        public void removeListDataListener(ListDataListener l) {
            listeners.remove(l);
        }

        public int getSize() {
            return size();
        }
    }
}
