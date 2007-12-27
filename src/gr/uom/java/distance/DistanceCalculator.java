package gr.uom.java.distance;

import java.util.Set;
import java.util.HashSet;

public class DistanceCalculator {

    public static double getDistance(Set<String> set1, Set<String> set2) {
        if(set1.isEmpty() && set2.isEmpty())
            return 1.0;
        return 1.0 - (double)intersection(set1,set2).size()/(double)union(set1,set2).size();
    }

    public static Set<String> union(Set<String> set1, Set<String> set2) {
        Set<String> set = new HashSet<String>();
        set.addAll(set1);
        set.addAll(set2);
        return set;
    }

    public static Set<String> intersection(Set<String> set1, Set<String> set2) {
        Set<String> set = new HashSet<String>();
        set.addAll(set1);
        set.retainAll(set2);
        return set;
    }
}
