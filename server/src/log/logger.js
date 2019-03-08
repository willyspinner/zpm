const { createLogger, format, transports } = require('winston');
const path = require('path');

const getShortPath = (filepath) => {
  const splitted = filepath.split(path.sep);
  if (splitted[splitted.length -1] === "app.js")
    return "app.js";
  return splitted.slice(-3).join(path.sep);
};

const real_logger= (fp, init) => {
  let env;
  switch (process.env.SHMRK_ENV){
    case undefined:
    case 'development':
    case 'devel':
      env = "dev";
      break;
    case 'staging':
      env ='stag';
      break;
    case 'production':
      env ='prod';
      break;
    default:
      env = process.env.SHMRK_ENV.substr(0,4);
  }
  if (init)
    env = '';
  const shortPath =  getShortPath(fp);
  return createLogger({
    level: 'debug',
    format: format.combine(
      // Use this function to create a label for additional text to display
      format.label({label: shortPath}),
      format.colorize(),
      format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
      format.printf(
        // We display the label text between square brackets using ${info.label} on the next line
        info => `${info.timestamp} ${env} ${process.pid} ${info.level} [${info.label}]: ${info.message}`
      )
    ),
    transports: [new transports.Console()]
  });
}

  if(process.env.DISABLE_LOG){
    module.exports= (func)=>({
      info: ()=>{},
      error: ()=>{},
      warn: ()=>{},
      debug: ()=>{}
    })
  }else {
    module.exports = real_logger;
  }