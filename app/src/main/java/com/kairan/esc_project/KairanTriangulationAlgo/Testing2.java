package com.kairan.esc_project.KairanTriangulationAlgo;

import android.net.wifi.ScanResult;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.nostra13.universalimageloader.utils.L.i;

public class Testing2 {
    private List<String> bssid = new ArrayList<>();
    private HashMap<String, Integer> mac_rssi = new HashMap<>();
    private HashMap<Point, HashMap<String, Integer>> position_ap;
    static List<String> ap_list;
    static ArrayList<Point> positionSet;
    private String DownloadURL;

    public static final int K = 5;
    private final double alpha = 0.5;

    static FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    static DatabaseReference database = FirebaseDatabase.getInstance().getReference("ScanResults").child(user.getUid());
    static DatabaseReference MapUrls = FirebaseDatabase.getInstance().getReference("MapURLs").child(user.getUid());
    static HashMap<Point, HashMap<String,Integer>> Map1;


    public Testing2(HashMap<Point, HashMap<String, Integer>> mappingData, List<String> ap) {
        this.position_ap = mappingData;
        this.ap_list = ap;
        this.positionSet = new ArrayList<Point>(position_ap.keySet());
    }

    public Testing2(String Download){
        this.DownloadURL = Download;
    }

    /**
     * Initialize testing class with the scan result of the unknown position
     */
    public void get_data_for_testing(String URLlink){
        //retrieve data from database
        HashMap<Point,HashMap<String, Integer>> dataSet = new HashMap<>();
        final Point[] prediction = new Point[1];
        MapUrls.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snapshot1 : snapshot.getChildren()){
                    if (snapshot1.getValue().toString().equals(URLlink) ){
                        database.child(Objects.requireNonNull(snapshot1.getKey())).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                Map<String, Map> map = (Map<String, Map>) snapshot.getValue();
                                for (String key: map.keySet()){
                                    HashMap<String, Integer> rssivalues = new HashMap<>();
                                    String[] separated = key.split(",");
                                    Point p = new Point(Double.parseDouble(separated[0]),Double.parseDouble(separated[1]));
//                                            Log.i("Test",p.toString());
//                                            Log.i("Test", separated[0]);
//                                            Log.i("Test", separated[1]);
                                    for(Object key1: map.get(key).keySet()){
                                        rssivalues.put(key1.toString(), Integer.valueOf(map.get(key).get(key1).toString()));
                                    }
                                    dataSet.put(p,rssivalues);
                                }

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    public void setScanResult(List<ScanResult> scanResult) {
        for (int i = 0; i < scanResult.size(); i++) {
            String bssid = scanResult.get(i).BSSID;
            bssid = bssid.substring(0, bssid.length() - 1);
            if (!ap_list.contains(bssid)) {
                ap_list.add(bssid);
            }
            if (Math.abs(scanResult.get(i).level) < 70) {
                if (mac_rssi.keySet() != null && mac_rssi.keySet().contains(bssid)) {
                    int level = (mac_rssi.get(bssid) + scanResult.get(i).level) / 2;
                    mac_rssi.put(bssid, level);
                } else {
                    mac_rssi.put(bssid, scanResult.get(i).level);
                }
            }
        }
        bssid = new ArrayList<>(mac_rssi.keySet());
    }

    public boolean isEmpty() {
        return position_ap.isEmpty();
    }


    /**
     * Single Weighted Euclidean Distance-Based WKNN Algorithm:
     * https://www.ncbi.nlm.nih.gov/pmc/articles/PMC6567165/
     */
    public Point predict() {

        if (position_ap.isEmpty()) {
            return null;
        }

        Map<Point, Float> distance_point = new HashMap<Point,Float>();
        Map<Point, Float> similarity_point = new HashMap<Point,Float>();
        ArrayList<Point> position_list = new ArrayList<Point>(position_ap.keySet());
        Log.i("TTTTT",position_ap.toString());

        for (Point point : position_list) {
            System.out.println("point: " + point);
            System.out.println("length: " + position_ap.get(point).size());

            HashMap<String, Integer> x;
            int sum = 0;
            float similarity = 0;
            float x_length = 0;
            float y_length = 0;

            if (position_ap.get(point).size() > bssid.size()) {
                x = position_ap.get(point);
            } else {
                x = mac_rssi;
            }

            for (String j : x.keySet()) {
                if (position_ap.get(point).containsKey(j) && mac_rssi.containsKey(j)) {
                    sum += Math.pow((int) position_ap.get(point).get(j) - mac_rssi.get(j), 2);
                    similarity += position_ap.get(point).get(j) * mac_rssi.get(j) * 1f;
                } else {
                    sum += Integer.MAX_VALUE; // abs(rssi) is smaller, the signal is stronger => put a large number to indicate the ap is not detected in both positions
                    similarity += Float.MAX_VALUE;
                }
            }

            System.out.println("sum: " + sum);
            float dev = (float) Math.sqrt(sum);
            distance_point.put(point, dev);

            x_length = (float) Math.sqrt(x_length);
            y_length = (float) Math.sqrt(y_length);
            similarity = similarity / (x_length * y_length);
            System.out.println("similarity: " + similarity);
            similarity_point.put(point, similarity);
        }

        distance_point = sortByComparator(distance_point, true);
        similarity_point = sortByComparator(similarity_point, true);
        /*Point DP = getPoint(distance_point);
        Point SP = getPoint(similarity_point);

        Log.i("TTTTT","DP: "+DP.toString());
        Log.i("TTTTT","SP: "+SP.toString());

        return new Point(DP.getX() * alpha + SP.getX() * (1 - alpha), DP.getY() * alpha + SP.getY() * (1 - alpha));*/
        Point DP = new ArrayList<Point>(distance_point.keySet()).get(0);
        Point SP = new ArrayList<Point>(similarity_point.keySet()).get(0);
        Log.i("TTTTT","DP: "+DP.toString());
        Log.i("TTTTT","SP: "+SP.toString());
        return DP;


    }

    public Point getPoint(Map<Point, Float> input) {
        int count = 0;
        double x = 0;
        double y = 0;
        for (Point point : input.keySet()) {
            if (input.get(point) == 0) {
                x += point.getX();
                y += point.getY();
                count += 1;
            } else if (count != 0) {
                x = x / count;
                y = y / count;
                break;
            } else {
                x += point.getX() / K;
                y += point.getY() / K;
            }
        }
        return new Point(x, y);
    }


    public List<String> getBssid() {
        return bssid;
    }

    public HashMap<Point, HashMap<String, Integer>> getPosition_ap() {
        return position_ap;
    }

    private static Map<Point, Float> sortByComparator(Map<Point, Float> unsortMap, final boolean order) {

        List<Map.Entry<Point, Float>> list = new LinkedList<Map.Entry<Point, Float>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<Point, Float>>() {
            public int compare(Map.Entry<Point, Float> o1,
                               Map.Entry<Point, Float> o2) {
                if (order) {
                    return o1.getValue().compareTo(o2.getValue());
                } else {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<Point, Float> sortedMap = new LinkedHashMap<Point, Float>();
        for (Map.Entry<Point, Float> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
}