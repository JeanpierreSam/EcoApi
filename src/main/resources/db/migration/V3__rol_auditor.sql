-- SC-B: rol AUDITOR con todos los privilegios de lectura y ninguno de escritura.
-- Se agrega donde viven los privilegios (rol_privilegio): ni SecurityConfig ni los
-- controladores cambian, porque ya autorizan por privilegio y no por nombre de rol.

-- Lectura de rutas ajenas: el auditor no tiene ruta asignada, pero debe poder revisar la del dia.
INSERT INTO privilegio (nombre) VALUES ('RUTA_LEER_TODAS');

INSERT INTO rol (nombre) VALUES ('AUDITOR');

INSERT INTO rol_privilegio (rol_id, privilegio_id)
SELECT r.id, p.id FROM rol r JOIN privilegio p ON
    (r.nombre = 'AUDITOR' AND p.nombre IN ('PUNTO_LEER', 'PESAJE_LEER', 'RUTA_LEER_TODAS'))
 OR (r.nombre = 'ADMINISTRADOR' AND p.nombre = 'RUTA_LEER_TODAS');

INSERT INTO usuario (username, password, nombre, rol_id) VALUES
    ('auditor1', crypt('Eco2026*', gen_salt('bf', 10)), 'Martha Rojas Vega', (SELECT id FROM rol WHERE nombre = 'AUDITOR'));
