package gov.nist.asbestos.proxyWar;

/**
 * Use this to control port for maven vs manual running of IT tests
 */
class ITConfig {
    private static final boolean forMavenBuild = true;

    static String getFhirPort() {
        // for running in automatic IT tests from Maven
        if (forMavenBuild)
            return "8877";
        // For running manually from IntelliJ with...
        // proxy running at appContext /proxy
        // fhir running in separate/external Tomcat
        return "8080";
    }

    static String getProxyPort() {
        // for running in automatic IT tests from Maven
        if (forMavenBuild)
            return "8877";
        // For running manually from IntelliJ with...
        // proxy running at appContext /proxy
        // fhir running in separate/external Tomcat
        return "8081";
    }

    static String getFhirBase() {
        return "http://localhost:" + getFhirPort() + "/fhir/fhir";
    }
}
