const fs = require('fs');
const log = require('../log/logger')(__filename, true);
const path = require('path')
/*
 *
 * This  laods env variables based on SHMRK_ENV, 
 * and checks if environment variables are defined properly or not. */

 module.exports = () =>{
   loadEnvVars();
   checkEnvVars();
 }
 loadEnvVars = () => {
   const environ = process.env.ENVIRON? process.env.ENVIRON: 'development';
   const envpath = path.resolve(__dirname, '..','..', `.${environ}.env`);
   log.info(`loading environment variables from: ${envpath}`);
   const stat_result = fs.statSync(envpath);
   stat_result.mode;
   const stringified_mode = (stat_result.mode & parseInt('777', 8)).toString(8);
   if(stringified_mode[1] !== '0' || stringified_mode[2] !== '0') {
     log.error(`${envpath} env file permissions are too open: ${stringified_mode}. Please restrict it to at least x00 where x is 4, 6 or 7.`)
     process.exit(1);
   }
   require('dotenv').config({path: envpath});
 }
checkEnvVars = ()=>{
  const env_variables = [
    "AUTH_TOKEN_SECRET",
    "PG_CONNSTR"
  ];
  const undefined_vars = env_variables.filter((variable_name) => !process.env[variable_name]);
  if (undefined_vars.length > 0) {
    throw new Error(`Environment Variables: ${undefined_vars} not defined. Please define them in the .env file.`)
  }
}
