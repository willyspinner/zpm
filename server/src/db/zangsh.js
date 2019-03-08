const {pgpool} = require('./db');
const log = require('../log/logger')(__filename);

module.exports.insertZpmData= async (zlj)=> {
    const {zangsh_taps, signature, interval, timestamp_start} = zlj;
    // no checking here. Assumed to be correct input.
    const zpm = Math.round(zangsh_taps.length * (60 / parseInt(interval)));
    const insert_zpm_table = 
    `
    INSERT INTO zpm(timestamp_start, zpm, interval, signature) 
        VALUES(to_timestamp($1), $2, $3, $4)
        ON CONFLICT(signature) DO NOTHING
        RETURNING zpm_id;
    `;
    const insert_zangsh_tap_table = `
        INSERT INTO zangsh_tap(timestamp, lat, lng, zpm_id)
        VALUES(to_timestamp($1), $2, $3, $4);
    `;
    const client = await pgpool.connect()
    let success = true;

    try {
      await client.query('BEGIN')
      const result = await client.query(insert_zpm_table, [timestamp_start, zpm, interval, signature]);
      if (result.rowCount === 0 ){
          throw new Error("insert into zpm table failed.");
      }
      const zpm_id = result.rows[0].zpm_id;
      const bulk_insert_results = await Promise.all(
          zangsh_taps.map((zangsh_tap)=> client.query(insert_zangsh_tap_table, [zangsh_tap.timestamp, zangsh_tap.lat, zangsh_tap.lng, zpm_id]))
      );
      const err_arrays = bulk_insert_results.filter((result) => result.rowCount === 0)
      if (err_arrays.length > 0) {
          throw new Error("bulk insert failed.");
      }
      await client.query('COMMIT')
    } catch (e) {
      await client.query('ROLLBACK')
      success = false;
      throw e
    } finally {
      client.release()
    }

    return success;
}

