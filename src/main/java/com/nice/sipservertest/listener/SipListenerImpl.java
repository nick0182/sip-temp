package com.nice.sipservertest.listener;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.sip.*;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.util.TooManyListenersException;

@Slf4j
public class SipListenerImpl implements SipListener {

    private final MessageFactory messageFactory;
    private final SipProvider sipProvider;

    public SipListenerImpl(MessageFactory messageFactory, SipProvider sipProvider) throws TooManyListenersException {
        this.messageFactory = messageFactory;
        this.sipProvider = sipProvider;
        this.sipProvider.addSipListener(this);
    }

    @SneakyThrows
    @Override
    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();

        log.info("Received a SIP request {}", request.getMethod());

        ServerTransaction transaction = requestEvent.getServerTransaction();

        if (transaction == null) {
            transaction = sipProvider.getNewServerTransaction(requestEvent.getRequest());
        }
        Response response = messageFactory.createResponse(500, requestEvent.getRequest());
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
