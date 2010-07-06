package gr.uom.java.ast.util.math;

import static gr.uom.java.ast.util.math.DoubleArray.deleteColumns;
import static gr.uom.java.ast.util.math.DoubleArray.deleteRows;
import static gr.uom.java.ast.util.math.DoubleArray.insertColumns;
import static gr.uom.java.ast.util.math.DoubleArray.insertRows;

import gr.uom.java.distance.Entity;

import java.util.ArrayList;

public class Hierarchical extends Clustering {
	
	private double threshold;
	
	public Hierarchical(double[][] distanceMatrix, double threshold) {
		this.distanceMatrix = distanceMatrix;
		this.threshold = threshold;
	}

	public ArrayList<Cluster> clustering(ArrayList<Entity> entities) {
		ArrayList<Cluster> clusters = new ArrayList<Cluster>();
		for(Entity entity : entities) {
			Cluster cluster = new Cluster();
			cluster.addEntity(entity);
			clusters.add(cluster);
		}
		while(clusters.size()>2) {
			double minVal = 2.0;
			int minRow = 0;
			int minCol = 1;
			for(int i=0; i<distanceMatrix.length;i++) {
				for(int j=0;j<distanceMatrix.length;j++) {
					if (i != j) {
						if (distanceMatrix[i][j] < minVal) {
							minVal = distanceMatrix[i][j];
							minRow = i;
							minCol = j;
						}
					}
				}
			}
			if(minVal >= threshold) break;
			
			if(minRow < minCol) {
				clusters.get(minRow).addEntities(clusters.get(minCol).getEntities());
				double[] newDistances = new double[distanceMatrix.length-1];
				for(int i=0;i<distanceMatrix.length;i++) {
						if (i != minCol) {
							if (i != minRow) {
								if (distanceMatrix[minRow][i] < distanceMatrix[minCol][i]) {
									if (i > minCol) {
										newDistances[i - 1] = distanceMatrix[minRow][i];
									} else {
										newDistances[i] = distanceMatrix[minRow][i];
									}
								} else {
									if (i > minCol) {
										newDistances[i - 1] = distanceMatrix[minCol][i];
									} else {
										newDistances[i] = distanceMatrix[minCol][i];
									}
								}
							}
							else {
								newDistances[i] = 0.0;
							}
						}
						
				}
				
				distanceMatrix = deleteRows(distanceMatrix, minRow, minCol);
				distanceMatrix = deleteColumns(distanceMatrix, minCol);
				distanceMatrix = insertRows(distanceMatrix, minRow, newDistances);
				distanceMatrix = deleteColumns(distanceMatrix, minRow);
				distanceMatrix = insertColumns(distanceMatrix, minRow, newDistances);
				clusters.remove(minCol);
			}
			else {
				clusters.get(minCol).addEntities(clusters.get(minRow).getEntities());
				double[] newDistances = new double[distanceMatrix.length-1];
				for(int i=0;i<distanceMatrix.length;i++) {
					if (i != minRow) {
						if (i != minCol) {
							if (distanceMatrix[minRow][i] < distanceMatrix[minCol][i]) {
								if (i > minRow) {
									newDistances[i - 1] = distanceMatrix[minRow][i];
								} else {
									newDistances[i] = distanceMatrix[minRow][i];
								}
							} else {
								if (i > minRow) {
									newDistances[i - 1] = distanceMatrix[minCol][i];
								} else {
									newDistances[i] = distanceMatrix[minCol][i];
								}
							}
						}
						else {
							newDistances[i] = 0.0;
						}
					}
					
				}
				distanceMatrix = deleteRows(distanceMatrix, minRow, minCol);
				distanceMatrix = deleteColumns(distanceMatrix, minCol);
				distanceMatrix = insertRows(distanceMatrix, minCol, newDistances);
				distanceMatrix = deleteColumns(distanceMatrix, minRow);
				distanceMatrix = insertColumns(distanceMatrix, minCol, newDistances);
				clusters.remove(minRow);
			}
		}
		return clusters;
	}

}
