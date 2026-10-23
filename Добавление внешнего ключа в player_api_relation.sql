-- Миграция для фичи интеграции с API Лиги тенниса (api_integration_participant).
-- player_api_relation.id хранит id игрока из нашей системы и ссылается на player.id:
-- связь не может указывать на несуществующего игрока. В коде FK объявлен через .references —
-- SchemaUtils.create создаст его только в новой БД, в существующей базе добавляется этим скриптом.

ALTER TABLE tennis_score_keeper.player_api_relation
    ADD CONSTRAINT player_api_relation_player_fk
    FOREIGN KEY (id) REFERENCES tennis_score_keeper.player (id);
