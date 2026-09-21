// server/avatar/avatarService.js — 구매 / 착용 / 해제 비즈니스 규칙
'use strict';

const { WEARABLE_SLOTS, normalizeSlot } = require('./avatarSlots');

class AvatarError extends Error {
  constructor(status, code, message) { super(message); this.status = status; this.code = code; }
}

class AvatarService {
  constructor(repo) { this.repo = repo; }

  // 프론트 state.js 가 기대하는 형태(owned/equipped 플래그 포함)로 카탈로그 반환
  async getCatalogForUser(userId) {
    const user = await this.repo.migrateEquipped(await this.repo.getUser(userId));
    const items = await this.repo.listItems();
    const equippedIds = new Set(Object.values(user.equipped));
    return items
      .filter((i) => i.genders.includes(user.gender))
      .map((i) => ({
        id: i.id, name: i.name, price: i.price, slot: i.slot, category: i.category, levelReq: i.levelReq,
        effect: i.effect, effectDesc: i.effectDesc, asset: i.asset, layerAssets: i.layerAssets, genders: i.genders,
        owned: user.owned.has(i.id), equipped: equippedIds.has(i.id),
      }));
  }

  async getState(userId) {
    const user = await this.repo.migrateEquipped(await this.repo.getUser(userId));
    return { userId, gender: user.gender, coins: user.coins, level: user.level, owned: [...user.owned], equipped: user.equipped };
  }

  async purchase(userId, itemId) {
    const [user, item] = await Promise.all([this.repo.getUser(userId), this.repo.getItem(itemId)]);
    if (!item) throw new AvatarError(404, 'ITEM_NOT_FOUND', '존재하지 않는 상품입니다.');
    if (user.owned.has(itemId)) throw new AvatarError(409, 'ALREADY_OWNED', '이미 보유한 상품입니다.');
    if (!item.genders.includes(user.gender)) throw new AvatarError(400, 'GENDER_MISMATCH', '현재 캐릭터에 착용할 수 없는 상품입니다.');
    if (user.level < item.levelReq) throw new AvatarError(403, 'LEVEL_TOO_LOW', `레벨 ${item.levelReq} 이상부터 구매할 수 있습니다.`);
    if (user.coins < item.price) throw new AvatarError(402, 'NOT_ENOUGH_COINS', '코인이 부족합니다.');
    user.coins -= item.price;
    user.owned.add(itemId);
    await this.repo.saveUser(user);
    return this.getState(userId);
  }

  // 같은 슬롯 배타 착용: 해당 슬롯에 이미 있는 아이템은 자동 해제
  async equip(userId, itemId) {
    const [user, item] = await Promise.all([this.repo.getUser(userId), this.repo.getItem(itemId)]);
    if (!item) throw new AvatarError(404, 'ITEM_NOT_FOUND', '존재하지 않는 상품입니다.');
    if (!user.owned.has(itemId)) throw new AvatarError(403, 'NOT_OWNED', '보유하지 않은 상품입니다.');
    if (!item.genders.includes(user.gender)) throw new AvatarError(400, 'GENDER_MISMATCH', '현재 캐릭터에 착용할 수 없는 상품입니다.');
    const slot = normalizeSlot(item.slot);
    if (!WEARABLE_SLOTS.includes(slot) && slot !== 'background') throw new AvatarError(400, 'INVALID_SLOT', '착용할 수 없는 슬롯입니다.');
    await this.repo.migrateEquipped(user);
    user.equipped[slot] = itemId;
    await this.repo.saveUser(user);
    return this.getState(userId);
  }

  async unequip(userId, itemId) {
    const user = await this.repo.migrateEquipped(await this.repo.getUser(userId));
    for (const [slot, id] of Object.entries(user.equipped)) if (id === itemId) delete user.equipped[slot];
    await this.repo.saveUser(user);
    return this.getState(userId);
  }

  async setGender(userId, gender) {
    const g = gender === 'female' ? 'female' : 'male';
    const user = await this.repo.migrateEquipped(await this.repo.getUser(userId));
    user.gender = g;
    // 성별 전용 아이템(예: 스크런치)은 성별 변경 시 자동 해제
    for (const [slot, id] of Object.entries(user.equipped)) {
      const item = await this.repo.getItem(id);
      if (item && !item.genders.includes(g)) delete user.equipped[slot];
    }
    await this.repo.saveUser(user);
    return this.getState(userId);
  }
}

module.exports = { AvatarService, AvatarError };
