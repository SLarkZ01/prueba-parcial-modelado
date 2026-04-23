# Tutorial: despliegue a produccion con contenedores (Spring Boot + PostgreSQL)

Este tutorial sigue la arquitectura propuesta: backend y base de datos en EC2 con Docker Compose.

## 1) Prerrequisitos

- Cuenta AWS
- Repositorio en GitHub
- Docker y Docker Compose instalados en EC2
- AWS CLI instalado en EC2

## 2) Crear repositorio ECR

En AWS, crea el repositorio de imagen:

- Nombre sugerido: `prueba-redes`

## 3) Preparar EC2

En la instancia EC2:

1. Crear carpeta del proyecto:

```bash
mkdir -p ~/prueba
cd ~/prueba
```

2. Copiar `docker-compose.ec2.yml` desde este repo (`deploy/docker-compose.ec2.yml`)
3. Crear `.env` usando `deploy/ec2.env.example` como base

Archivo `.env` minimo:

```env
IMAGE_URI=<aws_account_id>.dkr.ecr.<region>.amazonaws.com/prueba-redes:latest
POSTGRES_DB=techstock_db
POSTGRES_USER=techstock_user
POSTGRES_PASSWORD=cambia_esta_clave
CORS_ALLOWED_ORIGINS=https://tu-frontend.vercel.app
```

## 4) Configurar GitHub Secrets

En GitHub > Settings > Secrets and variables > Actions:

- `AWS_REGION`
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `EC2_HOST`
- `EC2_USER`
- `EC2_SSH_KEY`

## 5) Flujo CI/CD incluido

- `ci-backend.yml`: tests + export OpenAPI + artifact
- `docker-backend.yml`: build y push de imagen a ECR
- `deploy-ec2.yml`: SSH a EC2, login ECR, pull y up con Docker Compose

## 6) Ejecutar despliegue

Opciones:

- Push a `main` (despliegue automatico)
- O manual desde Actions con `Deploy to EC2`

## 7) Validar en produccion

- Ver contenedores:

```bash
docker ps
```

- Ver logs:

```bash
docker compose --env-file .env -f docker-compose.ec2.yml logs -f
```

- Ver endpoint OpenAPI:

`http://<IP_PUBLICA_EC2>:8080/v3/api-docs`

## 8) Recomendaciones de seguridad

- No abrir el puerto `5432` al publico
- Usar solo `80/443` para trafico web
- Mover a Nginx/Caddy + HTTPS
- Cambiar contrasenas por valores fuertes
- Rotar credenciales de IAM periodicamente
