-- Миграция для фичи блокировки redo при смене подачи (feature/serve_in_pair_per_set).
-- Колонка границы redo очереди подающих: номер строки, до которой redo доступен включительно, -
-- строки перед первой проекцией игрока пары в сете. SchemaUtils.create добавляет колонку
-- только при создании таблицы, не в существующую - существующие записи очереди сохраняются,
-- их границы заполнятся новыми строками по ходу матча.

ALTER TABLE tennis_score_keeper.doubles_match_first_serve_player
    ADD COLUMN IF NOT EXISTS point_number_redo_limit integer;
