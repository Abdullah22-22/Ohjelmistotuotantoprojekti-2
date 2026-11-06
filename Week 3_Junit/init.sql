DROP DATABASE IF EXISTS tempdb;
CREATE DATABASE tempdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tempdb;

CREATE TABLE temperature_units (
                                   unit_id   INT AUTO_INCREMENT PRIMARY KEY,
                                   unit_key  VARCHAR(20) NOT NULL UNIQUE,
                                   symbol    VARCHAR(5)  NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE unit_translation (
                                  unit_id     INT         NOT NULL,
                                  lang        VARCHAR(5)  NOT NULL,
                                  name        VARCHAR(50) NOT NULL,
                                  description VARCHAR(200),
                                  PRIMARY KEY (unit_id, lang),
                                  FOREIGN KEY (unit_id) REFERENCES temperature_units(unit_id) ON DELETE CASCADE
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE temp_records (
                              record_id       INT AUTO_INCREMENT PRIMARY KEY,
                              input_value     DOUBLE NOT NULL,
                              converted_value DOUBLE NOT NULL,
                              from_unit_id    INT,
                              to_unit_id      INT,
                              lang            VARCHAR(5) NOT NULL DEFAULT 'en',
                              created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              FOREIGN KEY (from_unit_id) REFERENCES temperature_units(unit_id),
                              FOREIGN KEY (to_unit_id)   REFERENCES temperature_units(unit_id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO temperature_units (unit_id, unit_key, symbol) VALUES
                                                              (1,'celsius','C'), (2,'fahrenheit','F'), (3,'kelvin','K');

INSERT INTO unit_translation (unit_id, lang, name, description) VALUES
                                                                    (1,'en','Celsius','Water freezes at 0 and boils at 100'),
                                                                    (1,'fi','Celsius','Vesi jäätyy 0:ssa ja kiehuu 100:ssa'),
                                                                    (1,'sv','Celsius','Vatten fryser vid 0 och kokar vid 100'),
                                                                    (1,'ja','摂氏','水は0度で凍り100度で沸騰する'),
                                                                    (1,'ar','سيليزيوس','يتجمد الماء عند 0 ويغلي عند 100'),
                                                                    (2,'en','Fahrenheit','Water freezes at 32 and boils at 212'),
                                                                    (2,'fi','Fahrenheit','Vesi jäätyy 32:ssa ja kiehuu 212:ssa'),
                                                                    (2,'sv','Fahrenheit','Vatten fryser vid 32 och kokar vid 212'),
                                                                    (2,'ja','華氏','水は32度で凍り212度で沸騰する'),
                                                                    (2,'ar','فهرنهايت','يتجمد الماء عند 32 ويغلي عند 212'),
                                                                    (3,'en','Kelvin','Absolute scale starting at absolute zero'),
                                                                    (3,'fi','Kelvin','Absoluuttinen asteikko alkaa absoluuttisesta nollasta'),
                                                                    (3,'sv','Kelvin','Absolut skala som börjar vid absoluta nollpunkten'),
                                                                    (3,'ja','ケルビン','絶対零度から始まる絶対温度'),
                                                                    (3,'ar','كلفن','مقياس مطلق يبدأ من الصفر المطلق');