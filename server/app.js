require('./src/utils/envVariables')();
const express = require('express');
const helmet = require('helmet')
const cookieParser = require('cookie-parser');
const bodyParser = require('body-parser')
const morgan = require('morgan');

const log = require('./src/log/logger')(__filename);
const db_init = require('./src/db/db');
const zangsh_db = require('./src/db/zangsh');
const middleware = require('./src/routes/middleware');

// we put clients here to shut them down on any of the SIGINTs and SIGTERM traps.
const {pgpool} = require('./src/db/db');
const app = express();
app.disable('x-powered-by');
if( ! process.env.DISABLE_LOG){
    app.use(morgan(`${process.pid} :remote-addr - :remote-user [:date[clf]] ":method :url HTTP/:http-version" :status :res[content-length] ":referrer" ":user-agent" :response-time ms`));
}
app.use(helmet()); // for strict HTTP header
app.use(bodyParser.json());       // to support JSON-encoded bodies
// handle JSON error parsing.
app.use((err, req, res, next) => {
    if (err) {
      res.status(400).json({error:'Invalid JSON body.'});
    } else {
      next();
    }
  });
app.use(cookieParser());
// server listening.

// health route before origin gchecking. 
app.get('/zangsh/health', (req, res) => res.status(200).end());


app.put('/zangsh/zpm',middleware.authMiddleware, async (req, res) =>{
    console.log('Successfully got body: ', req.body);
    if (! req.body.signature || !req.body.interval || !req.body.timestamp_start) {
        console.log('1')
        res.status(400).json({
            error: "invalid input.",
            status: "failed"
        })
        return;
    }
    if (! Array.isArray(req.body.zangsh_taps)) {
        console.log('2')
        res.status(400).json({
            error: "invalid input in zangsh_taps",
            status: "failed"
        })
        return;
    }

    try {
        req.body.zangsh_taps.forEach((zangsh_tap) => {
            if (!zangsh_tap.timestamp || !zangsh_tap.lat || !zangsh_tap.lng) {
                throw new Error("error")
            }
        })
    } catch(e){
        console.log('3')
        res.status(400).json({
            error: "invalid input in zangsh_taps' element.",
            status: "failed"
        })
        return;
    }

    const db_insert_success = await zangsh_db.insertZpmData(req.body);
    if (db_insert_success) {
        res.status(200).json({
            status:"success",
            error: null
        })
    } else {
        res.status(500).json({
            status:"failed",
            error: "Internal app error." 
        })
    }
})

app.options('*', (req,res)=>{
    res.end();
});


const port = process.env.PORT || 7200;
Promise.all(
    [
        db_init.initialize_tables()
    ]
).then(()=>{
    app.listen(port);
    log.info(`server running on port ${port}`);
}).catch((e) => {
    log.error(e);
    process.exit(1);
})
// trapping exit signals.
const shutDown = async (signal) => {
    log.warn(`signal ${signal} received, shutting down gracefully..`);
    try {
    // any cleanup you want to do before shutdown.
        await Promise.all([
            pgpool.end(),
        ])
        log.info("postgres db pool closed. ✅");
    }
    catch(e) {
        log.error("error caught in pre-shutdown procedure: ",e);
        log.error("Shutting down with exit code 1.. 👋");
        process.exit(1);
    }
    log.warn(`All pre-shutdown procedures successful. Bye... 👋`);
    process.exit(0);
}
process.on('SIGTERM', () => shutDown('SIGTERM'));
process.on('SIGINT', () => shutDown('SIGINT'));

module.exports = app;
