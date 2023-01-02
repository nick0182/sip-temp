package com.nice.sipservertest.listener;

import com.nice.sipservertest.dto.MediaSignalingEvent;
import com.nice.sipservertest.dto.SdpMessage;
import com.nice.sipservertest.factory.SipMediaSignalingEventFactory;
import com.nice.sipservertest.parser.SdpMessageParser;
import com.nice.sipservertest.util.SipBodyHelpers;
import gov.nist.javax.sip.header.ContentType;
import gov.nist.javax.sip.message.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.sip.*;
import javax.sip.address.AddressFactory;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ViaHeader;
import javax.sip.message.Message;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Iterator;
import java.util.TooManyListenersException;

@Slf4j
public class SipListenerImpl implements SipListener {

    private final MessageFactory messageFactory;
    private final HeaderFactory headerFactory;
    private final AddressFactory addressFactory;
    private final SipProvider sipProvider;

    public SipListenerImpl(MessageFactory messageFactory, HeaderFactory headerFactory, AddressFactory addressFactory,
                           SipProvider sipProvider) throws TooManyListenersException {
        this.messageFactory = messageFactory;
        this.headerFactory = headerFactory;
        this.addressFactory = addressFactory;
        this.sipProvider = sipProvider;
        this.sipProvider.addSipListener(this);
    }

    @SneakyThrows
    @Override
    public void processRequest(RequestEvent requestEvent) {
        SIPRequest request = (SIPRequest) requestEvent.getRequest();

        log.info("Received a SIP request {}", request);

        String contentString = SipBodyHelpers.getBodyContentString(request,
                "application/sdp");
        log.info("Content string: {}", contentString);
//        SdpMessage sdpMessage = SdpMessageParser.parse(contentString);
//        MediaSignalingEvent mediaSignalingEvent = SipMediaSignalingEventFactory.createMediaSignalingEvent(request, sdpMessage);

//        String encodedMessage = sdpMessage.getEncodedMessage();
        ServerTransaction transaction = requestEvent.getServerTransaction();

        if (transaction == null) {
            log.info("Creating new transaction");
            transaction = sipProvider.getNewServerTransaction(request);
        }
        ContentType c = new ContentType();
        c.setContentType("application");
        c.setContentSubType("sdp");
        Response response = messageFactory.createResponse(200, request, c, contentString.getBytes());
        ContactHeader contactHeader = headerFactory.createContactHeader(addressFactory.createAddress(String.format("sip:%s:%d", "54.190.30.227",
                5060)));
        response.addHeader(contactHeader);
        transaction.sendResponse(response);
        log.info("Sent response: {}", response);
    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        log.info("Process response: {}", responseEvent);
    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        log.info("Process timeout: {}", timeoutEvent);
    }

    @Override
    public void processIOException(IOExceptionEvent exceptionEvent) {
        log.info("Process IOException: {}", exceptionEvent);
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {
        log.info("Process transaction terminated: {}", transactionTerminatedEvent);
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
        log.info("Process dialog terminated: {}", dialogTerminatedEvent);
    }
}
