/*
 *  Use this to initialize the database and declare query methods.
 */

 const log = require('../log/logger')(__filename);
const {Pool} = require('pg');
const pgpool = new Pool (
    {
       connectionString: process.env.PG_CONNSTR
    }
)
pgpool.on('error', (err, client)=>{
  console.error("UNEXPECTED ERROR ON IDLE CLIENT", err);
  process.exit(1);
})


const initialize_tables = async() => {
  let connection;
  try {
    connection = await pgpool.connect();
  } catch(e){
    throw new Error(`ERROR conecting to pg pool. ${e}`);
  }
  try {
      await connection.query(
          `
      CREATE TABLE IF NOT EXISTS zpm (
        zpm_id BIGSERIAL NOT NULL,
        timestamp_start TIMESTAMP NOT NULL,
        zpm SMALLINT NOT NULL,
        interval INTEGER NOT NULL,
        signature TEXT UNIQUE,
        PRIMARY KEY (zpm_id)
      );
    `);
      await connection.query(
          `
      CREATE TABLE IF NOT EXISTS zangsh_tap (
        zangsh_tap_id BIGSERIAL NOT NULL,
        timestamp TIMESTAMP NOT NULL,
        lat DOUBLE PRECISION NOT NULL,
        lng DOUBLE PRECISION NOT NULL,
        zpm_id BIGSERIAL NOT NULL,
        FOREIGN KEY (zpm_id) REFERENCES zpm(zpm_id),
        PRIMARY KEY (zangsh_tap_id)
      );
    `);
    
  } finally{
      connection.release();
  }
  log.info("DB initialization successful.");
}
module.exports.initialize_tables = initialize_tables;
module.exports.pgpool = pgpool;


