SELECT
  CONSTRAINTS.CONSTRAINT_SCHEMA AS CONSTRAINT_CATALOG,
  NULL AS CONSTRAINT_SCHEMA,
  CONSTRAINTS.CONSTRAINT_NAME,
  CONSTRAINTS.TABLE_SCHEMA AS TABLE_CATALOG,
  NULL AS TABLE_SCHEMA,
  CONSTRAINTS.TABLE_NAME,
  COLUMNS.COLUMN_NAME,
  COLUMNS.ORDINAL_POSITION,
  CONSTRAINTS.CONSTRAINT_TYPE,
  CONSTRAINTS.ENFORCED
FROM
  INFORMATION_SCHEMA.TABLE_CONSTRAINTS CONSTRAINTS
  INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE COLUMNS
    ON
    COLUMNS.CONSTRAINT_CATALOG = CONSTRAINTS.CONSTRAINT_CATALOG
    AND COLUMNS.CONSTRAINT_SCHEMA = CONSTRAINTS.CONSTRAINT_SCHEMA
    AND COLUMNS.CONSTRAINT_NAME = CONSTRAINTS.CONSTRAINT_NAME
    AND COLUMNS.TABLE_SCHEMA = CONSTRAINTS.TABLE_SCHEMA
    AND COLUMNS.TABLE_NAME = CONSTRAINTS.TABLE_NAME
