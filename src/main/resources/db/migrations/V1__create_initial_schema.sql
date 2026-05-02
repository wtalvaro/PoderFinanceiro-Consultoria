-- Script de inicialização do banco de dados (Poder Financeiro) - Multi-Usuário

-- ==========================================
-- 1. TIPOS E ENUMS
-- ==========================================
-- NOVO: Enum para gerenciar o ciclo de vida da pessoa no seu funil
CREATE TYPE public.tipo_relacionamento AS ENUM (
    'LEAD',
    'PROPONENTE',
    'CLIENTE'
);

-- ATUALIZADO: Removido o 'Lead', agora começa em 'Digitada'
CREATE TYPE public.status_proposta AS ENUM (
    'Digitada',
    'Pendente',
    'Analise_Banco',
    'Aguardando_Doc',
    'Aprovada',
    'Reprovado',
    'Pago',
    'Cancelado'
);

-- ==========================================
-- 2. SEQUÊNCIAS
-- ==========================================
CREATE SEQUENCE public.usuarios_usuario_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.bancos_banco_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.comissoes_comissao_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.documentos_proponente_documento_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.historico_status_proposta_historico_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.interacoes_contato_interacao_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.proponentes_proponente_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.propostas_proposta_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.tabelas_juros_tabela_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- ==========================================
-- 3. TABELAS
-- ==========================================
CREATE TABLE public.usuarios (
    usuario_id bigint DEFAULT nextval('public.usuarios_usuario_id_seq'::regclass) NOT NULL,
    nome character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    senha_hash character varying(255) NOT NULL,
    papel character varying(50) DEFAULT 'CONSULTOR'::character varying,
    ativo boolean DEFAULT true,
    criado_em timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    ultimo_acesso timestamp without time zone
);

CREATE TABLE public.bancos (
    banco_id bigint DEFAULT nextval('public.bancos_banco_id_seq'::regclass) NOT NULL,
    nome_banco character varying(100) NOT NULL,
    taxa_media_juros numeric(5,2),
    taxa_minima numeric(5,2),
    taxa_maxima numeric(5,2),
    comissao_percentual numeric(5,2),
    prazo_maximo integer,
    link_portal_banco text,
    sistema_amortizacao character varying(50) DEFAULT 'Price'::character varying,
    permite_pos_fixado boolean DEFAULT false,
    ativo boolean DEFAULT true,
    criado_em timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.comissoes (
    comissao_id integer DEFAULT nextval('public.comissoes_comissao_id_seq'::regclass) NOT NULL,
    proposta_id integer NOT NULL,
    usuario_id bigint NOT NULL, -- Qual consultor recebe esta comissão
    valor_bruto_comissao numeric(12,2) NOT NULL,
    impostos_retidos numeric(12,2) DEFAULT 0.00,
    valor_liquido_consultor numeric(12,2) NOT NULL,
    data_previsao_pagamento date,
    status_pagamento character varying(20) DEFAULT 'Pendente'::character varying,
    data_recebimento timestamp without time zone,
    criado_em timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.documentos_proponente (
    documento_id integer DEFAULT nextval('public.documentos_proponente_documento_id_seq'::regclass) NOT NULL,
    proponente_id integer,
    usuario_id bigint NOT NULL, -- Quem fez o upload
    tipo_documento character varying(50) NOT NULL,
    arquivo_path text NOT NULL,
    hash_sha256 character varying(64),
    verificado boolean DEFAULT false,
    data_upload timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.historico_status_proposta (
    historico_id integer DEFAULT nextval('public.historico_status_proposta_historico_id_seq'::regclass) NOT NULL,
    proposta_id integer,
    usuario_id bigint, -- Quem mudou o status
    status_anterior character varying(50),
    status_novo character varying(50) NOT NULL,
    data_mudanca timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    motivo_mudanca text
);

CREATE TABLE public.interacoes_contato (
    interacao_id integer DEFAULT nextval('public.interacoes_contato_interacao_id_seq'::regclass) NOT NULL,
    proponente_id integer,
    usuario_id bigint NOT NULL, -- Qual consultor atendeu/enviou
    canal character varying(20) DEFAULT 'WhatsApp'::character varying,
    mensagem_texto text,
    direcao character varying(10),
    data_interacao timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.proponentes (
    proponente_id bigint DEFAULT nextval('public.proponentes_proponente_id_seq'::regclass) NOT NULL,
    usuario_id bigint NOT NULL, -- Dono da carteira do cliente
    nome_completo character varying(255) NOT NULL,
    cpf character varying(14) NOT NULL,
    telefone character varying(20),
    renda_mensal numeric(12,2),
    tipo_vinculo character varying(50),
    convenio_orgao character varying(100),
    matricula character varying(50),
    origem_consentimento text,
    classificacao public.tipo_relacionamento DEFAULT 'LEAD'::public.tipo_relacionamento, -- NOVO: Campo de classificação adicionado
    data_cadastro timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deletado_em timestamp without time zone,
    data_nascimento date
);

CREATE TABLE public.propostas (
    proposta_id integer DEFAULT nextval('public.propostas_proposta_id_seq'::regclass) NOT NULL,
    proponente_id integer NOT NULL,
    banco_id integer NOT NULL,
    usuario_id bigint NOT NULL, -- Consultor responsável pela proposta
    valor_solicitado numeric(12,2) NOT NULL,
    valor_aprovado numeric(12,2),
    taxa_aplicada numeric(5,2),
    quantidade_parcelas integer,
    status public.status_proposta DEFAULT 'Digitada'::public.status_proposta, -- ATUALIZADO: Default agora é 'Digitada'
    coeficiente numeric(10,6),
    valor_parcela numeric(12,2),
    modalidade_juros character varying(20) DEFAULT 'Prefixado'::character varying,
    custo_efetivo_total numeric(5,2),
    margem_utilizada numeric(12,2),
    eh_novacao boolean DEFAULT false,
    saldo_quitacao_anterior numeric(12,2) DEFAULT 0.00,
    valor_iof numeric(12,2) DEFAULT 0.00,
    taxa_administracao numeric(12,2) DEFAULT 0.00,
    data_solicitacao date DEFAULT CURRENT_DATE,
    observacoes text,
    ultima_atualizacao timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    tabela_id integer,
    usuario_atualizacao_id bigint -- Necessário para a trigger de histórico saber quem alterou
);

CREATE TABLE public.tabelas_juros (
    tabela_id bigint DEFAULT nextval('public.tabelas_juros_tabela_id_seq'::regclass) NOT NULL,
    banco_id bigint NOT NULL,
    nome_tabela character varying(100) NOT NULL,
    taxa_mensal numeric(6,4) NOT NULL,
    idade_minima integer DEFAULT 18,
    idade_maxima integer DEFAULT 100,
    renda_minima numeric(12,2) DEFAULT 0.00,
    prazo_maximo integer DEFAULT 96,
    ativo boolean DEFAULT true,
    criado_em timestamp(6) without time zone
);

-- ==========================================
-- 4. VÍNCULOS DE SEQUÊNCIAS (OWNERSHIP)
-- ==========================================
ALTER SEQUENCE public.usuarios_usuario_id_seq OWNED BY public.usuarios.usuario_id;
ALTER SEQUENCE public.bancos_banco_id_seq OWNED BY public.bancos.banco_id;
ALTER SEQUENCE public.comissoes_comissao_id_seq OWNED BY public.comissoes.comissao_id;
ALTER SEQUENCE public.documentos_proponente_documento_id_seq OWNED BY public.documentos_proponente.documento_id;
ALTER SEQUENCE public.historico_status_proposta_historico_id_seq OWNED BY public.historico_status_proposta.historico_id;
ALTER SEQUENCE public.interacoes_contato_interacao_id_seq OWNED BY public.interacoes_contato.interacao_id;
ALTER SEQUENCE public.proponentes_proponente_id_seq OWNED BY public.proponentes.proponente_id;
ALTER SEQUENCE public.propostas_proposta_id_seq OWNED BY public.propostas.proposta_id;
ALTER SEQUENCE public.tabelas_juros_tabela_id_seq OWNED BY public.tabelas_juros.tabela_id;

-- ==========================================
-- 5. CHAVES PRIMÁRIAS E ÚNICAS
-- ==========================================
ALTER TABLE ONLY public.usuarios ADD CONSTRAINT usuarios_pkey PRIMARY KEY (usuario_id);
ALTER TABLE ONLY public.usuarios ADD CONSTRAINT usuarios_email_key UNIQUE (email);
ALTER TABLE ONLY public.bancos ADD CONSTRAINT bancos_pkey PRIMARY KEY (banco_id);
ALTER TABLE ONLY public.comissoes ADD CONSTRAINT comissoes_pkey PRIMARY KEY (comissao_id);
ALTER TABLE ONLY public.documentos_proponente ADD CONSTRAINT documentos_proponente_pkey PRIMARY KEY (documento_id);
ALTER TABLE ONLY public.historico_status_proposta ADD CONSTRAINT historico_status_proposta_pkey PRIMARY KEY (historico_id);
ALTER TABLE ONLY public.interacoes_contato ADD CONSTRAINT interacoes_contato_pkey PRIMARY KEY (interacao_id);
ALTER TABLE ONLY public.proponentes ADD CONSTRAINT proponentes_cpf_usuario_key UNIQUE (cpf, usuario_id); 
ALTER TABLE ONLY public.proponentes ADD CONSTRAINT proponentes_pkey PRIMARY KEY (proponente_id);
ALTER TABLE ONLY public.propostas ADD CONSTRAINT propostas_pkey PRIMARY KEY (proposta_id);
ALTER TABLE ONLY public.tabelas_juros ADD CONSTRAINT tabelas_juros_pkey PRIMARY KEY (tabela_id);

-- ==========================================
-- 6. ÍNDICES
-- ==========================================
CREATE INDEX idx_comissoes_status_data ON public.comissoes USING btree (status_pagamento, data_previsao_pagamento);
CREATE INDEX idx_interacoes_contexto ON public.interacoes_contato USING btree (proponente_id, data_interacao DESC);
CREATE INDEX idx_proponentes_cpf_ativo ON public.proponentes USING btree (cpf) WHERE (deletado_em IS NULL);
CREATE INDEX idx_propostas_status_busca ON public.propostas USING btree (status);
CREATE INDEX idx_propostas_usuario ON public.propostas USING btree (usuario_id);
CREATE INDEX idx_proponentes_usuario ON public.proponentes USING btree (usuario_id);

-- ==========================================
-- 7. CHAVES ESTRANGEIRAS (FOREIGN KEYS)
-- ==========================================
ALTER TABLE ONLY public.comissoes ADD CONSTRAINT comissoes_proposta_id_fkey FOREIGN KEY (proposta_id) REFERENCES public.propostas(proposta_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.comissoes ADD CONSTRAINT comissoes_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(usuario_id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.documentos_proponente ADD CONSTRAINT documentos_proponente_proponente_id_fkey FOREIGN KEY (proponente_id) REFERENCES public.proponentes(proponente_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.documentos_proponente ADD CONSTRAINT documentos_proponente_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(usuario_id) ON DELETE SET NULL;

ALTER TABLE ONLY public.historico_status_proposta ADD CONSTRAINT historico_status_proposta_proposta_id_fkey FOREIGN KEY (proposta_id) REFERENCES public.propostas(proposta_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.historico_status_proposta ADD CONSTRAINT historico_status_proposta_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(usuario_id) ON DELETE SET NULL;

ALTER TABLE ONLY public.interacoes_contato ADD CONSTRAINT interacoes_contato_proponente_id_fkey FOREIGN KEY (proponente_id) REFERENCES public.proponentes(proponente_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.interacoes_contato ADD CONSTRAINT interacoes_contato_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(usuario_id) ON DELETE SET NULL;

ALTER TABLE ONLY public.proponentes ADD CONSTRAINT proponentes_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(usuario_id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.propostas ADD CONSTRAINT propostas_banco_id_fkey FOREIGN KEY (banco_id) REFERENCES public.bancos(banco_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public.propostas ADD CONSTRAINT propostas_proponente_id_fkey FOREIGN KEY (proponente_id) REFERENCES public.proponentes(proponente_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public.propostas ADD CONSTRAINT propostas_tabela_id_fkey FOREIGN KEY (tabela_id) REFERENCES public.tabelas_juros(tabela_id);
ALTER TABLE ONLY public.propostas ADD CONSTRAINT propostas_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(usuario_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public.propostas ADD CONSTRAINT propostas_usuario_atualizacao_id_fkey FOREIGN KEY (usuario_atualizacao_id) REFERENCES public.usuarios(usuario_id) ON DELETE SET NULL;

ALTER TABLE ONLY public.tabelas_juros ADD CONSTRAINT tabelas_juros_banco_id_fkey FOREIGN KEY (banco_id) REFERENCES public.bancos(banco_id) ON DELETE CASCADE;

-- ==========================================
-- 8. FUNÇÕES E TRIGGERS
-- ==========================================
CREATE FUNCTION public.trg_log_status_change() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- O Spring/Hibernate deve enviar o usuario_atualizacao_id no momento do UPDATE
    IF (OLD.status IS DISTINCT FROM NEW.status) THEN
        INSERT INTO public.historico_status_proposta(proposta_id, usuario_id, status_anterior, status_novo)
        VALUES (NEW.proposta_id, NEW.usuario_atualizacao_id, OLD.status::text, NEW.status::text);
    END IF;
    NEW.ultima_atualizacao = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trigger_status_update BEFORE UPDATE ON public.propostas FOR EACH ROW EXECUTE FUNCTION public.trg_log_status_change();