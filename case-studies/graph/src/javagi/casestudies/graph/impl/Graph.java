package javagi.casestudies.graph.impl;

public interface Graph [Node,Edge] {
    receiver Node {
        boolean touches(Edge e);
    }
    receiver Edge {
        void setSource(Node n);
        void setTarget(Node n);
        Node getSource();
        Node getTarget();
    }
}
