package com.chenxi.tdd.async

import spock.lang.Specification

/**
 *
 * @author chenxi <chenxi01@souche.com>
 * @date 2020/7/13
 */
class AsyncMailSenderTest extends Specification {

    def "SendMail"() {
        given:
        def mailBox = new Mailbox()
        def asyncMailSender =  new AsyncMailSender(mailBox)
        def msg = "hello world"

        when:
        asyncMailSender.sendMail(msg)
        println "---"
        then:
        println "==="
        mailBox.containsMail("other")
        mailBox.containsMail(msg)
    }
}
