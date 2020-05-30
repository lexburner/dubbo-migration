package moe.cnkirito.dubbo.consumer;

import moe.cnkirito.dubbo.DemoService;
import org.apache.dubbo.config.annotation.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
public class DubboConsumerBootstrap {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference()
    private DemoService demoService;

    public static void main(String[] args) {
        SpringApplication.run(DubboConsumerBootstrap.class).close();
    }

    @Bean
    public ApplicationRunner runner() {
        return (args) -> {
            while (true) {
                Thread.sleep(1000);
                try{
                    logger.info(demoService.sayHello("Provider"));
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        };
    }
}
