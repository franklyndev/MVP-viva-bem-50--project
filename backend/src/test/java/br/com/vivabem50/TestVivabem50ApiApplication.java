package br.com.vivabem50;

import org.springframework.boot.SpringApplication;

public class TestVivabem50ApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(Vivabem50ApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
