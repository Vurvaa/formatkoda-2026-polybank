# polybank

Откат миграции (делать только локально, чтобы не было конфликтов)
```bash
export POSTGRES_URL='jdbc:postgresql://localhost:5432/polybank_db'
export POSTGRES_USER='polybank_user'
export POSTGRES_PASSWORD='polybank_password'

mvn liquibase:rollback -Dliquibase.rollbackCount=1
```


# Предварительная версия схемы БД =)
![drawSQL-image-export-2026-06-24.jpg](database.jpg)