package com.chenxi.tdd.async


import groovy.util.logging.Slf4j
import org.spockframework.runtime.ConditionNotSatisfiedError
import spock.lang.FailsWith
import spock.lang.Specification
import spock.util.concurrent.AsyncConditions
import spock.util.concurrent.PollingConditions

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

import static org.awaitility.Awaitility.await
import static org.hamcrest.Matchers.equalTo

/**
 *
 * @author chenxi <chenxi01@souche.com>
 * @date 2020/7/13
 */
@Slf4j
class AsyncMailSenderTest extends Specification {

    Mailbox mailBox

    AsyncMailSender asyncMailSender

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

    // https://github.com/awaitility/awaitility/wiki/Groovy
    def "async assert with Awaitility"() {
        when: "invoke async operation"
        asyncMailSender.sendMail(msg)

        then:
        await().atLeast(Duration.ofMillis(10)).atMost(Duration.ofSeconds(3))
                .until({ mailBox.numOfReceivedMail() }, equalTo(1))
        and:
        mailBox.containsMail(msg)
    }

    def "async assert with PollingCondition"() {
        PollingConditions pollingConditions = new PollingConditions()

        when:
        asyncMailSender.sendMail(msg)

        then:
        pollingConditions.within(2, { mailBox.containsMail(msg) })
    }

    def "async assert with JDK Thread APIs"() {
        when:
        asyncMailSender.sendMail(msg)

        and: "wait that all tasks have completed execution in thread pool"
        ExecutorService executorService = asyncMailSender.getExecutorService()
        executorService.shutdown()
        executorService.awaitTermination(2, TimeUnit.SECONDS)
        log.debug(executorService.toString())

        then:
        mailBox.containsMail(msg)
    }

    def "async assert by modifying feat code, for example, adding hook"() {
        given:
        def f = new CompletableFuture()
        Mailbox mailbox = new Mailbox()
        mailbox.setReceivedHook { msg -> f.complete(msg) }
        AsyncMailSender asyncMailSender = new AsyncMailSender(mailbox)

        when:
        asyncMailSender.sendMail(msg)

        then:
        msg == f.get()
    }

    // byte generate tech
    //  https://dzone.com/articles/testing-asynchronous-operations-in-spring-with-spo

    //----------------------------------------------------


    def "mock async behavior with `AsyncCondition`"() {
        given:
        def asyncConditions = new AsyncConditions()
        Mailbox mailbox = Stub {
            receiveMail(_) >> { String _msg -> asyncConditions.evaluate { assert _msg == msg } }
        }
        AsyncMailSender asyncMailSender = new AsyncMailSender(mailbox)

        when:
        asyncMailSender.sendMail(msg)

        then:
        asyncConditions.await(2)
    }
}
