package gr.uom.java.distance;

import java.util.Map;
import java.util.LinkedHashMap;

public class SystemEntityPlacement {
    private Map<String,ClassEntityPlacement> entityPlacementMap;

    public SystemEntityPlacement() {
        entityPlacementMap = new LinkedHashMap<String,ClassEntityPlacement>();
    }

    public ClassEntityPlacement getClassEntityPlacement(String className) {
        if(entityPlacementMap.containsKey(className)) {
            return entityPlacementMap.get(className);
        }
        else {
            entityPlacementMap.put(className,new ClassEntityPlacement());
            return entityPlacementMap.get(className);
        }
    }

    public void putClassEntityPlacement(String className, ClassEntityPlacement classEntityPlacement) {
        entityPlacementMap.put(className,classEntityPlacement);
    }

    public double getSystemEntityPlacementValue() {
        double systemEntityPlacement = 0;
        for(ClassEntityPlacement entityPlacement : entityPlacementMap.values()) {
            Double value = entityPlacement.getClassEntityPlacementValue();
            if(value != null)
                systemEntityPlacement +=
                ((double)entityPlacement.getNumberOfInnerEntities()/(double)(entityPlacement.getNumberOfInnerEntities()+entityPlacement.getNumberOfOuterEntities())) * value;
        }
        return systemEntityPlacement;
    }
}
