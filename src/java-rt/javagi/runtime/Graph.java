package javagi.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Graph<V> {

    enum Color {
        White, Gray, Black;
    }
    
    Map<V, List<V>> edges = new HashMap<V, List<V>>();
    Set<V> vertices = new HashSet<V>();
    
    public Graph(Collection<V> vs) {
        vertices.addAll(vs);
    }
    
    public void addEdge(V from, V to) {
        if (!vertices.contains(from)) {
            throw new JavaGIRuntimeBug("Graph does not contain vertex " + from);
        }
        if (!vertices.contains(to)) {
            throw new JavaGIRuntimeBug("Graph does not contain vertex " + to);
        }
        List<V> l = edges.get(from);
        if (l == null) {
            l = new ArrayList<V>();
            edges.put(from, l);
        }
        l.add(to);
    }

    public <L extends List<V>> L topsort(L list) {
        Map<V, Color> colors = new HashMap<V,Color>();
        for (V v : vertices) {
            colors.put(v, Color.White);
        }
        for (V v : vertices) {
            if (Color.White == colors.get(v)) {
                dfsVisit(colors, list, v);
            }
        }
        return list;
    }
    
    private <L extends List<V>> void dfsVisit(Map<V, Color> colors, L res, V v) {
        colors.put(v, Color.Gray);
        List<V> targets = edges.get(v);
        if (targets != null) {
            for (V target : targets) {
                Color c = colors.get(target);
                if (c == Color.White) {
                    dfsVisit(colors, res, target);
                } else if (c == Color.Gray) {
                    throw new JavaGIRuntimeBug("Graph has cycles: " + toString());
                }
            }
        }
        colors.put(v, Color.Black);
        res.add(0, v);
    }

    public String toString() {
        return edges.toString();
    }
}
