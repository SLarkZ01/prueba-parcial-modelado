# Guia completa de despliegue (Spring Boot + PostgreSQL + Docker + ECR + EC2 + GitHub Actions)

Esta guia resume el flujo completo de despliegue por consola para repetirlo desde cero.
Incluye comandos, para que sirven, y en que momento usarlos.

## 0) Objetivo final

- Backend desplegado en EC2 con Docker.
- PostgreSQL en contenedor dentro de EC2.
- Imagen del backend publicada en Amazon ECR.
- Deploy automatico desde GitHub Actions via SSH.
- OpenAPI accesible publicamente.

## 1) Desarrollo local (base de datos en Docker)

### 1.1 Levantar PostgreSQL local
```bash
docker compose up -d
```
Inicia PostgreSQL en segundo plano.
Usalo antes de arrancar Spring Boot en local.

```bash
docker compose ps
```
Verifica estado de contenedores (`running`, `healthy`).

```bash
docker compose logs -f postgres
```
Muestra logs de PostgreSQL para diagnostico.

### 1.2 Apagar entorno local
```bash
docker compose down
```
Detiene contenedores sin borrar datos.

```bash
docker compose down -v
```
Detiene y elimina volumen de datos (reset total).

## 2) Backend en local

```powershell
.\mvnw.cmd spring-boot:run
```
Ejecuta backend en Windows.

```powershell
.\mvnw.cmd -q test
```
Ejecuta tests.

## 3) OpenAPI

### 3.1 Exportar OpenAPI en Windows
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\export-openapi.ps1
```
Genera:
- `docs/openapi/openapi.json`
- `docs/openapi/openapi.yaml`

### 3.2 Exportar OpenAPI en Linux/macOS/CI
```bash
chmod +x scripts/export-openapi.sh
./scripts/export-openapi.sh
```

### 3.3 URLs de OpenAPI local
```text
http://localhost:8080/v3/api-docs
http://localhost:8080/swagger-ui.html
```

## 4) GitHub CLI (diagnostico de workflows)

```bash
gh --version
```
Verifica instalacion de GitHub CLI.

```bash
gh auth status
```
Verifica autenticacion.

```bash
gh workflow list
```
Lista workflows del repositorio.

```bash
gh run list --limit 10
```
Lista ejecuciones recientes.

```bash
gh run view <RUN_ID>
```
Muestra detalle de una ejecucion.

```bash
gh run view <RUN_ID> --log-failed
```
Muestra logs de error de una ejecucion.

## 5) Build local de imagen backend

```bash
docker build -t techstock-backend:local .
```
Construye imagen local para validar Dockerfile.

## 6) ECR + CI

### 6.1 Secrets requeridos en GitHub
- `AWS_REGION` (ejemplo: `us-east-2`)
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`

### 6.2 Ejecutar build/push manual a ECR
```bash
gh workflow run "Docker Build and Push (ECR)" --ref main
```

## 7) Preparar EC2 (Ubuntu)

## 7.1 Conectar por SSH desde local
```bash
ssh -i "D:\permiso\backend-redes.pem" ubuntu@3.19.222.150
```

### 7.2 Si PowerShell bloquea `.pem` por permisos
Si aparece `UNPROTECTED PRIVATE KEY FILE`, ejecutar:

```powershell
$pem = "D:\permiso\backend-redes.pem"
icacls $pem /inheritance:r
icacls $pem /remove:g *S-1-5-11 *S-1-5-32-545 *S-1-1-0
icacls $pem /grant:r "${env:USERDOMAIN}\${env:USERNAME}:(R)"
```

Verificar:

```powershell
icacls $pem
```

Probar SSH de nuevo:

```powershell
ssh -i "D:\permiso\backend-redes.pem" ubuntu@3.19.222.150
```

## 8) Instalar Docker, Compose y AWS CLI en EC2

```bash
sudo apt update -o Acquire::ForceIPv4=true
sudo apt install -y docker.io docker-compose-v2 curl unzip
```

```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip -q awscliv2.zip
sudo ./aws/install --update
aws --version
```

```bash
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
newgrp docker
docker --version
docker compose version
```

## 9) Crear archivos de despliegue en EC2 (carpeta de trabajo)

```bash
mkdir -p ~/techstock-backend
cd ~/techstock-backend
ls -la
```

### 9.1 Crear `docker-compose.ec2.yml`
```bash
cat > docker-compose.ec2.yml <<'EOF'
services:
  backend:
    image: ${IMAGE_URI}
    container_name: techstock-backend
    restart: unless-stopped
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE}
      JPA_DDL_AUTO: ${JPA_DDL_AUTO}
      SERVER_HTTP2_ENABLED: ${SERVER_HTTP2_ENABLED}
      JAVA_TOOL_OPTIONS: ${JAVA_TOOL_OPTIONS}
      DB_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      DB_USERNAME: ${POSTGRES_USER}
      DB_PASSWORD: ${POSTGRES_PASSWORD}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS}
      DB_POOL_MAX_SIZE: ${DB_POOL_MAX_SIZE}
      DB_POOL_MIN_IDLE: ${DB_POOL_MIN_IDLE}
      JETTY_THREADS_MAX: ${JETTY_THREADS_MAX}
      JETTY_THREADS_MIN: ${JETTY_THREADS_MIN}
    depends_on:
      postgres:
        condition: service_healthy
    expose:
      - "8080"
    healthcheck:
      test: ["CMD-SHELL", "wget --spider -q http://localhost:8080/actuator/health/readiness || exit 1"]
      interval: 15s
      timeout: 5s
      retries: 10
      start_period: 40s

  postgres:
    image: postgres:16-alpine
    container_name: techstock-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5

  caddy:
    image: caddy:2-alpine
    container_name: techstock-caddy
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy_data:/data
      - caddy_config:/config
    depends_on:
      - backend

volumes:
  postgres_data:
  caddy_data:
  caddy_config:
EOF
```

### 9.2 Crear `.env` (conexion de PostgreSQL y backend)
```bash
cat > .env <<'EOF'
IMAGE_URI=291328562559.dkr.ecr.us-east-2.amazonaws.com/techstock-backend:latest

SPRING_PROFILES_ACTIVE=prod
JPA_DDL_AUTO=update
SERVER_HTTP2_ENABLED=false
JAVA_TOOL_OPTIONS=-XX:+UseSerialGC -Xms128m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom

POSTGRES_DB=techstock_db
POSTGRES_USER=techstock_user
POSTGRES_PASSWORD=CAMBIA_ESTA_CLAVE_FUERTE

DB_POOL_MAX_SIZE=3
DB_POOL_MIN_IDLE=1
JETTY_THREADS_MAX=30
JETTY_THREADS_MIN=4

CORS_ALLOWED_ORIGINS=https://frontend-redes.vercel.app
EOF
```

Que define este `.env`:
- `IMAGE_URI`: URI completa de la imagen en ECR.
- `SPRING_PROFILES_ACTIVE`: perfil de ejecucion del backend (`prod`).
- `JPA_DDL_AUTO`: estrategia de schema en runtime (`update` recomendado para este despliegue).
- `SERVER_HTTP2_ENABLED`: activa HTTP/2 en Jetty (`false` recomendado para evitar error de arranque por modulo faltante).
- `JAVA_TOOL_OPTIONS`: limite de memoria de la JVM para evitar `Out of memory` en instancias pequenas.
- `POSTGRES_DB`: nombre de la base de datos.
- `POSTGRES_USER`: usuario de PostgreSQL.
- `POSTGRES_PASSWORD`: clave de PostgreSQL.
- `DB_POOL_MAX_SIZE` y `DB_POOL_MIN_IDLE`: tuning del pool de conexiones (Hikari).
- `JETTY_THREADS_MAX` y `JETTY_THREADS_MIN`: tuning de hilos del servidor Jetty.
- `CORS_ALLOWED_ORIGINS`: dominio del frontend permitido.
- `DB_URL` no va directo en `.env`; se construye en `docker-compose.ec2.yml` para backend como:
  - `jdbc:postgresql://postgres:5432/${POSTGRES_DB}`
- `DB_USERNAME` y `DB_PASSWORD` del backend usan `POSTGRES_USER` y `POSTGRES_PASSWORD`.

### 9.3 Crear swap en EC2 (recomendado para evitar OOM)
```bash
sudo fallocate -l 2G /swapfile || sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h
```

Si tu instancia es micro/small y sigue con alta presion de memoria, usa tuning conservador en `.env`:

```text
DB_POOL_MAX_SIZE=3
DB_POOL_MIN_IDLE=1
JETTY_THREADS_MAX=30
JETTY_THREADS_MIN=4
```

### 9.4 Verificar archivos creados
```bash
ls -la ~/techstock-backend
```

## 10) Probar despliegue manual en EC2 (opcional, recomendado)

```bash
cd ~/techstock-backend
aws ecr get-login-password --region us-east-2 | docker login --username AWS --password-stdin 291328562559.dkr.ecr.us-east-2.amazonaws.com
docker compose --env-file .env -f docker-compose.ec2.yml pull
docker compose --env-file .env -f docker-compose.ec2.yml up -d
docker compose --env-file .env -f docker-compose.ec2.yml ps
```

Validar estado de health del backend:

```bash
docker inspect techstock-backend --format '{{json .State.Health}}'
```

Si actualizaste `Dockerfile` o el `healthcheck`, fuerza recreacion del backend:

```bash
docker compose --env-file .env -f docker-compose.ec2.yml up -d --force-recreate backend
```

Si hiciste build manual en EC2 (sin ECR), puedes construir y levantar asi:

```bash
docker build -t techstock-backend:manual .
```

## 11) Deploy automatico por GitHub Actions

### 11.1 Secrets extra requeridos
- `EC2_HOST`
- `EC2_USER`
- `EC2_SSH_KEY`

### 11.2 Ejecutar deploy manual
```bash
gh workflow run "Deploy to EC2" --ref main
```

## 12) Verificacion post-deploy

### 12.1 En EC2
```bash
docker compose --env-file .env -f docker-compose.ec2.yml ps
```

```bash
docker compose --env-file .env -f docker-compose.ec2.yml logs --no-color backend
```

### 12.2 Desde internet
```bash
curl -I https://parcial-redes.duckdns.org/v3/api-docs
```

## 13) HTTPS gratis con DuckDNS + Caddy (paso de endurecimiento)

### 13.1 En AWS Security Group
Agregar inbound:
- `HTTP` puerto `80` desde `0.0.0.0/0`
- `HTTPS` puerto `443` desde `0.0.0.0/0`

### 13.2 En EC2 crear `Caddyfile`
```bash
cd ~/techstock-backend
cat > Caddyfile <<'EOF'
parcial-redes.duckdns.org {
  reverse_proxy backend:8080
}
EOF
```

### 13.3 Actualizar `docker-compose.ec2.yml` para agregar Caddy
Si tu archivo viene de una version antigua, agrega el servicio `caddy` con puertos `80:80` y `443:443`, y deja `backend` con `expose: 8080`.
Si ya usas la version actual de esta guia, Caddy ya queda incluido desde el paso 9.1.

### 13.4 Levantar con Caddy
```bash
docker compose --env-file .env -f docker-compose.ec2.yml up -d
docker compose --env-file .env -f docker-compose.ec2.yml logs -f caddy
```

### 13.5 Verificar HTTPS
```bash
curl -I https://parcial-redes.duckdns.org/v3/api-docs
```

## 14) Troubleshooting rapido

### 14.1 `UNPROTECTED PRIVATE KEY FILE`
Causa: permisos abiertos del `.pem` en Windows.
Solucion: aplicar comandos `icacls` (seccion 7.2).

### 14.2 `i/o timeout` en puerto 22 durante deploy
Causa: Security Group bloquea SSH para GitHub runner.
Solucion: abrir temporalmente `22` o migrar deploy a SSM.

### 14.3 `NoCredentials` en EC2 durante deploy
Causa: variables AWS no pasadas a shell remoto.
Solucion: pasar `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` al paso SSH del workflow.

### 14.4 API no responde publicamente
Causa: regla de puerto faltante (`8080`, `80`, `443`) o no guardada.
Solucion: revisar inbound rules y estado de contenedores.

### 14.5 Backend `unhealthy` por `wget: not found`
Causa: el `healthcheck` del backend usa `wget` y la imagen runtime no lo incluye.
Solucion:

1. Asegurar que el `Dockerfile` instale `wget` en la etapa final (`jre`).
2. Reconstruir y publicar imagen nueva a ECR (`Docker Build and Push (ECR)`).
3. Desplegar de nuevo en EC2 y recrear backend:

```bash
docker compose --env-file .env -f docker-compose.ec2.yml pull
docker compose --env-file .env -f docker-compose.ec2.yml up -d --force-recreate backend
docker compose --env-file .env -f docker-compose.ec2.yml ps
```

### 14.6 `Out of memory: Killed process (java)`
Causa: la instancia se queda sin RAM y el kernel mata la JVM.
Solucion:

1. Limitar memoria del backend con `JAVA_TOOL_OPTIONS` en `.env` (incluido en esta guia).
2. Crear swap de 2 GB (seccion 9.3).
3. Bajar `DB_POOL_MAX_SIZE` y `JETTY_THREADS_MAX` para instancias pequenas.
4. Re-crear backend:

```bash
docker compose --env-file .env -f docker-compose.ec2.yml up -d --force-recreate backend
docker compose --env-file .env -f docker-compose.ec2.yml ps
docker inspect techstock-backend --format '{{.State.Health.Status}}'
```

## 15) Notas de seguridad para produccion

- No subir `.env` real al repositorio.
- Cambiar `POSTGRES_PASSWORD` por una clave fuerte.
- Restringir SSH (`22`) a tu IP o usar SSM.
- No exponer PostgreSQL (`5432`) publicamente.
- Preferir HTTPS con dominio (DuckDNS + Caddy o dominio propio).
