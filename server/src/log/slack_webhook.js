const { IncomingWebhook } = require('@slack/client');
let notify_slack = async (message ) => {
    console.log(`NOTIFY_SLACK (not sent): ${message}`);
    return;
}
let webhook;
let emoji = '';
// can only webhook to slack if not in devel AND has slack_webhook_url.
if (process.env.SLACK_WEBHOOK_URL && !(!process.env.SHMRK_ENV || process.env.SHMRK_ENV === "development" || process.env.SHMRK_ENV === 'test_e2e')) {
    webhook = new IncomingWebhook(process.env.SLACK_WEBHOOK_URL);
    switch(process.env.SHMRK_ENV){
        case 'staging': 
            emoji='🕵';
            break;
        case 'production':
            emoji='💂';
            break;
    }
    notify_slack = async (message) => webhook.send(`*${process.env.SHMRK_ENV} backend* ${emoji}: ${message}`);
}
module.exports.notify_slack = notify_slack;

// Send simple text to the webhook channel