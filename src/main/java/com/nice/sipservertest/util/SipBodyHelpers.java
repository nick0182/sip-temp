package com.nice.sipservertest.util;

import gov.nist.javax.sip.message.Content;
import gov.nist.javax.sip.message.MultipartMimeContent;
import gov.nist.javax.sip.message.MultipartMimeContentImpl;
import gov.nist.javax.sip.message.SIPMessage;

import javax.sip.header.ContentTypeHeader;
import javax.sip.message.Message;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class SipBodyHelpers {

    private static final String MULTIPART_MIXED = "multipart/mixed";

    public static String getBodyContentString(Message message, String contentType) {

        String content = null;

        if (message == null) {
            return null;
        }

        ContentTypeHeader mainContentTypeHeader = (ContentTypeHeader) message.getHeader(ContentTypeHeader.NAME);

        if (isContentTypeHeaderIncomplete(mainContentTypeHeader)) {
            return null;
        }

        String mainContentType = mainContentTypeHeader.getContentType() + "/" + mainContentTypeHeader.getContentSubType();

        // check for multipart content
        if (mainContentType.equalsIgnoreCase(MULTIPART_MIXED)) {
            // mixed multipart message body
            content = getMultipartBodyContentString(message, contentType);
        }

        // check for single body case
        if (mainContentType.equalsIgnoreCase(contentType)) {
            content = contentToString(message.getContent());
        }

        return content;
    }

    private static boolean isContentTypeHeaderIncomplete(ContentTypeHeader mainContentTypeHeader) {
        return mainContentTypeHeader == null || mainContentTypeHeader.getContentType() == null ||
                mainContentTypeHeader.getContentSubType() == null;
    }

    private static String getMultipartBodyContentString(Message message, String contentType) {
        // mixed multipart message body
        MultipartMimeContent multipartContent = getMultipartMimeContent(message);

        if (multipartContent == null) {
            return null;
        }
        for (Iterator<Content> it = multipartContent.getContents(); it.hasNext(); ) {
            Content content = it.next();
            ContentTypeHeader contentTypeHeader = content.getContentTypeHeader();
            String contentTypeAndSubtype = contentTypeHeader.getContentType() + "/" + contentTypeHeader.getContentSubType();

            if (contentTypeAndSubtype.equalsIgnoreCase(contentType)) {
                return contentToString(content.getContent());
            }
        }

        return null;
    }

    private static MultipartMimeContent getMultipartMimeContent(Message message) {
        if (message == null || message.getContentLength() == null ||
                message.getContentLength().getContentLength() == 0) {
            return null;
        }
        MultipartMimeContentImpl retval = new MultipartMimeContentImpl(((SIPMessage) message).getContentTypeHeader());
        byte[] rawContent = message.getRawContent();
        try {
            String body = new String(rawContent);
            retval.createContentList(body);
            return retval;
        } catch (Exception e) {
            return null;
        }
    }

    private static String contentToString(Object content) {
        String contentString;
        if (content instanceof String) {
            contentString = (String) content;
        } else if (content instanceof byte[]) {
            contentString = new String((byte[]) content, StandardCharsets.UTF_8);
        } else {
            contentString = content.toString();
        }
        return contentString;
    }
}
