package RegAlloc;

import FlowGraph.FlowGraph;
import Graph.Node;
import Graph.NodeList;
import Temp.Temp;
import Temp.TempList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Liveness extends InterferenceGraph {

    private FlowGraph flowGraph;
    private HashMap<Temp, Node> tempNodeMap = new HashMap<>();
    private HashMap<Node, Temp> nodeTempMap = new HashMap<>();
    private MoveList moves = null;

    private HashMap<Node, Set<Temp>> in = new HashMap<>();
    private HashMap<Node, Set<Temp>> out = new HashMap<>();

    public Liveness(FlowGraph flow) {
        this.flowGraph = flow;

        // Initialize in and out sets
        for (NodeList p = flow.nodes(); p != null; p = p.tail) {
            in.put(p.head, new HashSet<>());
            out.put(p.head, new HashSet<>());
        }

        buildDataFlow();
        buildInterferenceGraph();
    }

    private void buildDataFlow() {
        boolean changed = true;
        // Compute in reverse order for faster convergence
        // However, a simple loop is sufficient for our small methods
        while (changed) {
            changed = false;
            for (NodeList p = flowGraph.nodes(); p != null; p = p.tail) {
                Node n = p.head;

                Set<Temp> inPrime = new HashSet<>(in.get(n));
                Set<Temp> outPrime = new HashSet<>(out.get(n));

                // out[n] = U_{s in succ[n]} in[s]
                Set<Temp> newOut = new HashSet<>();
                for (NodeList s = n.succ(); s != null; s = s.tail) {
                    newOut.addAll(in.get(s.head));
                }
                out.put(n, newOut);

                // in[n] = use[n] U (out[n] - def[n])
                Set<Temp> newIn = new HashSet<>();
                TempList uses = flowGraph.use(n);
                for (TempList t = uses; t != null; t = t.tail) {
                    newIn.add(t.head);
                }

                Set<Temp> outMinusDef = new HashSet<>(out.get(n));
                TempList defs = flowGraph.def(n);
                for (TempList t = defs; t != null; t = t.tail) {
                    outMinusDef.remove(t.head);
                }
                newIn.addAll(outMinusDef);
                in.put(n, newIn);

                if (!inPrime.equals(newIn) || !outPrime.equals(newOut)) {
                    changed = true;
                }
            }
        }
    }

    private void buildInterferenceGraph() {
        for (NodeList p = flowGraph.nodes(); p != null; p = p.tail) {
            Node n = p.head;
            TempList defs = flowGraph.def(n);
            Set<Temp> liveOut = out.get(n);
            boolean isMove = flowGraph.isMove(n);
            
            Temp moveSrc = null;
            if (isMove) {
                TempList uses = flowGraph.use(n);
                if (uses != null) {
                    moveSrc = uses.head;
                    // Add to move list
                    if (defs != null && defs.head != null) {
                         Node srcNode = tnode(moveSrc);
                         Node dstNode = tnode(defs.head);
                         moves = new MoveList(srcNode, dstNode, moves);
                    }
                }
            }

            for (TempList d = defs; d != null; d = d.tail) {
                Node defNode = tnode(d.head);
                for (Temp t : liveOut) {
                    if (t == d.head) continue;
                    if (isMove && t == moveSrc) continue;
                    
                    Node liveNode = tnode(t);
                    addEdge(defNode, liveNode);
                    addEdge(liveNode, defNode);
                }
            }
        }
    }

    @Override
    public Node tnode(Temp temp) {
        if (!tempNodeMap.containsKey(temp)) {
            Node n = this.newNode();
            tempNodeMap.put(temp, n);
            nodeTempMap.put(n, temp);
        }
        return tempNodeMap.get(temp);
    }

    @Override
    public Temp gtemp(Node node) {
        return nodeTempMap.get(node);
    }

    @Override
    public MoveList moves() {
        return moves;
    }
}
