package demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * Verify the results:
 */
@EnableRedisHttpSession
@SpringBootApplication
public class DemoApplication {

    /*
     *  @Bean
     *  JedisConnectionFactory connectionFactory() throws Exception {
     *      return new JedisConnectionFactory();
     *  }
     */

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}

@RestController
class ExampleController {

    @RequestMapping("/")
    public String hello(HttpSession session) {
        UUID uid = (UUID) session.getAttribute("uid");
        if (uid == null) {
            uid = UUID.randomUUID();
        }
        session.setAttribute("uid", uid);
        return uid.toString();
    }

}
