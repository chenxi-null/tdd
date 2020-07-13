package com.chenxi.tdd.async


import org.spockframework.runtime.ConditionNotSatisfiedError
import spock.lang.FailsWith
import spock.lang.Specification

import java.time.Duration

import static org.awaitility.Awaitility.await
import static org.hamcrest.Matchers.equalTo
/**
 *
 * @author chenxi <chenxi01@souche.com>
 * @date 2020/7/13
 */
class AsyncMailSenderTest extends Specification {

    def mailBox
    def asyncMailSender
    def msg = "hello world"

    void setup() {
        mailBox = new Mailbox()
        asyncMailSender = new AsyncMailSender(mailBox)
    }

    @FailsWith(ConditionNotSatisfiedError)
    def "SendMail"() {
        when: "invoke async operation"
        asyncMailSender.sendMail(msg)

        then: "failed to assert because the async operation isn't finished"
        mailBox.containsMail(msg)
    }

    def "assert with Awaitility"() {
        when: "invoke async operation"
        asyncMailSender.sendMail(msg)

        then:
        await().atMost(Duration.ofSeconds(3))
                .until({ mailBox.numOfReceivedMail() }, equalTo(1))
        and:
        mailBox.containsMail(msg)
    }
}
