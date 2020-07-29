package com.chenxi.tdd.async


import groovy.util.logging.Slf4j
import org.spockframework.runtime.ConditionNotSatisfiedError
import spock.lang.FailsWith
import spock.lang.Ignore
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

    def "async assert with sleep"() {
        when: "invoke async operation"
        asyncMailSender.sendMail(msg)

        then:
        sleep(2000)
        and:
        mailBox.containsMail(msg)
    }

    // ---------------------- Polling Assertion ----------------------

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


    // ---------------------- Wait-and-Notify ----------------------

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

    def "async assert by mocking receiver"() {
        given:
        def f = new CompletableFuture()
        Mailbox mailbox = Stub {
            receiveMail(_) >> { String _msg -> f.complete(msg) }
        }
        AsyncMailSender asyncMailSender = new AsyncMailSender(mailbox)

        when:
        asyncMailSender.sendMail(msg)

        then:
        msg == f.get()
    }

    def "async assert with `AsyncCondition`"() {
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

    static class Message {
        int id
        String content
        String tag
    }

    @Ignore("just to demonstrate")
    def "AsyncConditions can report detailed failed result of assertion"() {
        def asyncConditions = new AsyncConditions()
        when:
        def msg = new Message(id: 100, content: 'content1', tag: 'tag1')
        Thread.start {
            asyncConditions.evaluate {
                verifyAll(msg) {
                    id == 101
                    content == 'content2'
                    tag == 'tag1'
                }
            }
        }

        then:
        asyncConditions.await(1)
    }

    // byte generate tech
    //  https://dzone.com/articles/testing-asynchronous-operations-in-spring-with-spo


    //----------------------------------------------------

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
}
