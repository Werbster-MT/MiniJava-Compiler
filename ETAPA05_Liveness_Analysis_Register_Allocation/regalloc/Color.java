package RegAlloc;

import Graph.Node;
import Graph.NodeList;
import Temp.Temp;
import Temp.TempList;
import Temp.TempMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.Set;

public class Color implements TempMap {

    private InterferenceGraph ig;
    private TempMap initial;
    private TempList registers;
    private int K;

    private TempList spills = null;
    private HashMap<Node, Temp> coloredNodes = new HashMap<>();
    private HashMap<Temp, Temp> coloring = new HashMap<>();

    public Color(InterferenceGraph ig, TempMap initial, TempList registers) {
        this.ig = ig;
        this.initial = initial;
        this.registers = registers;
        
        int count = 0;
        for (TempList r = registers; r != null; r = r.tail) {
            count++;
        }
        this.K = count;

        buildPrecolored();
        simplifyAndColor();
    }

    private void buildPrecolored() {
        for (TempList r = registers; r != null; r = r.tail) {
            Temp reg = r.head;
            Node n = ig.tnode(reg); // Ensure it's in the graph
            coloredNodes.put(n, reg);
            coloring.put(reg, reg);
        }
        
        // Also precolor any other special registers in initial map (like fp, sp, ra)
        for (NodeList p = ig.nodes(); p != null; p = p.tail) {
            Node n = p.head;
            Temp t = ig.gtemp(n);
            if (initial.tempMap(t) != null && !coloring.containsKey(t)) {
                coloredNodes.put(n, t);
                coloring.put(t, t);
            }
        }
    }

    private void simplifyAndColor() {
        Stack<Node> selectStack = new Stack<>();
        HashSet<Node> activeNodes = new HashSet<>();
        
        for (NodeList p = ig.nodes(); p != null; p = p.tail) {
            Node n = p.head;
            if (!coloredNodes.containsKey(n)) {
                activeNodes.add(n);
            }
        }

        HashMap<Node, Integer> degrees = new HashMap<>();
        for (Node n : activeNodes) {
            int deg = 0;
            for (NodeList adj = n.adj(); adj != null; adj = adj.tail) {
                if (activeNodes.contains(adj.head) || coloredNodes.containsKey(adj.head)) {
                    deg++;
                }
            }
            degrees.put(n, deg);
        }

        // Simplification phase
        while (!activeNodes.isEmpty()) {
            Node toSimplify = null;
            for (Node n : activeNodes) {
                if (degrees.get(n) < K) {
                    toSimplify = n;
                    break;
                }
            }

            // Potential spill: optimistic coloring
            if (toSimplify == null) {
                toSimplify = activeNodes.iterator().next(); // Heuristic: pick any node
            }

            activeNodes.remove(toSimplify);
            selectStack.push(toSimplify);

            // Update degrees of neighbors
            for (NodeList adj = toSimplify.adj(); adj != null; adj = adj.tail) {
                Node neighbor = adj.head;
                if (activeNodes.contains(neighbor)) {
                    degrees.put(neighbor, degrees.get(neighbor) - 1);
                }
            }
        }

        // Coloring phase
        while (!selectStack.isEmpty()) {
            Node n = selectStack.pop();
            
            Set<Temp> neighborColors = new HashSet<>();
            for (NodeList adj = n.adj(); adj != null; adj = adj.tail) {
                Node neighbor = adj.head;
                if (coloredNodes.containsKey(neighbor)) {
                    neighborColors.add(coloredNodes.get(neighbor));
                }
            }

            Temp colorFound = null;
            for (TempList r = registers; r != null; r = r.tail) {
                if (!neighborColors.contains(r.head)) {
                    colorFound = r.head;
                    break;
                }
            }

            if (colorFound != null) {
                coloredNodes.put(n, colorFound);
                coloring.put(ig.gtemp(n), colorFound);
            } else {
                spills = new TempList(ig.gtemp(n), spills);
            }
        }
    }

    public TempList spills() {
        return spills;
    }

    @Override
    public String tempMap(Temp t) {
        Temp color = coloring.get(t);
        if (color != null) {
            return initial.tempMap(color);
        }
        return initial.tempMap(t); // Fallback for pre-colored or uncolored temps
    }
}
