# DesktopApplicationClientJava

```bash
git pull --rebase
```

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
