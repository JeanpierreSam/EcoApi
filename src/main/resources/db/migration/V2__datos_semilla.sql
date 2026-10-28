-- Datos semilla del anexo 10. Las fechas son relativas a CURRENT_DATE para que
-- «la ruta del dia» exista el dia en que se crea la base (ver README: reinicio).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO privilegio (nombre) VALUES
    ('PUNTO_LEER'), ('PESAJE_LEER'), ('PESAJE_CREAR'), ('PESAJE_EDITAR'),
    ('PESAJE_EDITAR_TODOS'), ('PESAJE_ANULAR'), ('RUTA_CERRAR'), ('PUNTO_GESTIONAR');

INSERT INTO rol (nombre) VALUES ('OPERARIO'), ('SUPERVISOR'), ('ADMINISTRADOR');

-- Matriz de privilegios de la seccion 3.3. PESAJE_EDITAR_TODOS distingue
-- «Todos» de «Solo los propios» (RN-02) sin preguntar por el nombre del rol.
INSERT INTO rol_privilegio (rol_id, privilegio_id)
SELECT r.id, p.id FROM rol r JOIN privilegio p ON
    (r.nombre = 'OPERARIO' AND p.nombre IN ('PUNTO_LEER', 'PESAJE_LEER', 'PESAJE_CREAR', 'PESAJE_EDITAR'))
 OR (r.nombre = 'SUPERVISOR' AND p.nombre IN ('PUNTO_LEER', 'PESAJE_LEER', 'PESAJE_CREAR', 'PESAJE_EDITAR',
                                              'PESAJE_EDITAR_TODOS', 'PESAJE_ANULAR', 'RUTA_CERRAR'))
 OR (r.nombre = 'ADMINISTRADOR' AND p.nombre IN ('PUNTO_LEER', 'PESAJE_LEER', 'PUNTO_GESTIONAR'));

-- Contrasena Eco2026* cifrada con BCrypt ($2a$, compatible con BCryptPasswordEncoder)
INSERT INTO usuario (username, password, nombre, rol_id) VALUES
    ('operario1',   crypt('Eco2026*', gen_salt('bf', 10)), 'Carlos Quispe Mamani',  (SELECT id FROM rol WHERE nombre = 'OPERARIO')),
    ('operario2',   crypt('Eco2026*', gen_salt('bf', 10)), 'Rosa Huaman Ccori',     (SELECT id FROM rol WHERE nombre = 'OPERARIO')),
    ('supervisor1', crypt('Eco2026*', gen_salt('bf', 10)), 'Lucia Torres Paredes',  (SELECT id FROM rol WHERE nombre = 'SUPERVISOR')),
    ('admin',       crypt('Eco2026*', gen_salt('bf', 10)), 'Administrador EcoRecicla', (SELECT id FROM rol WHERE nombre = 'ADMINISTRADOR'));

-- 12 puntos de acopio en 3 zonas
INSERT INTO punto_acopio (razon_social, direccion, zona, material_habitual) VALUES
    ('Textiles Andinos SAC',        'Av. Argentina 1520, Cercado',      'NORTE',  'CARTON'),
    ('Distribuidora El Sol EIRL',   'Jr. Huaraz 340, Brena',            'NORTE',  'PLASTICO'),
    ('Imprenta Grafica Lima SA',    'Av. Venezuela 2210, Brena',        'NORTE',  'PAPEL'),
    ('Supermercados Vega SAC',      'Av. Tupac Amaru 880, Independencia','NORTE', 'CARTON'),
    ('Clinica San Gabriel SA',      'Av. Universitaria 4100, Los Olivos','NORTE', 'PLASTICO'),
    ('Bebidas del Valle SAC',       'Calle Los Pinos 77, San Martin',   'NORTE',  'VIDRIO'),
    ('Ferreteria Central SRL',      'Av. Abancay 456, Cercado',         'CENTRO', 'METAL'),
    ('Hotel Plaza Mayor SA',        'Jr. de la Union 900, Cercado',     'CENTRO', 'VIDRIO'),
    ('Oficinas Torre Lima SAC',     'Av. Arequipa 1200, Lince',         'CENTRO', 'PAPEL'),
    ('Restaurante El Fogon EIRL',   'Av. Brasil 2300, Jesus Maria',     'CENTRO', 'PLASTICO'),
    ('Colegio Santa Rosa',          'Av. Los Proceres 150, Surco',      'SUR',    'PAPEL'),
    ('Muebles Modernos SAC',        'Av. Pachacutec 3300, VMT',         'SUR',    'CARTON');

-- Ruta del dia anterior, ya cerrada (para RN-01) y ruta de hoy, abierta
INSERT INTO ruta (fecha, operario_id, supervisor_id, estado) VALUES
    (CURRENT_DATE - 1, (SELECT id FROM usuario WHERE username = 'operario1'),
                       (SELECT id FROM usuario WHERE username = 'supervisor1'), 'CERRADA'),
    (CURRENT_DATE,     (SELECT id FROM usuario WHERE username = 'operario1'),
                       (SELECT id FROM usuario WHERE username = 'supervisor1'), 'ABIERTA');

INSERT INTO ruta_punto (ruta_id, punto_id, secuencia) VALUES
    (1, 1, 1), (1, 2, 2), (1, 3, 3), (1, 4, 4), (1, 5, 5), (1, 6, 6),
    (2, 7, 1), (2, 8, 2), (2, 9, 3), (2, 10, 4), (2, 11, 5), (2, 12, 6);

-- 15 pesajes: 9 en la ruta cerrada; 6 en la de hoy sobre los puntos 7-9,
-- uno de operario2 (RN-02) y uno anulado. Los puntos 10-12 quedan sin pesar (RN-04).
INSERT INTO pesaje (ruta_id, punto_id, usuario_id, fecha_hora, material, peso_bruto, tara, peso_neto, observacion, anulado, motivo_anulacion) VALUES
    (1, 1, 1, CURRENT_DATE - 1 + TIME '08:10', 'CARTON',   120.50, 10.50, 110.00, NULL, FALSE, NULL),
    (1, 2, 1, CURRENT_DATE - 1 + TIME '08:45', 'PLASTICO',  64.00,  4.00,  60.00, NULL, FALSE, NULL),
    (1, 2, 1, CURRENT_DATE - 1 + TIME '08:50', 'CARTON',    30.00,  3.00,  27.00, 'Segundo contenedor', FALSE, NULL),
    (1, 3, 1, CURRENT_DATE - 1 + TIME '09:30', 'PAPEL',     95.00,  5.00,  90.00, NULL, FALSE, NULL),
    (1, 4, 1, CURRENT_DATE - 1 + TIME '10:15', 'CARTON',   210.00, 12.00, 198.00, NULL, FALSE, NULL),
    (1, 4, 1, CURRENT_DATE - 1 + TIME '10:20', 'PLASTICO',  18.00,  2.00,  16.00, NULL, FALSE, NULL),
    (1, 5, 1, CURRENT_DATE - 1 + TIME '11:00', 'PLASTICO',  45.00,  4.50,  40.50, 'Bolsas sueltas', FALSE, NULL),
    (1, 6, 1, CURRENT_DATE - 1 + TIME '11:40', 'VIDRIO',   150.00, 20.00, 130.00, NULL, FALSE, NULL),
    (1, 6, 1, CURRENT_DATE - 1 + TIME '11:45', 'VIDRIO',    80.00, 20.00,  60.00, NULL, FALSE, NULL),
    (2, 7, 1, CURRENT_DATE + TIME '08:05',     'METAL',     75.00,  8.00,  67.00, NULL, FALSE, NULL),
    (2, 7, 1, CURRENT_DATE + TIME '08:12',     'CARTON',    22.00,  2.00,  20.00, NULL, FALSE, NULL),
    (2, 8, 1, CURRENT_DATE + TIME '08:40',     'VIDRIO',    98.00, 18.00,  80.00, NULL, FALSE, NULL),
    (2, 8, 2, CURRENT_DATE + TIME '08:55',     'VIDRIO',    60.00, 18.00,  42.00, 'Registrado en el turno de operario2', FALSE, NULL),
    (2, 9, 1, CURRENT_DATE + TIME '09:20',     'PAPEL',     55.00,  5.00,  50.00, NULL, FALSE, NULL),
    (2, 9, 1, CURRENT_DATE + TIME '09:22',     'PAPEL',     55.00,  5.00,  50.00, 'Duplicado', TRUE, 'Pesaje duplicado por error');
