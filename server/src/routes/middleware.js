const safeCompare = require('safe-compare');
// used to define any middlewares needed.
//

// Session validation middleware - used before any protected routes.
module.exports.authMiddleware = async (req, res, next) => {
    const auth_token= req.headers['x-bbb-auth'];
    if (!auth_token){
        res.status(403).json({
            error: "Not Authenticated.",
            status: 'failed'
        });
        return;
    }
    if (safeCompare(process.env.AUTH_TOKEN_SECRET, auth_token)) {
        next();
    } else {
        res.status(403).json({
            error: "Not Authenticated.",
            status: 'failed'
        });
        return;

    }
}

