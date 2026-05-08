package br.com.poderfinanceiro.app;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AppApplication {

	public static void main(String[] args) {
		System.setProperty("XDG_CURRENT_DESKTOP", "GNOME");
		// Ele apenas delega a inicialização para a sua classe JavafxApplication real
		Application.launch(JavafxApplication.class, args);
	}

}