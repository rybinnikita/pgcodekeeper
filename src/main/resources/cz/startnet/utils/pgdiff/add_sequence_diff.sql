
CREATE SEQUENCE testseq
	START WITH 1
	INCREMENT BY 1
	NO MAXVALUE
	NO MINVALUE
	CACHE 1;

ALTER SEQUENCE testseq OWNER TO fordfrog;
