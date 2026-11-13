package com.abhrajit.dispatch.planning;

import com.abhrajit.dispatch.domain.DeliveryOrder;
import com.abhrajit.dispatch.domain.Priority;
import com.abhrajit.dispatch.domain.Vehicle;

import java.util.ArrayList;
import java.util.List;

/** The sample orders and vehicles from the assignment brief. */
final class SampleData {

    private SampleData() {
    }

    private static final Object[][] PLACES = {
            {"CP", 28.6139, 77.2090, "Connaught Place, Delhi, India"},
            {"KB", 28.7041, 77.1025, "Karol Bagh, Delhi, India"},
            {"NO", 28.5355, 77.3910, "Sector 18, Noida, Uttar Pradesh, India"},
            {"GG", 28.4595, 77.0266, "Cyber Hub, Gurgaon, Haryana, India"},
    };

    private static Object[] place(String key) {
        for (Object[] p : PLACES) if (p[0].equals(key)) return p;
        throw new IllegalArgumentException(key);
    }

    static List<DeliveryOrder> orders() {
        String[] at = "CP CP KB NO GG CP KB NO GG CP KB NO GG CP KB NO GG CP KB NO CP KB NO GG CP KB NO GG CP KB".split(" ");
        int[] weight = {15, 10, 20, 25, 30, 40, 20, 25, 15, 30, 20, 10, 25, 15, 20, 30, 15, 10, 20, 30, 25, 20, 30, 15, 20, 25, 20, 15, 30, 20};
        String[] pr = "H M L H M L H H M L H M H L H L M H L H M L H L M H M L H L".split(" ");
        List<DeliveryOrder> out = new ArrayList<>();
        for (int i = 0; i < at.length; i++) {
            Object[] p = place(at[i]);
            Priority priority = switch (pr[i]) { case "H" -> Priority.HIGH; case "M" -> Priority.MEDIUM; default -> Priority.LOW; };
            out.add(new DeliveryOrder(String.format("ORD%03d", i + 1), (double) p[1], (double) p[2], (String) p[3], weight[i], priority));
        }
        return out;
    }

    static List<Vehicle> vehicles() {
        Object[][] v = {{"VEH001", 100, "KB"}, {"VEH002", 80, "NO"}, {"VEH003", 120, "GG"}, {"VEH004", 90, "CP"}, {"VEH005", 110, "KB"}};
        List<Vehicle> out = new ArrayList<>();
        for (Object[] row : v) {
            Object[] p = place((String) row[2]);
            out.add(new Vehicle((String) row[0], (int) row[1], (double) p[1], (double) p[2], (String) p[3]));
        }
        return out;
    }
}
