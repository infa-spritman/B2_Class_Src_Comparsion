package ab.learn;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Level {
    int LENGTH = 12;
    double [][][]structure = new double[LENGTH][LENGTH][4];
    int bird;
    HashMap<Double, Double> angleScore = new HashMap<Double, Double>();

    public Level(double [][][]structure, int bird, HashMap<Double, Double> angleScore) {
        this.structure = structure;
        this.bird = bird;
        this.angleScore = angleScore;
    }

    public Level() {
    }

    public HashMap<Double, Double> getAngleScore() {
        return sortByValues(angleScore);        
    }

    public double getMaxScoreAngle() {
        double angle = -50;
        for (Map.Entry<Double,Double> entry: angleScore.entrySet()) {
            if (entry.getKey() > angle) angle = entry.getKey();
        }
        return angle;
    }

    public double distance(double [][][]structure) {
        double hammingDistance = 0.0;
        for (int i = 0; i < LENGTH; i++)
        for (int j = 0; j < LENGTH; j++)
        for (int k = 0; k < 4; k++)
            hammingDistance += Math.abs(this.structure[i][j][k] - structure[i][j][k]);
        return hammingDistance;
    }

    public static <K extends Comparable,V extends Comparable> HashMap<K,V> sortByValues(Map<K,V> map){
        List<Map.Entry<K,V>> entries = new LinkedList<Map.Entry<K,V>>(map.entrySet());
      
        Collections.sort(entries, new Comparator<Map.Entry<K,V>>() {

            @Override
            public int compare(Entry<K, V> o1, Entry<K, V> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
      
        //LinkedHashMap will keep the keys in the order they are inserted
        //which is currently sorted on natural ordering
        HashMap<K,V> sortedMap = new LinkedHashMap<K,V>();
      
        for(Map.Entry<K,V> entry: entries){
            sortedMap.put(entry.getKey(), entry.getValue());
        }
      
        return sortedMap;
    }
}
