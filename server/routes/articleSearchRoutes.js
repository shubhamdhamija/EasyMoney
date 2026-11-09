const express = require('express');
const { ArticleSearchError } = require('../services/articleSearchService');

function createArticleSearchRouter({ articleSearchService }) {
  const router = express.Router();

  router.get('/search', async (req, res, next) => {
    try {
      const result = await articleSearchService.searchArticles(req.query.q);
      return res.json({ ok: true, ...result });
    } catch (error) {
      if (error instanceof ArticleSearchError) {
        return res.status(error.statusCode).json({
          ok: false,
          error: { code: error.code, message: error.message },
          requestId: req.requestId || null
        });
      }
      return next(error);
    }
  });

  return router;
}

module.exports = { createArticleSearchRouter };
