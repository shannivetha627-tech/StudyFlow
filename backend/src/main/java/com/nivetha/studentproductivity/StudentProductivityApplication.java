package com.nivetha.studentproductivity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.nivetha.studentproductivity")
@EntityScan(basePackages = "com.nivetha.studentproductivity")
@EnableJpaRepositories(basePackages = "com.nivetha.studentproductivity")
@EnableScheduling
public class StudentProductivityApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudentProductivityApplication.class, args);
	}

}
