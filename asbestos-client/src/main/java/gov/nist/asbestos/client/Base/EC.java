package gov.nist.asbestos.client.Base;

import com.google.gson.Gson;
import gov.nist.asbestos.client.events.EventSummary;
import gov.nist.asbestos.client.events.UIEvent;
import gov.nist.asbestos.client.log.SimStore;
import gov.nist.asbestos.simapi.simCommon.SimId;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static gov.nist.asbestos.client.Base.Dirs.listOfDirectories;
import static gov.nist.asbestos.client.Base.Dirs.listOfFiles;

public class EC {
    private static Logger log = Logger.getLogger(EC.class);
    public File externalCache;

    public static final String MarkerType = "Marker";
    public static final String TEST_COLLECTIONS_DIR = "FhirTestCollections";
    public static final String TEST_ASSERTIONS_DIR = "FhirTestAssertions";
    public static final String TEST_ASSERTIONS_FILE = "assertions.json";
    public static final String CHANNELS_DIR = "FhirChannels";
    public static final String DOCUMENT_CACHE = "FhirDocCache";


    public EC(File externalCache) {
        this.externalCache = externalCache;
    }

     public List<String> getTestCollectionNames() {
        return getTestCollections().stream().map(File::getName).collect(Collectors.toList());
    }

     List<File> getTestCollections() {
//        URL aUrl = EC.class.getClassLoader().getResource("/TestCollections/testCollectionRoot.txt");
//        String aFile = aUrl.getFile();
//        File internalRoot = new File(aFile).getParentFile();
//        List<File> intList = listOfFiles(internalRoot);
         List<File> collections = new ArrayList<>();

        File externalRoot = new File(externalCache, EC.TEST_COLLECTIONS_DIR);
        List<File> extList = listOfFiles(externalRoot);
        collections.addAll(extList);

        return collections;
    }

     public List<String> getTestsInCollection(String collectionName) {
        return getTests(collectionName).stream().map(File::getName).collect(Collectors.toList());
    }

    public File getTest(String collectionName, String testName) {
        List<File> tests = getTests(collectionName);

        for (File test : tests) {
            if (test.getName().equalsIgnoreCase(testName)) return test;
        }
        return null;
    }

     public List<File> getTests(String collectionName) {
        File root = getTestCollectionBase(collectionName);
        if (root == null)
            return new ArrayList<>();
        return listOfDirectories(root);
    }

    private static Properties defaultProperties = new Properties();
    static {
        defaultProperties.setProperty("TestType", "server");
    }

    public Properties getTestCollectionProperties(String collectionName) {
        Properties props = new Properties();
        File root = getTestCollectionBase(collectionName);
        if (root == null)
            return props;
        File file = new File(root, "TestCollection.properties");
        try {
            props.load(new FileInputStream(file));
        } catch (IOException e) {
            return defaultProperties;
        }
        return props;
    }

    public String getTestCollectionDescription(String collectionName) {
        File root = getTestCollectionBase(collectionName);
        if (root == null)
            throw new Error("Test Collection " + collectionName + " not found");
        File file = new File(root, "description.txt");
        if (!file.exists())
            return null;
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public File getTestAssertionsFile() {
        return new File(new File(externalCache, TEST_ASSERTIONS_DIR), TEST_ASSERTIONS_FILE);
    }

    File getTestCollectionBase(String collectionName) {
        return externalTestCollectionBase(collectionName);
    }

     File externalTestCollectionBase(String collectionName) {
        File externalRoot = new File(externalCache, TEST_COLLECTIONS_DIR);
        if (!externalRoot.exists()) return null;
        if (!externalRoot.isDirectory()) return null;
        File collectionRoot = new File(externalRoot, collectionName);
        if (!collectionRoot.exists()) return null;
        if (!collectionRoot.isDirectory()) return null;
        return collectionRoot;
    }

     public File getTestLog(String channelId, String collectionName, String testName) {
        File testLogs = new File(externalCache, "FhirTestLogs");
        File forChannelId = new File(testLogs, channelId);
        File forCollection = new File(forChannelId, collectionName);
        forCollection.mkdirs();
        return new File(forCollection, testName + ".json");
    }

    // channelId is testSession__channel
    public File getTestLogDir(String channelId, String collectionName) {
        File testLogs = new File(externalCache, "FhirTestLogs");
        File forChannelId = new File(testLogs, channelId);
        File forCollection = (collectionName == null) ? forChannelId : new File(forChannelId, collectionName);
        forCollection.mkdirs();
        return forCollection;
    }

    public File getTestLogCacheDir(String channelId) {
        return new File(getTestLogDir(channelId, null), "cache");
    }

    // channelId is testSession__channel
    public List<File> getTestLogs(String channelId, String collectionName) {
        File testLogs = new File(externalCache, "FhirTestLogs");
        File forTestSession = new File(testLogs, channelId);
        File forCollection = new File(forTestSession, collectionName);

        List<File> testLogList = new ArrayList<>();
        File[] tests = forCollection.listFiles();
        if (tests != null) {
            for (File test : tests) {
                String name = test.toString();
                if (!name.endsWith(".json")) continue;
                if (name.startsWith(".")) continue;
                if (name.startsWith("_")) continue;
                testLogList.add(test);
            }
        }
        log.info("got " + testLogList.size() + " test logs from " + testLogs.toString());
        return testLogList;
    }

    public File getResourceType(String testSession, String channelId, String resourceType) {
        File fhir = fhirDir(testSession, channelId);
        return new File(fhir, resourceType);
    }

    public void buildJsonListingOfEvent(HttpServletResponse resp, String testSession, String channelId, String resourceType, String eventName) {
        UIEvent uiEvent = getEvent(testSession, channelId, resourceType, eventName);
        if (uiEvent == null) {
            resp.setStatus(resp.SC_NOT_FOUND);
            return;
        }

        String json = new Gson().toJson(uiEvent);
        resp.setContentType("application/json");
        try {
            resp.getOutputStream().write(json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        resp.setStatus(resp.SC_OK);
    }

    public UIEvent getEvent(String testSession, String channelId, String resourceType, String eventName) {
        File fhir = fhirDir(testSession, channelId);
        if (resourceType.equals("null")) {
            resourceType = resourceTypeForEvent(fhir, eventName);
            if (resourceType == null) {
                return null;
            }
        }
        File resourceTypeFile = new File(fhir, resourceType);
        File eventDir = new File(resourceTypeFile, eventName);

        UIEvent uiEvent = new UIEvent(new EC(externalCache)).fromEventDir(eventDir);
        uiEvent.setEventName(eventName);
        uiEvent.setResourceType(resourceType);
        return uiEvent;
    }

    public File fhirDir(String testSession, String channelId) {
        File psimdb = new File(externalCache, SimStore.PSIMDB);
        File testSessionFile = new File(psimdb, testSession);
        File channelFile = new File(testSessionFile, channelId);
        return new File(channelFile, "fhir");
    }

    public String resourceTypeForEvent(File fhir, String eventName) {
        File[] resourceTypeFiles = fhir.listFiles();
        if (resourceTypeFiles != null) {
            for (File resourceTypeDir : resourceTypeFiles) {
                File[] eventFiles = resourceTypeDir.listFiles();
                if (eventFiles != null) {
                    for (File eventFile : eventFiles) {
                        if (eventFile.getName().equals(eventName)) {
                            return resourceTypeDir.getName();
                        }
                    }
                }
            }
        }
        return null;
    }

    public void buildJsonListingOfEvents(HttpServletResponse resp, String testSession, String channelId, String resourceType) {
        File fhir = new EC(externalCache).fhirDir(testSession, channelId);
        File resourceTypeFile = new File(fhir, resourceType);

        List<String> events = Dirs.dirListingAsStringList(resourceTypeFile);
        returnJsonList(resp, events);
    }

    public void returnJsonList(HttpServletResponse resp, List<String> theList) {
        String json = new Gson().toJson(theList);
        resp.setContentType("application/json");
        try {
            resp.getOutputStream().print(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        resp.setStatus(resp.SC_OK);
    }

    public void buildJsonListingOfEventSummaries(HttpServletResponse resp, String testSession, String channelId) {
        File fhir = new EC(externalCache).fhirDir(testSession, channelId);
        List<String> resourceTypes = Dirs.dirListingAsStringList(fhir);
        List<EventSummary> eventSummaries = new ArrayList<>();
        for (String resourceType : resourceTypes) {
            File resourceDir = new File(fhir, resourceType);
            List<String> eventIds = Dirs.dirListingAsStringList(resourceDir);
            for (String eventId : eventIds) {
                File eventFile = new File(resourceDir, eventId);
                EventSummary summary = new EventSummary(eventFile);
                summary.resourceType = resourceType;
                summary.eventName = eventId;
                eventSummaries.add(summary);
            }
        }
        String json = new Gson().toJson(eventSummaries);
        resp.setContentType("application/json");
        try {
            resp.getOutputStream().print(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        resp.setStatus(resp.SC_OK);
    }

    public void buildJsonListingOfResourceTypes(HttpServletResponse resp, String testSession, String channelId) {
        File fhir = new EC(externalCache).fhirDir(testSession, channelId);

        List<String> resourceTypes = Dirs.dirListingAsStringList(fhir);
        new EC(externalCache).returnJsonList(resp, resourceTypes);
    }

    public String getLastMarker(String testSession, String channelId) {
        File markerDir = getResourceType(testSession, channelId, MarkerType);
        List<String> markers = Dirs.dirListingAsStringList(markerDir);
        if (markers.size() == 0) {
            return null;
        } else if (markers.size() == 1) {
            return markers.get(0);
        } else {
            markers.sort(String.CASE_INSENSITIVE_ORDER);
            return markers.get(markers.size() - 1);
        }
    }

    public List<File> getEventsSince(SimId simId, String marker) {
        List<File> eventsList = new ArrayList<>();
        SimStore simStore = new SimStore(externalCache, simId);
        List<File> resourceTypeDirs = simStore.getResourceTypeDirs();
        for (File resourceTypeDir : resourceTypeDirs) {
            List<File> events = Dirs.listOfDirectories(resourceTypeDir);
            for (File event : events) {
                String eventName = event.getName();
                if (marker == null)
                    eventsList.add(event);
                else if (eventName.compareTo(marker) > 0)
                    eventsList.add(event);
            }
        }
        return eventsList;
    }

    public File getEvent(SimId simId, String eventId) {
        SimStore simStore = new SimStore(externalCache, simId);
        List<File> resourceTypeDirs = simStore.getResourceTypeDirs();
        for (File resourceTypeDir : resourceTypeDirs) {
            List<File> events = Dirs.listOfDirectories(resourceTypeDir);
            for (File event : events) {
                String eventName = event.getName();
                if (eventName.equals(eventId))
                    return event;
            }
        }
        return null;
    }

    public File getDocumentCache() {
        return new File(externalCache, DOCUMENT_CACHE);
    }


    public File getCodesFile(String environment) {
        return new File(new File(new File(externalCache, "environment"), environment), "codes.xml");
    }
}
