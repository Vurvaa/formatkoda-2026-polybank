Откат миграции (делать только локально, чтобы не было конфликтов)

```bash
export POSTGRES_URL='jdbc:postgresql://localhost:5432/polybank_db'
export POSTGRES_USER='polybank_user'
export POSTGRES_PASSWORD='polybank_password'

mvn liquibase:rollback -Dliquibase.rollbackCount=1
```