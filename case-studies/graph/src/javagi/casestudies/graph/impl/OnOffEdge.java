package javagi.casestudies.graph.impl;

public class OnOffEdge extends AbstractEdge<OnOffNode> {
    boolean enabled = false;
    public OnOffEdge() {
    }
    public OnOffEdge(OnOffNode source, OnOffNode target) {
        super(source, target);
    }
}

