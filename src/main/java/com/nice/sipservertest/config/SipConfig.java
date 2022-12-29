package com.nice.sipservertest.config;

import com.nice.sipservertest.listener.SipListenerImpl;
import com.nice.sipservertest.listener.SipLogger;
import gov.nist.core.StackLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sip.*;
import javax.sip.address.AddressFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import java.util.Properties;
import java.util.TooManyListenersException;

@Configuration
public class SipConfig {

    @Bean
    SipFactory sipFactory() {
        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        return sipFactory;
    }

    @Bean
    SipStack sipStack(SipFactory sipFactory) throws PeerUnavailableException {
        Properties properties = new Properties();
        properties.setProperty("javax.sip.STACK_NAME", "sip-test");
        properties.setProperty("gov.nist.javax.sip.STACK_LOGGER", SipLogger.class.getName());
        return sipFactory.createSipStack(properties);
    }

    @Bean
    ListeningPoint listeningPoint(SipStack sipStack) throws TransportNotSupportedException, InvalidArgumentException {
        return sipStack.createListeningPoint("0.0.0.0", 5060, "UDP");
    }

    @Bean
    MessageFactory messageFactory(SipFactory sipFactory) throws PeerUnavailableException {
        return sipFactory.createMessageFactory();
    }

    @Bean
    HeaderFactory headerFactory(SipFactory sipFactory) throws PeerUnavailableException {
        return sipFactory.createHeaderFactory();
    }

    @Bean
    AddressFactory addressFactory(SipFactory sipFactory) throws PeerUnavailableException {
        return sipFactory.createAddressFactory();
    }

    @Bean
    SipListener sipListener(MessageFactory messageFactory, HeaderFactory headerFactory, AddressFactory addressFactory, SipStack sipStack,
                            ListeningPoint listeningPoint)
            throws TooManyListenersException, ObjectInUseException {
        SipProvider sipProvider = sipStack.createSipProvider(listeningPoint);
        return new SipListenerImpl(messageFactory, headerFactory, addressFactory, sipProvider);
    }

    @Bean
    StackLogger stackLogger() {
        return new SipLogger();
    }
}
