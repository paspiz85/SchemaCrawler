-- SchemaCrawler
-- http://www.schemacrawler.com
-- Copyright (c) 2000-2025, Sualeh Fatehi <sualeh@hotmail.com>.
-- All rights reserved.
-- SPDX-License-Identifier: EPL-2.0

-- Types
CREATE TYPE NAME_TYPE FROM VARCHAR(100);
CREATE TYPE AGE_TYPE FROM SMALLINT;

-- Table using types
CREATE TABLE Customers
(
  Id INTEGER NOT NULL,
  FirstName NAME_TYPE NOT NULL,
  LastName NAME_TYPE NOT NULL,
  Age AGE_TYPE,
  CONSTRAINT PK_Customers PRIMARY KEY (Id)
)
;
