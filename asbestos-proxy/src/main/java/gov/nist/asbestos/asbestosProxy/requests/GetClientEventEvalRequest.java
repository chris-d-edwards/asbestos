package gov.nist.asbestos.asbestosProxy.requests;


// 0 - empty
// 1 - appContext
// 2 - "engine"
// 3 - "clienteventeval"
// 4 - channelName   testSession__channelId
// 5 - testCollectionId
// 6 - testId
// 7 - eventId
// Run a client eval against single event

import gov.nist.asbestos.client.events.Event;
import gov.nist.asbestos.simapi.simCommon.SimId;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class GetClientEventEvalRequest {
    private static Logger log = Logger.getLogger(GetClientEventEvalRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return  request.uriParts.get(3).equals("clienteventeval") && request.uriParts.size() == 8;
    }

    public GetClientEventEvalRequest(Request request) {
        this.request = request;
    }

    public void run() {
        log.info("GetClientEventEvalRequest");
        request.parseChannelName(4);
        String testCollection = request.uriParts.get(5);
        String testId = request.uriParts.get(6);
        String channelName = request.uriParts.get(4);
        String eventId = request.uriParts.get(7);

        File testDir = request.ec.getTest(testCollection, testId);
        List<File> testDirs = Collections.singletonList(testDir);

//        Map<String, File> testIds = testDirs.stream().collect(Collectors.toMap(File::getName, x -> x));
//        // testId -> testScript
//        Map<String, TestScript> testScripts = testDirs.stream().collect(
//                Collectors.toMap(File::getName, TestEngine::loadTestScript)
//        );

        SimId simId = SimId.buildFromRawId(channelName);
        String testSession = simId.getTestSession().getValue();
        File eventFile = request.ec.getEvent(simId, eventId);
        Event event = new Event(eventFile);

        GetClientTestEvalRequest getClientTestEvalRequest = new GetClientTestEvalRequest(request);

        StringBuilder buf = getClientTestEvalRequest.evalClientTest(testDirs, testSession, Collections.singletonList(event));

        String myStr = buf.toString();

        File testLogDir = request.ec.getTestLogDir(request.fullChannelId(), testCollection);
        try {
            Files.write(Paths.get(new File(testLogDir, testId + ".json").toString()), myStr.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        Returns.returnString(request.resp, myStr);

    }
}
