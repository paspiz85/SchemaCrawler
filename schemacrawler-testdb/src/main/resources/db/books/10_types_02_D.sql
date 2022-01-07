-- Types
-- Informix syntax
CREATE DISTINCT TYPE NAME_TYPE AS VARCHAR(100);
CREATE DISTINCT TYPE AGE_TYPE AS INTEGER;

-- Table using types
CREATE TABLE Customers
(
  Id INTEGER NOT NULL,
  FirstName NAME_TYPE NOT NULL,
  LastName NAME_TYPE NOT NULL,
  Age AGE_TYPE,
  PRIMARY KEY (Id) CONSTRAINT PK_Customers
)
;
