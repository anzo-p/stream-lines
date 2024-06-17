package net.anzop.retro

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
@EnableScheduling
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
