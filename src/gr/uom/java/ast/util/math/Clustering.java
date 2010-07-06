package gr.uom.java.ast.util.math;

import gr.uom.java.distance.Entity;

import java.util.ArrayList;

public abstract class Clustering {

	protected ArrayList<ArrayList<Double>> distanceList;
	protected double[][] distanceMatrix;
	
	public static Clustering getInstance(int type, double[][] distanceMatrix, double threshold) {
		switch(type) {
		case 0:
			return new Hierarchical(distanceMatrix, threshold);
		default:
			return null;
		}
	}
	
	public abstract ArrayList<Cluster> clustering(ArrayList<Entity> entities);
	
	
}
