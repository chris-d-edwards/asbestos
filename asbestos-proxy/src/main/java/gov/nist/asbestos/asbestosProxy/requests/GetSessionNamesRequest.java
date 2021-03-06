package gov.nist.asbestos.asbestosProxy.requests;

import gov.nist.asbestos.client.Base.Dirs;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.List;
// 0 - empty
// 1 - app context
// 2 - "channel"
// 3 - "sessionNames"
// Return list of TestSession IDs

public class GetSessionNamesRequest {
    private static Logger log = Logger.getLogger(GetSessionNamesRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && "sessionNames".equalsIgnoreCase(request.uriParts.get(3));
    }

    public GetSessionNamesRequest(Request request) {
        this.request = request;
    }

    public void run()  {
        log.info("GetSessionNames");
        List<String> names = Dirs.dirListingAsStringList(new File(request.externalCache, "FhirChannels"));
        Returns.returnList(request.resp, names);
        log.info("OK");
    }
}
