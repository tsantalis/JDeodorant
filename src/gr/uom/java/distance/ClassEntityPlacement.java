package gr.uom.java.distance;

public class ClassEntityPlacement {
    private int numberOfInnerEntities;
    private int numberOfOuterEntities;
    private double innerSum;
    private double outerSum;

    public ClassEntityPlacement(int numberOfInnerEntities, int numberOfOuterEntities, double innerSum, double outerSum) {
        this.numberOfInnerEntities = numberOfInnerEntities;
        this.numberOfOuterEntities = numberOfOuterEntities;
        this.innerSum = innerSum;
        this.outerSum = outerSum;
    }

    public ClassEntityPlacement() {
        this.numberOfInnerEntities = 0;
        this.numberOfOuterEntities = 0;
        this.innerSum = 0;
        this.outerSum = 0;
    }

    public Double getClassEntityPlacementValue() {
        if(numberOfInnerEntities > 0)
            return (innerSum/numberOfInnerEntities)/(outerSum/numberOfOuterEntities);
        else
            return null;
    }

    public int getNumberOfInnerEntities() {
        return numberOfInnerEntities;
    }

    public void setNumberOfInnerEntities(int numberOfInnerEntities) {
        this.numberOfInnerEntities = numberOfInnerEntities;
    }

    public int getNumberOfOuterEntities() {
        return numberOfOuterEntities;
    }

    public void setNumberOfOuterEntities(int numberOfOuterEntities) {
        this.numberOfOuterEntities = numberOfOuterEntities;
    }

    public double getInnerSum() {
        return innerSum;
    }

    public void setInnerSum(double innerSum) {
        this.innerSum = innerSum;
    }

    public double getOuterSum() {
        return outerSum;
    }

    public void setOuterSum(double outerSum) {
        this.outerSum = outerSum;
    }

    public String toString() {
        if(numberOfInnerEntities > 0)
            return String.valueOf((innerSum/numberOfInnerEntities)/(outerSum/numberOfOuterEntities));
        else
            return null;
    }
}
