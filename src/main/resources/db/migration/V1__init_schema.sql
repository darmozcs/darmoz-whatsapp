-- Cuentas de Meta (una app de Meta / WhatsApp Business Account). Los campos
-- *_enc van cifrados con AES-GCM (WA_SECRETS_KEY), nunca en claro.
CREATE TABLE wa_meta_account (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    app_id VARCHAR(64),
    app_secret_enc TEXT NOT NULL,
    access_token_enc TEXT NOT NULL,
    verify_token VARCHAR(100) NOT NULL,
    waba_id VARCHAR(64) NOT NULL,
    graph_api_version VARCHAR(10) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_validated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Integracion con una Application de darmoz-auth: a donde y con que API key
-- llaman los nodos http de sus bots. application_id no es FK (otro servicio).
CREATE TABLE wa_app_integration (
    id UUID PRIMARY KEY,
    application_id UUID NOT NULL UNIQUE,
    application_name VARCHAR(150) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    auth_header_name VARCHAR(100),
    auth_header_value_enc TEXT,
    timeout_ms INTEGER NOT NULL DEFAULT 10000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE wa_bot (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    application_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    entry_flow_id UUID,
    fallback_message TEXT,
    session_timeout_minutes INTEGER NOT NULL DEFAULT 60,
    business_hours JSONB,
    out_of_hours_message TEXT,
    static_variables JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_wa_bot_application_id ON wa_bot (application_id);

CREATE TABLE wa_flow (
    id UUID PRIMARY KEY,
    bot_id UUID NOT NULL REFERENCES wa_bot (id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    draft_definition JSONB NOT NULL,
    published_definition JSONB,
    published_version INTEGER NOT NULL DEFAULT 0,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_wa_flow_bot_id ON wa_flow (bot_id);

ALTER TABLE wa_bot ADD CONSTRAINT fk_wa_bot_entry_flow
    FOREIGN KEY (entry_flow_id) REFERENCES wa_flow (id) ON DELETE SET NULL;

CREATE TABLE wa_phone_number (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES wa_meta_account (id) ON DELETE CASCADE,
    phone_number_id VARCHAR(64) NOT NULL UNIQUE,
    display_number VARCHAR(40),
    verified_name VARCHAR(150),
    bot_id UUID REFERENCES wa_bot (id) ON DELETE SET NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_wa_phone_number_account_id ON wa_phone_number (account_id);
CREATE INDEX idx_wa_phone_number_bot_id ON wa_phone_number (bot_id);

-- Estado del bot por contacto y numero: en que nodo esta y que variables junto.
CREATE TABLE wa_conversation (
    id UUID PRIMARY KEY,
    phone_number_id UUID NOT NULL REFERENCES wa_phone_number (id) ON DELETE CASCADE,
    contact_wa_id VARCHAR(32) NOT NULL,
    contact_name VARCHAR(150),
    current_flow_id UUID REFERENCES wa_flow (id) ON DELETE SET NULL,
    current_node_id VARCHAR(64),
    variables JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(20) NOT NULL,
    last_inbound_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_wa_conversation UNIQUE (phone_number_id, contact_wa_id),
    CONSTRAINT chk_wa_conversation_status CHECK (status IN ('ACTIVE', 'HANDOFF', 'CLOSED'))
);

-- Cada mensaje entrante o saliente. wa_message_id UNIQUE hace idempotente el
-- webhook (Meta reintenta si no respondemos rapido).
CREATE TABLE wa_message_log (
    id UUID PRIMARY KEY,
    conversation_id UUID REFERENCES wa_conversation (id) ON DELETE CASCADE,
    phone_number_id UUID REFERENCES wa_phone_number (id) ON DELETE SET NULL,
    direction VARCHAR(3) NOT NULL,
    wa_message_id VARCHAR(128) UNIQUE,
    contact_wa_id VARCHAR(32),
    type VARCHAR(30) NOT NULL,
    payload JSONB,
    status VARCHAR(20) NOT NULL,
    error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_wa_message_log_direction CHECK (direction IN ('IN', 'OUT')),
    CONSTRAINT chk_wa_message_log_status CHECK (status IN ('RECEIVED', 'SENT', 'DELIVERED', 'READ', 'FAILED'))
);

CREATE INDEX idx_wa_message_log_conversation ON wa_message_log (conversation_id, created_at);
CREATE INDEX idx_wa_message_log_created_at ON wa_message_log (created_at);
