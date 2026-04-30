package br.com.poderfinanceiro.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javafx.application.Application;

@SpringBootApplication
public class AppApplication {

	public static void main(String[] args) {
		Application.launch(JavafxApplication.class, args);
		SpringApplication.run(AppApplication.class, args);
	}

}
