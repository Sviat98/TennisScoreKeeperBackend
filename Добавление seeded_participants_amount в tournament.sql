-- Миграция для фичи seeded_participants_amount (ветка feature/seeded_participants_amount).
-- В таблицу tournament добавляется колонка seeded_participants_amount — количество сеяных участников турнира.
-- SchemaUtils.create добавляет колонку только при создании таблицы, не в существующую,
-- поэтому для уже существующей БД колонку нужно добавить вручную.

ALTER TABLE tennis_score_keeper.tournament
    ADD COLUMN IF NOT EXISTS seeded_participants_amount integer NOT NULL DEFAULT 0;

-- Для уже созданных турниров колонка инициализируется максимальным номером посева
-- участника в рамках турнира (0, если посева нет). Турнир хранит участников только
-- в одной из двух таблиц (одиночный или парный), поэтому GREATEST безопасен.

UPDATE tennis_score_keeper.tournament t
SET seeded_participants_amount = GREATEST(
    COALESCE((SELECT MAX(seed) FROM tennis_score_keeper.singles_participant sp WHERE sp.tournament_id = t.id), 0),
    COALESCE((SELECT MAX(seed) FROM tennis_score_keeper.doubles_participant dp WHERE dp.tournament_id = t.id), 0)
);
