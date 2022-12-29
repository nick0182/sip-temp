package com.nice.sipservertest.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.route53.Route53Client;
import software.amazon.awssdk.services.route53.model.*;

import java.util.Collections;

//@RestController
//@RequestMapping("/dns")
@AllArgsConstructor
@Slf4j
public class DNSController {

    private final Route53Client route53Client;

    @GetMapping
    public String getRecordResponse() {
        TestDnsAnswerResponse testDnsAnswerResponse = route53Client.testDNSAnswer(builder -> builder.hostedZoneId("Z008337866284ALQ4TSI").recordName("sip.siptest.click").recordType(RRType.A));
        return String.join("\n", testDnsAnswerResponse.recordData());
    }

    @PatchMapping
    public void updateRecord() {
        route53Client.changeResourceRecordSets(builder ->
                builder.hostedZoneId("Z008337866284ALQ4TSI").changeBatch(builder1 ->
                        builder1.changes(Collections.singletonList(Change
                                .builder().resourceRecordSet(ResourceRecordSet
                                        .builder().name("siptest.click").resourceRecords(Collections.singletonList(ResourceRecord
                                                .builder().value("sip.siptest.click").build())).name("").ttl(20L).build()).action(ChangeAction.UPSERT).build()))));
    }
}
