CREATE CONVERSION myconv FOR 'LATIN1' TO 'UTF8' FROM iso8859_1_to_utf8;
CREATE DEFAULT CONVERSION public.mydef2 FOR 'LATIN1' TO 'UTF8' FROM iso8859_1_to_utf8;
DROP CONVERSION myconv;
DROP CONVERSION mydef;
CREATE CONVERSION alt_conv1 FOR 'LATIN1' TO 'UTF8' FROM iso8859_1_to_utf8;
ALTER CONVERSION alt_conv1 RENAME TO alt_conv3;  -- OK
ALTER CONVERSION alt_conv2 OWNER TO regress_alter_generic_user3;  -- OK
ALTER CONVERSION alt_conv2 SET SCHEMA alt_nsp2;  -- OK
COMMENT ON CONVERSION myconv_bad IS 'foo';
COMMENT ON CONVERSION myconv IS NULL;