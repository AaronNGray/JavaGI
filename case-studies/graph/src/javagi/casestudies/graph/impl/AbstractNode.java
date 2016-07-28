package javagi.casestudies.graph.impl;

public class AbstractNode {
    boolean touchesAbstract(AbstractEdge e) {
        return (e.source == this ||
                e.target == this);
    }
}
