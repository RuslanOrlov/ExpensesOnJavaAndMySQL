CREATE TABLE Operation_Types (
    Id                  INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT, 
    Operation_type_name VARCHAR (15),
    Input_date          TIMESTAMP DEFAULT NOW(),
    Change_date         TIMESTAMP
    );

INSERT INTO Operation_Types (Operation_type_name) VALUES ("Доходы");
INSERT INTO Operation_Types (Operation_type_name) VALUES ("Расходы");
INSERT INTO Operation_Types (Operation_type_name) VALUES ("Активы");

SELECT * FROM Operation_Types;