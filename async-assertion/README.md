# TDD 实战 —— 如何测试异步任务

异步操作，包括进程内的异步操作，也包括跨系统的调用，如 MQ 的发布-订阅场景，异步任务中间件场景等，异步操作通常可以提升系统的吞吐量、降低模块耦合，但在我们平时的写单元/集成测试的过程中，如何对这些异步操作进行测试却成了一个难题。

本文列举了几种测试异步操作的解决方案，希望为大家提供一些思路，帮助大家写出更简洁优雅的代码😄


代码中的测试用例是用 Spock 写的，不熟悉 Spock 这一测试框架的同学可以看下我之前写的这篇文章 [Spock-Tutorial-for-Javaer](https://chenxi-null.github.io/2019/01/28/Spock-Tutorial-for-Javaer/)

下面以一个邮件发送的案例作为我们的例子：
`AsyncMailSender` 是邮件发送者，通过调用 `sendMail` 方法异步地发送邮件，相当于 MQ 场景中的 Producer 角色；
`MailBox` 是邮件的接受者，相当于 MQ 中的 Consumer 角色。


最简单暴力的方式就是使用 sleep 了:
```java
def "async assert with sleep"() {
    when: "invoke async operation"
    asyncMailSender.sendMail(msg)

    then:
    sleep(2000)
    and:
    mailBox.containsMail(msg)
}
```
这样的缺点就是 sleep 的时间长度难以控制，设置长了会增大测试的耗时，设置短了可能出现 consumer 还没收到消息的情况，导致测试失败

针对 sleep 的缺陷，更好的解决方案也比较容易想到，那就是使用**轮询**的方式，不断检查 consumer 是否接收到指定的消息，收到的话就返回 assert 成功，如果超过设置的最大等待时间还没有收到消息就返回失败。


## 轮询

对于这种需求，社区早就有比较成熟的工具，不需要我们再重复造轮子了

### Awaitility
同时支持 Java 和 Groovy，提供了丰富的 DSL 风格的 API

[Awaitility](https://github.com/awaitility/awaitility) 在 github 上的介绍:
> Awaitility is a small Java DSL for synchronizing asynchronous operations
> 
> Testing asynchronous systems is hard. Not only does it require handling threads, timeouts and concurrency issues, but the intent of the test code can be obscured by all these details. Awaitility is a DSL that allows you to express expectations of an asynchronous system in a concise and easy to read manner.

```groovy
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

```

### PollingCondition of Spock
PollingCondition 是 Spock 自带的，个人认为它的语法比 Awaitility 更简洁

```groovy
def "async assert with PollingCondition"() {
    PollingConditions pollingConditions = new PollingConditions()

    when:
    asyncMailSender.sendMail(msg)

    then:
    pollingConditions.within(2, { mailBox.containsMail(msg) })
}
```

---


## 主动通知

除了轮询 consumer、不断检查 consumer 状态这种方案，还可以基于 wait-notify 的模型，让 consumer 在满足条件后主动通知 "断言者"

### 在 Receiver 处添加 Hook/Callback
对代码有一定侵入性，如何这里 Hook 只是为测试服务的话
```groovy
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
```

### Mock Receiver
还有一种思路，如果不关心 receiver 的内部逻辑，只关心 receiver 的 receive 方法 (例子里是 `MailBox.receiveMail`) 是否被调用过的话，可以考虑直接对 receiver 进行 mock，在 mock 逻辑里添加 notify 的代码，然后在 assert 处等待，这里的 wait-and-notify 流程和上面的例子是一致的。


```groovy
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
```

### AsyncCondition of Spock
上面的两个例子是通过 JDK 的 `CompletableFuture` 实现 wait-and-notify 的，当然也可以使用其他的 JDK API，比如 `CountDownLatch` 等，这里推荐一个 Spock 内置的工具 —— AsyncCondition:
```groovy
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
```
AsyncCondition 的使用方法和 `CompletableFuture` 以及 `CountDownLatch` 是差不多的，但好处是可以利用 Spock 在 assert 失败时会打印详细的失败信息这一特性，在 assert 复杂对象出现失败时，方便排查，e.g:
```groovy
def "AsyncConditions can report detailed failed result of assertion"() {
    def asyncConditions = new AsyncConditions()
    when:
    def msg = new Message(id: 100, content: 'content1', tag: 'tag1');
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
```
![](https://chenxi-null.github.io/images/2020-07-29-11-43-33.png)

### 字节码生成工具

我在网上还看到过使用字节码生成工具来测试异步操作的“奇技淫巧”，有兴趣的朋友可以看下这篇文章：
[Testing Asynchronous Operations in Spring With Spock and Byteman - DZone Performance](https://dzone.com/articles/testing-asynchronous-operations-in-spring-with-spo)

---

## 其他
上面介绍的几种方法都是比较通用的，不管是针对进程内还是跨进程的异步场景都是适用的，但如果我们要测试的仅仅是进程内部的异步通信场景，其实可以尝试如下方式:
如果 Producer 是通过线程池的方式异步调用 Consumer 的 receive 方法，我们可以等待 Producer 的线程池执行结束后，再去 assert Consumer
```groovy
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
```




