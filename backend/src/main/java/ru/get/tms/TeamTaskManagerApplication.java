package ru.get.tms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TeamTaskManagerApplication {

  public static void main(String[] args) {
    SpringApplication.run(TeamTaskManagerApplication.class, args);
  }
}
