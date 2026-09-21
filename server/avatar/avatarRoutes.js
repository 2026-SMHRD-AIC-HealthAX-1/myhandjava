// server/avatar/avatarRoutes.js — Express 라우터
//   const { InMemoryAvatarItemRepository } = require('./avatar/avatarItemRepository');
//   const { AvatarService } = require('./avatar/avatarService');
//   app.use('/api/avatar', createAvatarRouter(new AvatarService(new InMemoryAvatarItemRepository())));
'use strict';

const express = require('express');
const { AvatarError } = require('./avatarService');

// 인증 미들웨어에서 req.user.id 를 채운다고 가정. 없으면 헤더/쿼리로 대체(개발용).
function getUserId(req) {
  return (req.user && req.user.id) || req.get('x-user-id') || req.query.userId || 'demo';
}

function createAvatarRouter(service) {
  const router = express.Router();
  const wrap = (fn) => (req, res, next) => fn(req, res).catch(next);

  // 상점 목록 (owned/equipped 플래그 포함) — 프론트 state.js 가 AVATAR_ITEM_CATALOG 와 병합
  router.get('/items', wrap(async (req, res) => {
    res.json({ items: await service.getCatalogForUser(getUserId(req)) });
  }));

  // 사용자 아바타 상태
  router.get('/state', wrap(async (req, res) => {
    res.json(await service.getState(getUserId(req)));
  }));

  router.post('/purchase', wrap(async (req, res) => {
    const { itemId } = req.body || {};
    if (!itemId) throw new AvatarError(400, 'BAD_REQUEST', 'itemId 가 필요합니다.');
    res.json(await service.purchase(getUserId(req), itemId));
  }));

  router.post('/equip', wrap(async (req, res) => {
    const { itemId } = req.body || {};
    if (!itemId) throw new AvatarError(400, 'BAD_REQUEST', 'itemId 가 필요합니다.');
    res.json(await service.equip(getUserId(req), itemId));
  }));

  router.post('/unequip', wrap(async (req, res) => {
    const { itemId } = req.body || {};
    if (!itemId) throw new AvatarError(400, 'BAD_REQUEST', 'itemId 가 필요합니다.');
    res.json(await service.unequip(getUserId(req), itemId));
  }));

  router.post('/gender', wrap(async (req, res) => {
    res.json(await service.setGender(getUserId(req), (req.body || {}).gender));
  }));

  // 에러 → JSON
  router.use((err, req, res, next) => { // eslint-disable-line no-unused-vars
    if (err instanceof AvatarError) return res.status(err.status).json({ error: err.code, message: err.message });
    console.error(err);
    return res.status(500).json({ error: 'INTERNAL', message: '서버 오류' });
  });

  return router;
}

module.exports = { createAvatarRouter };
