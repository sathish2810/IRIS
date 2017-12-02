package com.temenos.interaction.core.entity;

/*
 * #%L
 * interaction-core
 * %%
 * Copyright (C) 2012 - 2013 Temenos Holdings N.V.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


import java.util.HashMap;
import java.util.Map;

/**
 * This class represents an unordered map of entity properties 
 */
public class EntityProperties implements EntityTreeNode {
	
	private EntityTreeNode parent;
	private Map<String, EntityProperty> properties = new HashMap<String, EntityProperty>();

	/**
	 * Gets the specified entity property.
	 * @param name Entity property name
	 * @return Entity property
	 */
	public EntityProperty getProperty(String name) {
		return properties.get(name);
	}
	
	/**
	 * Returns a list of entity properties
	 * @return
	 */
	public Map<String, EntityProperty> getProperties() {
		return properties;
	}
	
	/**
	 * Puts the specified entity property.
	 * @param property Entity property
	 */
	public void setProperty(EntityProperty property) {
		property.setParent(this);
		properties.put(property.getName(), property);
	}
	
	/**
	 * Return a comma-separated list of property-value pairs
	 */
	public String toString() {
		StringBuilder str = new StringBuilder();
		for(String prop : properties.keySet()) {
			if(str.length() > 0) {
				str.append(", ");
			}
			str.append(prop + " = " + properties.get(prop));
		}
		return str.toString();
	}

	@Override
	public String getFullyQualifiedName() {
		if (parent != null) {
			return parent.getFullyQualifiedName(); 
		}
		return "";
	}

	@Override
	public void setParent(EntityTreeNode parent) {
		this.parent = parent;
	}
}
