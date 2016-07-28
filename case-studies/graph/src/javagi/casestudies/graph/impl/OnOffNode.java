package javagi.casestudies.graph.impl;

public class OnOffNode extends AbstractNode {
    boolean touchesOnOff(OnOffEdge e) {
        return e.enabled && touchesAbstract(e);
    }
}
