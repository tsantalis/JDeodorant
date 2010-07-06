package gr.uom.java.ast.util.math;

import gr.uom.java.distance.Entity;

import java.util.ArrayList;

public class Cluster {
	
	private ArrayList<Entity> entities;
	
	public Cluster() {
		entities = new ArrayList<Entity>();
	}
	
	public void addEntity(Entity entity) {
		if (!entities.contains(entity)) {
			entities.add(entity);
		}
	}

	public ArrayList<Entity> getEntities() {
		return entities;
	}
	
	public void addEntities(ArrayList<Entity> entities) {
		if (!this.entities.containsAll(entities)) {
			this.entities.addAll(entities);
		}
	}
	
	public boolean equals(Object o) {
		Cluster c = (Cluster)o;
		if(this.entities.size() == c.entities.size()) {
			int counter = 0;
			for(Entity entity : c.entities) {
				if(this.entities.contains(entity)) {
					counter++;
				}
			}
			if(counter == this.entities.size()) {
				return true;
			}
			else {
				return false;
			}
		}
		else{
			return false;
		}
	}

}
