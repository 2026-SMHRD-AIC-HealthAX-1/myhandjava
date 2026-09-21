// server/avatar/avatarItemRepository.js
// 상품 카탈로그 + 사용자 보유/착용 상태 저장소.
// 기본 구현은 인메모리이며, 같은 인터페이스로 DB(MySQL/PG/Mongo) 구현을 교체하면 됩니다.
'use strict';

const seed = require('../data/avatar-items.seed.json');
const { normalizeSlot } = require('./avatarSlots');

class InMemoryAvatarItemRepository {
  constructor() {
    this.items = new Map(seed.filter((i) => i.active).map((i) => [i.id, i]));
    // userId → { gender, coins, level, owned:Set<itemId>, equipped: { [slot]: itemId } }
    this.users = new Map();
  }

  // ---- catalog ----
  async listItems() { return [...this.items.values()]; }
  async getItem(id) { return this.items.get(id) || null; }

  // ---- user state ----
  async getUser(userId) {
    if (!this.users.has(userId)) {
      const owned = new Set(seed.filter((i) => i.defaultOwned).map((i) => i.id));
      this.users.set(userId, { userId, gender: 'male', coins: 1000, level: 1, owned, equipped: {} });
    }
    return this.users.get(userId);
  }

  async saveUser(user) { this.users.set(user.userId, user); return user; }

  // 저장된 equipped 맵의 구 슬롯 키(accessory 등)를 정규화
  async migrateEquipped(user) {
    const next = {};
    for (const [slot, itemId] of Object.entries(user.equipped || {})) {
      const item = this.items.get(itemId);
      const realSlot = item ? normalizeSlot(item.slot) : normalizeSlot(slot);
      next[realSlot] = itemId;
    }
    user.equipped = next;
    return user;
  }
}

module.exports = { InMemoryAvatarItemRepository };
