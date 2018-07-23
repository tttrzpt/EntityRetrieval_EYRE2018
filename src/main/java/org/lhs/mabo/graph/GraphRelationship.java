package org.lhs.mabo.graph;

/**
 * Created by mabo on 2018-6-13.
 */
public class GraphRelationship {

    private String relationshipName;
    private GraphNode startNode;
    private GraphNode endNode;

    public GraphRelationship(String relationshipName) {
        this.relationshipName = relationshipName;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public GraphNode getStartNode() {
        return startNode;
    }

    public GraphNode getEndNode() {
        return endNode;
    }

    public void setRelationshipName(String relationshipName) {
        this.relationshipName = relationshipName;
    }

    public void setStartNode(GraphNode startNode) {
        this.startNode = startNode;
    }

    public void setEndNode(GraphNode endNode) {
        this.endNode = endNode;
    }
}
