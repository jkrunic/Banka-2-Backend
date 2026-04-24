# Banka 2 — Backend

Spring Boot servis koji pokriva celokupnu bankarsku logiku: klijenti i zaposleni, racuni i kartice, placanja i transferi, berzanska trgovina (Celina 3) i OTC opcioni ugovori (Celina 4). Projekat iz predmeta **Softversko inzenjerstvo** na Racunarskom fakultetu 2025/26.

## Tech Stack

- **Java 17** + **Spring Boot 4.0.4**
- **PostgreSQL 16** (H2 u testovima, MODE=PostgreSQL)
- Spring Security + JWT (HS256) — access (15min) + refresh (7 dana) token
- Spring Data JPA / Hibernate 7
- Springdoc OpenAPI (Swagger UI)
- PDFBox (PDF potvrde placanja)
- Lombok, JaCoCo
- JUnit 5 + Mockito + AssertJ — **2165 testova**

## Pokretanje

### Docker (preporuceno)

```bash
docker compose up -d --build
```

Diza 4 kontejnera:

| Servis | Port (host → kontejner) | Opis |
|--------|-------------------------|------|
| `backend` | 8080 → 8080 | Spring Boot API |
| `db` | 5433 → 5432 | PostgreSQL 16 (alpine) |
| `adminer` | 9001 → 8080 | Web DB admin (user: `banka2user` / pass: `banka2pass` / db: `banka2`) |
| `seed` | - | Jednokratno popunjavanje seed podataka |

Seed zavrsava sa log-om `Seed uspesno ubasen!` (~60-90s od starta). Kada vidis to, API je spreman.

### Lokalno (bez Dockera)

Treba ti lokalni PostgreSQL 16 sa bazom `banka2` (user `banka2user`/pass `banka2pass`) na portu 5433. Zatim:

```bash
cd banka2_bek
./mvnw spring-boot:run
```

Za seed: `psql -h localhost -p 5433 -U banka2user -d banka2 -v ON_ERROR_STOP=1 -f ../seed.sql`

### Testovi

```bash
cd banka2_bek
./mvnw test                 # kompletan suite (2165 testova)
./mvnw test -Dtest=OrderServiceImplTest  # jedan test
./mvnw verify               # + JaCoCo coverage report u target/site/jacoco/
```

Testovi koriste **H2 in-memory u MODE=PostgreSQL** — ne treba ti DB.

## Arhitektura

```
rs.raf.banka2_bek/
├── auth/          # Login, JWT, password reset, permisije
├── account/       # Racuni (tekuci, devizni, poslovni, Business)
├── card/          # Kartice + zahtevi za izdavanje
├── client/        # CRUD klijenata, authorized_persons
├── company/       # Pravna lica
├── currency/      # EUR, USD, RSD, GBP, CHF, JPY, CAD, AUD
├── employee/      # CRUD zaposlenih + aktivacija
├── actuary/       # Supervisors + agents + dnevni trading limiti
├── exchange/      # Kursna lista (Fixer.io)
├── berza/         # Sama berza (NYSE, NASDAQ, CME, LSE, XETRA, BELEX)
├── stock/         # Listings (STOCK/FUTURES/FOREX)
├── order/         # Nalozi za trgovanje + scheduler za partial fills
├── option/        # Opcije + exercise
├── otc/           # OTC (Celina 4): ponude i opcioni ugovori
├── portfolio/     # Drzanje hartija + public_quantity za OTC
├── loan/          # Krediti + rate + prevremena otplata
├── margin/        # Margin racuni i transakcije
├── payment/       # Placanja + PDF potvrde + primaoci
├── transfers/     # Interni + FX transferi sa pessimistic lock-om
├── transaction/   # Istorija transakcija
├── tax/           # Porez na kapitalnu dobit 15%
├── notification/  # Email (async)
├── otp/           # OTP verifikacija placanja/transfera/ordera
└── loan/          # Krediti
```

Svaki modul prati konvenciju `controller/ dto/ mapper/ model/ repository/ service/`.

## Autentifikacija i role

- **JWT HS256**. Access token nosi `sub` (email), `role` (ADMIN/EMPLOYEE/CLIENT), `active`.
- FE posle logina fetchuje prave **permisije** sa `GET /employees?email=…` jer JWT nosi samo rolu.
- Hijerarhija:
  - **ADMIN** — sve; svaki admin je i supervizor
  - **SUPERVISOR** — aktuari portal, porez portal, odobravanje ordera
  - **AGENT** — Employee portal (racuni/kartice/klijenti) bez aktuari/porez
  - **CLIENT** — klijentski dashboard, racuni, placanja, berza (akcije + futures), OTC
- Permisije: `ADMIN`, `SUPERVISOR`, `AGENT`, `TRADE_STOCKS`, `VIEW_STOCKS`, `CONTRACT_MANAGER`, `CREATE_INSURANCE`, itd.

## Seed podaci i test kredencijali

| Tip | Email | Lozinka | Napomena |
|-----|-------|---------|----------|
| Admin | `marko.petrovic@banka.rs` | `Admin12345` | ADMIN + SUPERVISOR |
| Admin | `jelena.djordjevic@banka.rs` | `Admin12345` | ADMIN + SUPERVISOR |
| Supervisor | `nikola.milenkovic@banka.rs` | `Zaposleni12` | Samo SUPERVISOR (bez ADMIN) |
| Agent | `tamara.pavlovic@banka.rs` | `Zaposleni12` | limit 100K, needApproval=false |
| Agent | `djordje.jankovic@banka.rs` | `Zaposleni12` | limit 150K |
| Agent | `maja.ristic@banka.rs` | `Zaposleni12` | limit 200K, needApproval=true |
| Klijent | `stefan.jovanovic@gmail.com` | `Klijent12345` | 3 racuna + portfolio AAPL/MSFT/TSLA + CLM26 futures + OTC ponude i ugovori |
| Klijent | `milica.nikolic@gmail.com` | `Klijent12345` | 3 racuna + GOOG/AMZN + public za OTC |
| Klijent | `lazar.ilic@yahoo.com` | `Klijent12345` | 3 racuna + TSLA/GOOG |
| Klijent | `ana.stojanovic@hotmail.com` | `Klijent12345` | 2 racuna + NVDA/AAPL |

Seed sadrzi i 30 listinga, 6 berzi, 5 aktivnih OTC ponuda, 6 OTC ugovora (Stefan kao kupac + prodavac), margin racune sa transakcijama, i istorijske ordere.

## API pregled

Kompletna dokumentacija: **Swagger UI** na `http://localhost:8080/swagger-ui.html` (OpenAPI JSON na `/v3/api-docs`).

Skraceno:

### Autentifikacija
`POST /auth/login`, `/auth/refresh`, `/auth/password_reset/request`, `/auth/password_reset/confirm`, `POST /auth-employee/activate`

### Klijentski portal
- Racuni: `/accounts/my`, `/accounts/{id}`, `/accounts/requests`
- Kartice: `/cards`, `/cards/{id}/block`, `/cards/requests`
- Placanja: `/payments` (sa OTP), `/payments/{id}/receipt` (PDF), `/payment-recipients`
- Transferi: `/transfers/internal`, `/transfers/fx` (OTP obavezan)
- Berza: `/listings`, `/listings/{id}`, `/orders`, `/orders/my`
- OTC: `/otc/listings`, `/otc/offers/active`, `/otc/offers/{id}/accept`, `/otc/contracts/{id}/exercise`
- Portfolio: `/portfolio/my`, `/portfolio/summary`
- Porez: `/tax/my`

### Employee portal
- `/employees/**`, `/clients/**`, `/accounts/**` (svi klijenti)
- `/cards/requests/{id}/approve|reject`
- `/loans/requests`, `/loans/{id}/installments`

### Supervizor portal
- `/orders` (GET sve), `/orders/{id}/approve|decline`
- `/actuaries/**` (agent limits, reset)
- `/tax/collect` (sakupljanje poreza)

### Admin portal
- `/admin/employees/**` (kreiranje, aktivacija, permisije)
- `/exchanges/{acronym}/test-mode` (iskljuci AV u dev-u)

## Order execution engine

`OrderScheduler` svakih 10s pokupi APPROVED ordere i izvrsava ih preko `OrderExecutionService`:

- Random partial fills (1..remaining po ciklusu)
- After-hours orderi dobijaju +30min delay
- AON (All-or-None) se izvrsava atomicno
- Stop/Stop-limit prvo aktivira `StopOrderActivationService` pa postaju Market/Limit
- Provizije: MARKET `min(14% * cena, $7)`, LIMIT `min(24% * cena, $12)`. Zaposleni 0.
- FX komisija 1% se obracunava kad klijent trguje iz racuna u drugoj valuti

Rezultat: posle BUY, portfolio ti se napuni sam. SELL kasnije takodje. **Bez counterparty korisnika.**

## Cene hartija (real-time)

`POST /listings/refresh` okida `refreshPrices()`:

- **Stocks**: Alpha Vantage GLOBAL_QUOTE (4 API ključa u rotaciji za rate limit)
- **Forex**: Fixer.io
- **Futures**: random simulacija (nema free API)
- **Test mode**: kad je berza u test modu, AV/Fixer se preskacu — koristi se GBM simulacija da se ne trose kljucevi. `ListingDto.isTestMode` signalizira FE-u.

Scheduler (`ScheduledTasks.scheduledRefresh`) radi i periodicno.

## PostgreSQL migracija (april 2026)

Projekat je prebacen sa MySQL 8.0 na **PostgreSQL 16** zbog stabilnosti u Kubernetes klasteru:

- `pom.xml`: `com.mysql:mysql-connector-j` → `org.postgresql:postgresql`
- `application.properties`: JDBC url + dialect + `hibernate.type.preferred_boolean_jdbc_type=INTEGER` (boolean kolone idu kao `smallint` 0/1 za citljiv seed)
- 39 `@ColumnDefault` anotacija na `@Builder.Default` / NOT NULL poljima — Hibernate generise DB-level defaults pa seed ne mora da ih navodi eksplicitno
- `seed.sql`: automatska konverzija MySQL sintakse (DATE_SUB, DATE_ADD, CURDATE, INSERT IGNORE, ON DUPLICATE KEY, FROM DUAL, true/false literali)
- Testovi: H2 MODE=PostgreSQL

## Troubleshooting

- **Port 8080 zauzet (Windows Hyper-V)** — `net stop winnat && net start winnat` kao admin
- **Seed nije zavrsio** — `docker logs banka2_seed`; ako failuje, najcesce je neki ColumnDefault nedostaje, ili je seed dodat koji gadja nepostojece kolone
- **Mojibake u bazi** — `docker compose down -v` pa ponovo up (PG koristi UTF-8 default pa ovo ne bi trebalo da se desi)
- **Alpha Vantage rate limit** — ukljuci test mode za sve berze u admin portalu i refresh ce koristiti simulaciju

## Konfiguracija

Sve postavke u `banka2_bek/src/main/resources/application.properties`:

| Polje | Default | Opis |
|-------|---------|------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/banka2` | JDBC url (override preko env-a u Dockeru) |
| `jwt.secret` | hardkodiran | **MENJAJ U PRODUKCIJI** |
| `otp.expiry-minutes` | 5 | Koliko vazi OTP |
| `otp.max-attempts` | 3 | Posle 3 pogresna OTP → blokada |
| `stock.api.keys` | 4 kljuca u rotaciji | Alpha Vantage |
| `exchange.api.key` | Fixer.io | kursna lista |
| `orders.execution.initial-delay-seconds` | 60 | Scheduler ne fila odmah posle approval-a |
| `orders.afterhours.delay-seconds` | 60 | Dodatni delay za after-hours ordere |
| `bank.registration-number` | 22200022 | Matični broj banke |
| `state.registration-number` | 17858459 | Državni mb za porez |

## Tim

Banka 2025 Tim 2 — Racunarski fakultet, 2025/26. Predmet: **Softversko inzenjerstvo**.
