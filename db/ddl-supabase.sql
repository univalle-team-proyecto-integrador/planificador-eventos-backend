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
    horas_estimadas INT NOT NULL DEFAULT 6 CONSTRAINT chk_horas_evento CHECK (horas_estimadas > 0),
    lugar VARCHAR(255) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Migración 2026-10-08: horas estimadas del evento. Ejecutar UNA sola vez sobre
-- una base que ya tenga la tabla evento creada. El DEFAULT 6 es necesario para
-- que no falle sobre filas existentes; el DROP DEFAULT hace que a partir de
-- aquí la columna deje de rellenarse sola, que es lo que valida el DTO.
-- ALTER TABLE evento ADD COLUMN horas_estimadas INT NOT NULL DEFAULT 6;
-- ALTER TABLE evento ALTER COLUMN horas_estimadas DROP DEFAULT;
-- ALTER TABLE evento ADD CONSTRAINT chk_horas_evento CHECK (horas_estimadas > 0);

CREATE TABLE IF NOT EXISTS subtarea (
    id_subtarea SERIAL PRIMARY KEY,
    id_evento INT NOT NULL REFERENCES evento(id_evento) ON DELETE CASCADE,
    nombre_gestion VARCHAR(200) NOT NULL,
    fecha_objetivo DATE NOT NULL,
    fecha_objetivo_original DATE,
    horas_estimadas INT NOT NULL CONSTRAINT chk_horas_positivas CHECK (horas_estimadas > 0),
    estado VARCHAR(20) NOT NULL DEFAULT 'pendiente' CONSTRAINT chk_estado CHECK (estado IN ('pendiente', 'ejecutada', 'pospuesta')),
    nota_explicativa TEXT,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Migración 2026-10-09: fecha con la que la gestión se planificó por primera vez.
-- Es la que permite derivar el desfase (postergada / adelantada / reprogramada)
-- sin duplicar ese estado en otra columna. Ejecutar UNA sola vez, ANTES de
-- desplegar el backend: con ddl-auto=validate la app no arranca si la entidad
-- declara una columna que la base todavía no tiene.
-- Es nullable a propósito: las subtareas ya creadas no tienen línea base
-- recuperable y no se inventa ninguna. Se llena sola al próximo cambio de fecha.
-- ALTER TABLE subtarea ADD COLUMN fecha_objetivo_original DATE;