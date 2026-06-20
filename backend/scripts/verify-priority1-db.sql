-- Verificación Priority 1 + IA (PostgreSQL skillnet_db)
-- Ejecutar: psql -h localhost -U postgres -d skillnet_db -f scripts/verify-priority1-db.sql

\echo '=== Tablas esperadas ==='
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
    'core_coupon',
    'core_notification',
    'core_coursecertificate',
    'core_passwordresettoken',
    'core_enrollment',
    'core_progress',
    'payments_payment',
    'core_gammageneration',
    'core_podcastgeneration'
  )
ORDER BY table_name;

\echo '=== Conteos ==='
SELECT 'core_coupon' AS tbl, COUNT(*) FROM core_coupon
UNION ALL SELECT 'core_notification', COUNT(*) FROM core_notification
UNION ALL SELECT 'core_coursecertificate', COUNT(*) FROM core_coursecertificate
UNION ALL SELECT 'core_passwordresettoken', COUNT(*) FROM core_passwordresettoken
UNION ALL SELECT 'payments_payment', COUNT(*) FROM payments_payment
UNION ALL SELECT 'payments_with_coupon', COUNT(*) FROM payments_payment WHERE coupon_id IS NOT NULL
UNION ALL SELECT 'core_gammageneration', COUNT(*) FROM core_gammageneration
UNION ALL SELECT 'core_podcastgeneration', COUNT(*) FROM core_podcastgeneration;

\echo '=== Últimas notificaciones ==='
SELECT id, notification_type, title, is_read, created_at
FROM core_notification
ORDER BY created_at DESC
LIMIT 5;

\echo '=== Últimos certificados auto ==='
SELECT id, course_id, student_id, certificate_file, uploaded_at
FROM core_coursecertificate
WHERE certificate_file LIKE 'auto:%'
ORDER BY uploaded_at DESC
LIMIT 5;
