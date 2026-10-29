package andryukha

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class AndryukhaApplication

fun main(args: Array<String>) { runApplication<AndryukhaApplication>(*args) }
