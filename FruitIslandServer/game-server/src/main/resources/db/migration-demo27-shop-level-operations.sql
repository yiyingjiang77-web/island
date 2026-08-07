-- MySQL 8 default name for the first anonymous CHECK on drink_bar_batch.
-- Verify the constraint name with SHOW CREATE TABLE when upgrading a database
-- whose schema was customized.
ALTER TABLE drink_bar_batch DROP CHECK drink_bar_batch_chk_1;
ALTER TABLE drink_bar_batch
    ADD CONSTRAINT chk_drink_bar_batch_quantity
    CHECK (listed_quantity BETWEEN 1 AND 20);
