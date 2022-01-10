CREATE TABLE Org_Person_Types (
    Id                      INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT, 
    Org_person_type_name    VARCHAR (15),
    Input_date              TIMESTAMP DEFAULT NOW(),
    Change_date             TIMESTAMP
    );

INSERT INTO Org_Person_Types (Org_person_type_name) VALUES ("Лицо");
INSERT INTO Org_Person_Types (Org_person_type_name) VALUES ("Организация");

SELECT * FROM Org_Person_Types;