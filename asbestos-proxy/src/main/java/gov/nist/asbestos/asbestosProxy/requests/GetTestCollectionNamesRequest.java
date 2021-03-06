package gov.nist.asbestos.asbestosProxy.requests;

import com.google.gson.Gson;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
// 0 - empty
// 1 - app context
// 2 - "engine"
// 3 - "collections"
// Return list of test collection names

public class GetTestCollectionNamesRequest {
    private static Logger log = Logger.getLogger(GetTestCollectionNamesRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && "collections".equalsIgnoreCase(request.uriParts.get(3));
    }

    public GetTestCollectionNamesRequest(Request request) {
        this.request = request;
    }

    class Collection {
        String name;
        boolean server;
    }

    public void run() throws IOException {
        log.info("GetTestCollectionNames");
        List<Collection> collections = new ArrayList<>();
        List<String> names = request.ec.getTestCollectionNames();
        for (String name : names) {
            Collection collection = new Collection();
            Properties props = request.ec.getTestCollectionProperties(name);
            collection.server = "server".equals(props.getProperty("TestType"));
            collection.name = name;
            collections.add(collection);
        }
        String json = new Gson().toJson(collections);
        request.resp.setContentType("application/json");
        request.resp.getOutputStream().print(json);

        request.resp.setStatus(request.resp.SC_OK);

        log.info("OK");
    }
}
