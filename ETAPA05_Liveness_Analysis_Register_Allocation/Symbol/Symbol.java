package Symbol;

import java.util.HashMap;
import java.util.Map;

public class Symbol {
    private final String name;
    private static final Map<String, Symbol> table = new HashMap<>();

    private Symbol(String n) {
        name = n;
    }

    public static Symbol symbol(String n) {
        Symbol s = table.get(n);
        if (s == null) {
            s = new Symbol(n);
            table.put(n, s);
        }
        return s;
    }

    @Override
    public String toString() {
        return name;
    }
}
