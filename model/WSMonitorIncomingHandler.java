package org.nem.beta.model;

import org.nem.beta.utility.UtilityFunctions;

import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;

import java.lang.reflect.Type;
import net.sf.json.JSONObject;

public class WSMonitorIncomingHandler implements StompSessionHandler {

    private String address = null;

    public WSMonitorIncomingHandler(String address) {
        this.address = address;
    }

    private void monitor(String result) {
        UtilityFunctions.DecodeMessage(JSONObject.fromObject(result).getJSONObject("transaction"));
    }

    @Override
    public Type getPayloadType(StompHeaders arg0) {
        return String.class;
    }

    @Override
    public void handleFrame(StompHeaders arg0, Object arg1) { }

    @Override
    public void afterConnected(StompSession session, StompHeaders arg1) {
        // the address needs to be sent to server before subscribing
        session.send("/w/api/account/subscribe", "{\"account\":\"" + this.address + "\"}");
        session.subscribe("/transactions/" + this.address, new StompFrameHandler() {
            public Type getPayloadType(StompHeaders stompHeaders) {
                return String.class;
            }

            public void handleFrame(StompHeaders stompHeaders, Object result) {
                monitor(result.toString());
            }
        });
    }

    @Override
    public void handleException(StompSession arg0, StompCommand arg1, StompHeaders arg2, byte[] arg3, Throwable arg4) { }

    @Override
    public void handleTransportError(StompSession arg0, Throwable arg1) { }
}