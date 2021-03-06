package gov.nist.asbestos.asbestosProxy.requests;


// 0 - empty
// 1 - app context  (asbestos)
// 2 - "log"
// 3 - "ProxyBase"


import gov.nist.asbestos.serviceproperties.ServiceProperties;
import gov.nist.asbestos.serviceproperties.ServicePropertiesEnum;
import org.apache.log4j.Logger;

import java.io.IOException;

public class GetProxyBaseRequest {
    private static Logger log = Logger.getLogger(GetEventRequest.class);

    private Request request;

    public static boolean isRequest(Request request) {
        return request.uriParts.size() == 4 && "ProxyBase".equalsIgnoreCase(request.uriParts.get(3));
    }

    public GetProxyBaseRequest(Request request) {
        this.request = request;
    }

    public void run() {
        log.info("GetProxyBaseRequest");
        String base = ServiceProperties.getInstance().getPropertyOrStop(ServicePropertiesEnum.FHIR_TOOLKIT_BASE) + "/proxy";
        try {
            request.resp.getOutputStream().write(base.getBytes());
            request.resp.setStatus(request.resp.SC_OK);
        } catch (IOException e) {
            request.resp.setStatus(request.resp.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
