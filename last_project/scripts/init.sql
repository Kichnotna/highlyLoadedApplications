-- Создание основной таблицы логов
CREATE TABLE access_logs
(
    id          SERIAL,
    visit_time  TIMESTAMP NOT NULL,
    user_id     INT       NOT NULL,
    page_url    TEXT      NOT NULL,
    status_code INT,
    PRIMARY KEY (id, visit_time)
) PARTITION BY RANGE (visit_time);

-- Партиции по месяцам
CREATE TABLE access_logs_2025_01 PARTITION OF access_logs
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE access_logs_2025_02 PARTITION OF access_logs
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE access_logs_2025_03 PARTITION OF access_logs
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

-- Индексы на партиции
CREATE INDEX idx_user_2025_01 ON access_logs_2025_01 (user_id);
CREATE INDEX idx_time_2025_01 ON access_logs_2025_01 (visit_time);

CREATE INDEX idx_user_2025_02 ON access_logs_2025_02 (user_id);
CREATE INDEX idx_time_2025_02 ON access_logs_2025_02 (visit_time);

-- Глобальный индекс
CREATE INDEX idx_user_time_global ON access_logs (user_id, visit_time);

-- Демо-данные
INSERT INTO access_logs (visit_time, user_id, page_url, status_code)
VALUES ('2025-01-10 12:00:00', 101, '/home', 200),
       ('2025-01-15 08:45:00', 102, '/about', 200),
       ('2025-02-14 09:30:00', 103, '/login', 403),
       ('2025-02-20 17:20:00', 101, '/profile', 200);


-- Массовая вставка 100 000 записей: с января по март 2025
INSERT INTO access_logs (visit_time, user_id, page_url, status_code)
SELECT timestamp '2025-01-01 00:00:00' + (random() * interval '90 days'),
       (random() * 1000)::int + 1,
       '/page/' || (trunc(random() * 50)::int + 1),
       (ARRAY [200, 301, 403, 404, 500])[floor(random() * 5 + 1)]
FROM generate_series(1, 100000);