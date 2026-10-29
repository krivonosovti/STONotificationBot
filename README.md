# Андрюха

Локальный backend рекомендательной системы для автосервиса Эрика.

Стек: Spring Boot 4.0.7, Java 21, Kotlin 2.2.20, Gradle 8.14.3, PostgreSQL 16,
Spring Data JPA, Flyway, Prometheus и Grafana. Контракты Telegram → Kotlin DTO через OpenAPI Generator.

## Запуск

Два режима используют общую инфраструктуру и тома. Запускайте один режим за раз:

| Compose-файл | Где приложение | PostgreSQL, Grafana, Prometheus |
| --- | --- | --- |
| `docker/docker-compose-all-docked.yml` | В Docker с Java 21 | В Docker |
| `docker/docker-compose-local.yml` | В IntelliJ IDEA на компьютере | В Docker |

Команды выполняются из корня репозитория. При первом запуске скопируйте настройки:

```sh
cp docker/.env.example docker/.env  # не перезаписывайте существующий файл
```

Compose автоматически читает `docker/.env`. Приложение из IDEA этот файл автоматически не читает.

### Разработка в IDEA

```sh
# Если раньше приложение было запущено в Docker, сначала освободите порт 8080:
docker compose -f docker/docker-compose-all-docked.yml stop app
# Удаляет оставшийся контейнер приложения, сохраняя общие тома:
docker compose -f docker/docker-compose-local.yml up -d --wait --remove-orphans
```

В IDEA откройте корень проекта как Gradle-проект. Выберите JDK 21 в Project SDK
и Gradle JVM. Создайте Application Run Configuration:

- Main class: `andryukha.AndryukhaApplicationKt`.
- Classpath/module: `andryukha.main`.
- JRE: JDK 21.
- Environment variables (для стандартного `docker/.env.example`):

```dotenv
SPRING_PROFILES_ACTIVE=local
DB_URL=jdbc:postgresql://localhost:5433/andryukha_local
DB_USER=andryukha
DB_PASSWORD=local-db-password
API_USER=admin
API_PASSWORD=local-api-password
```

Если меняли имя БД, пользователя или пароль в `docker/.env`, перенесите соответствующие
значения в конфигурацию IDEA. Для сборки перед запуском используйте Gradle task `classes`:
она автоматически генерирует Telegram DTO. Можно также запускать Gradle task `bootRun`
с теми же переменными окружения.

Приложение слушает API на 8080 и Actuator на 9090. Prometheus в Docker собирает метрики
с `host.docker.internal:9090`, Grafana читает их из Prometheus. На macOS этот адрес
доступен через Docker Desktop; в Compose также есть host-gateway для Linux.
Management-порт должен быть доступен из Docker, поэтому не привязывайте его только к
loopback на Linux. Это режим для локальной разработки в доверенной сети.
До запуска приложения в IDEA статус цели Prometheus `DOWN` ожидаем.

### Весь стек в Docker

Сначала остановите приложение в IDEA, если оно запущено, затем:

```sh
docker compose -f docker/docker-compose-all-docked.yml up --build -d --wait
docker compose -f docker/docker-compose-all-docked.yml logs -f app
```

JDK на компьютере для этого режима не нужен. Prometheus собирает метрики с `app:9090`.
Общие определения PostgreSQL, Grafana и тестовых сервисов переиспользуются из local-файла
через `extends`; оба Compose-файла запускаются самостоятельно одной командой.

| Что | Адрес |
| --- | --- |
| API | http://localhost:8080/api/v1 |
| PostgreSQL | localhost:5433, база `andryukha_local` |
| Grafana | http://localhost:3000, дашборд «Андрюха — сервис» |
| Prometheus | http://localhost:9091 |

Локальные реквизиты из `.env.example`: API `admin / local-api-password`,
Grafana `admin / local-grafana-password`, PostgreSQL `andryukha / local-db-password`.
Это демонстрационные пароли только для локальной разработки. Swagger UI не устанавливается.
Опубликованные Docker-порты привязаны к `127.0.0.1`.

Остановка инфраструктуры (выберите Compose-файл используемого режима):

```sh
docker compose -f docker/docker-compose-local.yml down
```

Общее имя проекта `andryukha-local` сохраняет БД, дашборды и историю метрик при смене режима.
Обычный `down` сохраняет тома. `down -v` удаляет их, включая локальную БД.

## Проверка API

```sh
curl --fail-with-body -u admin:local-api-password \
  -H 'Content-Type: application/json' \
  --data-binary @examples/visits.json \
  http://localhost:8080/api/v1/imports/visits

curl --fail-with-body -u admin:local-api-password \
  'http://localhost:8080/api/v1/recommendations?asOf=2026-10-29'

curl --fail-with-body -u admin:local-api-password \
  'http://localhost:8080/api/v1/visits?page=0&size=50'
```

Пример содержит только вымышленные идентификаторы. Первый импорт добавит три визита;
повторный вернёт `unchanged: 3`. Для примера ожидается `READY` и срок `2026-10-29`.
`asOf` не может быть в будущем. Если не передать его, используется текущая дата сервиса.

Пакет содержит 1–1000 визитов. `externalId` — стабильный идентификатор документа источника;
один документ может содержать несколько `services`. Изменённый документ обновляет запись.
Для отмены передайте тот же идентификатор и `status: CANCELLED`. Отсутствие записи в новом
пакете не означает удаление: импорт инкрементальный. Конкурентный конфликт возвращает 409;
повторить нужно весь пакет. В этой версии поддерживается один автосервис и один источник.

## Telegram и генерация DTO

Контракт: `src/main/resources/openapi/telegram-bot-api.yaml`.
Это наш ограниченный контракт по [документации Telegram](https://core.telegram.org/bots/api),
а не официальный OpenAPI-файл Telegram. Пока описаны `getMe`, `sendMessage` и связанные модели.

```sh
# При наличии JDK 21 на компьютере:
./gradlew openApiValidate openApiGenerate
./gradlew test bootJar

# Только через Docker:
docker compose -f docker/docker-compose-local.yml run --rm --no-deps tests \
  ./gradlew --no-daemon openApiValidate openApiGenerate
```

`compileKotlin` автоматически запускает генерацию. DTO появляются в
`build/generated/telegram/src/main/kotlin/andryukha/integration/telegram/generated/model`.
Менять нужно YAML; сгенерированные файлы не редактируем и не коммитим.
Клиент написан вручную, внешние модели запросов и ответов сгенерированы.

Для подключения своего бота внесите в `docker/.env`:

```dotenv
TELEGRAM_ENABLED=true
TELEGRAM_BOT_TOKEN=токен_из_BotFather
```

После изменения окружения выполните `docker compose -f docker/docker-compose-all-docked.yml up -d app`.
Для запуска из IDEA задайте `TELEGRAM_ENABLED` и `TELEGRAM_BOT_TOKEN` в её Run Configuration
и перезапустите приложение. Токен не коммитится и не выводится в ответах API. Проверка подключения:

```sh
curl --fail-with-body -u admin:local-api-password \
  http://localhost:8080/api/v1/integrations/telegram/status
```

Для ручной тестовой отправки предусмотрен `POST /api/v1/integrations/telegram/test-message`
с телом `{"chatId":123456789,"text":"Проверка Андрюхи"}`. Это **реальная отправка**;
нужен chat ID своего тестового диалога, предварительно начатого пользователем.
Телефон из 1С не является Telegram chat ID. Повтор запроса отправит новое сообщение;
автоматических повторов у клиента нет. Входящие updates, привязка клиента и согласия ещё не реализованы.
Без токена статус сообщает `enabled: false`, отправка возвращает 503. Ошибка Telegram — 502.

## Тесты

```sh
docker compose -f docker/docker-compose-local.yml --profile test run --rm tests
docker compose -f docker/docker-compose-local.yml --profile test stop test-postgres
```

Это запускает unit-тесты и интеграционные тесты на отдельном PostgreSQL `andryukha_test`.
Тестовая БД хранится в tmpfs, не публикует порт и не использует данные приложения.
Проверка имени БД блокирует запуск интеграционных тестов на другой базе.
Telegram проверяется через HTTP-заглушку: настоящих отправок в тестах нет.
Отчёты: `build/reports/tests/test/index.html` и `build/reports/tests/integrationTest/index.html`.

## Структура и точки входа

```text
docker/                         два режима Compose, Dockerfile, мониторинг
consul/qa/                      внешняя конфигурация QA
consul/prod/                    внешняя конфигурация production
src/main/resources/openapi/     контракты внешних API
src/main/resources/db/migration миграции PostgreSQL
src/main/kotlin/andryukha/
  api/                          HTTP-контроллеры, входные DTO, обработка ошибок
  service/                      сценарии импорта, чтения, расчёта, Telegram
  domain/                       история визитов и независимые бизнес-правила
  persistence/                  JPA-сущности и репозиторий
  integration/telegram/         адаптер Telegram Bot API
  config/                       Clock, правила, авторизация
```

Сейчас выполнение запускается HTTP-запросом: `Controller → Service → Repository / Strategy / Gateway`.
`HistoryBasedRecommendationStrategy` оценивает срок ТО по медиане последних интервалов.
Она требует минимум три завершённых ТО в разные даты, использует максимум пять последних ТО,
не смешивает виды работ и возвращает причины отказа от рекомендации. Пробег пока служит
проверкой качества, а не основанием для прогноза. Это предварительная оценка визита, не диагностика.

Нет автоматического опроса 1С, расписания рассылок, очереди, обработчика ответов и учёта согласий.
Расчёт рекомендаций ничего не отправляет. Следующий этап описан в `docs/ARCHITECTURE.md`.

## Окружения

Compose используется только локально. В `consul/qa` и `consul/prod` лежат конфигурации для
уже развёрнутых внешних PostgreSQL и инфраструктуры. Секреты задаются переменными окружения.
Загрузка из HashiCorp Consul и CI/CD пока не подключены; подробности в `consul/README.md`.

Порядок запуска из IDEA и переключения режимов приведён в разделе «Запуск».

## Мониторинг

Actuator работает на отдельном порту 9090: внутри Docker в режиме all-docked
или на компьютере в режиме IDEA. Для каждого режима используется свой конфиг Prometheus
из `docker/monitoring/prometheus/`.
Prometheus собирает HTTP, JVM, Hikari и прикладные счётчики; Grafana получает источник и дашборд автоматически.
`andryukha_import_visits_total` считает результаты успешно зафиксированных импортов.
`andryukha_recommendations_evaluated_total` считает результаты запусков расчёта — не уникальных клиентов.
Идентификаторы клиентов, телефоны и токены не используются в метках метрик.
