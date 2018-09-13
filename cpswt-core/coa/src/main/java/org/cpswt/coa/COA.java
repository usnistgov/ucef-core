package org.cpswt.coa;

import org.cpswt.coa.edge.COAEdge;
import org.cpswt.coa.node.COANode;

import java.util.HashMap;

public class COA {
    private final String name;
    private final HashMap<String, COANode> nodes;
    private final HashMap<String, COAEdge> edges;
    private COANode root;

    public COA(String name) {
        this.name = name;
        this.nodes = new HashMap<>();
        this.edges = new HashMap<>();
    }

    public void addNode(COANode node) {
        nodes.put(node.getId(), node);
    }

    public void addEdge(COAEdge edge) {
        edges.put(edge.getId(), edge);

        // TODO: "connect" nodes
        // TODO: update root node
    }
}
