# Temperature Converter

Individual in-class assignments, Metropolia UAS.

A JavaFX desktop app that converts temperatures and saves every conversion to a MariaDB
database. It runs in five languages: English, Finnish, Swedish, Japanese and Arabic. The
buttons and titles come from `.properties` files, but the unit names and descriptions come
from the database in whichever language the user picked.

## 1. What the app does

- Pick a language at startup, right-to-left layout when Arabic is selected
- Convert between any two of Celsius, Fahrenheit and Kelvin
- Warn when the result is an extreme temperature
- Read unit names and descriptions from the database, falling back to English
- Save each conversion with both units, the result, the language and a timestamp
- Show the last 5 conversions in a table
- Handle bad input without crashing

Built, tested and shipped through JUnit, JaCoCo, Docker, Kubernetes and Jenkins.

## 2. Technologies

- **Java 17**, **Maven**
- **JavaFX 17.0.11** with FXML
- **MariaDB** with utf8mb4, JDBC driver 3.3.3
- **JUnit 5** and **H2** in-memory for the DAO tests
- **JaCoCo** for coverage, **Maven Shade** for the runnable JAR
- **Docker** (multi-stage), **minikube**, **Jenkins**, **Docker Hub**

The JavaFX natives come from a Maven profile: `win` on Windows, `linux` inside Docker. Having
both listed at once made the two sets of native libraries clash and the app would not start.

## 3. DB localization approach: translation table

I picked the **translation table**: a language-neutral base table plus a separate table with
one row per unit and language.

```sql
temperature_units
  unit_id    INT AUTO_INCREMENT PRIMARY KEY
  unit_key   VARCHAR(20) NOT NULL UNIQUE   -- celsius, fahrenheit, kelvin
  symbol     VARCHAR(5)  NOT NULL          -- C, F, K

unit_translation
  unit_id     INT         NOT NULL, FK -> temperature_units
  lang        VARCHAR(5)  NOT NULL         -- en, fi, sv, ja, ar
  name        VARCHAR(50) NOT NULL
  description VARCHAR(200)
  PRIMARY KEY (unit_id, lang)

temp_records
  record_id        INT AUTO_INCREMENT PRIMARY KEY
  input_value      DOUBLE NOT NULL
  converted_value  DOUBLE NOT NULL
  from_unit_id     INT, FK -> temperature_units
  to_unit_id       INT, FK -> temperature_units
  lang             VARCHAR(5) NOT NULL DEFAULT 'en'
  created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
```

**Why.** What convinced me was looking at what actually changes between languages. The symbol
`C` is the same everywhere, so is the ID, and so is every foreign key pointing at it. Only
the name and description change. The field approach would add a column per language; the row
approach would duplicate the symbol five times per unit. Splitting the table stores the
stable parts once and repeats only the text. Adding a sixth language is five INSERTs — no
schema change, no code change.

The second reason is the fallback. Because the translations sit in their own table, I can
join it twice in one query:

```sql
LEFT JOIN unit_translation t ON t.unit_id = u.unit_id AND t.lang = ?   -- requested
LEFT JOIN unit_translation f ON f.unit_id = u.unit_id AND f.lang = 'en'
...
COALESCE(t.name, f.name) AS name
```

`COALESCE` takes the requested translation when it exists and English when it does not. The
joins are `LEFT` on purpose: with `INNER JOIN` a unit with no translation would vanish from
the list instead of showing in English.

The cost is one extra join per query. For three units and five languages that is nothing.

**How the two layers stay in sync.** `Messages.setLocale()` swaps the ResourceBundle, and
`Messages.getLang()` returns the same locale as `en`/`fi`/`sv`/`ja`/`ar` — exactly what the
`lang` column stores. Every DAO call takes that string. That is the whole link.

## 4. Structure

```
fi.metropolia.tempconverter
├── Launcher              starts the app
├── MainApp               loads the FXML, picks a CJK-capable font, asks for the language
├── controller/
│   └── MainController    button handlers, fills the dropdowns from the DB
├── model/
│   ├── TemperatureConverter  the formulas, no DB and no JavaFX
│   ├── TemperatureUnit       unit_key + symbol + localized name and description
│   └── TempRecord            one saved conversion
├── dao/
│   ├── TemperatureUnitDAO    localized reads with the English fallback
│   └── TempRecordDAO         insert, and the last 5 with unit names joined in
├── db/
│   └── DBConnection      reads DB_URL / DB_USER / DB_PASS from the environment
└── i18n/
    └── Messages          the ResourceBundle, UTF-8, and getLang()
```

**Launcher** exists because a JavaFX `Application` cannot be the main class of a shaded JAR —
the JVM refuses with "JavaFX runtime components are missing".

**Conversion logic** pivots through Celsius, so `convert(value, from, to)` covers all nine
combinations with three formulas each way. Both switches run on `unit_key`, never on the
displayed name: the key is `"celsius"` in every language while the name is `摂氏` in Japanese.
If the logic depended on the name, switching language would break the app.

**GUI.** `main-view.fxml` uses `%key` placeholders so FXMLLoader pulls every label from the
selected bundle. The two dropdowns hold `TemperatureUnit` objects straight from the database;
`toString()` returns `name (symbol)`, so Japanese shows `摂氏 (C)` with no extra code.

I had to remove the `<Font name="Arial">` entries — Arial does not exist in the container and
JavaFX fell back to a Latin-only font, so Japanese rendered as empty boxes. `pickFont()` now
asks JavaFX which families exist and picks the first that can draw CJK.

**DBConnection** falls back to `localhost:3307` when the environment variables are unset,
which is what lets the same JAR run locally, in Docker and in Kubernetes untouched. Its
`setCredentials()` is what the tests use to point at H2.

**Docker** is multi-stage: Maven builds the JAR inside the image so it picks up the Linux
natives, then the runtime stage installs GTK, X11 and the Noto fonts.

**Jenkins** runs eight stages, from checkout to pushing both image tags. The Docker Hub token
comes from Jenkins credentials and is never in the repository.

## 5. Testing

69 tests in 7 classes, all passing. Models tested on their own, DAOs against H2 in MySQL mode.

I chose H2 over mocking JDBC because mocks would only prove the code calls JDBC, not that the
SQL is right — and the SQL is the interesting part here, with the double join and COALESCE.

| Class | Tests | Covers |
|---|---|---|
| TemperatureConverterTest | 14 | formulas, all nine pairs, unknown keys, extreme boundaries |
| TemperatureUnitDAOTest | 15 | localized reads, **four fallback cases**, insert, delete |
| TempRecordDAOTest | 13 | insert with language, last 5, localized history, fallback |
| MessagesTest | 10 | five bundles, UTF-8, RTL, getLang(), every key present |
| DBConnectionTest | 6 | reuse, close, recreate, credential switch |
| TemperatureUnitTest | 6 | model, key stable across languages |
| TempRecordTest | 5 | model, fields filled by the DAO join |

**The fallback tests.** The seeded data is deliberately incomplete — Kelvin has an English row
only. Four tests cover it: a missing translation, a unit with no translation still appearing
in the list, an unknown language (`de`) returning everything in English, and a new unit with
only English.

The one I like most is in `TempRecordDAOTest`: a Celsius-to-Kelvin record read in Japanese
comes back as `摂氏 → Kelvin` — source translated, target fallen back, same row. That proves
the fallback works per unit, not per query.

I used -40 on purpose in the conversion tests since it is the one point where Celsius and
Fahrenheit are equal. Alone it would still pass with the formula reversed, but together with
32 and 212 it catches it.

### Coverage

| Package | Line coverage |
|---|---|
| **model** | **100%** (branches 100%) |
| **dao** | **98%** |
| i18n | 96% |
| db | 93% |
| controller | 0% |
| Total | 61% |

The core logic is at 98–100%. `MainController` and `MainApp` are not unit tested because
JavaFX event handlers need a running toolkit — which is exactly why the formulas live in
`TemperatureConverter` and the queries in the DAOs. What stays untested is layout and button
wiring, checked by hand.

Report at `target/site/jacoco/index.html`, regenerated on every build.

### Manual testing

1. Started in Japanese: dropdowns showed `摂氏 (C)`, `華氏 (F)`, `ケルビン (K)` from the database.
2. Started in Arabic: layout flipped right-to-left, names in Arabic.
3. 212 °F to Celsius gave 100.00 and a new row.
4. 122 °F, exactly 50 °C, was not flagged as extreme.
5. Saved two conversions in Arabic, restarted in Japanese: same rows, Japanese names,
   `ar` still in the language column.
6. Same unit in both dropdowns gave "pick two different units".
7. "abc" gave "invalid number" in red. Nothing saved, no crash.
8. After 8 conversions the table still showed only the newest 5.
9. Stopped MariaDB: the app opened and showed the connection error in red.
10. Docker image against an X server: Japanese and Arabic both rendered.
11. minikube: `kubectl get pods` showed 1/1 Running and the window appeared.
12. Jenkins: all eight stages green, both tags on Docker Hub.

## 6. How to run

**You need** JDK 17, Maven 3.6+, MariaDB on port 3307. Docker and an X server for the
container, minikube for the deployment. The tests need none of it — they use H2.

### Database

```bash
mysql -u root -p -P 3307 < init.sql
```

Drops and recreates `tempdb` with utf8mb4, creates the three tables, fills in all five
languages.

### Build and run

```bash
mvn clean package
mvn javafx:run
```

or `java -jar target/week3-junit-1.0-SNAPSHOT.jar`

### Docker

Start the X server with access control off first:

```bash
"/c/Program Files (x86)/Xming/Xming.exe" -multiwindow -clipboard -ac &
```

```bash
docker build -t temperature-converter .

docker run --rm \
  -e DISPLAY=host.docker.internal:0.0 \
  -e DB_URL="jdbc:mariadb://host.docker.internal:3307/tempdb?useUnicode=true&characterEncoding=utf8" \
  -e DB_USER=root -e DB_PASS=root \
  temperature-converter
```

Also on Docker Hub: `docker pull abdullah22hel/temperature-converter:latest`

### Kubernetes

```bash
minikube start
kubectl apply -f deployment.yaml
kubectl get pods
```

`deployment.yaml` points `DISPLAY` and `DB_URL` at the host's IP, since both the X server and
MariaDB run outside the cluster.

### Tests alone

```bash
mvn test
```

Results in `target/surefire-reports/`, coverage at `target/site/jacoco/index.html`.

### Settings

| Variable | Default |
|---|---|
| DB_URL | jdbc:mariadb://localhost:3307/tempdb?useUnicode=true&characterEncoding=utf8 |
| DB_USER | root |
| DB_PASS | root |

Change any of them without rebuilding.