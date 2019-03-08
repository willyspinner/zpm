
const {pgpool} = require('./db');

module.exports.genericSelect = async (query, params) => {
    const result = await pgpool.query(query, params);
    const rows = result.rows;
    if (rows && rows.length > 0) {
        return rows ; // array of rows returned. 
    } else{
        return null;
    }
}