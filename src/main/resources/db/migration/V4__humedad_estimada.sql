-- SC-C: humedad estimada del material (%) en cada pesaje. V4 y no V3 para no chocar con la
-- migracion del rol AUDITOR si ambas solicitudes llegan a develop.
ALTER TABLE pesaje
    ADD COLUMN humedad_estimada NUMERIC(5,2) NOT NULL DEFAULT 0
        CHECK (humedad_estimada BETWEEN 0 AND 100);

-- Datos semilla actualizados: valores tipicos por material.
UPDATE pesaje SET humedad_estimada = CASE material
    WHEN 'CARTON'   THEN 12.00
    WHEN 'PAPEL'    THEN 8.50
    WHEN 'PLASTICO' THEN 3.00
    WHEN 'VIDRIO'   THEN 1.50
    WHEN 'METAL'    THEN 0.50
    ELSE 5.00 END;
