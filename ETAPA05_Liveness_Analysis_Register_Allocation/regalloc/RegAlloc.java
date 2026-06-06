package RegAlloc;

import Assem.InstrList;
import FlowGraph.AssemFlowGraph;
import Temp.Temp;
import Temp.TempMap;

public class RegAlloc implements TempMap {
    private Color color;
    public InstrList instrList;

    public RegAlloc(frame.Frame f, InstrList il) {
        this.instrList = il;
        
        AssemFlowGraph flowGraph = new AssemFlowGraph(il);
        Liveness liveness = new Liveness(flowGraph);
        
        TempMap initial = (TempMap) f;
        
        this.color = new Color(liveness, initial, f.registers());
        
        if (color.spills() != null) {
            System.err.println("Aviso: Spill detectado durante a alocacao de registradores. A compilacao gerada ignorara os spills.");
        }
    }

    @Override
    public String tempMap(Temp temp) {
        return color.tempMap(temp);
    }
}
