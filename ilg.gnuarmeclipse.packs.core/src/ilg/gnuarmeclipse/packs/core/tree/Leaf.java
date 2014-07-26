/*******************************************************************************
 * Copyright (c) 2014 Liviu Ionescu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Liviu Ionescu - initial implementation.
 *******************************************************************************/

package ilg.gnuarmeclipse.packs.core.tree;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;

public class Leaf implements Comparable<Leaf>, IAdaptable {

	protected String fType;
	protected Node fParent;
	protected Map<String, String> fProperties;

	public Leaf(String type) {
		fType = type;
		fParent = null;
		fProperties = null;
	}

	// Does not copy properties!
	public Leaf(Leaf node) {

		fType = node.fType;
		fProperties = null;
		fParent = null;

		String name = node.getProperty(Property.NAME);
		if (name != null) {
			setName(name.trim());
		}

		String description = node.getProperty(Property.DESCRIPTION);
		if (description != null) {
			setDescription(description.trim());
		}
	}

	public String getType() {
		return fType;
	}

	public boolean isType(String type) {
		return fType.equals(type);
	}

	public void setType(String type) {
		this.fType = type;
	}

	public String getName() {

		String name = getProperty(Property.NAME);
		if (name != null) {
			return name.trim();
		}
		return "";
	}

	public void setName(String name) {
		putProperty(Property.NAME, name);
	}

	public String getDescription() {

		String description = getProperty(Property.DESCRIPTION);
		if (description != null) {
			return description.trim();
		}
		return "";
	}

	public void setDescription(String description) {
		putProperty(Property.DESCRIPTION, description);
	}

	public boolean hasChildren() {
		return false;
	}

	public Node getParent() {
		return fParent;
	}

	public boolean hasProperties() {
		return (fProperties != null && !fProperties.isEmpty());
	}

	public boolean hasRelevantProperties() {

		if (!hasProperties()) {
			return false;
		}
		for (String key : fProperties.keySet()) {
			if (Property.NAME.equals(key)) {
				continue; // skip name
			}
			if (Property.DESCRIPTION.equals(key)) {
				continue; // skip description
			}
			return true;
		}

		return false;
	}

	public Map<String, String> getProperties() {
		return fProperties;
	}

	public Object putProperty(String name, String value) {

		if (fProperties == null) {
			// Linked to preserve order
			fProperties = new LinkedHashMap<String, String>();
		}

		return fProperties.put(name, value);
	}

	public Object putNonEmptyProperty(String name, String value) {

		if (value != null && value.length() > 0) {
			return putProperty(name, value);
		}

		return null;
	}

	// May return null!
	public String getProperty(String name) {

		if (fProperties == null) {
			return null;
		}

		if (!fProperties.containsKey(name)) {
			return null;
		}

		return fProperties.get(name);
	}

	public String getProperty(String name, String defaultValue) {
		String property = getProperty(name);
		if (property == null) {
			return defaultValue;
		} else {
			return property;
		}
	}

	public Map<String, String> copyPropertiesRef(Leaf node) {
		fProperties = node.fProperties;
		return fProperties;
	}

	public void copyProperties(Leaf node) {

		if (node.hasProperties()) {
			for (String key : node.fProperties.keySet()) {
				if (Property.NAME.equals(key)) {
					if (node.getProperty(Property.NAME) != null) {
						continue; // leave name unchanged
					}
				} else if (Property.DESCRIPTION.equals(key)) {
					if (node.getProperty(Property.DESCRIPTION) != null) {
						continue; // leave description unchanged
					}
				}
				putProperty(key, node.getProperty(key));
			}
		}
	}

	public boolean isBooleanProperty(String name) {

		// Return true if the given property is true.
		return (String.valueOf(true).equals(getProperty(name, "")));
	}

	public void setBooleanProperty(String name, boolean value) {

		// Set the property to true/false.
		putProperty(name, String.valueOf(value));
	}

	// Required by the sorter, don't mess with it.
	public String toString() {
		return getName();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int compareTo(Leaf o) {
		return getName().compareTo(o.getName());
	}

	// ------------------------------------------------------------------------

	public static Leaf addNewChild(Node parent, String type) {

		assert (parent != null);

		Leaf node = new Leaf(type);
		parent.addChild(node);
		return node;
	}

	public static Leaf addNewChild(Node parent, Leaf from) {

		assert (parent != null);

		Leaf node = new Leaf(from);
		parent.addChild(node);
		return node;
	}

	public static Leaf addUniqueChild(Node parent, String type, String name) {

		assert (parent != null);

		Leaf node = parent.getChild(type, name);
		if (node == null) {

			node = new Leaf(type);
			parent.addChild(node);

			node.setName(name);
		}

		return node;
	}

	// ------------------------------------------------------------------------
}
