-- Esquema del Planificador de Eventos (Supabase/PostgreSQL).
-- Ejecutar en el SQL Editor de Supabase. Compatible con spring.jpa.hibernate.ddl-auto=validate.
-- Mantener sincronizado con las entidades JPA en src/main/java/uv/isj/planificadoreventosbackend/model/.

CREATE TABLE IF NOT EXISTS tipo_evento (
    id_tipo_evento SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO tipo_evento (nombre)
VALUES ('Boda'), ('Corporativo'), ('Social'), ('Cumpleaños'), ('Otro')
ON CONFLICT (nombre) DO NOTHING;

CREATE TABLE IF NOT EXISTS usuario (
    id_usuario SERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    limite_horas_diarias INT NOT NULL DEFAULT 6 CONSTRAINT chk_limite_horas CHECK (limite_horas_diarias BETWEEN 1 AND 16)
);

CREATE TABLE IF NOT EXISTS evento (
    id_evento SERIAL PRIMARY KEY,
    id_usuario INT NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    id_tipo_evento INT NOT NULL REFERENCES tipo_evento(id_tipo_evento),
    nombre VARCHAR(150) NOT NULL,
    cliente VARCHAR(150) NOT NULL,
    fecha_evento TIMESTAMP NOT NULL,
    lugar VARCHAR(255) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS subtarea (
    id_subtarea SERIAL PRIMARY KEY,
    id_evento INT NOT NULL REFERENCES evento(id_evento) ON DELETE CASCADE,
    nombre_gestion VARCHAR(200) NOT NULL,
    fecha_objetivo DATE NOT NULL,
    horas_estimadas INT NOT NULL CONSTRAINT chk_horas_positivas CHECK (horas_estimadas > 0),
    estado VARCHAR(20) NOT NULL DEFAULT 'pendiente' CONSTRAINT chk_estado CHECK (estado IN ('pendiente', 'ejecutada', 'pospuesta')),
    nota_explicativa TEXT,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);