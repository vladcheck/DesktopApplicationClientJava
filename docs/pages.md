# Страницы приложения

## RBAC

Иерархия: `USER <- MODER <- ADMIN` (старшая наследует права младшей).
Доступ `USER` означает `USER, MODER, ADMIN`; `MODER` означает `MODER, ADMIN`.

Маршрут — путь к FXML для `Navigator.goTo(...)`.

## Паттерн взаимодействия с API

UI не ходит в HTTP напрямую. Контроллер вызывает сервис («middleware»):
`Controller → services.getXService() → ApiClient → HTTP + Session (Bearer)`.
Сервис кладёт пользователя/токены в `Session`, ошибки пробрасывает как
`ServiceException`, контроллер показывает их через `Alert`.
Пример: docs/examples/how-to-interact-with-api-from-ui-via-middleware.md.

| №   | Название страницы                            | Маршрут (FXML)                | Доступ            | Назначение                                                                                                                                                       | Ответственный | Статус                        |
| --- | -------------------------------------------- | ----------------------------- | ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------- | ----------------------------- |
| 1   | Вход (email + пароль)                        | `/fxml/LoginForm.fxml`        | гость (публичная) | Аутентификация через `AuthService`, после успеха → №2                                                                                                            | @vladcheck     | ✅ готово                      |
| 2   | Список ресурсов и сортировка (главная)       | `/fxml/ResourceList.fxml`     | `USER`            | Поиск по `title` и `description`, удаление с подтверждением, сортировка, пагинация `offset/count` (`PAGE_SIZE=20`) через `ResourceService.search`                    | @miyagi18      | ✅ готово                      |
| 3   | Просмотр метаданных ресурса                  | `/fxml/ResourceDetails.fxml`  | `USER`            | Только чтение: `ResourceService.getById` → `title, description, createdAt, updatedAt, uuid`; таблица файлов (имя/размер/тип + «Скачать»)                           | @vladcheck     | ⚠️ частично (см. DOD №3)       |
| 4   | Форма ресурса (создание/редактирование)      | `/fxml/ResourceForm.fxml`     | `MODER`           | `create/update` через `ResourceService`; удаление — действием из №2/№3 с подтверждением, без отдельной страницы                                                  | @miyagi18      | ✅ готово                      |
| 5   | Список пользователей                         | `/fxml/UserList.fxml` (нет)   | `ADMIN`           | Поиск/фильтр через `UsersService.search` + `UserFilter`; удаление действием из списка                                                                            | @deatheh       | ❌ нет (сервис готов, UI нет)  |
| 6   | Форма пользователя (создание/редактирование) | `/fxml/UserForm.fxml` (нет)   | `ADMIN`           | `create` через `RegisterData`, `update` через `UpdateUserData`, `getById` для предзаполнения                                                                     | @deatheh       | ❌ нет (сервис готов, UI нет)  |

## DOD для ⚠️-страниц

### №3 — Просмотр метаданных ресурса

* Есть: `ResourceDetailsController` (read-only): метаданные + таблица файлов (`name, size, contentType` + колонка «Скачать»), скачивание через `FileService.downloadFile` (`DirectoryChooser`, имя из `Content-Disposition`, fallback `fileUuid`), вход из №2 через `setResourceId(uuid)` + `load()`, кнопка «Редактировать» только для `MODER/ADMIN` (переход в №4), кнопка «Удалить» с диалогом подтверждения только для `MODER/ADMIN` (после удаления → №2).
* Нет: кнопок загрузки/удаления файлов в UI нет; кнопки экспорта архива файлов нет.
* Готово когда: заведены upload/delete (`FileService.{uploadFile, deleteFile}` уже есть) и экспорт (`ExportService.exportResourceFiles` уже есть) либо зафиксировано решение «в №3 только скачивание».

## Интеграции страниц с эндпоинтами (через сервисы)

Базовый URL: `api.baseUrl` (`http://localhost:8080`, см. `src/main/resources/api.properties`).

| №   | Действие UI                             | Сервис (middleware)                                  | ApiClient → HTTP                                                                                                                       |
| --- | --------------------------------------- | ---------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | Войти                                   | `AuthService.login(email, password)`                 | `POST /api/v1/auth/login` (`LoginRequest`), затем `GET /api/v1/user/me`; токены → `Session`                                            |
| 1   | Выйти                                   | `AuthService.logout()`                               | `POST /api/v1/auth/logout` (`RefreshTokenRequest`), затем `Session.clear()`                                                            |
| 2   | Поиск/сортировка ресурсов | `ResourceService.search(filter, offset, count[, sortBy, sortDir])` | `GET /api/v1/resource?title=&description=&offset=&count=&sortBy=&sortDir=` (UI шлёт `title` и `description`; сортировка `createdAt/title asc/desc`) |
| 3   | Открыть метаданные                      | `ResourceService.getById(id)`                        | `GET /api/v1/resource/{id}`                                                                                                            |
| 3   | Скачать файл (UI-факт)                  | `FileService.downloadFile`                           | `GET /api/v1/resource/{id}/file/{fileId}` (имя из `Content-Disposition`)                                                               |
| 3   | Загрузить/удалить файл (сервис готов, UI не заведено) | `FileService.{uploadFile, deleteFile}`    | `POST /api/v1/resource/{id}/file` (multipart), `DELETE /api/v1/resource/{id}/file/{fileId}`                                             |
| 3   | Экспорт файлов ресурса (сервис готов, UI не заведено) | `ExportService.exportResourceFiles(id, dir)` | `GET /api/v1/admin/resource/{id}/files/export` (бинарный ответ → файл `files-{uuid}.xlsx`)                                              |
| 4   | Создать ресурс                          | `ResourceService.create(title, description)`         | `POST /api/v1/resource` (`CreateResourceRequest`)                                                                                      |
| 4   | Обновить ресурс                         | `ResourceService.update(id, title, description)`     | `PUT /api/v1/resource/{id}` (`UpdateResourceRequest`)                                                                                  |
| 4   | Удалить ресурс из №2/№3 | `ResourceService.delete(id)`              | `DELETE /api/v1/resource/{id}`                                                                                                         |
| 5   | Поиск/фильтр пользователей (сервис готов, UI не заведено) | `UsersService.search(filter, offset, count)` | `GET /api/v1/admin?email=&firstName=&lastName=&role=&enabled=&offset=&count=&sortBy=createdAt&sortDir=desc` (сортировка захардкожена в `UsersServiceImpl`) |
| 5   | Удалить пользователя (сервис готов, UI не заведено) | `UsersService.delete(id)`                 | `DELETE /api/v1/admin/{id}`                                                                                                            |
| 5   | Экспорт пользователей/ресурсов (сервис готов, UI не заведено) | `ExportService.{exportUsers, exportResources}` | `GET /api/v1/admin/users/export` (→ `users.xlsx`), `GET /api/v1/admin/resource/export` (→ `resources.xlsx`)                             |
| 6   | Создать пользователя (сервис готов, UI не заведено) | `UsersService.create(RegisterData)`       | `POST /api/v1/admin` (`RegisterRequest`: email, password, role, firstName, lastName)                                                   |
| 6   | Открыть пользователя для редактирования (сервис готов, UI не заведено) | `UsersService.getById(id)` | `GET /api/v1/admin/{id}`                                                                                                               |
| 6   | Обновить пользователя (сервис готов, UI не заведено) | `UsersService.update(id, UpdateUserData)` | `PUT /api/v1/admin/{id}` (`UpdateUserRequest`: email, enabled, role, firstName, lastName)                                              |

Примечания:
* `ApiClient` при 401 (кроме `/api/v1/auth/*`) один раз обновляет токены через
  `POST /api/v1/auth/refresh` и повторяет запрос; если refresh нет/не удался —
  исходный ответ уходит в `ServiceException`.
* Серверные ошибки (`ApiException` → `ServiceException`) показывает контроллер;
  проверка ввода выполняется до вызова сервиса (см. «Примечания по реализации»).

## Примечания по реализации

* №1: ✅ готово — `LoginFormController` только email + пароль: проверка непустоты + `EmailValidator.validate`, вызов `AuthService.login`, после успеха → №2.
* №2: ✅ готово — `ResourceListController`: поиск по `title` и `description` (`ResourceFilter`), сортировка, пагинация `PAGE_SIZE=20`, переход в №3 дабл-кликом, кнопки «+ Создать документ» и «Удалить» (с диалогом подтверждения) только для `MODER/ADMIN`.
* №3: частично — `ResourceDetailsController` (read-only): метаданные + таблица файлов
  со скачиванием (`FileService.downloadFile`), вход из №2 — `Navigator.goTo(path, c -> ...)`
  с `setResourceId(uuid)` + `load()`. Upload/delete/экспорт не заведены.
* №4: частично — `ResourceFormController` в режимах `create/edit` по наличию `resourceId`
  (предзаполнение через `getById`); проверка `title` вручную (`3–255`), не через `validation/*Validator`.
* №5–№6: ❌ UI нет (`UserList.fxml`/`UserForm.fxml`, контроллеров нет). Сервисный слой готов:
  `UsersServiceImpl` + `ApiClient` (`getUsersByFilters/createUser/getUserById/updateUser/deleteUser`).
* Роли: `AuthServiceImpl.hasRole` сравнивает по уровню (`USER=0 < MODER=1 < ADMIN=2`),
  поэтому `isModerator()` возвращает true и для `ADMIN` — соответствует RBAC выше.
* SoC: списки (№2) не содержат логики форм; форма №4 работает в двух режимах `create/edit` по наличию `uuid`.
* Кнопки: `create`/`delete` (№2) и `edit`/`delete` (№3) рендерятся по роли (`isAdmin() || isModerator()` из `Session.getCurrentUser().role`), а не отдельными страницами.
* Проверка ввода (факт): №1 — `EmailValidator`; №4 — ручные проверки длины; классы
  `PasswordValidator/FirstNameValidator/LastNameValidator` на FormsFX существуют как задел под №6, в текущих страницах не используются.
* Терминология: в доке используется «ресурс» per `GLOSSARY.md` (avoid «документ»).
  Факт расхождения: строки UI/FXML до сих пор говорят «документ» («Новый документ»,
  «Редактирование документа», «Архив документов», «Документов пока нет») — правится отдельно, не в этом доке.

## Тесты (факт)

Стек: JUnit5 + AssertJ; TestFX headless настроен в `maven-surefire-plugin`
(`-Dtestfx.headless=true`, Monocle, `--enable-native-access=ALL-UNNAMED`,
`-XX:+EnableDynamicAgentLoading`). WireMock в `pom.xml` отсутствует.

* Дефолтный прогон (без бэкенда): unit + UI на in-memory моках — `ServiceRegistry.mock`
  (`MockAuthService/MockUsersService/MockResourceService/MockFileService/MockExportService` + `MockStore`),
  `MOCK_DATA=true` / `INITIAL_PAGE_PATH` в `Program`. Покрыты: `LoginFormController`,
  `ResourceListController`, `ResourceDetailsController`, `ResourceFormController`, мапперы, валидаторы.
* Удаление ресурсов: компонентные тесты настоящей модалки (`ResourceListControllerTest`,
  `ResourceDetailsControllerUiTest`: открытие/отмена/подтверждение/ошибка 404) + live-цикл
  в `ResourceListIntegrationTest` (создание → удаление → проверка отсутствия на бэкенде;
  кейс за MODER — `@Disabled`, сида MODER в бэкенде нет).
* Live-интеграционные (`@Tag("integration")`, `src/test/.../ui/integration/`):
  исключены из дефолтного билда/CI через `excludedGroups` в surefire; требуют живой
  бэкенд `localhost:8080` и `.env` (`IT_ADMIN_EMAIL`, `IT_ADMIN_PASSWORD`, `dotenv.path`).
  Запуск явно: `mvn test -Dgroups=integration -DexcludedGroups=`.
* Скачивание/экспорт (факт кода, без UI-кнопок): `ApiClient.downloadFile` сохраняет в
  `destinationDir`, имя из `Content-Disposition` (fallback `fileUuid`); `ExportServiceImpl`
  пишет `users.xlsx` / `resources.xlsx` / `files-{uuid}.xlsx`.
* Refresh (факт кода): `ApiClient.executeWithRetry` при 401 (кроме `/api/v1/auth/*`)
  один раз делает `POST /api/v1/auth/refresh` и повторяет запрос с новым Bearer без участия UI.
