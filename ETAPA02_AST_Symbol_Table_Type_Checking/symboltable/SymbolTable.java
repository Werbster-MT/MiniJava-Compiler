package symboltable;

import syntaxtree.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tabela de símbolos global: mapeia nomes de classes para ClassBinding. */
public class SymbolTable {
    private final Map<String, ClassBinding> classes = new LinkedHashMap<>();

    public void addClass(ClassBinding cb)         { classes.put(cb.name, cb); }
    public ClassBinding lookupClass(String name)  { return classes.get(name); }

    /**
     * Busca método na classe, seguindo cadeia de herança.
     */
    public MethodBinding lookupMethod(String className, String methodName) {
        ClassBinding cb = classes.get(className);
        if (cb == null) return null;
        if (cb.methods.containsKey(methodName)) return cb.methods.get(methodName);
        if (cb.superClass != null) return lookupMethod(cb.superClass, methodName);
        return null;
    }

    /**
     * Busca variável: locais/params do método → campos da classe → superclasse.
     */
    public Type lookupVariable(String varName, String className, String methodName) {
        if (className == null) return null;
        ClassBinding cb = classes.get(className);
        if (cb == null) return null;

        // 1. Escopo do método
        if (methodName != null) {
            MethodBinding mb = cb.methods.get(methodName);
            if (mb != null) {
                Type t = mb.lookupVariable(varName);
                if (t != null) return t;
            }
        }
        // 2. Campos da classe
        if (cb.fields.containsKey(varName)) return cb.fields.get(varName);

        // 3. Superclasse (sem escopo de método)
        if (cb.superClass != null)
            return lookupVariable(varName, cb.superClass, null);

        return null;
    }

    public Map<String, ClassBinding> getClasses() { return classes; }
}
