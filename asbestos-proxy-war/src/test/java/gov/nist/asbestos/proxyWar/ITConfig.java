package gov.nist.asbestos.proxyWar;

/**
 * Use this to control port for maven vs manual running of IT tests
 */
class ITConfig {
    private static final boolean forMavenBuild = false;

    static String getFhirPort() {
        // for running in automatic IT tests from Maven
        if (forMavenBuild)
            return System.getProperty("fhir.port", "8080");
        // For running manually from IntelliJ with...
        // proxy running at appContext /proxy
        // fhir running in separate/external Tomcat
        return "8080"; //System.getProperty("fhir.port", "8080");
    }

    static String getProxyPort() {
        // for running in automatic IT tests from Maven
        if (forMavenBuild)
            return System.getProperty("proxy.port", "8081");
        // For running manually from IntelliJ with...
        // proxy running at appContext /proxy
        // fhir running in separate/external Tomcat
        return "8081"; //System.getProperty("proxy.port", "8081");
    }

    static String getFhirBase() {
        return "http://localhost:" + getFhirPort() + "/fhir/fhir";
    }
}
