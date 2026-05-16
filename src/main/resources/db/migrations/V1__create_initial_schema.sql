-- Script de inicialização do banco de dados (Poder Financeiro) - Multi-Usuário

-- ==========================================
-- 1. TIPOS E ENUMS
-- ==========================================
-- Enum para gerenciar o ciclo de vida da pessoa no seu funil
CREATE TYPE public.tipo_relacionamento_enum AS ENUM (
    'LEAD',
    'PROPONENTE',
    'CLIENTE'
);

-- Status da proposta
CREATE TYPE public.status_proposta_enum AS ENUM (
    'DIGITADA',
    'PENDENTE',
    'ANALISE_BANCO',
    'AGUARDANDO_DOC',
    'APROVADA',
    'REPROVADA',
    'PAGO',
    'CANCELADO'
);

-- Enum para a origem da captação (Lead)
CREATE TYPE public.origem_consentimento_enum AS ENUM (
    'WHATSAPP',
    'PANFLETO',
    'INDICACAO',
    'FACEBOOK',
    'PASSOU_NA_PORTA'
);

-- Enum para o tipo de vínculo do proponente
CREATE TYPE public.tipo_vinculo_enum AS ENUM (
    'APOSENTADO', 
    'PENSIONISTA', 
    'SERVIDOR_ATIVO', 
    'MILITAR', 
    'CLT'
);

CREATE TYPE public.tipo_convenio_enum AS ENUM (
    'INSS_CONSIGNADO',
    'CLT_CONSIGNADO',
    'BOLSA_FAMILIA',
    'CREDITO_PESSOAL',
    'CONTA_LUZ',
    'SIAPE',
    'PADRAO'
);

-- Enum para os Estados Brasileiros (UF)
CREATE TYPE public.uf_enum AS ENUM (
    'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 
    'MA', 'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 
    'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
);

-- Enum para Tipos de Logradouro
CREATE TYPE public.tipo_logradouro_enum AS ENUM (
    'RUA', 'AVENIDA', 'TRAVESSA', 'ALAMEDA', 'PRACA', 
    'RODOVIA', 'ESTRADA', 'BECO', 'LOTEAMENTO', 'OUTRO'
);

-- Enum para Categorias de Links
CREATE TYPE public.categoria_link_enum AS ENUM (
    'BANCO',
    'GOVERNO',
    'CONSULTA',
    'INTERNO',
    'OUTROS'
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
CREATE SEQUENCE public.enderecos_proponente_endereco_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.links_uteis_link_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- ==========================================
-- 3. TABELAS
-- ==========================================
CREATE TABLE public.usuarios (
    usuario_id bigint DEFAULT nextval('public.usuarios_usuario_id_seq'::regclass) NOT NULL,
    username character varying(50) NOT NULL,
    nome character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    senha_hash character varying(255) NOT NULL,
    papel character varying(50) DEFAULT 'CONSULTOR'::character varying,
    ativo boolean DEFAULT true,
    criado_em timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    ultimo_acesso timestamp without time zone,
    CONSTRAINT usuarios_username_key UNIQUE (username)
);

CREATE TABLE public.bancos (
    banco_id bigint DEFAULT nextval('public.bancos_banco_id_seq'::regclass) NOT NULL,
    codigo_banco character varying(10),
    nome_banco character varying(100) NOT NULL,
    link_portal_banco text,
    telefone_suporte character varying(50),
    sistema_amortizacao character varying(50) DEFAULT 'Price'::character varying,
    permite_pos_fixado boolean DEFAULT false,
    ativo boolean DEFAULT true,
    criado_em timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.comissoes (
    comissao_id bigint DEFAULT nextval('public.comissoes_comissao_id_seq'::regclass) NOT NULL,
    proposta_id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    
    -- Financeiro (Valores)
    valor_bruto_comissao numeric(12,2) NOT NULL,
    impostos_retidos numeric(12,2) DEFAULT 0.00,
    valor_liquido_consultor numeric(12,2) NOT NULL,
    valor_pago_pela_poder numeric(12,2) DEFAULT 0.00, -- Confirmação do valor final transferido
    contestada boolean DEFAULT false,
    
    -- Marco 1: Fluxo Banco -> Correspondente (Quarta-feira)
    data_recebimento_banco timestamp without time zone,
    
    -- Marco 2: Fluxo de Conferência/Auditoria (Quinta-feira)
    verificado_consultor boolean DEFAULT false,
    data_verificacao timestamp without time zone,
    
    -- Marco 3: Fluxo Correspondente -> Consultor (Sexta-feira)
    previsao_pagamento date, -- Data alvo (Sexta)
    data_pagamento_consultor timestamp without time zone, -- Liquidação final
    
    -- Controle de Estado, Auditoria e Ciclos
    status_pagamento character varying(20) DEFAULT 'Pendente'::character varying,
    ciclo_referencia character varying(10),
    data_limite_contestacao timestamp without time zone,
    observacao_ajuste text,
    criado_em timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.documentos_proponente (
    documento_id bigint DEFAULT nextval('public.documentos_proponente_documento_id_seq'::regclass) NOT NULL,
    proponente_id bigint,
    proposta_id bigint, -- NOVO: Vincula o exame à internação
    usuario_id bigint NOT NULL,
    tipo_documento character varying(50) NOT NULL,
    arquivo_path text NOT NULL,
    hash_sha256 character varying(64),
    verificado boolean DEFAULT false,
    data_upload timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.historico_status_proposta (
    historico_id bigint DEFAULT nextval('public.historico_status_proposta_historico_id_seq'::regclass) NOT NULL,
    proposta_id bigint,
    usuario_id bigint,
    status_anterior character varying(50),
    status_novo character varying(50) NOT NULL,
    data_mudanca timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    motivo_mudanca text
);

CREATE TABLE public.interacoes_contato (
    interacao_id bigint DEFAULT nextval('public.interacoes_contato_interacao_id_seq'::regclass) NOT NULL,
    proponente_id bigint,
    usuario_id bigint NOT NULL,
    canal character varying(20) DEFAULT 'WhatsApp'::character varying,
    mensagem_texto text,
    direcao character varying(10),
    data_interacao timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.proponentes (
    proponente_id bigint DEFAULT nextval('public.proponentes_proponente_id_seq'::regclass) NOT NULL,
    usuario_id bigint NOT NULL,
    nome_completo character varying(255) NOT NULL,
    cpf character varying(14) NOT NULL,
    telefone character varying(20),
    renda_mensal numeric(12,2),
    tipo_vinculo public.tipo_vinculo_enum DEFAULT 'CLT'::public.tipo_vinculo_enum,
    matricula character varying(50),
    origem_consentimento public.origem_consentimento_enum DEFAULT 'WHATSAPP'::public.origem_consentimento_enum,
    classificacao public.tipo_relacionamento_enum DEFAULT 'LEAD'::public.tipo_relacionamento_enum,
    indicado_por_id bigint,
    data_cadastro timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deletado_em timestamp without time zone,
    data_nascimento date
);

CREATE TABLE public.propostas (
    proposta_id bigint DEFAULT nextval('public.propostas_proposta_id_seq'::regclass) NOT NULL,
    proponente_id bigint NOT NULL,
    banco_id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    convenio_orgao public.tipo_convenio_enum DEFAULT 'PADRAO'::public.tipo_convenio_enum,
    valor_solicitado numeric(12,2) NOT NULL,
    valor_aprovado numeric(12,2),
    taxa_aplicada numeric(5,2),
    quantidade_parcelas integer,
    status public.status_proposta_enum DEFAULT 'DIGITADA'::public.status_proposta_enum,
    coeficiente numeric(10,6),
    valor_parcela numeric(12,2),
    modalidade_juros character varying(20) DEFAULT 'Prefixado'::character varying,
    custo_efetivo_total numeric(5,2),
    margem_utilizada numeric(12,2),
    eh_novacao boolean DEFAULT false,
    saldo_quitacao_anterior numeric(12,2) DEFAULT 0.00,
    valor_iof numeric(12,2) DEFAULT 0.00,
    taxa_administracao numeric(12,2) DEFAULT 0.00,
    comissao_estimada numeric(12,2) DEFAULT 0.00,
    valor_final_cliente numeric(12,2) DEFAULT 0.00,
    data_solicitacao date DEFAULT CURRENT_DATE,
    observacoes text,
    ultima_atualizacao timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    tabela_id bigint,
    usuario_atualizacao_id bigint
);

CREATE TABLE public.tabelas_juros (
    tabela_id bigint DEFAULT nextval('public.tabelas_juros_tabela_id_seq'::regclass) NOT NULL,
    banco_id bigint NOT NULL,
    nome_tabela character varying(100) NOT NULL,
    taxa_mensal numeric(6,4) NOT NULL,
    idade_minima integer DEFAULT 18,
    idade_maxima integer DEFAULT 100,
    renda_minima numeric(12,2) DEFAULT 0.00,
    prazo_minimo integer DEFAULT 1,
    prazo_maximo integer DEFAULT 96,
    valor_minimo_emprestimo numeric(12,2) DEFAULT 0.00,
    valor_maximo_emprestimo numeric(12,2) DEFAULT 999999.99,
    tipo_convenio public.tipo_convenio_enum DEFAULT 'PADRAO'::public.tipo_convenio_enum,
    comissao_percentual numeric(5,2) DEFAULT 0.00,
    inicio_vigencia date DEFAULT CURRENT_DATE,
    fim_vigencia date,
    ativo boolean DEFAULT true,
    criado_em timestamp(6) without time zone
);

CREATE TABLE public.enderecos_proponente (
    endereco_id bigint DEFAULT nextval('public.enderecos_proponente_endereco_id_seq'::regclass) NOT NULL,
    proponente_id bigint NOT NULL,
    cep character varying(8) NOT NULL,
    tipo_logradouro public.tipo_logradouro_enum DEFAULT 'RUA'::public.tipo_logradouro_enum,
    logradouro character varying(255) NOT NULL,
    numero character varying(20) NOT NULL,
    complemento character varying(100),
    bairro character varying(100) NOT NULL,
    cidade character varying(100) NOT NULL,
    uf public.uf_enum DEFAULT 'SP'::public.uf_enum,
    principal boolean DEFAULT false, -- NOVO: Flag de endereço principal
    criado_em timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    ultima_atualizacao timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.links_uteis (
    link_id bigint DEFAULT nextval('public.links_uteis_link_id_seq'::regclass) NOT NULL,
    titulo character varying(100) NOT NULL,
    url text NOT NULL,
    descricao character varying(255),
    categoria public.categoria_link_enum DEFAULT 'OUTROS'::public.categoria_link_enum,
    tipo_convenio public.tipo_convenio_enum, -- NOVO: Filtro inteligente (Pode ser nulo para links gerais)
    tags_busca character varying(255),
    criado_em timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT links_uteis_pkey PRIMARY KEY (link_id)
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
ALTER SEQUENCE public.enderecos_proponente_endereco_id_seq OWNED BY public.enderecos_proponente.endereco_id;
ALTER SEQUENCE public.links_uteis_link_id_seq OWNED BY public.links_uteis.link_id;

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
ALTER TABLE ONLY public.proponentes ADD CONSTRAINT proponentes_pkey PRIMARY KEY (proponente_id);
ALTER TABLE ONLY public.propostas ADD CONSTRAINT propostas_pkey PRIMARY KEY (proposta_id);
ALTER TABLE ONLY public.tabelas_juros ADD CONSTRAINT tabelas_juros_pkey PRIMARY KEY (tabela_id);
ALTER TABLE ONLY public.enderecos_proponente ADD CONSTRAINT enderecos_proponente_pkey PRIMARY KEY (endereco_id);

-- ==========================================
-- 6. ÍNDICES
-- ==========================================
CREATE INDEX idx_comissoes_status_data ON public.comissoes USING btree (status_pagamento, data_pagamento_consultor);
CREATE INDEX idx_comissoes_ciclo ON public.comissoes USING btree (ciclo_referencia);
CREATE INDEX idx_interacoes_contexto ON public.interacoes_contato USING btree (proponente_id, data_interacao DESC);
CREATE INDEX idx_proponentes_cpf_ativo ON public.proponentes USING btree (cpf) WHERE (deletado_em IS NULL);
CREATE INDEX idx_propostas_status_busca ON public.propostas USING btree (status);
CREATE INDEX idx_propostas_usuario ON public.propostas USING btree (usuario_id);
CREATE INDEX idx_proponentes_usuario ON public.proponentes USING btree (usuario_id);
CREATE INDEX idx_enderecos_proponente_id ON public.enderecos_proponente USING btree (proponente_id);
CREATE INDEX idx_enderecos_cep ON public.enderecos_proponente USING btree (cep);
CREATE UNIQUE INDEX proponentes_cpf_usuario_key ON public.proponentes (cpf, usuario_id) WHERE cpf <> '';

-- ==========================================
-- 7. CHAVES ESTRANGEIRAS (FOREIGN KEYS)
-- ==========================================
ALTER TABLE ONLY public.comissoes ADD CONSTRAINT comissoes_proposta_id_fkey FOREIGN KEY (proposta_id) REFERENCES public.propostas(proposta_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.comissoes ADD CONSTRAINT comissoes_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(usuario_id) ON DELETE RESTRICT;
ALTER TABLE ONLY public.documentos_proponente ADD CONSTRAINT documentos_proponente_proponente_id_fkey FOREIGN KEY (proponente_id) REFERENCES public.proponentes(proponente_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.documentos_proponente ADD CONSTRAINT documentos_proponente_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(usuario_id) ON DELETE SET NULL;
ALTER TABLE ONLY public.documentos_proponente ADD CONSTRAINT fk_documento_proposta FOREIGN KEY (proposta_id) REFERENCES public.propostas(proposta_id) ON DELETE CASCADE;
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
ALTER TABLE ONLY public.enderecos_proponente ADD CONSTRAINT enderecos_proponente_proponente_id_fkey FOREIGN KEY (proponente_id) REFERENCES public.proponentes(proponente_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.proponentes ADD CONSTRAINT fk_proponente_indicador FOREIGN KEY (indicado_por_id) REFERENCES public.proponentes(proponente_id);

-- ==========================================
-- 8. FUNÇÕES E TRIGGERS
-- ==========================================
CREATE FUNCTION public.trg_log_status_change() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF (OLD.status IS DISTINCT FROM NEW.status) THEN
        INSERT INTO public.historico_status_proposta(proposta_id, usuario_id, status_anterior, status_novo)
        VALUES (NEW.proposta_id, NEW.usuario_atualizacao_id, OLD.status::text, NEW.status::text);
    END IF;
    NEW.ultima_atualizacao = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION public.trg_update_timestamp()
RETURNS trigger AS $$
BEGIN
    NEW.ultima_atualizacao = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_status_update BEFORE UPDATE ON public.propostas FOR EACH ROW EXECUTE FUNCTION public.trg_log_status_change();
CREATE TRIGGER trigger_update_timestamp_endereco BEFORE UPDATE ON public.enderecos_proponente FOR EACH ROW EXECUTE FUNCTION public.trg_update_timestamp();

-- ==========================================
-- 9. COMANDOS DE MANUTENÇÃO E MIGRAÇÃO
-- ==========================================
-- Comandos para atualizar base existente caso os campos não tenham sido criados acima

-- 1. Vincular tabelas de juros ao tipo de produto/convênio
DO $$ BEGIN
    ALTER TABLE public.tabelas_juros ADD COLUMN tipo_convenio public.tipo_convenio_enum DEFAULT 'PADRAO';
EXCEPTION WHEN duplicate_column THEN RAISE NOTICE 'Coluna tipo_convenio já existe em tabelas_juros.';
END $$;

-- 2. Melhorar a visibilidade da comissão na proposta
DO $$ BEGIN
    ALTER TABLE public.propostas ADD COLUMN comissao_estimada numeric(12,2) DEFAULT 0.00;
    ALTER TABLE public.propostas ADD COLUMN valor_final_cliente numeric(12,2) DEFAULT 0.00;
EXCEPTION WHEN duplicate_column THEN RAISE NOTICE 'Colunas de comissão/valor final já existem em propostas.';
END $$;

-- 3. Sistema de Indicações
DO $$ BEGIN
    ALTER TABLE public.proponentes ADD COLUMN indicado_por_id bigint REFERENCES public.proponentes(proponente_id);
EXCEPTION WHEN duplicate_column THEN RAISE NOTICE 'Coluna indicado_por_id já existe em proponentes.';
END $$;

-- 4. Detalhamento da conferência de valores
DO $$ BEGIN
    ALTER TABLE public.comissoes ADD COLUMN valor_pago_pela_poder numeric(12,2) DEFAULT 0.00;
    ALTER TABLE public.comissoes ADD COLUMN contestada boolean DEFAULT false;
EXCEPTION WHEN duplicate_column THEN RAISE NOTICE 'Colunas de conferência já existem em comissoes.';
END $$;

-- 5. Versionamento das Tabelas de Juros
DO $$ BEGIN
    ALTER TABLE public.tabelas_juros ADD COLUMN inicio_vigencia date DEFAULT CURRENT_DATE;
    ALTER TABLE public.tabelas_juros ADD COLUMN fim_vigencia date;
EXCEPTION WHEN duplicate_column THEN RAISE NOTICE 'Colunas de vigência já existem em tabelas_juros.';
END $$;

-- 6. Movendo e adicionando as faixas de comissão (Tabela de Juros e Bancos)
DO $$ BEGIN
    ALTER TABLE public.bancos DROP COLUMN IF EXISTS comissao_percentual;
EXCEPTION WHEN undefined_column THEN RAISE NOTICE 'Coluna comissao_percentual já foi removida de bancos.';
END $$;

DO $$ BEGIN
    ALTER TABLE public.tabelas_juros ADD COLUMN comissao_percentual numeric(5,2) DEFAULT 0.00;
    ALTER TABLE public.tabelas_juros ADD COLUMN valor_minimo_emprestimo numeric(12,2) DEFAULT 0.00;
    ALTER TABLE public.tabelas_juros ADD COLUMN valor_maximo_emprestimo numeric(12,2) DEFAULT 999999.99;
EXCEPTION WHEN duplicate_column THEN RAISE NOTICE 'Colunas de faixa de comissão já existem em tabelas_juros.';
END $$;

-- 7. Atualização da tabela de Bancos (Clean Architecture)
DO $$ BEGIN
    ALTER TABLE public.bancos ADD COLUMN codigo_banco character varying(10);
    ALTER TABLE public.bancos ADD COLUMN telefone_suporte character varying(50);
EXCEPTION WHEN duplicate_column THEN RAISE NOTICE 'Colunas codigo/telefone já existem.';
END $$;

DO $$ BEGIN
    ALTER TABLE public.bancos DROP COLUMN IF EXISTS taxa_media_juros;
    ALTER TABLE public.bancos DROP COLUMN IF EXISTS taxa_minima;
    ALTER TABLE public.bancos DROP COLUMN IF EXISTS taxa_maxima;
    ALTER TABLE public.bancos DROP COLUMN IF EXISTS prazo_maximo;
    ALTER TABLE public.bancos DROP COLUMN IF EXISTS comissao_percentual;
EXCEPTION WHEN undefined_column THEN RAISE NOTICE 'Colunas antigas já foram removidas.';
END $$;