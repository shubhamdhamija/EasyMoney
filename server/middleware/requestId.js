const {
  randomUUID
} = require('node:crypto');

function requestIdMiddleware(
  req,
  res,
  next
) {
  const suppliedRequestId =
    req.get('x-request-id');

  req.requestId =
    suppliedRequestId?.trim() ||
    randomUUID();

  res.set(
    'x-request-id',
    req.requestId
  );

  next();
}

module.exports = {
  requestIdMiddleware
};

