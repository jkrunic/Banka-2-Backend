# 🐳 Docker setup (Windows) – kompletne instrukcije

Ovaj dokument sadrži sve korake potrebne da se pokrene backend + MySQL baza koristeći Docker na Windows sistemu.

---

# 1. Instalacija Docker Desktop-a

Preuzmi Docker Desktop:

https://www.docker.com/products/docker-desktop/

Tokom instalacije obavezno:
- Enable WSL 2
- Install required components

Nakon instalacije:
- Restartuj računar

---

# 2. Provera instalacije

Otvoriti PowerShell ili CMD i pokrenuti:

docker --version
docker run hello-world

Ako vidiš poruku o uspešnom pokretanju → Docker radi ispravno.

---

# 3. Login na GitHub Container Registry

docker login ghcr.io

Unesi:
- Username: GitHub username
- Password: Personal Access Token (PAT)

PAT se kreira ovde:
https://github.com/settings/tokens

Potrebne permisije:
- read:packages

---

# 4. Preuzimanje backend image-a

docker pull ghcr.io/raf-si-2025/banka2-backend:96e880386a63044a1773746249ec6e91295658f9

---

# 5. Pokretanje MySQL baze

docker run -d ^
  --name banka-mysql ^
  -e MYSQL_ROOT_PASSWORD=root ^
  -e MYSQL_DATABASE=banka ^
  -p 3306:3306 ^
  mysql:8

Napomena:
- znak ^ u Windows CMD označava novi red
- može se napisati i u jednom redu

---

# 6. Provera da li je baza spremna

docker logs banka-mysql

Čekati izlaz:

ready for connections

---

# 7. Kreiranje Docker mreže

docker network create banka-net

---

# 8. Povezivanje baze na mrežu

docker network connect banka-net banka-mysql

---

# 9. Pokretanje backend-a

docker run -p 8080:8080 ^
  --network banka-net ^
  -e SPRING_DATASOURCE_URL=jdbc:mysql://banka-mysql:3306/banka ^
  -e SPRING_DATASOURCE_USERNAME=root ^
  -e SPRING_DATASOURCE_PASSWORD=root ^
  ghcr.io/raf-si-2025/banka2-backend:96e880386a63044a1773746249ec6e91295658f9

---

# 10. Testiranje aplikacije

Otvori u browser-u:

http://localhost:8080

---

# 11. Debug opcije

## Lista container-a

docker ps -a

## Logovi container-a

docker logs <container_id>

---

# 12. BONUS – docker-compose (PREPORUČENO)

Umesto ručnog pokretanja koristi docker-compose.

## docker-compose.yml

version: '3.8'

services:
  mysql:
    image: mysql:8
    container_name: banka-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: banka
    ports:
      - "3306:3306"

  backend:
    image: ghcr.io/raf-si-2025/banka2-backend:96e880386a63044a1773746249ec6e91295658f9
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/banka
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root

---

## Pokretanje

docker compose up

---

# 13. Najčešći problemi

## unauthorized
- Nisi uradio docker login

## connection refused
- MySQL nije pokrenut

## backend se gasi odmah
- nema konekciju ka bazi

---

# 14. TL;DR

docker login ghcr.io
docker compose up

✔ backend radi
✔ baza radi
✔ sistem spreman
