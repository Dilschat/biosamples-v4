//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.10.05 at 09:09:35 AM BST 
//


package uk.ac.ebi.biosamples.model.legacyxml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for publicationType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="publicationType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="DOI" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}stringValueType" minOccurs="0"/&gt;
 *         &lt;element name="PubMedID" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}stringValueType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "publicationType", propOrder = {
    "doi",
    "pubMedID"
})
public class PublicationType {

    @XmlElement(name = "DOI")
    protected String doi;
    @XmlElement(name = "PubMedID")
    protected String pubMedID;

    /**
     * Gets the value of the doi property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDOI() {
        return doi;
    }

    /**
     * Sets the value of the doi property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDOI(String value) {
        this.doi = value;
    }

    /**
     * Gets the value of the pubMedID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPubMedID() {
        return pubMedID;
    }

    /**
     * Sets the value of the pubMedID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPubMedID(String value) {
        this.pubMedID = value;
    }

}
