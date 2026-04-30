package br.com.poderfinanceiro.app;

import br.com.poderfinanceiro.app.model.Proponente;
import br.com.poderfinanceiro.app.repository.ProponenteRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class MainController {

    @Autowired
    private ProponenteRepository repository;

    // Componentes da Tabela
    @FXML
    private TableView<Proponente> tabelaProponentes;

    // Componentes do Formulário de Cadastro
    @FXML
    private TextField fldNome, fldCpf, fldTelefone, fldRenda, fldMatricula;
    @FXML
    private DatePicker fldDataNascimento;
    @FXML
    private ComboBox<String> fldConvenio;

    @FXML
    public void initialize() {
        fldConvenio.getItems().addAll("Real Grandeza", "INSS", "SIAPE", "Marinha");
        atualizarTabela();
    }

    @FXML
    public void salvarLead() {
        try {
            // 1. Criar o objeto usando o Builder do Lombok
            Proponente novo = Proponente.builder()
                    .nomeCompleto(fldNome.getText())
                    .cpf(fldCpf.getText())
                    .dataNascimento(fldDataNascimento.getValue())
                    .telefone(fldTelefone.getText())
                    .rendaMensal(new BigDecimal(fldRenda.getText()))
                    .convenioOrgao(fldConvenio.getValue())
                    .matricula(fldMatricula.getText())
                    .build();

            // 2. Salvar no Banco
            repository.save(novo);

            // 3. Feedback visual (AtlantaFX Style)
            mostrarAlerta("Sucesso!", "Cliente " + novo.getNomeCompleto() + " foi cadastrado.");

            // 4. Limpar e Atualizar
            limparFormulario();
            atualizarTabela();

        } catch (Exception e) {
            mostrarAlerta("Erro ao Salvar", "Verifique se o CPF é único e se os campos estão corretos.");
        }
    }

    @FXML
    public void atualizarTabela() {
        tabelaProponentes.getItems().setAll(repository.findAll());
    }

    @FXML
    public void limparFormulario() {
        fldNome.clear();
        fldCpf.clear();
        fldTelefone.clear();
        fldRenda.clear();
        fldMatricula.clear();
        fldDataNascimento.setValue(null);
        fldConvenio.setValue(null);
    }

    private void mostrarAlerta(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}