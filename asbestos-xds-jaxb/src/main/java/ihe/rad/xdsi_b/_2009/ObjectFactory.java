//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2019.05.31 at 05:40:53 AM EDT 
//


package ihe.rad.xdsi_b._2009;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the ihe.rad.xdsi_b._2009 package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _RetrieveImagingDocumentSetRequest_QNAME = new QName("urn:ihe:rad:xdsi-b:2009", "RetrieveImagingDocumentSetRequest");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: ihe.rad.xdsi_b._2009
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link RetrieveImagingDocumentSetRequestType }
     * 
     */
    public RetrieveImagingDocumentSetRequestType createRetrieveImagingDocumentSetRequestType() {
        return new RetrieveImagingDocumentSetRequestType();
    }

    /**
     * Create an instance of {@link RetrieveImagingDocumentSetRequestType.StudyRequest }
     * 
     */
    public RetrieveImagingDocumentSetRequestType.StudyRequest createRetrieveImagingDocumentSetRequestTypeStudyRequest() {
        return new RetrieveImagingDocumentSetRequestType.StudyRequest();
    }

    /**
     * Create an instance of {@link RetrieveImagingDocumentSetRequestType.TransferSyntaxUIDList }
     * 
     */
    public RetrieveImagingDocumentSetRequestType.TransferSyntaxUIDList createRetrieveImagingDocumentSetRequestTypeTransferSyntaxUIDList() {
        return new RetrieveImagingDocumentSetRequestType.TransferSyntaxUIDList();
    }

    /**
     * Create an instance of {@link RetrieveImagingDocumentSetRequestType.StudyRequest.SeriesRequest }
     * 
     */
    public RetrieveImagingDocumentSetRequestType.StudyRequest.SeriesRequest createRetrieveImagingDocumentSetRequestTypeStudyRequestSeriesRequest() {
        return new RetrieveImagingDocumentSetRequestType.StudyRequest.SeriesRequest();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RetrieveImagingDocumentSetRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "urn:ihe:rad:xdsi-b:2009", name = "RetrieveImagingDocumentSetRequest")
    public JAXBElement<RetrieveImagingDocumentSetRequestType> createRetrieveImagingDocumentSetRequest(RetrieveImagingDocumentSetRequestType value) {
        return new JAXBElement<RetrieveImagingDocumentSetRequestType>(_RetrieveImagingDocumentSetRequest_QNAME, RetrieveImagingDocumentSetRequestType.class, null, value);
    }

}
