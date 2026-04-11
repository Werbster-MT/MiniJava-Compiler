package symboltable;

import syntaxtree.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/** Representa uma classe: campos e métodos. */
public class ClassBinding {
    public final String name;
    public final String superClass; // null se não tem extends
    public final Map<String, Type>          fields  = new LinkedHashMap<>();
    public final Map<String, MethodBinding> methods = new LinkedHashMap<>();

    public ClassBinding(String name, String superClass) {
        this.name       = name;
        this.superClass = superClass;
    }

    public void addField (String name, Type type)    { fields .put(name, type);       }
    public void addMethod(MethodBinding mb)           { methods.put(mb.name, mb);      }
    public MethodBinding getMethod(String name)       { return methods.get(name);      }
    public Type          getField (String name)       { return fields .get(name);      }
}
