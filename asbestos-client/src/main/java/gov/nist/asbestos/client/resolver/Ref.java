package gov.nist.asbestos.client.resolver;

import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class Ref {
    private URI uri;

    public Ref(URI uri) {
        Objects.requireNonNull(uri);
        this.uri = httpize(uri);
    }

    public Ref(String ref)  {
        Objects.requireNonNull(ref);
        uri = build(ref);
    }

    public Ref(URI base, String resourceType, SearchParms searchParms) {
        this(base.toString(), resourceType, searchParms.getParms());
    }

    public Ref(URI base, String resourceType, String id) {
        this(base.toString(), resourceType, id);
    }

    public Ref(String base, String resourceType, String id)  {
        String theRef;
        if (id == null || id.equals("") || id.startsWith("?"))
            theRef = String.join("/", base, resourceType);
        else
            theRef = String.join("/", base, resourceType, id);
        if (id.startsWith("?"))
            theRef = theRef + id;
        uri = build(theRef);
    }

    public Ref(Ref base, String resourceType, String id, String version)  {
        String theRef;
        if (id == null || id.equals(""))
            theRef = String.join("/", base.toString(), resourceType);
        else if (version == null || version.equals(""))
            theRef = String.join("/", base.toString(), resourceType, id);
        else
            theRef = String.join("/", base.toString(), resourceType, id, "_history", version);

        if (theRef.endsWith("/"))
            theRef = theRef.substring(0, theRef.length() - 1);
        uri = build(theRef);
    }

    public Ref(Reference reference) {
        Objects.requireNonNull(reference);
        uri = build(reference.getReference());
    }

    public String getParameters() {
        if (uri == null)
            return null;
        return uri.getQuery();
//        String[] parts = uri.toString().split("\\?");
//        if (parts.length == 0 || parts.length == 1)
//            return null;
//        return parts[1];
    }

    public Map<String, String> getParametersAsMap() {
        Map<String, String> map = new HashMap<>();

        String parms = getParameters();
        if (parms == null)
            return map;
        String[] parts = parms.split(";");
        for (int i=0; i<parts.length; i++) {
            String parm = parts[i];
            List<String> namevalue = Arrays.asList(parm.split("=", 2));
            if (!namevalue.get(0).equals(""))
                map.put(namevalue.get(0), namevalue.get(1));
        }
        return map;
    }

    private URI httpize(URI theUri) {
        String s = theUri.toString();
        try {
            if (s.startsWith("https"))
                theUri = new URI(s.replace("https", "http"));
        } catch (Exception e) {
            throw new Error(e);
        }
        return theUri;
    }

    public Ref httpizeTo(URI reference) {
        try {
            String port;
            if (reference.getPort() == -1) {
                if (this.uri.getPort() == -1) {
                    port = "";
                } else {
                    port = ":" + this.uri.getPort();
                }
            } else {
                port = ":" + reference.getPort();
            }
            return new Ref(new URI((reference.getScheme() == null ? "http" : reference.getScheme())
                    + "://"
                    + ((reference.getHost() == null) ? this.uri.getHost() : reference.getHost())
                    + port
                    + uri.getPath()));
        } catch (URISyntaxException e) {
            throw new Error(e);
        }
    }

    public Ref withHostPort(String hostPort) {
        String scheme = uri.getScheme();
        if (scheme == null)
            scheme = "http";
        String[] hp = hostPort.split(":");
        if (hp.length == 2) {
            String host = hp[0];
            String port = hp[1];
            try {
                return new Ref(new URI(scheme
                        + "://"
                        + host
                        + ":"
                        + port
                        + uri.getPath()));
            } catch (URISyntaxException e) {
                throw new Error(e);
            }
        }
        return this;  // oops
    }

    public boolean isContained() {
        return uri.toString().startsWith("#");
    }

    public Resource getContained(DomainResource domainResource) {
        if (!isContained())
            return null;
        String id = uri.toString();
        for (Resource resource1 : domainResource.getContained()) {
            if (id.equals(resource1.getId()))
                return resource1;
        }
        return null;
    }

    public String getId() {
        String path = uri.getPath();
        if (!path.contains("/")) return path;
        String[] parts = path.split("/");
        for (int i=0; i<parts.length-1; i++) {
            if (resourceNames.contains(parts[i])) {
                String id = parts[i+1];
                if (id.endsWith(".xml"))
                    id = id.replace(".xml","");
                if (id.endsWith(".json"))
                    id = id.replace(".json","");
                return id;
            }
        }
        return "";
    }

    public boolean hasId() {
        return !getId().equals("");
    }

    public String getResourceType() {
        String path = uri.getPath();
        String[] parts = path.split("/");
        for (int i=0; i<parts.length; i++) {
            if (resourceNames.contains(parts[i]))
                return parts[i];
        }
        return "";
    }

    public Ref getRelative() {  // without base
        String path = uri.getPath();
        List<String> parts = Arrays.asList(path.split("/"));
        for (int i=0; i<parts.size(); i++) {
            if (resourceNames.contains(parts.get(i)))
                return new Ref(String.join("/", parts.subList(i, parts.size())));
        }
        return new Ref("");
    }

    public Ref getBase() {
        String path = uri.toString();
        List<String> parts = Arrays.asList(path.split("/"));
        for (int i=0; i<parts.size(); i++) {
            if (resourceNames.contains(parts.get(i)))
                return new Ref(String.join("/", parts.subList(0, i)));
        }
        return new Ref(uri.toString());
    }

    // TODO needs test
    public boolean hasResource() {
        return ! ("".equals(getResourceType()));
    }

    // TODO needs test
    public Ref withResource(String type) {
        return new Ref(getBase(), type, null, null);
    }

    // TODO needs test?
    public Ref withNewId(String newId) {
        Objects.requireNonNull(newId);
        return new Ref(getBase(), getResourceType(), newId, null);
    }

    // TODO all needs tests history present
    public Ref rebase(String newBase) {
        Objects.requireNonNull(newBase);
        Ref theBase = new Ref(newBase).getBase();
        String resourceType = getResourceType();
        String id = getId();
        String version = getVersion();
        return new Ref(theBase, resourceType, id, version).httpizeTo(uri);
    }

    public Ref rebase(Ref newBase) {
        Objects.requireNonNull(newBase);
        return new Ref(newBase.getBase(), getResourceType(), getId(), getVersion()).httpizeTo(uri);
    }

    public Ref rebase(URI theUri) {
        Objects.requireNonNull(theUri);
        return rebase(new Ref(theUri));
    }

    public Ref getFull()  {  // without version
        String baseStr = getBase().toString();
        String resourceTypeStr = getResourceType();
        String idStr = getId();
        boolean hasScheme = uri.getScheme() != null;
        if (!hasScheme && baseStr != null && resourceTypeStr != null && !idStr.isEmpty())
            return new Ref(baseStr, resourceTypeStr, idStr);
        if (!hasScheme && resourceTypeStr != null && idStr.isEmpty())
            return new Ref(getResourceType());
        if (!hasScheme && resourceTypeStr != null)
            return new Ref(String.format("%s/%s", resourceTypeStr, idStr));
        if (uri.toString().contains("_history"))
            return new Ref(uri.toString().split("/_history", 2)[0]);
        return new Ref(uri);
    }

    public String getVersion() {
        String[] parts = uri.toString().split("_history/");
        if (parts.length < 2)
            return null;
        return parts[1];
    }

    public boolean isAbsolute() {
        if (uri == null) return false;
        if (uri.toString().startsWith("http")) return true;
        if (uri.toString().startsWith("file")) return true;
        return false;
    }

    public boolean isRelative() {
        if (uri == null) return false;
        return !isAbsolute();
    }

    @Override
    public String toString() {
        return uri.toString();
    }

    public String asString() { return uri.toString(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ref ref = (Ref) o;
        return Objects.equals(uri, ref.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    private URI build(String ref) {
        try {
            return httpize(new URI(ref));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public boolean isQuery() {
        return uri != null && uri.toString().contains("?");
    }

    public URI getUri() {
        return uri;
    }

    static private List<String> resourceNames = Arrays.asList(
            "CapabilityStatement",
            "StructureDefinition",
            "ImplementationGuide",
            "SearchParameter",
            "MessageDefinition",
            "OperationDefinition",
            "CompartmentDefinition",
            "StructureMap",
            "GraphDefinition",
            "ExampleScenario",
            "CodeSystem",
            "ValueSet",
            "ConceptMap",
            "NamingSystem",
            "TerminologyCapabilities",
            "Provenance",
            "AuditEvent",
            "Consent",
            "Composition",
            "DocumentManifest",
            "DocumentReference",
            "CatalogEntry",
            "Basic",
            "Binary",
            "Bundle",
            "Linkage",
            "MessageHeader",
            "OperationOutcome",
            "Parameters",
            "Subscription",
            "Patient",
            "Practitioner",
            "PractitionerRole",
            "RelatedPerson",
            "Person",
            "Group",
            "Organization",
            "OrganizationAffiliation",
            "HealthcareService",
            "Endpoint",
            "Location",
            "Substance",
            "BiologicallyDerivedProduct",
            "Device",
            "DeviceMetric",
            "Task",
            "Appointment",
            "AppointmentResponse",
            "Schedule",
            "Slot",
            "VerificationResult",
            "Encounter",
            "EpisodeOfCare",
            "Flag",
            "List",
            "Library",
            "AllergyIntolerance",
            "AdverseEvent",
            "Condition",
            "Procedure",
            "FamilyMemberHistory",
            "ClinicalImpression",
            "DetectedIssue",
            "Observation",
            "Media",
            "DiagnosticReport",
            "Specimen",
            "BodyStructure",
            "ImagingStudy",
            "QuestionnaireResponse",
            "MolecularSequence",
            "MedicationRequest",
            "MedicationAdministration",
            "MedicationDispense",
            "MedicationStatement",
            "Medication",
            "MedicationKnowledge",
            "Immunization",
            "ImmunizationEvaluation",
            "ImmunizationRecommendation",
            "CarePlan",
            "CareTeam",
            "Goal",
            "ServiceRequest",
            "NutritionOrder",
            "VisionPrescription",
            "RiskAssessment",
            "RequestGroup",
            "Communication",
            "CommunicationRequest",
            "DeviceRequest",
            "DeviceUseStatement",
            "GuidanceResponse",
            "SupplyRequest",
            "SupplyDelivery",
            "Coverage",
            "CoverageEigibilityRequest",
            "CoverageEligibilityResponse",
            "EnrollmentRequest",
            "EnrollmentResponse",
            "Claim",
            "ClaimResponse",
            "Invoice",
            "PaymentNotice",
            "PaymentReconciliation",
            "Account",
            "ChargeItem",
            "ChargeItemDefinition",
            "Contract",
            "ExplanationOfBenefit",
            "InsurancePlan",
            "ResearchStudy",
            "ResearchSubject",
            "ActivityDefinition",
            "DeviceDefinition",
            "EventDefinition",
            "ObservationDefinition",
            "PlanDefinition",
            "Questionnaire",
            "SpecimenDefinition",
            "ResearchDefinition",
            "ResearchElementDefinition",
            "Evidence",
            "EvidenceVariable",
            "EffectEvidenceSynthesis",
            "RiskEvidenceSynthesis",
            "Measure",
            "MeasureReport",
            "TestScript",
            "TestReport",
            "MedicinalProduct",
            "MedicinalProductAuthorization",
            "MedicinalProductContraindication",
            "MedicinalProductIndication",
            "MedicinalProductIngredient",
            "MedicinalProductInteraction",
            "MedicinalProductManufactured",
            "MedicinalProductPackaged",
            "MedicinalProductPharmaceutical",
            "MedicinalProductUndesirableEffect 0",
            "SubstancePolymer",
            "SubstanceReferenceInformation",
            "SubstanceSpecification",
            "metadata"
    );
}
