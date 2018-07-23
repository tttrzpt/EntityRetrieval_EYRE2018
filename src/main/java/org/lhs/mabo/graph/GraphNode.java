package org.lhs.mabo.graph;

import java.util.List;
import java.util.Set;

/**
 * Created by mabo on 2018-6-13.
 */
public class GraphNode {

    private long id;
    private String nodeName;
    private Set<NodeProperty> properties;
    private Set<GraphRelationship> relationships;

    public GraphNode(long id){
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public String getNodeName() {
        return nodeName;
    }

    public Set<NodeProperty> getProperties() {
        return properties;
    }

    public Set<GraphRelationship> getRelationships() {
        return relationships;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public void setProperties(Set<NodeProperty> properties) {
        this.properties = properties;
    }

    public void setRelationships(Set<GraphRelationship> relationships) {
        this.relationships = relationships;
    }
}
