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

## Git hooks

Перед каждым коммитом автоматически проверяются: линт (`checkstyle:check`),
формат (`spotless:check`) и unit-тесты (`mvn test`, без интеграционных).
Настройка один раз на клон:

```powershell
# Windows (PowerShell 5.1 / 7+):
.\setup.ps1

# macOS / Linux:
pip install pre-commit
pre-commit install
```

Ручная проверка всего сразу: `pre-commit run --all-files`.
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
