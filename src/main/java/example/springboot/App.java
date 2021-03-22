package example.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableAutoConfiguration
@EnableConfigurationProperties
public class App {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(App.class);
		application.run(args);
	}
		
}
