-- Fase 8.9: verificación de persistencia en pgAdmin (Skillnet / PostgreSQL)
-- Tablas reales del backend Spring: core_course, core_section, core_lesson

-- Verificar el curso creado y su estado
SELECT id, title, course_format, status, price, description, image_url, created_at
FROM core_course
ORDER BY created_at DESC
LIMIT 5;

-- Verificar el temario (módulos y lecciones) del último curso
SELECT s.name AS modulo, l.title AS leccion, l.content_type
FROM core_section s
LEFT JOIN core_lesson l ON s.id = l.section_id
WHERE s.course_id = (SELECT id FROM core_course ORDER BY created_at DESC LIMIT 1)
ORDER BY s."order", l."order";

-- Confirmar publicación (status = 'published')
SELECT id, title, status, price
FROM core_course
WHERE status = 'published'
ORDER BY created_at DESC;
