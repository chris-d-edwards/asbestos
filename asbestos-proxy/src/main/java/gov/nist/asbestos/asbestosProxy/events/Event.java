package gov.nist.asbestos.asbestosProxy.events;


import gov.nist.asbestos.http.headers.Headers;

public class Event {
    Headers _requestHeaders = null;
    byte[] _requestRawBody = null;
    String _requestBody = null;

    Headers _responseHeaders = null;
    byte[] _responseRawBody = null;
    String _responseBody = null;

    EventStore store = null;
    SimId channelId = null;
    String resource = null;
    String eventId = null; // within resource

    public Event() {

    }

    public Event(EventStore store, SimId channelId, String resource, String eventId) {
        this.store = store;
        this.channelId = channelId;
        this.resource = resource;
        this.eventId = eventId;
    }

    public boolean isComplete() {
        return store != null  && channelId != null && resource != null && eventId != null;
    }



}
