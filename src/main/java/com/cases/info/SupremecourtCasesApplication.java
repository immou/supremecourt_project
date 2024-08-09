package com.cases.info;

import com.cases.info.service.CaseDataService;
import com.cases.info.service.CaseDataServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@SpringBootApplication
@EnableAutoConfiguration
public class SupremecourtCasesApplication implements CommandLineRunner {
	@Value("#{'${list.of.year}'.split(',')}")
	private List<String> yearList;

	public static void main(String[] args) {

		SpringApplication.run(SupremecourtCasesApplication.class, args);
	}

	@Bean
	public CaseDataService getCaseService(){
		return new CaseDataServiceImpl();
	}

	@Bean(name="restTemplate")
	public RestTemplate restTemplate(){
		return new RestTemplate();
	}

	@Override
	public void run(String... args) throws Exception {
		getCaseService().fetchCaseVolumePerYear(yearList);
	}

}
