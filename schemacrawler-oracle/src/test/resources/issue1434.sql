CREATE TABLE COMMUNICATION
(
  ID NUMBER(11) NOT NULL,
  COMMUNICATION_CODE VARCHAR2(3) NOT NULL,
  DESCRIPTION VARCHAR2(100) NOT NULL
);

ALTER TABLE COMMUNICATION
ADD CONSTRAINT COMMUNICATION_UQ
  UNIQUE (COMMUNICATION_CODE);

CREATE TABLE CHANNEL
(
  ID NUMBER(11) NOT NULL,
  CHANNEL_CODE VARCHAR2(2) NOT NULL,
  DESCRIPTION VARCHAR2(20),
  COMMUNICATION_CODE_FK VARCHAR2(3) NOT NULL
);

ALTER TABLE CHANNEL
ADD CONSTRAINT CHANNEL_FK1
  FOREIGN KEY (COMMUNICATION_CODE_FK)
  REFERENCES COMMUNICATION (COMMUNICATION_CODE);