package com.solumesl.aims.saas.adapter.mdm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication 
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@ComponentScan({"com.solumesl"})
public class AimsSaasAdapterMdmApplication {

	public static void main(String[] args) {
		 
		SpringApplication.run(AimsSaasAdapterMdmApplication.class, args);
	}

}
