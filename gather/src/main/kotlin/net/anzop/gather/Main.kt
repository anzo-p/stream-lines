package net.anzop.gather

import io.github.cdimascio.dotenv.Dotenv
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
@EnableScheduling
class Application

fun main(args: Array<String>) {
    Dotenv
        .configure()
        .ignoreIfMalformed()
        .ignoreIfMissing()
        .load()
        .entries()
        .forEach { entry -> System.setProperty(entry.key, entry.value) }

    runApplication<Application>(*args)
}
