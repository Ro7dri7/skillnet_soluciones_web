"""
Crea o verifica el superadmin en PostgreSQL (tabla core_user).

Uso:
  python backend/scripts/seed_superadmin.py

Credenciales por defecto:
  admin@skillnet.com / Admin123!

El backend también persiste este usuario al arrancar (AdminBootstrapRunner).
Este script sirve para comprobar o sembrar la BD sin levantar Spring Boot.
"""

from __future__ import annotations

import sys

import bcrypt
import psycopg2
from psycopg2.extras import RealDictCursor

ADMIN_EMAIL = "admin@skillnet.com"
ADMIN_PASSWORD = "Admin123!"
ADMIN_USERNAME = "superadmin"

DB = {
    "host": "localhost",
    "dbname": "skillnet_db",
    "user": "postgres",
    "password": "postgres123",
}


def bcrypt_hash(plain: str) -> str:
    return bcrypt.hashpw(plain.encode("utf-8"), bcrypt.gensalt(rounds=10)).decode("utf-8")


def fetch_admin(cur) -> dict | None:
    cur.execute(
        """
        SELECT id, username, email, is_superuser, is_staff, role, active_role,
               is_student, is_infoproductor
        FROM core_user
        WHERE email ILIKE %s
        LIMIT 1
        """,
        (ADMIN_EMAIL,),
    )
    return cur.fetchone()


def insert_admin(cur) -> int:
    password_hash = bcrypt_hash(ADMIN_PASSWORD)
    cur.execute(
        """
        INSERT INTO core_user (
            password, is_superuser, username, first_name, last_name, email,
            is_staff, is_active, date_joined, role, is_student, is_infoproductor,
            is_affiliate, active_role, phone_verified, email_verified,
            years_experience, specialties, social_links, profile_visibility,
            identity_verified, is_verified_infoproductor, behavior_verified,
            identity_provider
        ) VALUES (
            %s, TRUE, %s, 'Super', 'Admin', %s,
            TRUE, TRUE, NOW(), 'admin', TRUE, TRUE,
            FALSE, 'admin', FALSE, FALSE,
            0, '{}'::jsonb, '{}'::jsonb, TRUE,
            FALSE, FALSE, FALSE,
            'aws_rekognition'
        )
        RETURNING id
        """,
        (password_hash, ADMIN_USERNAME, ADMIN_EMAIL),
    )
    row = cur.fetchone()
    return row["id"] if isinstance(row, dict) else row[0]


def repair_admin(cur, admin_id: int) -> None:
    password_hash = bcrypt_hash(ADMIN_PASSWORD)
    cur.execute(
        """
        UPDATE core_user SET
            username = %s,
            password = %s,
            first_name = 'Super',
            last_name = 'Admin',
            is_superuser = TRUE,
            is_staff = TRUE,
            is_active = TRUE,
            role = 'admin',
            active_role = 'admin',
            is_student = TRUE,
            is_infoproductor = TRUE,
            is_affiliate = FALSE,
            specialties = COALESCE(specialties, '{}'::jsonb),
            social_links = COALESCE(social_links, '{}'::jsonb)
        WHERE id = %s
        """,
        (ADMIN_USERNAME, password_hash, admin_id),
    )


def main() -> int:
    try:
        conn = psycopg2.connect(**DB)
    except psycopg2.Error as exc:
        print(f"No se pudo conectar a PostgreSQL: {exc}")
        return 1

    try:
        with conn:
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                existing = fetch_admin(cur)
                if existing:
                    repair_admin(cur, existing["id"])
                    print("Superadmin ya existía; permisos y contraseña verificados.")
                    print(f"  id={existing['id']} email={existing['email']} role={existing['role']}")
                else:
                    admin_id = insert_admin(cur)
                    print("Superadmin insertado en core_user.")
                    print(f"  id={admin_id} email={ADMIN_EMAIL}")

                print(f"\nCredenciales: {ADMIN_EMAIL} / {ADMIN_PASSWORD}")
                return 0
    except psycopg2.Error as exc:
        print(f"Error SQL: {exc}")
        return 1
    finally:
        conn.close()


if __name__ == "__main__":
    sys.exit(main())
