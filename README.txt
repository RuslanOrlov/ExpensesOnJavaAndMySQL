ENG: Application for accounting expenses/incomes/assets in the household 
     (the repository is based on the MySQL database). 
RU:  Приложение по учету расходов/доходов/активов в домохозяйстве 
     (хранилище основано на БД MySQL).

1. MainClass.java       - the main application                               / - основное приложение
   
                          MainClass (2021-12-30 18-06 CRS).java                - the version where data access is organized via CachedRowSet 
                                                                                 (версия, где доступ к данным организуется через CachedRowSet)
                          MainClass (2021-12-30 18-15 ResultSet).java          - the version where data access is organized via ResultSet
                                                                                 (версия, где доступ к данным организуется через ResultSet)

2. Income_Expenses.sql  - script for creating the main database table        / - скрипт создания основной таблицы БД
3. Operation_Types.sql  - script for creating a table of operations types    / - скрипт создания таблицы типов операций
4. Org_Person_Types.sql - script for creating a table of correspondent types / - скрипт создания таблицы типов корреспондентов
5. Details.sql          - script for creating a table of details             / - скрипт создания таблицы подробностей
                          about expenses and assets                          /   о расходах и активах

6. database.properties  - the settings file for connecting to the database   / - файл настроек для подключения к БД