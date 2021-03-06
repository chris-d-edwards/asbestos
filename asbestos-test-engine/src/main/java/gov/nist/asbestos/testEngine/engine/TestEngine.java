package gov.nist.asbestos.testEngine.engine;

import ca.uhn.fhir.parser.IParser;
import gov.nist.asbestos.client.Base.EC;
import gov.nist.asbestos.client.Base.ProxyBase;
import gov.nist.asbestos.client.client.FhirClient;
import gov.nist.asbestos.client.client.Format;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.resolver.Ref;
import gov.nist.asbestos.client.resolver.ResourceCacheMgr;
import gov.nist.asbestos.client.resolver.ResourceWrapper;
import gov.nist.asbestos.simapi.validation.Val;
import gov.nist.asbestos.simapi.validation.ValE;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;


import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * See http://hl7.org/fhir/testing.html
 */
public class TestEngine  {
    private File testDef = null;
    private URI sut = null;
    private TestScript testScript = null;
    private boolean failOverride = false;
    private FixtureMgr fixtureMgr = new FixtureMgr();
    private Val val;
    private ValE engineVal;
    private FhirClient fhirClientForFixtures;
    private TestReport testReport = new TestReport();
    private List<String> errors;
    private FhirClient fhirClient = null;
    private String testSession = null;
    private String channelId = null;
    private File externalCache = null;
    private String testCollection = null;

    public static final String LAST_OP = "_LAST_OP_";

    /**
     *
     * @param testDef  directory containing test definition
     * @param sut base address of fhir server under test
     */
    public TestEngine(File testDef, URI sut) {
        Objects.requireNonNull(testDef);
        Objects.requireNonNull(sut);
        this.testDef = testDef;
        this.sut = sut;
        // make test definition dir a temporary resource cache so elements of the TestScript
        // can be found
        ResourceCacheMgr inTestResources = new ResourceCacheMgr(testDef, new Ref(""));
        fhirClientForFixtures = new FhirClient().setResourceCacheMgr(inTestResources);
    }

    // used for evaluation
    public TestEngine(File testDef) {
        Objects.requireNonNull(testDef);
        this.testDef = testDef;
        ResourceCacheMgr inTestResources = new ResourceCacheMgr(testDef, new Ref(""));
        fhirClientForFixtures = new FhirClient().setResourceCacheMgr(inTestResources);
    }

    public TestEngine(File testDef, TestScript testScript) {
        this.testDef = testDef;
        this.testScript = testScript;
        ResourceCacheMgr inTestResources = new ResourceCacheMgr(testDef, new Ref(""));
        fhirClientForFixtures = new FhirClient().setResourceCacheMgr(inTestResources);
    }

    public TestEngine setTestSession(String testSession) {
        this.testSession = testSession;
        return this;
    }

    public TestEngine setChannelId(String channelId) {
        this.channelId = channelId;
        return this;
    }

    public TestEngine setExternalCache(File externalCache) {
        this.externalCache = externalCache;
        return this;
    }

    public TestEngine runTest() {
        Objects.requireNonNull(val);
        Objects.requireNonNull(testSession);
        Objects.requireNonNull(externalCache);
        engineVal = new ValE(val);
        engineVal.setMsg("TestEngine");
        try {
            doWorkflow();
        } catch (Throwable t) {
            reportException(t);
        }
        //returnTestReport();

        return this;
    }

    // TODO - This interpretation of TestScript does not use assert.direction for client tests.  Instead it uses separate fixtures for request and response. This is reflected in the tests written.

    // if inputResource == null then this is a test
    // if null then this is an evaluation
    public TestEngine runEval(ResourceWrapper requestResource, ResourceWrapper responseResource) {
        Objects.requireNonNull(val);
        Objects.requireNonNull(testSession);
        Objects.requireNonNull(externalCache);
        engineVal = new ValE(val);
        engineVal.setMsg("TestEngine");
        try {
            initWorkflow();
            doLoadFixtures();
            if (requestResource != null)
                fixtureMgr.put("request", new FixtureComponent(requestResource));
            if (responseResource != null)
                fixtureMgr.put("response", new FixtureComponent(responseResource));
            doTest(); // should only be asserts
            errorOut();
        } catch (Throwable t) {
            reportException(t);
        }
        //returnTestReport();

        return this;
    }

    public List<String> getTestReportErrors() {
        List<String> errors = new ArrayList<>();
        TestReport.TestReportSetupComponent testComponent = testReport.getSetup();
            for (TestReport.SetupActionComponent actionComponent : testComponent.getAction()) {
                if (actionComponent.hasAssert()) {
                    TestReport.SetupActionAssertComponent assertComponent = actionComponent.getAssert();
                    if (assertComponent.hasResult()) {
                        TestReport.TestReportActionResult actionResult = assertComponent.getResult();
                        if (actionResult.equals(TestReport.TestReportActionResult.ERROR) ||
                            actionResult.equals(TestReport.TestReportActionResult.FAIL))
                        errors.add(assertComponent.getMessage());
                    }
                }
            }

        for (TestReport.TestReportTestComponent testComponent1 : testReport.getTest()) {
            for (TestReport.TestActionComponent actionComponent : testComponent1.getAction()) {
                if (actionComponent.hasAssert()) {
                    TestReport.SetupActionAssertComponent assertComponent = actionComponent.getAssert();
                    if (assertComponent.hasResult()) {
                        TestReport.TestReportActionResult actionResult = assertComponent.getResult();
                        if (actionResult.equals(TestReport.TestReportActionResult.ERROR) ||
                                actionResult.equals(TestReport.TestReportActionResult.FAIL))
                            errors.add(assertComponent.getMessage());
                    }
                }
            }
        }
        return errors;
    }

    private void reportException(Throwable t) {
        String trace = ExceptionUtils.getStackTrace(t);
        TestReport.TestReportSetupComponent setup = testReport.getSetup();
        TestReport.SetupActionComponent comp = setup.addAction();
        TestReport.SetupActionAssertComponent asComp = new TestReport.SetupActionAssertComponent();
        asComp.setMessage(trace);
        asComp.setResult(TestReport.TestReportActionResult.ERROR);
        comp.setAssert(asComp);
        propagateStatus(testReport);
    }

    private void returnTestReport() {
        File logDir = new File(new File(externalCache, testSession), testDef.getName());
        logDir.mkdirs();
        TestReport testReport = getTestReport();
        String json = ProxyBase.encode(testReport, Format.JSON);
        Path path = new File(logDir, "TestReport.json").toPath();
        try (BufferedWriter writer = Files.newBufferedWriter(path))
        {
            writer.write(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void doWorkflow() {
        initWorkflow();

        try {
            doPreProcessing();
            if (errorOut()) return;
            doLoadVariables();
            if (errorOut()) return;
            doLoadFixtures();
            if (errorOut()) return;
            doAutoCreates();
            if (errorOut()) return;
            doSetup();
            if (errorOut()) return;
            doTest();
            if (errorOut()) return;
            doTearDown();
        } finally {
            doAutoDeletes();
            doPostProcessing();
            if (failOverride)
                testReport.setResult(TestReport.TestReportResult.FAIL);
        }
    }

    private void initWorkflow() {
        if (testScript == null)
            testScript = loadTestScript(testDef);
        testReport.setName(testScript.getName());
        testReport.setTestScript(new Reference(testScript.getId()));
        testReport.setIssued(new Date());
        TestReport.TestReportParticipantComponent part = testReport.addParticipant();
        part.setType(TestReport.TestReportParticipantType.SERVER);
        if (sut != null)
            part.setUri(sut.toString());
        part.setDisplay("NIST Asbestos Proxy");

        part = testReport.addParticipant();
        part.setType(TestReport.TestReportParticipantType.TESTENGINE);
        part.setUri("https://github.com/usnistgov/asbestos");
        part.setDisplay("NIST Asbestos TestEngine");
    }

    private boolean errorOut() {
//        propagateStatus(testReport);
        errors = doReportResult();
        if (hasError()) {
            doTearDown();
            return true;
        }
        return false;
    }

    public boolean hasError() {
        return testReport.hasResult() && testReport.getResult() == TestReport.TestReportResult.FAIL;
    }

    private List<String> doReportResult() {

        if (testReport.getStatus() == TestReport.TestReportStatus.ENTEREDINERROR) {
            // more complicated (TestScript dependency) - trust result
            return new ArrayList<>();
        }

        List<String> failingComponents = new ArrayList<>();

        TestReport.TestReportSetupComponent setup = testReport.getSetup();
        for (TestReport.SetupActionComponent action : setup.getAction()) {
            if (action.hasOperation()) {
                TestReport.SetupActionOperationComponent op = action.getOperation();
                TestReport.TestReportActionResult result = op.getResult();
                if (result == TestReport.TestReportActionResult.FAIL || result == TestReport.TestReportActionResult.ERROR)
                    failingComponents.add(op.getMessage());
            }
            if (action.hasAssert()) {
                TestReport.SetupActionAssertComponent as = action.getAssert();
                TestReport.TestReportActionResult result2 = as.getResult();
                if (result2 == TestReport.TestReportActionResult.FAIL || result2 == TestReport.TestReportActionResult.ERROR)
                    failingComponents.add(as.getMessage());
            }
        }
        for (TestReport.TestReportTestComponent test : testReport.getTest()) {
            for (TestReport.TestActionComponent action : test.getAction()) {
                if (action.hasOperation()) {
                    TestReport.SetupActionOperationComponent op = action.getOperation();
                    TestReport.TestReportActionResult result = op.getResult();
                    if (result == TestReport.TestReportActionResult.FAIL || result == TestReport.TestReportActionResult.ERROR)
                        failingComponents.add(op.getMessage());
                }
                if (action.hasAssert()) {
                    TestReport.SetupActionAssertComponent as = action.getAssert();
                    TestReport.TestReportActionResult result2 = as.getResult();
                    if (result2 == TestReport.TestReportActionResult.FAIL || result2 == TestReport.TestReportActionResult.ERROR)
                        failingComponents.add(as.getMessage());
                }
            }
        }
        TestReport.TestReportTeardownComponent teardown = testReport.getTeardown();
        for (TestReport.TeardownActionComponent action : teardown.getAction()) {
            if (action.hasOperation()) {
                TestReport.SetupActionOperationComponent op = action.getOperation();
                TestReport.TestReportActionResult result = op.getResult();
                if (result == TestReport.TestReportActionResult.FAIL || result == TestReport.TestReportActionResult.ERROR)
                    failingComponents.add(op.getMessage());
            }
        }

        testReport.setResult(
                failingComponents.isEmpty()
                        ? TestReport.TestReportResult.PASS
                        : TestReport.TestReportResult.FAIL
        );
        return failingComponents;
    }

    private void doPreProcessing() {

    }

    private void doLoadVariables() {
        if (testScript.hasVariable()) {
            ValE fVal = new ValE(engineVal).setMsg("Variables");

            for(TestScript.TestScriptVariableComponent comp : testScript.getVariable()) {
                if (!comp.hasName())
                    throw new Error("Variable defined without name");

            }
        }
    }


    private void doLoadFixtures() {
        Objects.requireNonNull(fhirClientForFixtures);
        if (testScript.hasFixture()) {
            ValE fVal = new ValE(engineVal).setMsg("Fixtures");

            try {
            for (TestScript.TestScriptFixtureComponent comp : testScript.getFixture()) {
                String id = comp.getId();
                if (id == null || id.equals("")) {
                    throw new Error("Static Fixture has no id and cannot be referenced");
                }
//                if (!comp.hasAutocreate())
//                    throw new Error("fixture.autocreate is a required field");
//                if (!comp.hasAutodelete())
//                    throw new Error("fixture.autodelete is a required field");
                Ref ref = new Ref(comp.getResource().getReference());
                Optional<ResourceWrapper> optWrapper = fhirClientForFixtures.readCachedResource(ref);

                // never happens - Throwable thrown if not found
                if (!optWrapper.isPresent())
                    throw new Error("Static Fixture " + ref + " cannot be loaded");
                ResourceWrapper wrapper = optWrapper.get();
                FixtureComponent fixtureComponent;
                try {
                    fixtureComponent = new FixtureComponent(id).setResource(wrapper).setVal(fVal).load(wrapper);
                    if (fixtureComponent != null)
                        fixtureMgr.put(id, fixtureComponent);
                } catch (Throwable e) {
                    throw new Error(e);
                }
            }
            } catch (Throwable t) {
                reportParsingError(testReport.addTest(), t.getMessage());
            }

        }
    }

    private void doAutoCreates() {
        if (testScript.hasFixture()) {
            ValE fVal = new ValE(engineVal).setMsg("Fixtures.autocreate");

            TestReport.TestReportSetupComponent setupReportComponent = testReport.getSetup();
            for (TestScript.TestScriptFixtureComponent comp : testScript.getFixture()) {
                if (comp.hasAutocreate()) {
                    if (comp.getAutocreate()) {
                        TestReport.SetupActionComponent actionReport = setupReportComponent.addAction();
                        Reference resource = comp.getResource();
                        String resourceType = resource.getType();
                        TestReport.SetupActionOperationComponent operationReport = actionReport.getOperation();
                        operationReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten
                        SetupActionCreate create = new SetupActionCreate(fixtureMgr)
                                .setFhirClient(fhirClient)
                                .setSut(sut)
                                .setType("fixture.autocreate")
                                .setVal(fVal)
                                .setVariableMgr(new VariableMgr(testScript, fixtureMgr)
                                        .setVal(fVal)
                                        .setOpReport(operationReport));
                        create.run(comp.getId(), comp.getResource(), operationReport);
                        if (propagateStatus(testReport))
                            return;  // fail
                    }
                }
            }
        }
    }

    private void doAutoDeletes() {
        if (testScript.hasFixture()) {
            ValE fVal = new ValE(engineVal).setMsg("Fixtures.autodelete");

            TestReport.TestReportTeardownComponent teardownReportComponent = testReport.getTeardown();
            for (TestScript.TestScriptFixtureComponent comp : testScript.getFixture()) {
                if (comp.hasAutodelete()) {
                    if (comp.getAutodelete()) {
                        TestReport.TeardownActionComponent actionReport = teardownReportComponent.addAction();
                        Reference resource = comp.getResource();
                        String resourceType = resource.getType();
                        TestReport.SetupActionOperationComponent operationReport = actionReport.getOperation();
                        operationReport.setResult(TestReport.TestReportActionResult.PASS);  // may be overwritten
                        SetupActionDelete delete = new SetupActionDelete(fixtureMgr)
                                .setFhirClient(fhirClient)
                                .setSut(sut)
                                .setType("fixture.autodelete")
                                .setVal(fVal)
                                .setVariableMgr(new VariableMgr(testScript, fixtureMgr)
                                        .setVal(fVal)
                                        .setOpReport(operationReport));
                        delete.run(comp.getId(), comp.getResource(), operationReport);
                        if (propagateStatus(testReport))
                            return;  // fail
                    }
                }
            }
        }
    }

    private void doSetup() {
        if (testScript.hasSetup()) {
            TestScript.TestScriptSetupComponent comp = testScript.getSetup();
            ValE fVal = new ValE(engineVal).setMsg("Setup");
            TestReport.TestReportSetupComponent setupReportComponent = testReport.getSetup();
            if (comp.hasAction()) {
                String typePrefix = "setup.action";
                for (TestScript.SetupActionComponent action : comp.getAction()) {
                    TestReport.SetupActionComponent actionReportComponent = setupReportComponent.addAction();
                    if (action.hasOperation() && action.hasAssert()) {
                        Reporter reporter = new Reporter(fVal, actionReportComponent.getOperation(), "", "");
                        reporter.reportError( "action has both operation and assertion");
                        return;
                    }
                    if (action.hasOperation())
                        doOperation(typePrefix, action.getOperation(), actionReportComponent.getOperation());
                    if (action.hasAssert()) {
                        TestReport.SetupActionAssertComponent actionReport = doAssert(typePrefix, action.getAssert());

                    }
                    if (hasError())
                        return;
                }
            }
        }
    }

//    private void doAction(String typePrefix, boolean isOperation, TestScript.SetupActionOperationComponent operation, TestReport.TestActionComponent actionReport, boolean isAssert, TestScript.SetupActionAssertComponent anAssert) {
//        if (isOperation) {
//            TestScript.SetupActionOperationComponent opComponent = operation;
//            TestReport.SetupActionOperationComponent opReport = actionReport.getOperation();
//            OperationRunner runner = new OperationRunner(fixtureMgr)
//                    .setVal(new ValE(val).setMsg(typePrefix))
//                    .setTypePrefix(typePrefix)
//                    .setFhirClient(fhirClient)
//                    .setSut(sut)
//                    .setTestReport(testReport)
//                    .setTestScript(testScript);
//            runner.run(opComponent, opReport);
//        } else if (isAssert) {
//            TestScript.SetupActionAssertComponent actionAssertComponent = anAssert;
//            TestReport.SetupActionAssertComponent assertComponent = actionReport.getAssert();
//            AssertionRunner runner = new AssertionRunner(fixtureMgr)
//                    .setVal(new ValE(val).setMsg(typePrefix))
//                    .setTypePrefix(typePrefix)
//                    .setTestReport(testReport)
//                    .setTestScript(testScript);
//            runner.run(actionAssertComponent, assertComponent);
//        }
//        propagateStatus(testReport);
//    }

    private void doOperation(String typePrefix, TestScript.SetupActionOperationComponent operation, TestReport.SetupActionOperationComponent report) {
        try {
            OperationRunner runner = new OperationRunner(fixtureMgr)
                    .setVal(new ValE(val).setMsg(typePrefix))
                    .setTypePrefix(typePrefix)
                    .setFhirClient(fhirClient)
                    .setSut(sut)
                    .setTestReport(testReport)
                    .setTestScript(testScript);
            runner.run(operation, report);
        } catch (Throwable t) {
            report.setMessage(ExceptionUtils.getStackTrace(t));
            report.setResult(TestReport.TestReportActionResult.ERROR);
        }
        propagateStatus(testReport);
    }

    private TestReport.SetupActionAssertComponent doAssert(String typePrefix, TestScript.SetupActionAssertComponent operation) {
        TestReport.SetupActionAssertComponent report;
        try {
            ValE vale = new ValE(val);
            AssertionRunner runner = new AssertionRunner(fixtureMgr)
                    .setVal(vale.setMsg(typePrefix))
                    .setTypePrefix(typePrefix)
                    .setVariableMgr(new VariableMgr(testScript, fixtureMgr)
                            .setVal(vale)
                            .setOpReport(testReport.getSetup().addAction().getOperation()))
//                    .setTestReport(testReport)
                    .setTestScript(testScript);
            report = runner.run(operation);
        } catch (Throwable t) {
            report = new TestReport.SetupActionAssertComponent();
            report.setMessage(ExceptionUtils.getStackTrace(t));
            report.setResult(TestReport.TestReportActionResult.ERROR);
        }
        return report;
    }

    private void doTest() {
        if (testScript.hasTest()) {
            ValE fVal = new ValE(engineVal).setMsg("Test");

            try {
                int testCounter = 1;
                for (TestScript.TestScriptTestComponent testComponent : testScript.getTest()) {
                    String testName = testComponent.getName();
                    if (testName == null || testName.equals(""))
                        testName = "Test" + testCounter;
                    testCounter++;
                    ValE tVal = new ValE(fVal).setMsg(testName);
                    TestReport.TestReportTestComponent testReportComponent = testReport.addTest();

                    // handle condition
                    TestScript containedTestScript = getConditional(testComponent, testReportComponent);
                    testReportComponent = testReport.addTest();
                    boolean conditionalResult = true;


                    if (containedTestScript != null) {
                        List<TestScript.TestScriptTestComponent> tests = containedTestScript.getTest();
                        if (tests.size() != 2) {
                            reportParsingError(testReportComponent, "test condition must contain two test elements");
                            return;
                        }

                        TestReport containedTestReport = new TestReport();

                        Extension extension = new Extension("https://github.com/usnistgov/asbestos/wiki/TestScript-Conditional",
                                new Reference(containedTestReport));
                        testReportComponent.addModifierExtension(extension);

                        // basic operation and validation
                        TestScript.TestScriptTestComponent basicOperationTest = tests.get(0);
                        TestReport.TestReportTestComponent containedTestReportComponent = containedTestReport.addTest();
                        boolean opResult = doTestPart(basicOperationTest, containedTestReportComponent, containedTestReport, false);

                        if (!opResult) {
                            failOverride = true;
//                        testReport.setResult(TestReport.TestReportResult.FAIL);
                            containedTestReportComponent = containedTestReport.addTest();
                            reportSkip(containedTestReportComponent);
                            reportSkip(testReportComponent);
                            return;
                        }
                        // asserts to trigger conditional
                        TestScript.TestScriptTestComponent conditionalTest = tests.get(1);

                        containedTestReportComponent = containedTestReport.addTest();
                        conditionalResult = doTestPart(conditionalTest, containedTestReportComponent, containedTestReport, true);

                        if (containedTestReport.getResult() == TestReport.TestReportResult.FAIL)
                            return;
                    }


                    if (conditionalResult) {
                        doTestPart(testComponent, testReportComponent, testReport, false);
                    } else {
                        reportSkip(testReportComponent);
                    }
                }
            } catch (Throwable t) {
                reportParsingError(testReport.addTest(), t.getMessage());
            }

        }
    }

    private void reportSkip(TestReport.TestReportTestComponent reportComponent) {
        TestReport.TestActionComponent testActionComponent = reportComponent.addAction();
        TestReport.SetupActionOperationComponent setupActionOperationComponent = testActionComponent.getOperation();
        setupActionOperationComponent.setResult(TestReport.TestReportActionResult.SKIP);
        setupActionOperationComponent.setMessage("skipped");
    }

    private boolean doTestPart(TestScript.TestScriptTestComponent testScriptElement, TestReport.TestReportTestComponent testReportComponent, TestReport testReport, boolean reportAsConditional) {
        ValE fVal = new ValE(engineVal).setMsg("Test");
        boolean result = true;
        if (testScriptElement.hasAction()) {
            String typePrefix = "contained.action";
            for (TestScript.TestActionComponent action : testScriptElement.getAction()) {
                TestReport.TestActionComponent actionReportComponent = testReportComponent.addAction();
                if (invalidAction(action, actionReportComponent, fVal))
                    return false;
                if (action.hasOperation()) {
                    doOperation(typePrefix, action.getOperation(), actionReportComponent.getOperation());
                    TestReport.SetupActionOperationComponent opReport = actionReportComponent.getOperation();
                    if (opReport.getResult() == TestReport.TestReportActionResult.ERROR) {
                        testReport.setStatus(TestReport.TestReportStatus.COMPLETED);
                        testReport.setResult(TestReport.TestReportResult.FAIL);
                        return false;
                    }
                }
                if (action.hasAssert()) {
                    TestReport.SetupActionAssertComponent actionReport = doAssert(typePrefix, action.getAssert());
                    actionReportComponent.setAssert(actionReport);
                    if ("fail".equals(actionReport.getResult().toCode())) {
                        if (reportAsConditional) {
                            testReport.setStatus(TestReport.TestReportStatus.ENTEREDINERROR);
                            testReport.setResult(TestReport.TestReportResult.PASS);
                        } else {
                            testReport.setStatus(TestReport.TestReportStatus.COMPLETED);
                            testReport.setResult(TestReport.TestReportResult.FAIL);
                        }
                        result = false;
                        //return false;  // don't jump ship on first assertion failure
                    }
                    if ("error".equals(actionReport.getResult().toCode())) {
                        testReport.setStatus(TestReport.TestReportStatus.COMPLETED);
                        testReport.setResult(TestReport.TestReportResult.FAIL);
                        return false;
                    }
                }
            }
        }
        return result;
    }

    private TestScript getConditional(TestScript.TestScriptTestComponent testComponent, TestReport.TestReportTestComponent testReportComponent) {
        if (testComponent.hasModifierExtension()) {
            Extension extension = testComponent.getModifierExtensionFirstRep();
            if (!extension.hasUrl() || !extension.getUrl().equals("https://github.com/usnistgov/asbestos/wiki/TestScript-Conditional")) {
                reportParsingError(testReportComponent, "Do not understand modifierExtension");
                return null;
            }
            if (extension.getValue() instanceof Reference) {
                Reference ref = (Reference) extension.getValue();
                if (ref.getResource() instanceof TestScript) {
                    return (TestScript) ref.getResource();
                }
            }
        }
        return null;
    }

        private void reportParsingError(TestReport.TestReportTestComponent testReportComponent, String message) {
            ValE fVal = new ValE(engineVal).setMsg("Test");
            TestReport.TestActionComponent actionReportComponent = testReportComponent.addAction();
            Reporter reporter = new Reporter(fVal, actionReportComponent.getOperation(), "", "");
            reporter.reportError( message);
            return;
        }

    private boolean invalidAction(TestScript.TestActionComponent action, TestReport.TestActionComponent actionReportComponent, ValE fVal) {
        if (action.hasOperation() && action.hasAssert()) {
            Reporter reporter = new Reporter(fVal, actionReportComponent.getOperation(), "", "");
            reporter.reportError( "action has both operation and assertion");
            return true;
        }
        return false;
    }

    private TestScript.TestScriptTestComponent findTest(TestScript testScript, String name) {
        for (TestScript.TestScriptTestComponent testComponent : testScript.getTest()) {
            if (name.equals(testComponent.getName()))
                return testComponent;
        }
        return null;
    }

    private void doTearDown() {
        if (testScript.hasTeardown()) {
            String typePrefix = "teardown";
            ValE fVal = new ValE(engineVal).setMsg("Teardown");
            TestReport.TestReportTeardownComponent teardownReportComponent = testReport.getTeardown();
            TestScript.TestScriptTeardownComponent testScriptTeardownComponent = testScript.getTeardown();
            if (testScriptTeardownComponent.hasAction()) {
                TestReport.TeardownActionComponent actionReport = teardownReportComponent.addAction();
                for (TestScript.TeardownActionComponent action : testScriptTeardownComponent.getAction()) {
                    if (action.hasOperation()) {
                        TestScript.SetupActionOperationComponent setupActionOperationComponent = action.getOperation();
                        if (action.hasOperation())
                            doOperation(typePrefix, action.getOperation(), actionReport.getOperation());

                        if (hasError())
                            return;
                    }
                }
            }
        }
    }

    private void doPostProcessing() {
        if (testCollection == null)
            return;
        if (sut == null)
            return;
        EC ec = new  EC(externalCache);
        Properties tcProperties = ec.getTestCollectionProperties(testCollection);
        String useCache = tcProperties.getProperty("cache");
        if (useCache == null || !useCache.equals("true"))
            return;
        for (TestReport.TestReportTestComponent testComponent : testReport.getTest()) {
            for (TestReport.TestActionComponent actionResult : testComponent.getAction()) {
                if (!actionResult.hasOperation())
                    continue;
                TestReport.SetupActionOperationComponent op = actionResult.getOperation();
                buildCacheEntry(op, ec);
            }
            if (testComponent.hasModifierExtension()) {
                Extension extension = testComponent.getModifierExtensionFirstRep();
                if (extension.getUrl().equals("https://github.com/usnistgov/asbestos/wiki/TestScript-Conditional")) {
                    if (extension.getValue() instanceof Reference) {
                        Reference reference = (Reference) extension.getValue();
                        if (reference.getResource() instanceof TestReport) {
                            TestReport containedTestReport = (TestReport) reference.getResource();
                            for (TestReport.TestReportTestComponent containedTestComponent : containedTestReport.getTest()) {
                                for (TestReport.TestActionComponent actionResult : containedTestComponent.getAction()) {
                                    if (actionResult.hasOperation()) {
                                        TestReport.SetupActionOperationComponent op = actionResult.getOperation();
                                        buildCacheEntry(op, ec);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void buildCacheEntry(TestReport.SetupActionOperationComponent op, EC ec) {
        if ("pass".equals(op.getResult().toCode())) {
            if (op.getMessage().startsWith("GET") || op.getMessage().startsWith("CREATE")) {
                URI uri;
                try {
                    uri = new URI(op.getDetail());
                } catch (URISyntaxException e) {
                    throw new Error(e);
                }
                UIEvent uiEvent = new UIEvent(ec).fromURI(uri);
                buildCacheEntry(uiEvent, ec);
            }
        }
    }

    private void buildCacheEntry(UIEvent uiEvent, EC ec) {
        if (uiEvent != null) {
            // add to cache
            File cacheDir = ec.getTestLogCacheDir(channelId);
            String responseBody = uiEvent.getClientTask().getResponseBody();
            BaseResource baseResource = ProxyBase.parse(responseBody, Format.fromContent(responseBody));
            if (baseResource instanceof Bundle) {
                Bundle bundle = (Bundle) baseResource;
                for (Bundle.BundleEntryComponent comp : bundle.getEntry()) {
                    if (comp.getResource() instanceof Patient) {
                        String fullUrl = comp.getFullUrl();
                        if (fullUrl != null && !fullUrl.equals("") && bundle.getTotal() == 1)
                            buildCacheEntry(cacheDir, bundle, (Patient) comp.getResource());
                    }
                }
            }
        }
    }

    private void buildCacheEntry(File cacheDir, Bundle bundle, Patient patient) {
        File resourceTypeFile = new File(cacheDir, "Patient");
        resourceTypeFile.mkdirs();

        String given = patient.getNameFirstRep().getGiven().get(0).toString();
        String family = patient.getNameFirstRep().getFamily();
        if (given != null &&!given.equals("") && family != null && !family.equals("")) {
            ProxyBase.toFile(bundle, resourceTypeFile, given + "_" + family, Format.JSON);
        }
    }

    public TestEngine addCache(File cacheDir) {
        fhirClientForFixtures.getResourceCacheMgr().addCache(cacheDir);
        return this;
    }

    public static TestScript loadTestScript(File testDefDir) {
        Objects.requireNonNull(testDefDir);
        File location = findTestScriptFile(testDefDir);
        InputStream is;
        try {
            is = new FileInputStream(location);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        IParser parser = (location.toString().endsWith("xml") ? ProxyBase.getFhirContext().newXmlParser() : ProxyBase.getFhirContext().newJsonParser());
        IBaseResource resource = parser.parseResource(is);
        assert resource instanceof TestScript;
        TestScript testScript = (TestScript) resource;
        testScript.setName(location.toString());
        return testScript;
    }

    public static File findTestScriptFile(File testDefDir) {
        File location = new File(testDefDir, "TestScript.xml");
        if (location.exists())
            return location;
        location = new File(testDefDir, "TestScript.json");
        if (location.exists())
            return location;
        location = new File(testDefDir, "../TestScript.xml");
        if (location.exists())
            return location;
        location = new File(testDefDir, "../TestScript.json");
        if (location.exists())
            return location;
        throw new RuntimeException("Cannot load TestScript (.xml or .json) from " + testDefDir + " or " + testDefDir + "/..");
    }

    private boolean isFixtureDefined(String id) {
        return fixtureMgr.containsKey(id);
    }

    private TestEngine addFixture(FixtureComponent fixtureComp) {
        fixtureMgr.put(fixtureComp.getId(), fixtureComp);
        return this;
    }

    FixtureMgr getFixtures() {
        return fixtureMgr;
    }

    private boolean fixturesOk() {
        for (FixtureComponent fixtureComp : fixtureMgr.values()) {
            if (!fixtureComp.IsOk())
                return false;
        }
        return true;
    }

    /**
     *
     * @param testReport
     * @return fail
     */
    boolean propagateStatus(TestReport testReport) {
        testReport.setResult(TestReport.TestReportResult.PASS);
        if (testReport.hasSetup()) {
            TestReport.TestReportSetupComponent setupComponent = testReport.getSetup();
            for (TestReport.SetupActionComponent setupActionComponent : setupComponent.getAction()) {
                if (setupActionComponent.hasOperation()) {
                    if (reportOnOperation(testReport, setupActionComponent.getOperation()))
                        return true;
                }
                if (setupActionComponent.hasAssert()) {
                    if (reportOnAssertion(testReport, setupActionComponent.getAssert()))
                        return true;
                }
            }
        }
        if (testReport.hasTest()) {
            for (TestReport.TestReportTestComponent testComponent : testReport.getTest()) {
                if (testComponent.hasAction()) {
                    for (TestReport.TestActionComponent testActionComponent : testComponent.getAction()) {
                        if (testActionComponent.hasOperation()) {
                            if (reportOnOperation(testReport, testActionComponent.getOperation()))
                                return true;
                        }
                        if (testActionComponent.hasAssert()) {
                            if (reportOnAssertion(testReport, testActionComponent.getAssert()))
                                return true;
                        }
                    }
                }
            }
        }
        if (testReport.hasTeardown()) {
            TestReport.TestReportTeardownComponent teardownActionComponent = testReport.getTeardown();
            for (TestReport.TeardownActionComponent teardownActionComponent1 : teardownActionComponent.getAction()) {
                if (teardownActionComponent1.hasOperation()) {
                    if (reportOnOperation(testReport, teardownActionComponent1.getOperation()))
                        return true;
                }
            }
        }
        testReport.setStatus(TestReport.TestReportStatus.COMPLETED);
        return false;
    }

    /**
     *
     * @param testReport
     * @param setupActionOperationComponent
     * @return fail
     */
    private boolean reportOnOperation(TestReport testReport, TestReport.SetupActionOperationComponent setupActionOperationComponent) {
        if (setupActionOperationComponent.hasResult()) {
            TestReport.TestReportActionResult testReportActionResult = setupActionOperationComponent.getResult();
            if (testReportActionResult == TestReport.TestReportActionResult.ERROR
                    || testReportActionResult == TestReport.TestReportActionResult.FAIL) {
                setupActionOperationComponent.setResult(testReportActionResult);
                testReport.setResult(TestReport.TestReportResult.FAIL);
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param testReport
     * @param setupActionAssertionComponent
     * @return fail
     */
    private boolean reportOnAssertion(TestReport testReport, TestReport.SetupActionAssertComponent setupActionAssertionComponent) {
        if (setupActionAssertionComponent.hasResult()) {
            TestReport.TestReportActionResult testReportActionResult = setupActionAssertionComponent.getResult();
            if (testReportActionResult == TestReport.TestReportActionResult.ERROR
                    || testReportActionResult == TestReport.TestReportActionResult.FAIL) {
                setupActionAssertionComponent.setResult(testReportActionResult);
                testReport.setResult(TestReport.TestReportResult.FAIL);
                return true;
            }
        }
        return false;
    }

    public TestEngine setVal(Val val) {
        this.val = val;
        return this;
    }

    public TestReport getTestReport() {
        return testReport;
    }

    public String getTestReportAsJson() {
        return ProxyBase
                .getFhirContext()
                .newJsonParser()
                .setPrettyPrint(true)
                .encodeResourceToString(testReport);
    }

    public List<String> getErrors() {
        return errors;
    }

    public TestScript getTestScript() {
        return testScript;
    }

    public FhirClient getFhirClient() {
        return fhirClient;
    }

    public TestEngine setFhirClient(FhirClient fhirClient) {
        this.fhirClient = fhirClient;
        return this;
    }

    public FixtureMgr getFixtureMgr() {
        return fixtureMgr;
    }

    public TestEngine setSut(URI sut) {
        this.sut = sut;
        return this;
    }

    public void setTestScript(TestScript testScript) {
        this.testScript = testScript;
    }

    public TestEngine setTestCollection(String testCollection) {
        this.testCollection = testCollection;
        return this;
    }
}
