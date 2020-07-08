package com.chenxi.tdd.embeddeddb

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.context.annotation.Import
import spock.lang.Specification
/**
 *
 * @author chenxi <chenxi01@souche.com>
 * @date 2020/7/8
 */
@Import(UserDao)
@JdbcTest
class UserDaoTest extends Specification {

    @Autowired
    UserDao userDao

    def "save and get"() {
        when:
        int id = userDao.save('chenxi')
        then:
        'chenxi' == userDao.getName(id)
    }
}
