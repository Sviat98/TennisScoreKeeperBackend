-- Миграция для фичи интеграции с API Лиги тенниса (api_integration_tournament).
-- tournament_api_relation.id хранит id турнира из нашей системы и ссылается на tournament.id:
-- связь не может указывать на несуществующий турнир. В коде FK объявлен через .references —
-- SchemaUtils.create создаст его только в новой БД, в существующей базе добавляется этим скриптом.

ALTER TABLE tennis_score_keeper.tournament_api_relation
    ADD CONSTRAINT tournament_api_relation_tournament_fk
    FOREIGN KEY (id) REFERENCES tennis_score_keeper.tournament (id);
