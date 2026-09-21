// server/avatar/avatarSlots.js — 프론트 js/avatar-items.js 와 동일한 슬롯 정의 (단일 소스로 유지하세요)
'use strict';

const WEARABLE_SLOTS = ['socks', 'top', 'bottom', 'legwear', 'outer', 'shoes', 'hair', 'head', 'eyewear', 'bag', 'wrist', 'neck', 'hands'];
const ALL_SLOTS = [...WEARABLE_SLOTS, 'background'];

// 구 버전 저장 데이터 호환: accessory → wrist (메달은 카탈로그에서 neck 으로 재분류)
const SLOT_ALIASES = { accessory: 'wrist' };

function normalizeSlot(slot) {
  return SLOT_ALIASES[slot] || slot;
}

function isValidSlot(slot) {
  return ALL_SLOTS.includes(normalizeSlot(slot));
}

module.exports = { WEARABLE_SLOTS, ALL_SLOTS, SLOT_ALIASES, normalizeSlot, isValidSlot };
