/*Упрощенный вариант струтуры таблицы БД*/
CREATE TABLE Simple_Details (
    Id                  INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT, 
    Id_ie               INT UNSIGNED,
    Name                VARCHAR (50),
    Quantity            SMALLINT UNSIGNED,
    Quantity_type       VARCHAR (10),
    Sizes               SMALLINT UNSIGNED,
    Sizes_type          VARCHAR (10),
    Input_date          TIMESTAMP DEFAULT NOW(),
    Change_date         TIMESTAMP,

    CONSTRAINT SimpleDetails_IncomeExpenses_fk
    FOREIGN KEY (Id_ie) REFERENCES Income_Expenses (Id) ON DELETE CASCADE
    );

/*Продвинутый вариант струтуры таблицы БД*/
CREATE TABLE Details (
    Id                  INT UNSIGNED NOT NULL PRIMARY KEY AUTO_INCREMENT, 
    Id_ie               INT UNSIGNED,
    Name                VARCHAR (50),
    Quantity            SMALLINT UNSIGNED,
    Quantity_type       INT UNSIGNED NULL,
    Sizes               SMALLINT UNSIGNED,
    Sizes_type          INT UNSIGNED NULL,
    Input_date          TIMESTAMP DEFAULT NOW(),
    Change_date         TIMESTAMP,

    CONSTRAINT Details_IncomeExpenses_fk
    FOREIGN KEY (Id_ie) REFERENCES Income_Expenses (Id) ON DELETE CASCADE,
    CONSTRAINT Details_Measures_fk1
    FOREIGN KEY (quantity_type) REFERENCES Measures (Id) ON DELETE SET NULL,
    CONSTRAINT Details_Measures_fk2
    FOREIGN KEY (sizes_type) REFERENCES Measures (Id) ON DELETE SET NULL
    );