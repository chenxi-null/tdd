import groovy.util.logging.Slf4j
import spock.lang.Specification
/**
 *
 * @author chenxi <chenxi01@souche.com>
 * @date 2020/2/12
 */
@Slf4j
class TmpTest extends Specification {

    def "Name"() {
        expect:

        log.info(">>>")
        1 == 1
    }
}
