--
-- PostgreSQL database dump
--

SET client_encoding = 'UTF8';
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: another_triggers; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA another_triggers;


ALTER SCHEMA another_triggers OWNER TO postgres;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

--CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

--COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


SET search_path = pg_catalog;

--
-- Name: test_table; Type: TABLE; Schema: another_triggers; Owner: postgres; Tablespace: 
--

CREATE TABLE another_triggers.test_table_a (
    id integer NOT NULL
);


ALTER TABLE another_triggers.test_table_a OWNER TO postgres;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

