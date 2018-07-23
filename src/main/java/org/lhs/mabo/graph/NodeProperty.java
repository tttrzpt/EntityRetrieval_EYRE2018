package org.lhs.mabo.graph;

/**
 * Created by mabo on 2018-6-13.
 */
public class NodeProperty {

    private String propertyName;
    private Object propertyValue; // value could be String or GraphNode

    public NodeProperty(String propertyName, String propertyValue) {
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Object getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public void setPropertyValue(Object propertyValue) {
        this.propertyValue = propertyValue;
    }

}
