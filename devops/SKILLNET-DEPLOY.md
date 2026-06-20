# Despliegue SkillNet en AWS (skillnet.com.pe)

Guía para publicar **backend (Spring Boot)**, **frontend (Angular/ S3)** y **base de datos (RDS)** en producción.

## Arquitectura recomendada

| Componente | Servicio AWS | Notas |
|------------|--------------|--------|
| API Java | EC2 + systemd (`devops/skillnet.service`) | Puerto 8080 detrás de Nginx |
| Frontend estático | S3 + CloudFront | Build Angular → subir a bucket |
| PostgreSQL | RDS | Configura `SPRING_DATASOURCE_*` en el servidor |
| Media (PDF, audio, imágenes) | S3 `skillnet-media-prod` | `SKILLNET_MEDIA_STORAGE=s3` |
| DNS | Route 53 | `skillnet.com.pe` → CloudFront (www) y EC2/ALB (api) |

## 1. Backend (EC2)

### Prerrequisitos en la instancia
- Java 21+
- PostgreSQL client (opcional)
- Nginx como reverse proxy TLS

### Variables de entorno (ver `devops/skillnet.service`)

Copia `devops/skillnet.service` al servidor y sustituye todos los valores `CHANGE_ME` / `YOUR_*`.
**Nunca commitees claves reales** (AWS, RDS, Stripe, Gamma, etc.); usa el `.env` del servidor o `Environment=` en systemd.

- `SPRING_DATASOURCE_URL` → RDS
- `AWS_S3_BUCKET`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
- `GAMMA_API_KEY`, `ELEVENLABS_API_KEY` (secretos, no commitear)
- `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` (modo test o live)

### Despliegue del JAR

Desde tu máquina (Git Bash):

```bash
cd backend
mvn clean package -DskipTests

scp -i ~/.ssh/tu-llave.pem target/skillnet-backend-0.0.1-SNAPSHOT.jar ubuntu@TU_EC2:/tmp/backend.jar

ssh -i ~/.ssh/tu-llave.pem ubuntu@TU_EC2
sudo mkdir -p /opt/skillnet
sudo mv /tmp/backend.jar /opt/skillnet/backend.jar
sudo cp devops/skillnet.service /etc/systemd/system/skillnet.service
sudo systemctl daemon-reload
sudo systemctl enable skillnet
sudo systemctl restart skillnet
sudo systemctl status skillnet
```

> **Seguridad:** rota credenciales expuestas en archivos locales; usa IAM roles en EC2 en lugar de keys en el unit file cuando sea posible.

### Nginx (ejemplo API)

```nginx
server {
  listen 443 ssl;
  server_name api.skillnet.com.pe;

  location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
}
```

## 2. Frontend (S3 + CloudFront)

```bash
cd skillnet-frontend
# environment.prod.ts → apiUrl: 'https://api.skillnet.com.pe/api/v1'
npm ci
npm run build -- --configuration=production

aws s3 sync dist/skillnet-frontend/browser/ s3://skillnet-web-prod/ --delete
aws cloudfront create-invalidation --distribution-id EXXXXXXXXX --paths "/*"
```

CloudFront debe servir `index.html` para rutas SPA (error 403/404 → `/index.html`).

## 3. Base de datos (RDS)

- Hibernate `ddl-auto=update` en prod solo para iteración inicial; migrar a Flyway/Liquibase antes de escala.
- Backups automáticos RDS activados.
- Security group: solo EC2 → RDS:5432.

## 4. Stripe (compras simuladas / test)

1. Dashboard Stripe → claves **test**.
2. Backend: `STRIPE_SECRET_KEY=sk_test_...`, webhook apuntando a `https://api.skillnet.com.pe/api/v1/payments/stripe/webhook`.
3. Frontend: clave publicable en checkout si aplica.
4. Tras pago exitoso: enrollment + dashboards infoproductor/estudiante se actualizan vía API existente.

## 5. Checklist post-despliegue

- [ ] `https://api.skillnet.com.pe/api/v1/health` → `{"ok":true}`
- [ ] Login admin / infoproductor / estudiante
- [ ] Crear ebook → Gamma → guardar lección → publicar
- [ ] Crear podcast → guion IA → audio → adjuntar lección
- [ ] Compra test Stripe → curso en dashboard estudiante
- [ ] Ventas visibles en dashboard infoproductor
- [ ] `/admin/audit-log` registra publish/purchase
- [ ] Media PDF/audio sirve desde S3

## 6. Dominio skillnet.com.pe

| Registro | Destino |
|----------|---------|
| `skillnet.com.pe` / `www` | CloudFront (frontend) |
| `api.skillnet.com.pe` | EC2 o ALB (backend) |

Certificados: ACM en us-east-1 (CloudFront) y us-east-2 (ALB/EC2 según región).

## 7. Script existente

`devops/deploy.sh` compila y sube el JAR; actualízalo para usar `skillnet.service` y `/opt/skillnet/` en lugar de rutas Lernymart si aún no lo hiciste.

---

**Estado local:** desarrollo en `localhost:8080` (API) + `localhost:4200` (Angular).  
**Estado prod objetivo:** `https://skillnet.com.pe` + `https://api.skillnet.com.pe` con RDS y S3 persistentes.
