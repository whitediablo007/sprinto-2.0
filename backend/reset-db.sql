-- Скрипт для пересоздания базы данных в dev окружении
-- Выполните этот скрипт от имени пользователя postgres или tms_user

-- Отключиться от tms_db_dev если подключены
\c postgres

-- Удалить базу данных если существует
DROP DATABASE IF EXISTS tms_db_dev;

-- Создать базу данных заново
CREATE DATABASE tms_db_dev;

-- Выдать права пользователю
GRANT ALL PRIVILEGES ON DATABASE tms_db_dev TO tms_user;

-- Подключиться к новой базе
\c tms_db_dev

-- Готово!
\echo 'База данных tms_db_dev успешно пересоздана!'
\echo 'Теперь запустите приложение: ./gradlew bootRun'

