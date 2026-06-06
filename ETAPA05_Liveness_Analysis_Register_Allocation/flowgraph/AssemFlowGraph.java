package FlowGraph;

import Assem.Instr;
import Assem.InstrList;
import Assem.LABEL;
import Assem.MOVE;
import Assem.Targets;
import Graph.Node;
import Temp.Label;
import Temp.LabelList;
import Temp.TempList;
import java.util.HashMap;

public class AssemFlowGraph extends FlowGraph {

    private HashMap<Node, Instr> nodeInstrMap = new HashMap<>();
    private HashMap<Instr, Node> instrNodeMap = new HashMap<>();

    public AssemFlowGraph(InstrList instrs) {
        // First pass: create nodes and label map
        HashMap<Label, Node> labelMap = new HashMap<>();
        InstrList curr = instrs;
        
        while (curr != null) {
            Node n = this.newNode();
            nodeInstrMap.put(n, curr.head);
            instrNodeMap.put(curr.head, n);
            
            if (curr.head instanceof LABEL) {
                labelMap.put(((LABEL)curr.head).label, n);
            }
            curr = curr.tail;
        }

        // Second pass: add edges
        curr = instrs;
        Node prevNode = null;
        for (Graph.NodeList p = this.nodes(); p != null; p = p.tail, curr = curr.tail) {
            Node n = p.head;
            Instr inst = curr.head;

            if (prevNode != null) {
                // If previous instruction falls through (no explicit targets)
                if (nodeInstrMap.get(prevNode).jumps() == null) {
                    this.addEdge(prevNode, n);
                }
            }

            Targets jumps = inst.jumps();
            if (jumps != null) {
                for (LabelList ll = jumps.labels; ll != null; ll = ll.tail) {
                    Node targetNode = labelMap.get(ll.head);
                    if (targetNode != null) {
                        this.addEdge(n, targetNode);
                    }
                }
            }
            prevNode = n;
        }
    }

    public Instr instr(Node n) {
        return nodeInstrMap.get(n);
    }

    @Override
    public TempList def(Node node) {
        return nodeInstrMap.get(node).def();
    }

    @Override
    public TempList use(Node node) {
        return nodeInstrMap.get(node).use();
    }

    @Override
    public boolean isMove(Node node) {
        return nodeInstrMap.get(node) instanceof MOVE;
    }
}
