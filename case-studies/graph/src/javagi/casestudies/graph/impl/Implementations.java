package javagi.casestudies.graph.impl;

public implementation Graph[SimpleNode, SimpleEdge] {
    receiver SimpleNode {
        public boolean touches(SimpleEdge e) {
            return touchesAbstract(e);
        }
    }
    receiver SimpleEdge {
        public void setSource(SimpleNode n) {
            this.source = n;
        }
        public void setTarget(SimpleNode n) {
            this.target = n;
        }
        public SimpleNode getSource() {
            return this.source;
        }
        public SimpleNode getTarget() {
            return this.target;
        }
    }
}

public implementation Graph[OnOffNode, OnOffEdge] {
    receiver OnOffNode {
        public boolean touches(OnOffEdge e) {
            return touchesOnOff(e);
        }
    }
    receiver OnOffEdge {
        public void setSource(OnOffNode n) {
            this.source = n;
        }
        public void setTarget(OnOffNode n) {
            this.target = n;
        }
        public OnOffNode getSource() {
            return this.source;
        }
        public OnOffNode getTarget() {
            return this.target;
        }
    }
}
