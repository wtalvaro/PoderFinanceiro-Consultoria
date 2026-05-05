package br.com.poderfinanceiro.app.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.poderfinanceiro.app.model.PlaybookItem;
import br.com.poderfinanceiro.app.model.TipoConvenio;
import br.com.poderfinanceiro.app.strategy.DocumentStrategy;

@Service
public class PlaybookService {

        private final List<DocumentStrategy> documentStrategies;

        public PlaybookService(List<DocumentStrategy> documentStrategies) {
                this.documentStrategies = documentStrategies;
        }

        /**
         * Retorna a lista completa de itens para a TreeView,
         * incluindo scripts de venda e checklists de documentos[cite: 1, 2].
         */
        public List<PlaybookItem> listarTudoParaOPlaybook() {
                List<PlaybookItem> itens = new ArrayList<>(listarScriptsVenda());

                // Adiciona os Checklists de Documentos baseados nas Strategies
                itens.addAll(gerarChecklistsDeDocumentos());
                itens.addAll(gerarItensRegrasSeguranca());

                return itens;
        }

        private List<PlaybookItem> gerarChecklistsDeDocumentos() {
                List<PlaybookItem> checklists = new ArrayList<>();

                for (TipoConvenio convenio : TipoConvenio.values()) {
                        // Busca a strategy que suporta este convênio
                        documentStrategies.stream()
                                        .filter(s -> s.supports(convenio.name()))
                                        .findFirst()
                                        .ifPresent(strategy -> {
                                                checklists.add(new PlaybookItem(
                                                                "Checklists de Documentos", // Categoria para o
                                                                                            // agrupamento na TreeView
                                                                "Docs: " + convenio.getLabel(), // Título amigável vindo
                                                                                                // do Enum
                                                                strategy.getChecklist(),
                                                                "-Fotos nítidas, sem cortes e sem reflexos para o convênio "
                                                                                + convenio.getLabel() + "."));
                                        });
                }
                return checklists;
        }

        public List<PlaybookItem> listarScriptsVenda() {
                List<PlaybookItem> scripts = new ArrayList<>();

                // 1. PRODUTO: BOLSA FAMÍLIA
                scripts.add(new PlaybookItem(
                                "Bolsa Família",
                                "Abordagem e Simulação Inicial",
                                "Consegui verificar aqui e podemos simular o empréstimo do Bolsa Família, você recebe o benefício do governo? Vou realizar uma simulação sem compromisso, topa?",
                                "Gatilho de curiosidade e serviço gratuito. Foque em clientes reprovados em outros produtos."));

                // 2. PRODUTO: FGTS
                scripts.add(new PlaybookItem(
                                "FGTS",
                                "Modo Noturno (Remarketing FGTS)",
                                "Trabalha ou já trabalhou com carteira assinada?\nVocê pode receber seu FGTS hoje no PIX.\nSem parcelas, 100% online e liberação rápida.",
                                "Ideal para enviar à noite (19h-21h). O cliente em casa pensa em dinheiro e responde mais. Use com quem visualizou e não respondeu de dia."));

                scripts.add(new PlaybookItem(
                                "FGTS",
                                "Lista de Transmissão (Status/WhatsApp)",
                                "📢 Antecipe seu FGTS e receba via PIX ainda hoje!\nSe você já trabalhou ou trabalha com carteira assinada, pode ter um valor disponível para saque imediato.\n✅ Atendimento gratuito\n✅ Processo rápido e sem burocracia\n✅ Liberação rápida direto na sua conta\nSou especialista no assunto e posso verificar gratuitamente. 📲 Me chame agora para fazer sua simulação.",
                                "Sempre acompanhe o envio com imagens atrativas. Troque as imagens diariamente."));

                scripts.add(new PlaybookItem(
                                "FGTS",
                                "Estratégia Dia 20 (Atualização de Saldos Caixa)",
                                "Oi, tudo bem? A Caixa Econômica acabou de atualizar os saldos do FGTS dos trabalhadores. Como você já havia me procurado antes, passei aqui para refazermos sua simulação. Muitas pessoas que não tinham saldo semana passada, agora têm valor liberado! Vamos testar?",
                                "Do dia 21 ao dia 23. Foque em clientes que reprovaram anteriormente por falta de saldo."));

                // 3. PRODUTO: CLT
                scripts.add(new PlaybookItem(
                                "CLT",
                                "Prospecção (Grupos e Marketplace)",
                                "Trabalha de carteira assinada?\nPode ter dinheiro liberado hoje na conta.\nSimulação gratuita pelo WhatsApp.",
                                "Primeiro produto a ser ofertado na esteira devido ao ticket mais alto."));

                scripts.add(new PlaybookItem(
                                "CLT",
                                "Links de Auto Contratação e Acompanhamento",
                                "Simulação Rápida CLT (Facta, Paraná, Presença): https://simulador.poderfinanceiro.com.br/page/clt/indicacao/9a0863e5-d404-4957-b891-2100e6f3afb9\n\nConsultar Status das Propostas: https://simulador.poderfinanceiro.com.br/page/acompanhar",
                                "Uso interno do operador. Atalhos cruciais para digitar propostas rapidamente."));

                // 4. FECHAMENTO E RECUPERAÇÃO
                scripts.add(new PlaybookItem(
                                "Fechamento",
                                "Recuperação de Indecisos ('Vou pensar')",
                                "Passei para avisar que alguns clientes já garantiram o valor hoje. Posso finalizar sua solicitação agora também?",
                                "Gatilho de escassez e prova social. Aplicar preferencialmente no final da tarde (15h - 17h)."));

                // 5. INDICAÇÃO E MULTIPLICAÇÃO DE LEADS
                scripts.add(new PlaybookItem(
                                "Prospecção",
                                "Comando de Indicação Direta",
                                "Você conhece alguém que também trabalha de carteira assinada e pode precisar desse dinheiro agora?",
                                "Use sempre após uma venda aprovada ou com clientes indecisos pedindo 3 contatos CLT da agenda."));

                scripts.add(new PlaybookItem(
                                "Prospecção",
                                "Recusa Convertida em Indicação",
                                "Oi, tudo bem? Passei aqui porque hoje estou revisando algumas simulações e queria saber se você conseguiu analisar a proposta. Caso ainda não seja o momento, você poderia me indicar 3 pessoas que trabalham registradas (CLT)? Eu verifico gratuitamente e te agradeço muito!",
                                "Excelente para contatos frios ou propostas recusadas. Enviar junto com o cartão de indicação estratégico."));

                // 6. DICAS DE CONVERSÃO GERAL
                scripts.add(new PlaybookItem(
                                "Estratégia de Vendas",
                                "Dica de Ouro: Uso de Áudios",
                                "Sempre que possível envie um áudio na conversa após o primeiro contato por texto.",
                                "O áudio humaniza o atendimento, gera confiança no cliente que tem medo de golpe e aumenta drasticamente a taxa de conversão."));

                return scripts;
        }

        /**
         * Converte a lista de Strings de segurança em PlaybookItems para a
         * TreeView[cite: 3].
         */
        private List<PlaybookItem> gerarItensRegrasSeguranca() {
                List<PlaybookItem> itensRegras = new ArrayList<>();
                List<String> regrasBrutas = listarRegrasSeguranca();

                for (String regra : regrasBrutas) {
                        // Dividimos o texto no primeiro ":" para separar o Título do Conteúdo
                        String[] partes = regra.split(":", 2);
                        String titulo = partes.length > 1 ? partes[0].trim() : "Regra Geral";
                        String conteudo = partes.length > 1 ? partes[1].trim() : regra;

                        itensRegras.add(new PlaybookItem(
                                        "Regras e Segurança", // Categoria para agrupamento na árvore
                                        titulo,
                                        conteudo,
                                        "Dica: Siga rigorosamente para garantir o pagamento das suas comissões[cite: 1, 2]."));
                }
                return itensRegras;
        }

        public List<String> listarRegrasSeguranca() {
                return Arrays.asList(
                                "Ordem de Oferta Estratégica: 1º CLT (ticket mais alto) > 2º FGTS > 3º Bolsa Família.",
                                "Estratégia de Aprovação CLT (V8): CPF recusado em um banco pode aprovar em outro. Teste toda a esteira. ATENÇÃO: Durante instabilidades de sistema, digite propostas CLT sempre SEM SEGURO para evitar que fiquem paradas na esteira.",
                                "Regras Saque-Aniversário FGTS (2026): Antecipação só após 90 dias da adesão. Parcelas devem ser de R$ 100 a R$ 500. Regra de limite: 1 contrato por ano (com transição para até 3 parcelas anuais).",
                                "Atualização Saldo FGTS (Caixa): Empregadores pagam dia 20. A Caixa tem até 5 dias úteis para atualizar. FOCO TOTAL em retrabalhar CPFs reprovados entre os dias 21 e 23. Teste tabelas de menor comissão para liberar valores maiores.",
                                "Pausa Operacional CLT (Virada de Folha): Todo mês, do dia 20 (22h) ao dia 23 (06h). Sem consultas de margem e novas contratações.",
                                "Operação Bolsa Família: Obrigatório o uso do usuário Crefisa próprio. O uso de fichas manuais foi descontinuado.",
                                "Ciclo de Comissão: Fechamento quarta-feira às 23:59. Quinta-feira PDF de conferência. Pagamento na sexta-feira até as 18h.");
        }
}