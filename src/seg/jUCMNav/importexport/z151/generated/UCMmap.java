//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.9-03/31/2009 04:14 PM(snajper)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.07.19 at 07:21:12 PM EDT 
//


package seg.jUCMNav.importexport.z151.generated;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for UCMmap complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="UCMmap">
 *   &lt;complexContent>
 *     &lt;extension base="{}UCMmodelElement">
 *       &lt;sequence>
 *         &lt;element name="singleton" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="parentStub" type="{http://www.w3.org/2001/XMLSchema}IDREF" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="contRefs" type="{}ComponentRef" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="connections" type="{}NodeConnection" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="nodes" type="{}PathNode" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="comments" type="{}Comment" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UCMmap", propOrder = {
    "singleton",
    "parentStub",
    "contRefs",
    "connections",
    "nodes",
    "comments"
})
public class UCMmap
    extends UCMmodelElement
{

    protected boolean singleton;
    @XmlElementRef(name = "parentStub", type = JAXBElement.class)
    protected List<JAXBElement<Object>> parentStub;
    protected List<ComponentRef> contRefs;
    protected List<NodeConnection> connections;
    protected List<PathNode> nodes;
    protected List<Comment> comments;

    /**
     * Gets the value of the singleton property.
     * 
     */
    public boolean isSingleton() {
        return singleton;
    }

    /**
     * Sets the value of the singleton property.
     * 
     */
    public void setSingleton(boolean value) {
        this.singleton = value;
    }

    /**
     * Gets the value of the parentStub property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the parentStub property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getParentStub().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link Object }{@code >}
     * 
     * 
     */
    public List<JAXBElement<Object>> getParentStub() {
        if (parentStub == null) {
            parentStub = new ArrayList<JAXBElement<Object>>();
        }
        return this.parentStub;
    }

    /**
     * Gets the value of the contRefs property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the contRefs property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getContRefs().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ComponentRef }
     * 
     * 
     */
    public List<ComponentRef> getContRefs() {
        if (contRefs == null) {
            contRefs = new ArrayList<ComponentRef>();
        }
        return this.contRefs;
    }

    /**
     * Gets the value of the connections property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the connections property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getConnections().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link NodeConnection }
     * 
     * 
     */
    public List<NodeConnection> getConnections() {
        if (connections == null) {
            connections = new ArrayList<NodeConnection>();
        }
        return this.connections;
    }

    /**
     * Gets the value of the nodes property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the nodes property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNodes().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PathNode }
     * 
     * 
     */
    public List<PathNode> getNodes() {
        if (nodes == null) {
            nodes = new ArrayList<PathNode>();
        }
        return this.nodes;
    }

    /**
     * Gets the value of the comments property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the comments property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getComments().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Comment }
     * 
     * 
     */
    public List<Comment> getComments() {
        if (comments == null) {
            comments = new ArrayList<Comment>();
        }
        return this.comments;
    }

}
