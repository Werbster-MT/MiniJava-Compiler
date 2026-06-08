package symboltable;

import syntaxtree.Type;
import java.util.LinkedHashMap;

/** Representa um método: tipo de retorno, parâmetros e variáveis locais. */
public class MethodBinding {
    public final String name;
    public final Type   returnType;
    public final LinkedHashMap<String, Type> formals = new LinkedHashMap<>();
    public final LinkedHashMap<String, Type> locals  = new LinkedHashMap<>();

    public MethodBinding(String name, Type returnType) {
        this.name       = name;
        this.returnType = returnType;
    }

    public void addFormal(String name, Type type) { formals.put(name, type); }
    public void addLocal (String name, Type type) { locals .put(name, type); }

    /** Busca variável: locais → parâmetros. */
    public Type lookupVariable(String varName) {
        if (locals .containsKey(varName)) return locals .get(varName);
        if (formals.containsKey(varName)) return formals.get(varName);
        return null;
    }
}
