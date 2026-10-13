# DesktopApplicationClientJava

```bash
git pull --rebase
```

## Запуск

Требования: JDK 25, Maven.

### С реальным бэкендом

```bash
# 1. Инфраструктура бэкенда (из корня этого репозитория):
cp .env.example .env
docker compose up -d postgres redis

# 2. Приложение бэкенда (из каталога соседнего репозитория, http://localhost:8080):
./mvnw spring-boot:run -Dmaven.test.skip=true

# 3. Приложение (из корня этого репозитория):
mvn javafx:run
```

### Без бэкенда (мок-данные для проверки UI)

```bash
MOCK_DATA=true mvn javafx:run
```

Приложение стартует на in-memory данных (`services/mock`, окно помечено `[MOCK]`):
вход — любой непустой email/пароль, роль по префиксу email (`admin*` → ADMIN,
`moder*` → MODER, иначе USER). Засижены 3 пользователя и 2 ресурса с файлами,
скачивание пишет реальный файл-плейсхолдер.

### Переменные окружения (.env)

| Переменная | Дефолт | Назначение |
| ---------- | ------ | ---------- |
| `INITIAL_PAGE_PATH` | `/fxml/LoginForm.fxml` | Стартовая страница (удобно открывать страницу напрямую для проверки вёрстки) |
| `MOCK_DATA` | `false` | `true` — работать на моках без бэкенда |
| `IT_ADMIN_EMAIL` / `IT_ADMIN_PASSWORD` | — | Сид-админ локального бэкенда для интеграционных тестов (тесты сами читают `.env`; без пароля скипаются) |
| `POSTGRES_*`, `REDIS_PASSWORD`, `APP_PORT` | см. `.env.example` | Инфраструктура и бэкенд |
| `MINIO_*` | см. `.env.example` | Только с `docker compose --profile files up -d` |

Свои значения — в `.env` (скопировать из `.env.example`): приложение читает
переменные из окружения (`set -a && source .env && set +a` перед запуском),
интеграционные тесты подхватывают `.env` сами.

Откроется окно 800×600 со стартовой страницы (по умолчанию вход, `LoginForm.fxml`).

### Запуск фронта в Docker

Образ (`Dockerfile`): сборка в `maven:3-eclipse-temurin-25`
(`mvn -B package -DskipTests`; spotless/checkstyle живут в `verify`,
поэтому `package` достаточно), запуск в `eclipse-temurin:25-jre`
с нативными библиотеками JavaFX для Jammy (GTK/GL/X11/звук/шрифты).
Точка входа запускает `com.DesktopApplicationClientJava.Main`,
classpath — собранный jar + `target/libs` (runtime-зависимости,
разложенные через `dependency:copy-dependencies`).

```bash
# Сборка образа:
docker build -t desktop-client .

# Запуск GUI на Linux (нужен X-сервер):
xhost +local:docker
docker run --rm -e DISPLAY=$DISPLAY -v /tmp/.X11-unix:/tmp/.X11-unix:ro desktop-client

# Запуск GUI на macOS (XQuartz с разрешёнными подключениями):
xhost +localhost
docker run --rm -e DISPLAY=host.docker.internal:0 desktop-client

# Через compose (профиль gui, тест-инфра по умолчанию не затрагивается):
docker compose --profile gui run --rm client
```

Переменные окружения (`-e`): `INITIAL_PAGE_PATH` (стартовая страница),
`MOCK_DATA=true` (работа без бэкенда), `JAVA_OPTS` (по умолчанию
`-Dprism.order=sw` — в контейнере нет GPU — и
`--enable-native-access=ALL-UNNAMED` — доступ JavaFX к нативным либам,
обязательный на JDK 25).

Ограничения: JavaFX — оконный GUI, в headless-контейнере без проброса
`DISPLAY`/X11-сокета окно не отобразится (процесс при этом стартует).
Клиент ходит в бэкенд по HTTP (порт `APP_PORT` на хосте), напрямую
к postgres/redis из `compose.yaml` не подключается.

## Git hooks

Перед каждым коммитом автоматически проверяются: линт (`checkstyle:check`),
формат (`spotless:check`) и unit-тесты (`mvn test`, без интеграционных).
Настройка один раз на клон:

```powershell
# Windows (PowerShell 5.1 / 7+):
.\setup.ps1

# macOS / Linux:
brew install lefthook
lefthook install
```

Ручная проверка всего сразу: `lefthook run pre-commit`.
Если упал формат — поправить: `mvn spotless:apply`.

## RBAC

В системе предусмотрено 3 роли: пользователь, модератор и администратор:

```plain
Пользователь (USER) <- Модератор (MODER) <- Администратор (ADMIN)
```

## Тесты

Два вида тестов:

- **Unit** (`mvn test`): API мокается, внешнего бэкенда не нужно.
- **Integration** (классы с `@Tag("integration")`): ходят в живой API без моков,
  им нужен поднятый бэкенд. Из дефолтной сборки и из CI они исключены
  (surefire `excludedGroups` в `pom.xml`), чтобы джобы не падали без бэкенда.

Локальный запуск интеграционных тестов:

```bash
cp .env.example .env
docker compose up -d
# для файловых/экспортных тестов: docker compose --profile files up -d

# запустить бэкенд из соседнего репозитория (порт 8080 из APP_PORT):
#   cd ../pks-java && ./mvnw spring-boot:run

mvn test -Dgroups=integration -DexcludedGroups=
```
