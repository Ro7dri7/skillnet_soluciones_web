"""
Datos demo para KPIs y gráficos en dashboards (estudiante + infoproductor).

Uso:
  python backend/scripts/seed_demo_analytics.py

Requisitos: PostgreSQL skillnet_db, usuario infoproductor id=1 (rodrigocenasperez@gmail.com).
Crea ~16 cursos (publicados/borrador), compradores demo, matrículas y pagos repartidos en 2026.
"""

from __future__ import annotations

import json
import random
from datetime import datetime, timedelta, timezone
from decimal import Decimal

import bcrypt
import psycopg2
from psycopg2.extras import Json, RealDictCursor

DB = {
    "host": "localhost",
    "dbname": "skillnet_db",
    "user": "postgres",
    "password": "postgres123",
}

PROFESSOR_ID = 1
DEMO_PASSWORD = "Demo123!"
CATEGORIES = [
    "Tecnología",
    "Finanzas y Negocios",
    "Marketing y Ventas",
    "Diseño",
    "Desarrollo Personal",
]

COURSE_TITLES = [
    ("Excel Financiero Avanzado", "published", "Finanzas y Negocios", 49.99),
    ("Marketing Digital 2026", "published", "Marketing y Ventas", 79.99),
    ("Python para Data Science", "published", "Tecnología", 99.99),
    ("UX/UI desde Cero", "published", "Diseño", 59.99),
    ("Liderazgo Emprendedor", "published", "Desarrollo Personal", 39.99),
    ("SEO y Contenido", "published", "Marketing y Ventas", 44.99),
    ("React + TypeScript Pro", "published", "Tecnología", 89.99),
    ("Finanzas Personales", "published", "Finanzas y Negocios", 29.99),
    ("Branding Visual", "draft", "Diseño", 54.99),
    ("Automatización con n8n", "draft", "Tecnología", 69.99),
    ("Ventas B2B", "draft", "Marketing y Ventas", 64.99),
    ("Power BI Express", "published", "Finanzas y Negocios", 74.99),
    ("Figma para Producto", "published", "Diseño", 49.99),
    ("Productividad Extrema", "draft", "Desarrollo Personal", 19.99),
    ("Java Spring Boot", "published", "Tecnología", 119.99),
    ("Copywriting Persuasivo", "published", "Marketing y Ventas", 34.99),
]


def slugify(title: str) -> str:
    import re

    s = title.lower()
    s = re.sub(r"[^a-z0-9\s-]", "", s)
    s = re.sub(r"\s+", "-", s.strip())
    return s[:280]


def bcrypt_hash(password: str) -> str:
    return bcrypt.hashpw(password.encode(), bcrypt.gensalt(rounds=12)).decode()


def main() -> None:
    random.seed(42)
    conn = psycopg2.connect(**DB)
    conn.autocommit = False
    cur = conn.cursor(cursor_factory=RealDictCursor)

    cur.execute("SELECT id FROM core_user WHERE id = %s", (PROFESSOR_ID,))
    if not cur.fetchone():
        raise SystemExit(f"No existe professor_id={PROFESSOR_ID}. Ajusta PROFESSOR_ID en el script.")

    cur.execute("SELECT * FROM core_course WHERE id = 4")
    template = cur.fetchone()
    if not template:
        raise SystemExit("Curso plantilla id=4 no encontrado.")

    # Compradores demo
    buyer_ids: list[int] = []
    for i in range(1, 6):
        email = f"demo.alumno{i}@skillnet.local"
        username = f"demo_alumno_{i}"
        cur.execute("SELECT id FROM core_user WHERE email = %s", (email,))
        row = cur.fetchone()
        if row:
            buyer_ids.append(row["id"])
            continue
        pwd = bcrypt_hash(DEMO_PASSWORD)
        now = datetime.now(timezone.utc)
        cur.execute(
            """
            INSERT INTO core_user (
                password, is_superuser, username, first_name, last_name, email,
                is_staff, is_active, date_joined, role, is_student, is_infoproductor,
                is_affiliate, active_role, phone_verified, email_verified,
                years_experience, specialties, social_links, profile_visibility,
                identity_verified, is_verified_infoproductor, behavior_verified
            ) VALUES (
                %s, false, %s, %s, %s, %s,
                false, true, %s, 'student', true, false,
                false, 'student', false, true,
                0, %s, %s, true,
                false, false, false
            ) RETURNING id
            """,
            (
                pwd,
                username,
                f"Alumno",
                f"Demo {i}",
                email,
                now,
                Json({}),
                Json({}),
            ),
        )
        buyer_ids.append(cur.fetchone()["id"])

    created_courses = 0
    created_payments = 0
    created_enrollments = 0
    course_ids: list[int] = []

    for title, status, category, price in COURSE_TITLES:
        slug = slugify(title)
        cur.execute("SELECT id FROM core_course WHERE slug = %s", (slug,))
        existing = cur.fetchone()
        if existing:
            course_ids.append(existing["id"])
            continue

        created_at = datetime.now(timezone.utc) - timedelta(days=random.randint(10, 120))
        cur.execute(
            """
            INSERT INTO core_course (
                title, description, what_you_will_learn, target_audience,
                duration_hours, duration_minutes, requirements, welcome_message,
                congratulations_message, category, subcategory, level, language,
                course_format, software, has_subtitles, is_flexible_schedule,
                has_practical_experience, original_price, price, currency,
                is_on_sale, tax_included, professor_id, status, created_at, slug,
                image_url, video_url, affiliate_commission, affiliate_policy, ally,
                security_status
            ) VALUES (
                %s, %s, %s, %s,
                0, 0, NULL, %s,
                %s, %s, %s, 'beginner', 'es',
                'course', %s, false, false,
                false, %s, %s, 'USD',
                false, false, %s, %s, %s, %s,
                '', '', 0, 'all', 'intercert',
                'pending'
            ) RETURNING id
            """,
            (
                title,
                f"Curso demo: {title}",
                "Objetivos de aprendizaje demo",
                "estudiante",
                "Bienvenido al curso demo",
                "Felicitaciones por completar",
                category,
                category,
                Json([]),
                Decimal(str(price)),
                Decimal(str(price)),
                PROFESSOR_ID,
                status,
                created_at,
                slug,
            ),
        )
        course_ids.append(cur.fetchone()["id"])
        created_courses += 1

    # Pagos + matrículas en distintos meses de 2026 (solo cursos publicados)
    cur.execute(
        "SELECT id, price, title FROM core_course WHERE professor_id = %s AND status = 'published'",
        (PROFESSOR_ID,),
    )
    published = cur.fetchall()

    for month in range(1, 6):
        period_start = datetime(2026, month, 1, 12, 0, 0, tzinfo=timezone.utc)
        sales_this_month = random.randint(2, 5)
        for _ in range(sales_this_month):
            course = random.choice(published)
            buyer = random.choice(buyer_ids)
            day = random.randint(1, 28)
            paid_at = period_start.replace(day=day)

            cur.execute(
                """
                SELECT 1 FROM core_enrollment
                WHERE user_id = %s AND course_id = %s
                """,
                (buyer, course["id"]),
            )
            if cur.fetchone():
                continue

            cur.execute(
                """
                INSERT INTO core_enrollment (
                    user_id, course_id, enrolled_at, enrollment_type, is_completed
                ) VALUES (%s, %s, %s, 'PAID', false)
                """,
                (buyer, course["id"], paid_at),
            )
            created_enrollments += 1

            cur.execute(
                """
                INSERT INTO payments_payment (
                    user_id, course_id, amount, payment_method, status,
                    document_type, document_sent, accounting_notified,
                    company_name, company_rut, company_address, company_phone, company_email,
                    created_at, updated_at
                ) VALUES (
                    %s, %s, %s, 'demo', 'SUCCEEDED', 'boleta', false, false,
                    'InterCert Latam', '76.123.456-7', 'Av. Principal 123, Santiago, Chile',
                    '+56 2 2345 6789', 'info@intercertlatam.net',
                    %s, %s
                )
                """,
                (buyer, course["id"], course["price"], paid_at, paid_at),
            )
            created_payments += 1

    conn.commit()
    cur.close()
    conn.close()

    print("Seed demo analytics completado.")
    print(f"  Cursos nuevos: {created_courses}")
    print(f"  Matriculas nuevas: {created_enrollments}")
    print(f"  Pagos nuevos: {created_payments}")
    print(f"  Compradores demo: {len(buyer_ids)} (password: {DEMO_PASSWORD})")
    print("Reinicia el backend si estaba corriendo y recarga los dashboards (Mayo 2026).")


if __name__ == "__main__":
    main()
