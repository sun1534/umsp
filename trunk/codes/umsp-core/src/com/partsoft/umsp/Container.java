package com.partsoft.umsp;

import java.util.EventListener;

import com.partsoft.umsp.log.Log;
import com.partsoft.utils.ListUtils;

public class Container {
	private Object _listeners;

	public synchronized void addEventListener(Container.Listener listener) {
		_listeners = ListUtils.add(_listeners, listener);
	}

	public synchronized void removeEventListener(Container.Listener listener) {
		_listeners = ListUtils.remove(_listeners, listener);
	}

	/**
	 * Update single parent to child relationship.
	 * 
	 * @param parent
	 *            The parent of the child.
	 * @param oldChild
	 *            The previous value of the child. If this is non null and
	 *            differs from <code>child</code>, then a remove event is
	 *            generated.
	 * @param child
	 *            The current child. If this is non null and differs from
	 *            <code>oldChild</code>, then an add event is generated.
	 * @param relationship
	 *            The name of the relationship
	 */
	public synchronized void update(Object parent, Object oldChild, final Object child, String relationship) {
		if (oldChild != null && !oldChild.equals(child))
			remove(parent, oldChild, relationship);
		if (child != null && !child.equals(oldChild))
			add(parent, child, relationship);
	}

	/**
	 * Update single parent to child relationship.
	 * 
	 * @param parent
	 *            The parent of the child.
	 * @param oldChild
	 *            The previous value of the child. If this is non null and
	 *            differs from <code>child</code>, then a remove event is
	 *            generated.
	 * @param child
	 *            The current child. If this is non null and differs from
	 *            <code>oldChild</code>, then an add event is generated.
	 * @param relationship
	 *            The name of the relationship
	 * @param addRemoveBean
	 *            If true add/remove is called for the new/old children as well
	 *            as the relationships
	 */
	public synchronized void update(Object parent, Object oldChild, final Object child, String relationship,
			boolean addRemove) {
		if (oldChild != null && !oldChild.equals(child)) {
			remove(parent, oldChild, relationship);
			if (addRemove)
				removeBean(oldChild);
		}

		if (child != null && !child.equals(oldChild)) {
			if (addRemove)
				addBean(child);
			add(parent, child, relationship);
		}
	}

	/**
	 * Update multiple parent to child relationship.
	 * 
	 * @param parent
	 *            The parent of the child.
	 * @param oldChildren
	 *            The previous array of children. A remove event is generated
	 *            for any child in this array but not in the
	 *            <code>children</code> array. This array is modified and
	 *            children that remain in the new children array are nulled out
	 *            of the old children array.
	 * @param children
	 *            The current array of children. An add event is generated for
	 *            any child in this array but not in the
	 *            <code>oldChildren</code> array.
	 * @param relationship
	 *            The name of the relationship
	 */
	public synchronized void update(Object parent, Object[] oldChildren, final Object[] children, String relationship) {
		update(parent, oldChildren, children, relationship, false);
	}

	/**
	 * Update multiple parent to child relationship.
	 * 
	 * @param parent
	 *            The parent of the child.
	 * @param oldChildren
	 *            The previous array of children. A remove event is generated
	 *            for any child in this array but not in the
	 *            <code>children</code> array. This array is modified and
	 *            children that remain in the new children array are nulled out
	 *            of the old children array.
	 * @param children
	 *            The current array of children. An add event is generated for
	 *            any child in this array but not in the
	 *            <code>oldChildren</code> array.
	 * @param relationship
	 *            The name of the relationship
	 * @param addRemoveBean
	 *            If true add/remove is called for the new/old children as well
	 *            as the relationships
	 */
	public synchronized void update(Object parent, Object[] oldChildren, final Object[] children, String relationship,
			boolean addRemove) {
		Object[] newChildren = null;
		if (children != null) {
			newChildren = new Object[children.length];

			for (int i = children.length; i-- > 0;) {
				boolean new_child = true;
				if (oldChildren != null) {
					for (int j = oldChildren.length; j-- > 0;) {
						if (children[i] != null && children[i].equals(oldChildren[j])) {
							oldChildren[j] = null;
							new_child = false;
						}
					}
				}
				if (new_child)
					newChildren[i] = children[i];
			}
		}

		if (oldChildren != null) {
			for (int i = oldChildren.length; i-- > 0;) {
				if (oldChildren[i] != null) {
					remove(parent, oldChildren[i], relationship);
					if (addRemove)
						removeBean(oldChildren[i]);
				}
			}
		}

		if (newChildren != null) {
			for (int i = 0; i < newChildren.length; i++)
				if (newChildren[i] != null) {
					if (addRemove)
						addBean(newChildren[i]);
					add(parent, newChildren[i], relationship);
				}
		}
	}

	public void addBean(Object obj) {
		if (_listeners != null) {
			for (int i = 0; i < ListUtils.size(_listeners); i++) {
				Listener listener = (Listener) ListUtils.get(_listeners, i);
				listener.addBean(obj);
			}
		}
	}

	public void removeBean(Object obj) {
		if (_listeners != null) {
			for (int i = 0; i < ListUtils.size(_listeners); i++)
				((Listener) ListUtils.get(_listeners, i)).removeBean(obj);
		}
	}

	/**
	 * Add a parent child relationship
	 * 
	 * @param parent
	 * @param child
	 * @param relationship
	 */
	private void add(Object parent, Object child, String relationship) {
		if (Log.isDebugEnabled())
			Log.debug("Container " + parent + " + " + child + " as " + relationship);
		if (_listeners != null) {
			Relationship event = new Relationship(this, parent, child, relationship);
			for (int i = 0; i < ListUtils.size(_listeners); i++)
				((Listener) ListUtils.get(_listeners, i)).add(event);
		}
	}

	/**
	 * remove a parent child relationship
	 * 
	 * @param parent
	 * @param child
	 * @param relationship
	 */
	private void remove(Object parent, Object child, String relationship) {
		if (Log.isDebugEnabled())
			Log.debug("Container " + parent + " - " + child + " as " + relationship);
		if (_listeners != null) {
			Relationship event = new Relationship(this, parent, child, relationship);
			for (int i = 0; i < ListUtils.size(_listeners); i++)
				((Listener) ListUtils.get(_listeners, i)).remove(event);
		}
	}

	/**
	 * A Container event.
	 * 
	 * @see Listener
	 * 
	 */
	public static class Relationship {
		private Object _parent;
		private Object _child;
		private String _relationship;
		private Container _container;

		private Relationship(Container container, Object parent, Object child, String relationship) {
			_container = container;
			_parent = parent;
			_child = child;
			_relationship = relationship;
		}

		public Container getContainer() {
			return _container;
		}

		public Object getChild() {
			return _child;
		}

		public Object getParent() {
			return _parent;
		}

		public String getRelationship() {
			return _relationship;
		}

		public String toString() {
			return _parent + "---" + _relationship + "-->" + _child;
		}

		public int hashCode() {
			return _parent.hashCode() + _child.hashCode() + _relationship.hashCode();
		}

		public boolean equals(Object o) {
			if (o == null || !(o instanceof Relationship))
				return false;
			Relationship r = (Relationship) o;
			return r._parent == _parent && r._child == _child && r._relationship.equals(_relationship);
		}
	}

	/**
	 * Listener. A listener for Container events.
	 */
	public interface Listener extends EventListener {
		public void addBean(Object bean);

		public void removeBean(Object bean);

		public void add(Container.Relationship relationship);

		public void remove(Container.Relationship relationship);

	}
}
