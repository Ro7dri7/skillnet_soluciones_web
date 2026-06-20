import json
import os
import psycopg2
from psycopg2.extras import RealDictCursor

conn = psycopg2.connect(
    host=os.environ.get("PGHOST", "localhost"),
    dbname=os.environ.get("PGDATABASE", "skillnet_db"),
    user=os.environ.get("PGUSER", "postgres"),
    password=os.environ.get("PGPASSWORD", ""),
)
cur = conn.cursor(cursor_factory=RealDictCursor)

print("=" * 80)
print("1. ULTIMOS PAGOS (comprador + curso)")
print("=" * 80)
cur.execute(
    """
SELECT
    p.id AS payment_id,
    p.status AS pago_estado,
    p.amount AS monto,
    p.payment_method AS metodo_pago,
    p.stripe_checkout_id AS stripe_charge_id,
    p.created_at AS fecha_pago,
    u.id AS comprador_id,
    u.email AS comprador_email,
    TRIM(CONCAT(COALESCE(u.first_name,''), ' ', COALESCE(u.last_name,''))) AS comprador_nombre,
    c.id AS curso_id,
    c.title AS curso_titulo,
    c.slug AS curso_slug,
    c.price AS curso_precio_lista,
    c.status AS curso_estado
FROM payments_payment p
JOIN core_user u ON u.id = p.user_id
LEFT JOIN core_course c ON c.id = p.course_id
ORDER BY p.created_at DESC
LIMIT 5
"""
)
rows = cur.fetchall()
if not rows:
    print("(sin registros)")
else:
    for r in rows:
        for k, v in r.items():
            print(f"  {k}: {v}")
        print("-" * 40)

print()
print("=" * 80)
print("2. ULTIMAS MATRICULAS")
print("=" * 80)
cur.execute(
    """
SELECT
    e.id AS enrollment_id,
    e.enrollment_type AS tipo,
    e.enrolled_at AS fecha_matricula,
    e.is_completed AS completado,
    u.id AS comprador_id,
    u.email AS comprador_email,
    TRIM(CONCAT(COALESCE(u.first_name,''), ' ', COALESCE(u.last_name,''))) AS comprador_nombre,
    c.id AS curso_id,
    c.title AS curso_titulo,
    c.slug AS curso_slug
FROM core_enrollment e
JOIN core_user u ON u.id = e.user_id
JOIN core_course c ON c.id = e.course_id
ORDER BY e.enrolled_at DESC
LIMIT 5
"""
)
rows = cur.fetchall()
if not rows:
    print("(sin registros)")
else:
    for r in rows:
        for k, v in r.items():
            print(f"  {k}: {v}")
        print("-" * 40)

print()
print("=" * 80)
print("3. PAGO + MATRICULA (flujo completo)")
print("=" * 80)
cur.execute(
    """
SELECT
    p.id AS payment_id,
    p.status,
    p.amount,
    p.stripe_checkout_id,
    p.created_at AS pago_en,
    u.email AS comprador,
    c.title AS curso,
    e.id AS enrollment_id,
    e.enrolled_at AS matricula_en,
    CASE
        WHEN p.status IN ('COMPLETED', 'SUCCEEDED') AND e.id IS NOT NULL THEN 'OK: pago + matricula'
        WHEN p.status IN ('COMPLETED', 'SUCCEEDED') AND e.id IS NULL THEN 'Pago si, matricula NO'
        ELSE 'Revisar estado del pago'
    END AS resultado
FROM payments_payment p
JOIN core_user u ON u.id = p.user_id
LEFT JOIN core_course c ON c.id = p.course_id
LEFT JOIN core_enrollment e ON e.user_id = p.user_id AND e.course_id = p.course_id
ORDER BY p.created_at DESC
LIMIT 5
"""
)
rows = cur.fetchall()
if not rows:
    print("(sin registros)")
else:
    for r in rows:
        for k, v in r.items():
            print(f"  {k}: {v}")
        print("-" * 40)

print()
print("=" * 80)
print("4. STRIPE JSON (ultimo pago)")
print("=" * 80)
cur.execute(
    """
SELECT p.id, p.stripe_checkout_id, p.gateway_response
FROM payments_payment p
ORDER BY p.created_at DESC
LIMIT 1
"""
)
row = cur.fetchone()
if not row:
    print("(sin registros)")
else:
    print(f"  id: {row['id']}")
    print(f"  stripe_checkout_id: {row['stripe_checkout_id']}")
    gr = row["gateway_response"]
    if gr:
        print("  gateway_response:")
        print(json.dumps(gr, indent=2, ensure_ascii=False))
    else:
        print("  gateway_response: NULL")

cur.close()
conn.close()
