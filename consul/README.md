# Конфигурации внешних окружений

`qa/application.yml` и `prod/application.yml` — конфигурации приложения для окружений
с уже развёрнутыми PostgreSQL, Prometheus и Grafana. Docker Compose для них не нужен.

Это подготовленные файлы для дальнейшей загрузки в Consul. Сам Consul Config client,
адрес Consul, ACL-токен, структура KV и CI/CD пока не настроены. Файлы не читаются
из этого каталога автоматически при локальном запуске.

До подключения Consul можно загрузить конфиг стандартным механизмом Spring:

```sh
SPRING_PROFILES_ACTIVE=qa \
SPRING_CONFIG_ADDITIONAL_LOCATION=file:./consul/qa/ \
java -jar build/libs/andryukha.jar
```

Перед запуском задаются `DB_URL`, `DB_USER`, `DB_PASSWORD`, `API_USER`, `API_PASSWORD`.
Например, `DB_URL` указывает на реальный PostgreSQL окружения; адрес и реквизиты здесь не выдумываем.
`SERVICE_TIMEZONE`, `TELEGRAM_ENABLED` и `TELEGRAM_BOT_TOKEN` при необходимости задаются отдельно.
Для prod аналогично используются профиль `prod` и каталог `consul/prod/`.

Порт 9090 предназначен для Prometheus во внутренней сети. Доступ к нему ограничивается
инфраструктурой окружения; внешний API с Basic Auth должен публиковаться через HTTPS.
Резервное копирование, роли БД, TLS, доступы, маршрутизация и процесс выпуска — отдельный следующий этап.
