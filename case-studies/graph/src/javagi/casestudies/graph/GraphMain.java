package javagi.casestudies.graph;

// We place the implementation of the graphs into a different package
// to hide the internal touchesSimple and touchesOnOff methods.
// Every access should be performed via the Graph interface. This ensures
// that the casts needed to implement the Graph interface always
// succeed.

import javagi.casestudies.graph.impl.*;

public class GraphMain {

    static <N,E> void build(N n, E e, boolean b) where N*E implements Graph {
        e.setSource(n);
        e.setTarget(n);
        if (b == n.touches(e)) {
            System.out.println("OK");
        }
    }

    static <N,E> void listBuild(N[] ns, E[] es, boolean[] bs) where N*E implements Graph {
        for (int i = 0; i < ns.length; i++) {
            build(ns[i], es[i], bs[i]);
        }
    }

/*
    static buildFromPackages(GraphPackage[] pkgs, boolean[][] bss) {
        for (int i = 0; i < pkgs.length; i++) {
            GraphPackage pkg = pkgs[i];
            boolean[] bs = bss[i];
            Pair<N
        }
    }
*/
    public static void main(String[] args) {
        build(new SimpleNode(), new SimpleEdge(), true);
        build(new OnOffNode(), new OnOffEdge(), false);
        // build(new OnOffNode(), new SimpleEdge(), true); correctly rejected
        // build(new SimpleNode(), new OnOffEdge(), true); correctly rejected

        listBuild(new SimpleNode[]{new SimpleNode(),
                                   new SimpleNode()},
                  new SimpleEdge[]{new SimpleEdge(),
                                   new SimpleEdge()},
                  new boolean[]{true, true});

        listBuild(new OnOffNode[]{new OnOffNode(),
                                  new OnOffNode()},
                  new OnOffEdge[]{new OnOffEdge(),
                                  new OnOffEdge()},
                  new boolean[]{true, true});
    }
}
