# DesktopApplicationClientJava

```bash
git pull --rebase
```

## Запуск

Требования: JDK 25, Maven, запущенный бэкенд (иначе вход покажет ошибку соединения).

```bash
# 1. Инфраструктура бэкенда (из корня этого репозитория):
cp .env.example .env
docker compose up -d postgres redis

# 2. Приложение бэкенда (из каталога соседнего репозитория, http://localhost:8080):
./mvnw spring-boot:run -Dmaven.test.skip=true

# 3. Приложение (из корня этого репозитория):
mvn javafx:run
```

Откроется окно 800×600 со страницы входа (`LoginForm.fxml` — она же `INITIAL_PAGE_PATH`
в `Program.java`). Для проверки вёрстки отдельной страницы можно временно подменить
`INITIAL_PAGE_PATH` на нужный FXML.

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
