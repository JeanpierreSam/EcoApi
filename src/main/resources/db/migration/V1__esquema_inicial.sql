-- Seguridad: el rol es un conjunto de privilegios y la autorizacion
-- se verifica siempre contra el privilegio (rol_privilegio).
CREATE TABLE privilegio (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(40) NOT NULL UNIQUE
);

CREATE TABLE rol (
    id     BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE rol_privilegio (
    rol_id        BIGINT NOT NULL REFERENCES rol (id),
    privilegio_id BIGINT NOT NULL REFERENCES privilegio (id),
    PRIMARY KEY (rol_id, privilegio_id)
);

CREATE TABLE usuario (
    id       BIGSERIAL PRIMARY KEY,
    username VARCHAR(40)  NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,          -- hash BCrypt, nunca en claro
    nombre   VARCHAR(100) NOT NULL,
    rol_id   BIGINT       NOT NULL REFERENCES rol (id),
    activo   BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Negocio
CREATE TABLE punto_acopio (
    id                BIGSERIAL PRIMARY KEY,
    razon_social      VARCHAR(120) NOT NULL,
    direccion         VARCHAR(200) NOT NULL,
    zona              VARCHAR(40)  NOT NULL,
    material_habitual VARCHAR(40)  NOT NULL
);

CREATE TABLE ruta (
    id          BIGSERIAL PRIMARY KEY,
    fecha       DATE        NOT NULL,
    operario_id BIGINT      NOT NULL REFERENCES usuario (id),
    supervisor_id BIGINT    NOT NULL REFERENCES usuario (id),
    estado      VARCHAR(10) NOT NULL CHECK (estado IN ('ABIERTA', 'CERRADA')),
    UNIQUE (fecha, operario_id)
);

CREATE TABLE ruta_punto (
    id             BIGSERIAL PRIMARY KEY,
    ruta_id        BIGINT  NOT NULL REFERENCES ruta (id),
    punto_id       BIGINT  NOT NULL REFERENCES punto_acopio (id),
    secuencia      INTEGER NOT NULL,
    motivo_omision VARCHAR(200),
    UNIQUE (ruta_id, punto_id),
    UNIQUE (ruta_id, secuencia)
);

CREATE TABLE pesaje (
    id               BIGSERIAL PRIMARY KEY,
    ruta_id          BIGINT        NOT NULL REFERENCES ruta (id),
    punto_id         BIGINT        NOT NULL REFERENCES punto_acopio (id),
    usuario_id       BIGINT        NOT NULL REFERENCES usuario (id),
    fecha_hora       TIMESTAMP     NOT NULL,
    material         VARCHAR(40)   NOT NULL,
    peso_bruto       NUMERIC(10,2) NOT NULL,
    tara             NUMERIC(10,2) NOT NULL,
    peso_neto        NUMERIC(10,2) NOT NULL CHECK (peso_neto > 0),  -- RN-03 tambien en la base
    observacion      VARCHAR(250),
    anulado          BOOLEAN       NOT NULL DEFAULT FALSE,
    motivo_anulacion VARCHAR(200)
);

CREATE INDEX idx_pesaje_punto ON pesaje (punto_id, fecha_hora DESC);
CREATE INDEX idx_pesaje_ruta ON pesaje (ruta_id);
