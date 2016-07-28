package javagi.casestudies.graph.impl;

public class AbstractEdge<Edge> {
    Edge source;
    Edge target;
    public AbstractEdge() {
    }
    public AbstractEdge(Edge s, Edge t) {
        source = s;
        target = t;
    }
}

