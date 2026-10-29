# Работа с проектом

- Согласованный стек: Spring Boot 4, Java 21, Kotlin, Gradle, PostgreSQL.
- Docker Compose только для локальной разработки, в `docker/`.
  `docker-compose-all-docked.yml` запускает весь стек; `docker-compose-local.yml` — инфраструктуру без приложения для IDEA.
  Сохранять общее имя проекта/томов; Prometheus использует app:9090 или host.docker.internal:9090 соответственно.
- Конфиги внешних QA/prod лежат в `consul/qa/` и `consul/prod/`; CI/CD пока вне текущего этапа.
- Внешние OpenAPI-контракты лежат в `src/main/resources/openapi/`.
  Telegram DTO генерировать через Gradle; не писать их вручную и не коммитить `build/generated`.
  Swagger UI нашего сервиса пользователь не запрашивает.
- Простые слои Controller → Service → Repository; правила отдельно от Spring и внешних API.
- Перед изменением поведения проверять README и docs/ARCHITECTURE.md, отмечать реализованное и запланированное.
- Не помещать секреты, реальные контакты и дампы в Git; тесты только на синтетических данных.
- Интеграционные тесты запускать на выделенной базе andryukha_test. Не использовать базу приложения.
- Тестовые вызовы Telegram — HTTP-заглушки, не реальные сообщения.
- Не подключать запись в 1С; future import adapter должен быть read-only.
- Основные проверки: `./gradlew openApiValidate test integrationTest bootJar` с JDK 21 и тестовой PostgreSQL;
  либо тестовый сервис в локальном Compose. После изменения Compose проверить `docker compose ... config --quiet`.
- Пользователь хочет участвовать в разработке: держать изменения обозримыми, объяснять решения и следующие точки расширения.
