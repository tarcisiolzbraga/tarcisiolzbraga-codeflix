-- A desativação passou a ser registrada só na coluna active; deleted_at não tem mais significado.
ALTER TABLE category DROP COLUMN deleted_at;

ALTER TABLE category_aud DROP COLUMN deleted_at;
