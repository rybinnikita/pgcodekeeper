CREATE OR REPLACE FUNCTION public.f(s integer, k integer = 43) RETURNS integer
    LANGUAGE plpgsql
    AS $$ SELECT 1111; $$;

ALTER FUNCTION public.f(s integer, k integer) OWNER TO shamsutdinov_lr;

CREATE FUNCTION public.f(s integer, k double precision = 5.1) RETURNS integer
    LANGUAGE plpgsql
    AS $$ SELECT 1111; $$;

ALTER FUNCTION public.f(s integer, k double precision) OWNER TO shamsutdinov_lr;

CREATE TABLE public.tbl (
    c1 integer DEFAULT public.f(1),
    c2 integer
);

ALTER TABLE public.tbl OWNER TO shamsutdinov_lr;

COMMENT ON COLUMN public.tbl.c1 IS 'some comment';
