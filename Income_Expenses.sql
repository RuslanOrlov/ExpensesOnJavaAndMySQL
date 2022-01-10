CREATE TABLE Income_Expenses (
    Id                  INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT, 
    Date_operation      TIMESTAMP,
    Sum_operation       DECIMAL(10,2),
    Id_operation_type   INT UNSIGNED,
    Operation_name      VARCHAR (50),
    Id_org_person_type  INT UNSIGNED,
    Org_person_name     VARCHAR (50),
    Description         VARCHAR (1000),
    Input_date          TIMESTAMP DEFAULT NOW(),
    Change_date         TIMESTAMP,

    CONSTRAINT IncomeExpenses_OperationTypes_fk
    FOREIGN KEY (Id_operation_type) REFERENCES Operation_Types (Id) ON DELETE RESTRICT,
    CONSTRAINT IncomeExpenses_OrgPersonTypes_fk
    FOREIGN KEY (Id_org_person_type) REFERENCES Org_Person_Types (Id) ON DELETE RESTRICT
    );