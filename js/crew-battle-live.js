// crew-battle-live.js
// 크루대전 실시간 WebSocket 연결과 운동 판정 전송을 담당합니다.

let crewBattleStompClient = null;
let crewBattleSubscription = null;

function connectCrewBattleSocket(battleId) {
  if (!state.token || !battleId) {
    console.error('크루대전 소켓 연결 정보가 없습니다.');
    return;
  }

  disconnectCrewBattleSocket();

  const socket = new SockJS(`${API_BASE}/ws`);

  crewBattleStompClient = Stomp.over(socket);
  crewBattleStompClient.debug = null;

  crewBattleStompClient.connect(
    {
      Authorization: 'Bearer ' + state.token
    },

    function () {
      crewBattleSubscription =
        crewBattleStompClient.subscribe(
          `/topic/crew-battles/${battleId}`,

          function (frame) {
            const battleEvent = JSON.parse(frame.body);
            handleCrewBattleEvent(battleEvent);
          }
        );

      console.log('크루대전 실시간 연결 완료:', battleId);
    },

    function (error) {
      console.error('크루대전 실시간 연결 실패:', error);
      toast('크루대전 실시간 연결에 실패했습니다.');
    }
  );
}

function disconnectCrewBattleSocket() {
  if (crewBattleSubscription) {
    crewBattleSubscription.unsubscribe();
    crewBattleSubscription = null;
  }

  if (crewBattleStompClient && crewBattleStompClient.connected) {
    crewBattleStompClient.disconnect();
  }

  crewBattleStompClient = null;
}

function sendCrewBattleRep(grade) {
  const battle = state.crewBattle;

if (!battle ||
    battle.status !== 'ACTIVE' ||
    battle.result) {

  console.error('현재 진행 중인 크루대전이 없습니다.');
  return;
}

  // 데모(가짜 상대) 대전은 서버 없이 로컬에서만 판정을 반영한다.
  if (battle.demo) {
    const me = battle.participants.find(
      p => Number(p.userId) === Number(state.user.id)
    );

    if (me) {
      applyCrewBattleDemoGrade(me, grade);
    }

    return;
  }

  if (!crewBattleStompClient || !crewBattleStompClient.connected) {
    toast('크루대전 서버 연결 중입니다. 잠시 후 다시 시도해주세요.');
    return;
  }

  crewBattleStompClient.send(
    '/app/crew-battles/reps',
    {},
    JSON.stringify({
      battleId: battle.id,
      grade: grade
    })
  );
}

function handleCrewBattleEvent(battleEvent) {
  const battle = state.crewBattle;

  if (!battle) {
    return;
  }

  if (Number(battle.id) !== Number(battleEvent.battleId)) {
    return;
  }

  const participant = (battle.participants || []).find(
    member =>
      Number(member.userId) === Number(battleEvent.userId)
  );

  if (!participant) {
    console.warn(
      '크루대전 참가자를 화면에서 찾지 못했습니다.',
      battleEvent
    );

    return;
  }

  const grade = battleEvent.grade;

  // 서버가 보내준 개인 유효 횟수로 변경
  participant.score = Number(
    battleEvent.personalCount || 0
  );

  participant.latestGrade = grade;

  // 개인별 판정 횟수 갱신
  if (!participant.gradeCounts) {
    participant.gradeCounts = {
      PERFECT: 0,
      GREAT: 0,
      GOOD: 0,
      MISS: 0
    };
  }

  if (grade && participant.gradeCounts[grade] !== undefined) {
    participant.gradeCounts[grade] += 1;
  }

  const isMe =
    Number(participant.userId) === Number(state.user.id);

  const isMyCrew =
    Number(participant.crewId) === Number(state.crew.id);

  if (isMe) {
    battle.myScore = participant.score;
    battle.myGradeCounts = participant.gradeCounts;
  }

  if (isMyCrew) {
    battle.myCrewScore = Number(
      battleEvent.crewScore || 0
    );
  } else {
    battle.oppScore = Number(
      battleEvent.crewScore || 0
    );
  }

  // 기존 점수 표시 부분도 서버 데이터로 갱신
  updateBattleUI(null, 0);

  // 해당 캐릭터 위에 판정 문구 표시
  showCrewBattleGrade(
    participant.userId,
    grade,
    battleEvent.counted
  );

  console.log('크루대전 운동 판정 반영:', battleEvent);
}


function showCrewBattleGrade(userId, grade, counted) {
  const participantElement = document.querySelector(
    `[data-battle-user-id="${userId}"]`
  );

  if (!participantElement) {
    return;
  }

  const gradeElement = participantElement.querySelector(
    '.battle-grade-label'
  );

  if (gradeElement) {
    gradeElement.textContent = grade;
    gradeElement.style.color = gradeColor(grade);

    gradeElement.classList.remove('show');

    // 같은 판정이 연속으로 발생해도 애니메이션 재실행
    void gradeElement.offsetWidth;

    gradeElement.classList.add('show');

    clearTimeout(gradeElement._hideTimer);

    gradeElement._hideTimer = setTimeout(() => {
      gradeElement.classList.remove('show');
    }, 900);
  }

  // MISS는 횟수가 올라가지 않으므로 스쿼트 동작을 실행하지 않음
  if (!counted) {
    return;
  }

  const characterElement = participantElement.querySelector(
    '.battle-character'
  );

  if (!characterElement) {
    return;
  }

  characterElement.classList.remove('show-squat');

  // 연속으로 운동해도 애니메이션 재실행
  void characterElement.offsetWidth;

  characterElement.classList.add('show-squat');

  clearTimeout(characterElement._squatTimer);

  characterElement._squatTimer = setTimeout(() => {
    characterElement.classList.remove('show-squat');
  }, 650);
}

async function loadCrewBattleParticipants(battleId) {
  if (!state.token || !battleId) {
    toast('크루대전 참가자 정보를 불러올 수 없습니다.');
    return false;
  }

  try {
    const response = await fetch(
      `${API_BASE}/api/crew-battles/${battleId}/participants`,
      {
        headers: {
          Authorization: 'Bearer ' + state.token
        }
      }
    );

    const body = await response.json();

    if (!response.ok || !body.success) {
      toast(body.message || '크루대전 참가자 조회에 실패했습니다.');
      return false;
    }

    const participants = (body.data || []).map((participant, index) => ({
      userId: Number(participant.userId),
      n: participant.nickname,
      crewId: Number(participant.crewId),
      crewName: participant.crewName,
      score: participant.personalCount || 0,
      latestGrade: participant.latestGrade || null,

      gradeCounts: {
        PERFECT: participant.perfectCount || 0,
        GREAT: participant.greatCount || 0,
        GOOD: participant.goodCount || 0,
        MISS: participant.missCount || 0
      },

      // 본인은 실제 성별을 사용하고 다른 참가자는 임시 표시합니다.
      // 추후 백엔드 응답에 성별과 착용 아이템을 추가할 수 있습니다.
      gender:
        Number(participant.userId) === Number(state.user.id)
          ? state.user.gender || 'male'
          : index % 2 === 0
            ? 'male'
            : 'female'
    }));

    const myCrewId = Number(state.crew.id);

    const myCrewParticipants = participants.filter(
      participant => participant.crewId === myCrewId
    );

    const opponentParticipants = participants.filter(
      participant => participant.crewId !== myCrewId
    );

    if (myCrewParticipants.length === 0 ||
        myCrewParticipants.length !== opponentParticipants.length) {
      toast(
        `참가 인원이 맞지 않습니다. ` +
        `우리 크루 ${myCrewParticipants.length}명, ` +
        `상대 크루 ${opponentParticipants.length}명`
      );

      return false;
    }

    const me = myCrewParticipants.find(
      participant =>
        participant.userId === Number(state.user.id)
    );

    if (!me) {
      toast('현재 사용자는 크루대전 참가자가 아닙니다.');
      return false;
    }

    state.crewBattle.participants = participants;
    state.crewBattle.myParticipants = myCrewParticipants;
    state.crewBattle.opponentParticipants = opponentParticipants;

    // 기존 크루대전 화면 구조와 연결
    state.crewBattle.myScore = me.score;
    state.crewBattle.myGradeCounts = me.gradeCounts;

    state.crewBattle.teammates = myCrewParticipants.filter(
      participant => participant.userId !== Number(state.user.id)
    );

    state.crewBattle.oppTeammates = opponentParticipants;

    state.crewBattle.oppScore = opponentParticipants.reduce(
      (total, participant) => total + participant.score,
      0
    );

    if (opponentParticipants.length > 0) {
      state.crewBattle.opponent = {
        ...(state.crewBattle.opponent || {}),
        name: opponentParticipants[0].crewName
      };
    }

    console.log('크루대전 참가자 조회 완료:', participants);

    return true;

  } catch (error) {
    console.error('크루대전 참가자 조회 오류:', error);
    toast('서버에서 참가자 정보를 불러오지 못했습니다.');

    return false;
  }
}

async function loadMyCrewBattles() {
  if (!state.token) {
    return [];
  }

  try {
    const response = await fetch(
      `${API_BASE}/api/crew-battles/me`,
      {
        headers: {
          Authorization: 'Bearer ' + state.token
        }
      }
    );

    const body = await response.json();

    if (!response.ok || !body.success) {
      console.error('크루대전 목록 조회 실패:', body.message);
      return [];
    }

    state.crewBattles = body.data || [];

    loadFinishedCrewBattleResults();

    return state.crewBattles;

  } catch (error) {
    console.error('크루대전 목록 조회 오류:', error);
    return [];
  }
}

// 종료된 대전은 목록 응답(Response)에 EXP·참가자별 정확도가 안 들어있어서, 상세 결과
// (GET /{id}/result)를 따로 불러와 캐시해야 대전 결과 카드에 EXP와 "자세히보기"를 보여줄 수 있다.
// 이미 캐시된 battleId는 다시 부르지 않는다.
async function loadFinishedCrewBattleResults() {
  const toLoad = (state.crewBattles || [])
    .filter(b => b.status === 'FINISHED' && !state.crewBattleResults[b.id]);

  if (!toLoad.length) {
    return;
  }

  await Promise.all(
    toLoad.map(b => loadCrewBattleResultDetail(b.id))
  );

  render();
}

async function loadCrewBattleResultDetail(battleId) {
  if (!state.token || !battleId) {
    return null;
  }

  try {
    const response = await fetch(
      `${API_BASE}/api/crew-battles/${battleId}/result`,
      {
        headers: {
          Authorization: 'Bearer ' + state.token
        }
      }
    );

    const body = await response.json();

    if (!response.ok || !body.success) {
      console.error('크루대전 결과 상세 조회 실패:', body.message);
      return null;
    }

    state.crewBattleResults[battleId] = body.data;

    return body.data;

  } catch (error) {
    console.error('크루대전 결과 상세 조회 오류:', error);
    return null;
  }
}


// 백엔드가 상대 지정 방식에서 자동 매칭 방식으로 바뀌면서, 신청 시 상대를 직접 고르지 않고
// {teamSize, participantUserIds, exerciseType}만 보낸다 — 같은 조건으로 대기 중인 크루가 있으면
// 서버가 즉시 매칭해서 ACTIVE로, 없으면 WAITING으로 돌려준다. durationMinutes은 서버가 2분 고정.
async function requestCrewBattle(teamSize, participantUserIds, exerciseType) {
  if (!state.token) {
    toast('로그인이 필요합니다.');
    return null;
  }

  try {
    const response = await fetch(
      `${API_BASE}/api/crew-battles`,
      {
        method: 'POST',

        headers: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer ' + state.token
        },

        body: JSON.stringify({
          teamSize: Number(teamSize),
          participantUserIds: participantUserIds.map(Number),
          exerciseType: exerciseType || 'squat'
        })
      }
    );

    const body = await response.json();

    if (!response.ok || !body.success) {
      toast(body.message || '크루대전 신청에 실패했습니다.');
      return null;
    }

    await loadMyCrewBattles();

    toast(
      body.data.status === 'WAITING'
        ? '매칭 상대를 찾고 있습니다.'
        : '상대 크루와 매칭되어 대전이 시작됩니다.'
    );

    return body.data;

  } catch (error) {
    console.error('크루대전 신청 오류:', error);
    toast('크루대전 서버에 연결할 수 없습니다.');

    return null;
  }
}


// 자동 매칭 대기(WAITING) 취소 — 신청한 본인만 취소할 수 있다(백엔드 CrewBattleService.cancelMatching).
async function cancelMatchedCrewBattle(battleId) {
  if (!state.token || !battleId) {
    return null;
  }

  try {
    const response = await fetch(
      `${API_BASE}/api/crew-battles/${battleId}/matching`,
      {
        method: 'DELETE',

        headers: {
          Authorization: 'Bearer ' + state.token
        }
      }
    );

    const body = await response.json();

    if (!response.ok || !body.success) {
      toast(body.message || '매칭 취소에 실패했습니다.');
      return null;
    }

    await loadMyCrewBattles();

    toast('매칭 신청을 취소했습니다.');

    render();

    return body.data;

  } catch (error) {
    console.error('크루대전 매칭 취소 오류:', error);
    toast('크루대전 서버에 연결할 수 없습니다.');

    return null;
  }
}

function findLatestCrewBattle(status) {
  return [...(state.crewBattles || [])]
    .filter(battle => battle.status === status)
    .sort((a, b) => Number(b.id) - Number(a.id))[0] || null;
}


function renderCrewBattleServerActions() {
  const activeBattle = findLatestCrewBattle('ACTIVE');
  const waitingBattle = findLatestCrewBattle('WAITING');

  const memberCount = (state.crew.members || []).length;
  let message = '2~5명이서 실시간 크루대전을 신청할 수 있습니다.';
  let buttonHtml = `
    <button
      class="btn crew-battle-cta"
      style="padding:16px 24px;font-size:15px;"
      onclick="openCrewBattleSetup()"
      ${memberCount < 2 ? 'disabled' : ''}>
      대전 신청
    </button>
  `;

  if (memberCount < 2) {
    message = '대전을 신청하려면 크루원이 2명 이상이어야 합니다.';
  }

  if (activeBattle) {
    message = '진행 중인 크루대전이 있습니다.';

    buttonHtml = `
      <button
        class="btn crew-battle-cta"
        style="padding:16px 24px;font-size:15px;"
        onclick="enterCrewBattleFromServer(${activeBattle.id})">
        대전 입장
      </button>
    `;
  } else if (waitingBattle) {
    const isMine =
      Number(waitingBattle.requesterUserId) === Number(state.user.id);

    message =
      `매칭 상대를 찾고 있습니다 (${waitingBattle.teamSize}vs${waitingBattle.teamSize})`;

    buttonHtml = isMine
      ? `
        <button
          class="btn crew-battle-cta"
          style="padding:16px 24px;font-size:15px;"
          onclick="cancelMatchedCrewBattle(${waitingBattle.id})">
          매칭 취소
        </button>
      `
      : `
        <button
          class="btn crew-battle-cta"
          style="padding:16px 24px;font-size:15px;"
          disabled>
          매칭 대기중
        </button>
      `;
  }

  return `
    <div style="
      display:flex;
      flex-direction:column;
      align-items:center;
      gap:8px;
      flex:none;
      max-width:340px;
    ">
      <span style="
        color:#fff;
        font-family:var(--font-display);
        font-size:20px;
        letter-spacing:.04em;
      ">VS</span>

      <p style="
        margin:0;
        color:rgba(255,255,255,.85);
        font-size:12px;
        text-align:center;
        white-space:nowrap;
      ">${message}</p>

      ${buttonHtml}
    </div>
  `;
}


/* ---------- 크루대전 신청 폼: 몇 명(teamSize)이서 누구(participantIds)와 나갈지 고른다 ----------
   백엔드가 상대를 자동으로 골라주므로, 프론트가 할 일은 "우리 팀 구성"을 서버에 보내는 것뿐이다. */
function openCrewBattleSetup() {
  if (!state.crew.created || !state.crew.id) {
    toast('먼저 크루에 가입해주세요.');
    return;
  }

  const maxTeamSize = Math.min(5, (state.crew.members || []).length);

  if (maxTeamSize < 2) {
    toast('대전을 신청하려면 크루원이 2명 이상이어야 합니다.');
    return;
  }

  state.crewBattleSetup = {
    open: true,
    teamSize: maxTeamSize,
    participantIds: [Number(state.user.id)]
  };

  render();
}

function closeCrewBattleSetup() {
  state.crewBattleSetup.open = false;
  render();
}

function setCrewBattleTeamSize(teamSize) {
  state.crewBattleSetup.teamSize = teamSize;
  // 인원수를 바꾸면 이전 선택이 새 인원수와 안 맞을 수 있어 나(신청자)만 남기고 다시 고르게 한다.
  state.crewBattleSetup.participantIds = [Number(state.user.id)];
  render();
}

function toggleCrewBattleParticipant(userId) {
  const setup = state.crewBattleSetup;

  if (Number(userId) === Number(state.user.id)) {
    return; // 신청자 본인은 항상 참가자에 포함되어야 하므로 뺄 수 없다.
  }

  const ids = setup.participantIds;
  const idx = ids.indexOf(Number(userId));

  if (idx >= 0) {
    ids.splice(idx, 1);
  } else if (ids.length >= setup.teamSize) {
    toast(`참가자는 ${setup.teamSize}명까지 선택할 수 있어요`);
    return;
  } else {
    ids.push(Number(userId));
  }

  render();
}

async function submitCrewBattleRequest() {
  const setup = state.crewBattleSetup;

  if (setup.participantIds.length !== setup.teamSize) {
    toast(
      `참가자를 ${setup.teamSize}명 선택해주세요 ` +
      `(현재 ${setup.participantIds.length}명)`
    );
    return;
  }

  const battle = await requestCrewBattle(
    setup.teamSize,
    setup.participantIds,
    'squat'
  );

  if (battle) {
    state.crewBattleSetup.open = false;
    render();
  }
}

function renderCrewBattleSetupPanel() {
  const setup = state.crewBattleSetup;

  if (!setup.open) {
    return '';
  }

  const maxTeamSize = Math.min(5, (state.crew.members || []).length);
  const sizes = [];

  for (let n = 2; n <= maxTeamSize; n++) {
    sizes.push(n);
  }

  return `
  <div class="card" style="margin-top:14px;">
    <div class="flex-between">
      <p class="section-label" style="margin:0;">대전 신청 — 팀 구성</p>
      <button class="btn btn-ghost btn-sm" onclick="closeCrewBattleSetup()">닫기</button>
    </div>
    <div class="field">
      <label>인원수</label>
      <div style="display:flex;flex-wrap:wrap;gap:8px;">
        ${sizes.map(n => `<button type="button" class="btn btn-sm ${setup.teamSize === n ? 'btn-primary' : 'btn-secondary'}" onclick="setCrewBattleTeamSize(${n})">${n}vs${n}</button>`).join('')}
      </div>
    </div>
    <div class="field">
      <label>참가자 (${setup.participantIds.length}/${setup.teamSize}명, 나는 자동 포함)</label>
      <div style="display:flex;flex-wrap:wrap;gap:8px;">
        ${(state.crew.members || []).map(m => {
          const isMe = Number(m.userId) === Number(state.user.id);
          const selected = setup.participantIds.includes(Number(m.userId));
          return `<button type="button" class="btn btn-sm ${selected ? 'btn-primary' : 'btn-secondary'}" ${isMe ? 'disabled' : ''} onclick="toggleCrewBattleParticipant(${m.userId})">${m.n}${isMe ? ' (나)' : ''}</button>`;
        }).join('')}
      </div>
    </div>
    <button class="btn btn-primary btn-block" onclick="submitCrewBattleRequest()">매칭 신청</button>
  </div>`;
}


/* ---------- 크루대전 UI 미리보기(데모) ----------
   실제 매칭 상대를 구하기 어려운 테스트 상황(크루원이 나 혼자뿐 등)에서도 대전 화면을 볼 수
   있도록, 가짜 팀원·가짜 상대를 만들어 로컬에서만 돌아가는 버전이다. 서버 API를 전혀 부르지
   않고 state.crewBattle을 직접 채운 뒤 실제 대전 화면(renderCrewBattle)을 그대로 재사용한다.
   내 카메라 판정만 진짜고, 나머지 참가자 점수는 타이머로 흉내낸다. */
const CREW_BATTLE_DEMO_NAMES = ['헬린이', '스쿼트왕', '런닝러버', '플랭크신', '다이어터'];
const CREW_BATTLE_DEMO_GRADES = ['PERFECT', 'PERFECT', 'GREAT', 'GREAT', 'GREAT', 'GOOD', 'GOOD', 'MISS'];
let crewBattleDemoTickerId = null;

function renderCrewBattleDemoButtons() {
  return `
  <div class="card" style="margin-top:14px;">
    <p class="section-label" style="margin:0 0 4px;">UI 미리보기 (데모)</p>
    <p class="hint" style="margin:0 0 10px;">실제 매칭 없이 가짜 상대로 대전 화면을 바로 볼 수 있어요. 서버에 기록되지 않습니다.</p>
    <div style="display:flex;flex-wrap:wrap;gap:8px;">
      ${[2, 3, 4, 5].map(n => `<button type="button" class="btn btn-sm btn-secondary" onclick="startCrewBattleDemo(${n})">${n}vs${n} 시뮬레이션</button>`).join('')}
    </div>
  </div>`;
}

function makeCrewBattleDemoParticipant(userId, name, crewId, crewName, index) {
  return {
    userId: Number(userId),
    n: name,
    crewId: Number(crewId),
    crewName: crewName,
    score: 0,
    latestGrade: null,
    gradeCounts: { PERFECT: 0, GREAT: 0, GOOD: 0, MISS: 0 },
    gender: index % 2 === 0 ? 'male' : 'female'
  };
}

async function startCrewBattleDemo(teamSize) {
  if (!state.user.calibration) {
    toast('크루대전을 시작하려면 캘리브레이션이 필요합니다.');
    openCalibrationModal();
    return;
  }

  const me = makeCrewBattleDemoParticipant(
    state.user.id,
    state.user.nickname || '나',
    state.crew.id || 0,
    state.crew.name || '내 크루',
    0
  );
  me.gender = state.user.gender || 'male';

  const myTeammates = [];
  for (let i = 1; i < teamSize; i++) {
    myTeammates.push(
      makeCrewBattleDemoParticipant(
        -1000 - i,
        CREW_BATTLE_DEMO_NAMES[(i - 1) % CREW_BATTLE_DEMO_NAMES.length],
        me.crewId,
        me.crewName,
        i
      )
    );
  }

  const opponentParticipants = [];
  for (let i = 0; i < teamSize; i++) {
    opponentParticipants.push(
      makeCrewBattleDemoParticipant(
        -2000 - i,
        CREW_BATTLE_DEMO_NAMES[i % CREW_BATTLE_DEMO_NAMES.length],
        -1,
        '상대 크루 (데모)',
        i
      )
    );
  }

  const myParticipants = [me, ...myTeammates];
  const allParticipants = [...myParticipants, ...opponentParticipants];

  disconnectCrewChat();

  state.crewBattle = {
    id: null,
    demo: true,
    status: 'ACTIVE',
    teamSize: teamSize,
    startedAt: new Date().toISOString(),
    endsAt: null,
    remainingSeconds: 120,

    target: 300,

    opponent: { name: '상대 크루 (데모)', level: '-' },

    myScore: 0,
    oppScore: 0,
    myGradeCounts: { PERFECT: 0, GREAT: 0, GOOD: 0, MISS: 0 },

    teammates: myTeammates,
    oppTeammates: opponentParticipants,
    participants: allParticipants,
    myParticipants: myParticipants,
    opponentParticipants: opponentParticipants,

    tickId: null,
    result: null
  };

  state.exercise = freshExerciseState({ picked: 'squat' });

  exBattleCountdownStarted = false;

  state.menu = 'crewBattle';

  render();

  startCrewBattleTimer();
  startCrewBattleDemoTicker();
}

// 나를 제외한 가짜 참가자들의 점수를 주기적으로 올려 실제 대전처럼 보이게 한다.
function startCrewBattleDemoTicker() {
  stopCrewBattleDemoTicker();

  crewBattleDemoTickerId = setInterval(() => {
    const battle = state.crewBattle;

    if (!battle || !battle.demo || battle.status !== 'ACTIVE') {
      stopCrewBattleDemoTicker();
      return;
    }

    const others = battle.participants.filter(
      p => Number(p.userId) !== Number(state.user.id)
    );

    if (!others.length) return;

    const picked = others[Math.floor(Math.random() * others.length)];
    const grade =
      CREW_BATTLE_DEMO_GRADES[
        Math.floor(Math.random() * CREW_BATTLE_DEMO_GRADES.length)
      ];

    applyCrewBattleDemoGrade(picked, grade);
  }, 1100);
}

function stopCrewBattleDemoTicker() {
  if (crewBattleDemoTickerId) {
    clearInterval(crewBattleDemoTickerId);
    crewBattleDemoTickerId = null;
  }
}

// 내 카메라 판정(sendCrewBattleRep)과 가짜 참가자 판정(startCrewBattleDemoTicker) 둘 다 여기로 모여
// 실제 서버 이벤트(handleCrewBattleEvent)와 똑같은 방식으로 점수·판정 문구를 갱신한다.
function applyCrewBattleDemoGrade(participant, grade) {
  if (grade !== 'MISS') {
    participant.score += 1;
  }

  participant.latestGrade = grade;
  participant.gradeCounts[grade] += 1;

  const isMe = Number(participant.userId) === Number(state.user.id);

  if (isMe) {
    state.crewBattle.myScore = participant.score;
    state.crewBattle.myGradeCounts = participant.gradeCounts;
  }

  updateBattleUI();
  showCrewBattleGrade(participant.userId, grade, grade !== 'MISS');
}

function finishCrewBattleDemo() {
  const battle = state.crewBattle;

  if (!battle || battle.finishing) {
    return;
  }

  battle.finishing = true;
  battle.status = 'FINISHED';
  battle.remainingSeconds = 0;

  stopCrewBattleDemoTicker();
  disconnectCrewBattleSocket();
  stopCrewBattleCamera();

  const myCrewTotal = battle.myParticipants.reduce((s, p) => s + p.score, 0);
  const oppCrewTotal = battle.opponentParticipants.reduce((s, p) => s + p.score, 0);

  battle.result =
    myCrewTotal === oppCrewTotal ? 'draw' : myCrewTotal > oppCrewTotal ? 'win' : 'lose';

  showCrewBattleResult(battle.result);
}


/* ---------- 크루대전 결과 목록: 카드 한 줄 요약 + "자세히보기"로 참가자별 상세 펼치기 ----------
   목록(GET /me)에는 EXP·참가자 정확도가 없어서, 카드에 EXP를 바로 보여주려면 상세 결과
   (GET /{id}/result, loadFinishedCrewBattleResults가 미리 캐시해둔다)가 로드돼 있어야 한다.
   아직 안 왔으면 "…"로 보여주다가 도착하는 대로 다시 그려진다. */
function renderCrewBattleHistory() {
  const finished = (state.crewBattles || [])
    .filter(b => b.status === 'FINISHED')
    .sort((a, b) => Number(b.id) - Number(a.id));

  if (!finished.length) {
    return `
    <div class="card" style="margin-top:14px;">
      <p class="section-label" style="margin:0;">크루대전 결과</p>
      <p class="hint" style="margin:6px 0 0;">아직 완료된 크루대전이 없어요.</p>
    </div>`;
  }

  const myCrewId = Number(state.crew.id);

  return `
  <div style="margin-top:14px;">
    <p class="section-label" style="margin:0 0 8px;">크루대전 결과</p>
    <div style="display:flex;flex-direction:column;gap:10px;">
      ${finished.map(b => renderCrewBattleHistoryCard(b, myCrewId)).join('')}
    </div>
  </div>`;
}

function renderCrewBattleHistoryCard(battle, myCrewId) {
  const isChallenger = Number(battle.challengerCrewId) === myCrewId;
  const oppCrewName = isChallenger ? battle.opponentCrewName : battle.challengerCrewName;
  const myScore = isChallenger ? battle.challengerScore : battle.opponentScore;
  const oppScore = isChallenger ? battle.opponentScore : battle.challengerScore;

  const resultLabel = battle.drawResult
    ? '무'
    : Number(battle.winnerCrewId) === myCrewId
      ? '승'
      : '패';

  const resultClass =
    resultLabel === '승' ? 'pill-accent' : resultLabel === '패' ? 'pill-coral' : 'pill-muted';

  const detail = state.crewBattleResults[battle.id];
  const myTeamResult = detail
    ? (Number(detail.challenger.crewId) === myCrewId ? detail.challenger : detail.opponent)
    : null;
  const expText = myTeamResult ? `+${myTeamResult.rewardExp}EXP` : '…';

  const isOpen = state.crewBattleHistoryOpenId === battle.id;

  return `
  <div class="card">
    <div class="flex-between" style="gap:12px;">
      <span class="pill ${resultClass}" style="font-size:14px;padding:6px 14px;flex:none;">${resultLabel}</span>
      <div style="flex:1;min-width:0;">
        <p style="margin:0;font-weight:700;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">VS ${oppCrewName}</p>
        <p class="mono hint" style="margin:2px 0 0;">${myScore} : ${oppScore}</p>
      </div>
      <span class="mono" style="color:var(--accent);font-weight:700;flex:none;">${expText}</span>
      <button class="btn btn-sm btn-secondary" style="flex:none;" onclick="toggleCrewBattleHistoryDetail(${battle.id})">${isOpen ? '접기' : '자세히보기'}</button>
    </div>
    ${isOpen ? renderCrewBattleHistoryDetail(battle, myCrewId) : ''}
  </div>`;
}

function toggleCrewBattleHistoryDetail(battleId) {
  if (state.crewBattleHistoryOpenId === battleId) {
    state.crewBattleHistoryOpenId = null;
    render();
    return;
  }

  state.crewBattleHistoryOpenId = battleId;
  render();

  if (!state.crewBattleResults[battleId]) {
    loadCrewBattleResultDetail(battleId).then(() => render());
  }
}

function renderCrewBattleHistoryDetail(battle, myCrewId) {
  const detail = state.crewBattleResults[battle.id];

  if (!detail) {
    return `<p class="hint" style="margin:12px 0 0;">결과를 불러오는 중...</p>`;
  }

  const myTeam = Number(detail.challenger.crewId) === myCrewId ? detail.challenger : detail.opponent;
  const oppTeam = Number(detail.challenger.crewId) === myCrewId ? detail.opponent : detail.challenger;

  return `
  <div class="grid grid-fixed-2" style="margin-top:14px;gap:14px;">
    <div>
      <p class="section-label" style="margin:0 0 8px;">${myTeam.crewName} · 우리 크루</p>
      ${myTeam.participants.map((p, i) => renderCrewBattleHistoryParticipantRow(p, i, false)).join('')}
    </div>
    <div>
      <p class="section-label" style="margin:0 0 8px;text-align:right;">${oppTeam.crewName} · 상대 크루</p>
      ${oppTeam.participants.map((p, i) => renderCrewBattleHistoryParticipantRow(p, i, true)).join('')}
    </div>
  </div>`;
}

// mirrored=false(우리 크루, 왼쪽): 캐릭터 → 이름 → 점수 → 정확도 순.
// mirrored=true(상대 크루, 오른쪽): 정확도 → 점수 → 이름 → 캐릭터 순으로 뒤집어서, 카드 가운데를
// 기준으로 두 팀이 데칼코마니처럼 마주보게 만든다(캐릭터는 항상 바깥쪽, 정확도는 안쪽).
function renderCrewBattleHistoryParticipantRow(p, index, mirrored) {
  const avatar = `<span class="user-avatar" style="width:32px;height:32px;flex:none;background:${avatarColor(index)}">${avatarInitial(p.nickname)}</span>`;
  const name = `<span style="flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;${mirrored ? 'text-align:right;' : ''}">${p.nickname}</span>`;
  const score = `<span class="mono" style="font-weight:700;flex:none;min-width:50px;text-align:center;">${p.personalScore}점</span>`;
  const grades = `
    <div class="mono hint" style="display:flex;gap:5px;font-size:10.5px;flex:none;white-space:nowrap;">
      <span style="color:#6FBBEE;">P${p.perfectCount}</span>
      <span style="color:var(--accent);">G${p.greatCount}</span>
      <span style="color:var(--ink-dim);">O${p.goodCount}</span>
      <span style="color:var(--coral);">M${p.missCount}</span>
    </div>`;

  const parts = mirrored ? [grades, score, name, avatar] : [avatar, name, score, grades];

  return `<div style="display:flex;align-items:center;gap:8px;padding:6px 0;">${parts.join('')}</div>`;
}


async function enterCrewBattleFromServer(battleId) {
  if (!state.user.calibration) {
    toast('크루대전을 시작하려면 캘리브레이션이 필요합니다.');
    openCalibrationModal();
    return;
  }

  try {
    const response = await fetch(
      `${API_BASE}/api/crew-battles/${battleId}`,
      {
        headers: {
          Authorization: 'Bearer ' + state.token
        }
      }
    );

    const body = await response.json();

    if (!response.ok || !body.success) {
      toast(body.message || '크루대전 정보를 불러오지 못했습니다.');
      return;
    }

    const battle = body.data;

    if (battle.status !== 'ACTIVE') {
      toast('현재 진행 중인 크루대전이 아닙니다.');
      return;
    }

    state.crewBattle = {
      id: Number(battle.id),
      status: battle.status,
      teamSize: Number(battle.teamSize),
      startedAt: battle.startedAt,
      endsAt: battle.endsAt,
      remainingSeconds: battle.remainingSeconds,

      // 기존 화면과 연결하기 위한 임시값입니다.
      // 다음 단계에서 시간제 대전 화면으로 변경합니다.
      target: 300,

      opponent: {
        name:
          Number(battle.challengerCrewId) === Number(state.crew.id)
            ? battle.opponentCrewName
            : battle.challengerCrewName,
        level: '-'
      },

      myScore: 0,
      oppScore: 0,
      myGradeCounts: {
        PERFECT: 0,
        GREAT: 0,
        GOOD: 0,
        MISS: 0
      },

      teammates: [],
      oppTeammates: [],
      participants: [],
      myParticipants: [],
      opponentParticipants: [],

      tickId: null,
      result: null
    };

    const loaded =
      await loadCrewBattleParticipants(battle.id);

    if (!loaded) {
      state.crewBattle = null;
      return;
    }

    state.exercise = freshExerciseState({ picked: 'squat' });

    exBattleCountdownStarted = false;

    disconnectCrewChat();

    state.menu = 'crewBattle';

    connectCrewBattleSocket(battle.id);

    render();

    startCrewBattleTimer();

  } catch (error) {
    console.error('크루대전 입장 오류:', error);
    toast('크루대전에 입장하지 못했습니다.');
  }
}


// 실제 서버 대전에서는 가짜 점수 타이머를 실행하지 않습니다.
function startBattleTicker() {
  console.log('서버의 실시간 운동 결과를 기다립니다.');
}

function renderLiveBattleParticipant(participant, myTeam) {
  const isMe =
    Number(participant.userId) === Number(state.user.id);

  const squatImage =
    participant.gender === 'female'
      ? 'assets/battle/avatar-female-squat.png'
      : 'assets/battle/avatar-male-squat.png';

  return `
    <div
      class="battle-participant ${myTeam ? 'my-team' : 'opponent-team'} ${isMe ? 'is-me' : ''}"
      data-battle-user-id="${participant.userId}"
    >
      <div class="battle-grade-label"></div>

      <div class="battle-character">
        <canvas
          class="battle-standing-character"
          data-battle-canvas="${participant.userId}"
          width="90"
          height="110"
        ></canvas>

        <img
          class="battle-squat-character"
          src="${squatImage}"
          alt="스쿼트 자세"
          draggable="false"
        >
      </div>

      <p class="battle-participant-name">
        ${participant.n}
        ${isMe ? '<span class="pill pill-accent">나</span>' : ''}
      </p>

      <p class="battle-participant-count mono">
        <span data-battle-count="${participant.userId}">
          ${participant.score}
        </span>개
      </p>
    </div>
  `;
}


function renderLiveBattleTeam(title, participants, myTeam) {
  return `
    <div class="card battle-team-card ${myTeam ? 'my-team' : 'opponent-team'}">
      <p class="section-label">
        ${title}
      </p>

      <div class="battle-participant-grid">
        ${participants
          .map(participant =>
            renderLiveBattleParticipant(participant, myTeam)
          )
          .join('')}
      </div>
    </div>
  `;
}


function renderCrewBattle() {
  const battle = state.crewBattle;

  if (!battle) {
    return `
      <div class="empty-note">
        크루대전 정보를 불러올 수 없습니다.
      </div>
    `;
  }

  const myParticipants = battle.myParticipants || [];
  const opponentParticipants =
    battle.opponentParticipants || [];

  const myCrewTotal = myParticipants.reduce(
    (total, participant) =>
      total + Number(participant.score || 0),
    0
  );

  const opponentTotal = opponentParticipants.reduce(
    (total, participant) =>
      total + Number(participant.score || 0),
    0
  );

  return `
    <div class="view-head flex-between">
      <div>
        <h1 style="margin:0;">${battle.teamSize}vs${battle.teamSize} 크루대전</h1>
        <p class="hint" style="margin:5px 0 0;">
          카메라 영상은 내 화면에만 표시됩니다.
        </p>
      </div>

      <button
        class="btn btn-ghost btn-sm"
        onclick="exitLiveCrewBattle()">
        나가기
      </button>
    </div>

    <div class="card battle-scoreboard">
      <div class="battle-score-side my-team">
        <p>${state.crew.name}</p>

        <strong
          id="battle-team-total"
          class="mono">
          ${myCrewTotal}
        </strong>

        <span>개</span>
      </div>

      <div class="battle-score-center">
        <span>VS</span>

        <p
          id="battle-remaining-time"
          class="mono">
          ${formatCrewBattleTime(battle.remainingSeconds)}
        </p>
      </div>

      <div class="battle-score-side opponent-team">
        <p>${battle.opponent.name}</p>

        <strong
          id="battle-opp-total"
          class="mono">
          ${opponentTotal}
        </strong>

        <span>개</span>
      </div>
    </div>

    <div class="grid cal-grid battle-camera-row">
      <div>
        <div class="cam-stage" id="cam-stage">

          <div
            class="cam-placeholder"
            id="cam-placeholder">
            카메라를 확인하는 중...<br>
            브라우저의 카메라 권한을 허용해주세요.
          </div>

          <video
            id="cam-video"
            autoplay
            playsinline
            muted
            style="display:none;">
          </video>

          <canvas
            class="cam-overlay-canvas"
            id="cam-canvas">
          </canvas>

          <div class="cam-badge">
            <span class="rec-dot"></span>
            <span id="cam-status">대기중</span>
          </div>

          <div
            class="cam-timer mono"
            id="cam-timer">
            00:00
          </div>

          <div class="cam-battle-hud">
            <div class="scores">
              <span
                class="my mono"
                id="cam-battle-my">
                ${myCrewTotal}
              </span>

              <span class="sep">:</span>

              <span
                class="opp mono"
                id="cam-battle-opp">
                ${opponentTotal}
              </span>
            </div>
          </div>

          <div
            id="cam-grade-flash"
            class="cam-grade-flash">
          </div>

          <div
            class="cam-battle-countdown"
            id="cam-battle-countdown">
          </div>
        </div>

        <p class="hint" style="margin-top:8px;">
          이 영상은 서버나 다른 참가자에게 전송되지 않습니다.
        </p>
      </div>

      <div class="card">
        <p class="section-label">실시간 판정 안내</p>

        <ul class="steplist">
          <li>
            <span class="num">1</span>
            스쿼트 1회가 끝나면 판정 결과가 서버로 전송됩니다.
          </li>

          <li>
            <span class="num">2</span>
            PERFECT/GREAT/GOOD이면 횟수가 1개 올라갑니다.
          </li>

          <li>
            <span class="num">3</span>
            MISS는 문구만 표시되고 횟수는 올라가지 않습니다.
          </li>

          <li>
            <span class="num">4</span>
            다른 참가자의 카메라 영상은 표시되지 않습니다.
          </li>
        </ul>
      </div>
    </div>

    <div class="battle-teams">
      ${renderLiveBattleTeam(
        `${state.crew.name} · 우리 크루`,
        myParticipants,
        true
      )}

      ${renderLiveBattleTeam(
        `${battle.opponent.name} · 상대 크루`,
        opponentParticipants,
        false
      )}
    </div>
  `;
}


function drawBattleTeammates() {
  if (!state.crewBattle) {
    return;
  }

  document
    .querySelectorAll('[data-battle-canvas]')
    .forEach(canvas => {
      const userId = Number(
        canvas.dataset.battleCanvas
      );

      const participant =
        state.crewBattle.participants.find(
          member =>
            Number(member.userId) === userId
        );

      if (!participant) {
        return;
      }

      const isMe =
        userId === Number(state.user.id);

      const equipment =
        isMe && typeof getEquipState === 'function'
          ? getEquipState()
          : {};

      drawPixelCharacter(
        canvas,
        equipment,
        participant.gender || 'male'
      );
    });
}


function updateBattleUI() {
  const battle = state.crewBattle;

  if (!battle) {
    return;
  }

  const myCrewTotal =
    (battle.myParticipants || []).reduce(
      (total, participant) =>
        total + Number(participant.score || 0),
      0
    );

  const opponentTotal =
    (battle.opponentParticipants || []).reduce(
      (total, participant) =>
        total + Number(participant.score || 0),
      0
    );

  const setText = (id, value) => {
    const element = document.getElementById(id);

    if (element) {
      element.textContent = value;
    }
  };

  setText('battle-team-total', myCrewTotal);
  setText('battle-opp-total', opponentTotal);
  setText('cam-battle-my', myCrewTotal);
  setText('cam-battle-opp', opponentTotal);

  (battle.participants || []).forEach(participant => {
    const countElement = document.querySelector(
      `[data-battle-count="${participant.userId}"]`
    );

    if (countElement) {
      countElement.textContent =
        Number(participant.score || 0);
    }
  });
}


function formatCrewBattleTime(seconds) {
  const safeSeconds = Math.max(
    0,
    Number(seconds || 0)
  );

  const minutes = String(
    Math.floor(safeSeconds / 60)
  ).padStart(2, '0');

  const remainingSeconds = String(
    safeSeconds % 60
  ).padStart(2, '0');

  return `${minutes}:${remainingSeconds}`;
}


function exitLiveCrewBattle() {
  stopCrewBattleTimer();
  stopCrewBattleDemoTicker();
  disconnectCrewBattleSocket();

  if (state.exercise.camStream) {
    state.exercise.camStream
      .getTracks()
      .forEach(track => track.stop());
  }

  clearInterval(state.exercise.timerId);

  state.crewBattle = null;

  state.exercise = freshExerciseState();

  state.menu = 'crew';
  state.subtabs.crew = 0;

  connectCrewChat();
  loadMyCrewBattles(); // 방금 끝난 대전(있다면)이 결과 목록에 바로 보이도록 새로고침
  render();
}

let crewBattleClockId = null;


function startCrewBattleTimer() {
  stopCrewBattleTimer();

  const battle = state.crewBattle;

  if (!battle || battle.status !== 'ACTIVE') {
    return;
  }

  const parsedEndTime = battle.endsAt
    ? new Date(battle.endsAt).getTime()
    : NaN;

  const endTime = Number.isNaN(parsedEndTime)
    ? Date.now() + Number(battle.remainingSeconds || 0) * 1000
    : parsedEndTime;

  function updateTimer() {
    if (!state.crewBattle) {
      stopCrewBattleTimer();
      return;
    }

    const remainingSeconds = Math.max(
      0,
      Math.ceil((endTime - Date.now()) / 1000)
    );

    state.crewBattle.remainingSeconds = remainingSeconds;

    const timerElement = document.getElementById(
      'battle-remaining-time'
    );

    if (timerElement) {
      timerElement.textContent =
        formatCrewBattleTime(remainingSeconds);
    }

    if (remainingSeconds <= 0) {
      stopCrewBattleTimer();

      if (state.crewBattle.demo) {
        finishCrewBattleDemo();
      } else {
        finishCrewBattleFromServer();
      }
    }
  }

  updateTimer();

  crewBattleClockId = setInterval(
    updateTimer,
    1000
  );
}


function stopCrewBattleTimer() {
  if (crewBattleClockId) {
    clearInterval(crewBattleClockId);
    crewBattleClockId = null;
  }
}


async function finishCrewBattleFromServer() {
  const battle = state.crewBattle;

  if (!battle || battle.finishing) {
    return;
  }

  battle.finishing = true;

  try {
    const response = await fetch(
      `${API_BASE}/api/crew-battles/${battle.id}`,
      {
        headers: {
          Authorization: 'Bearer ' + state.token
        }
      }
    );

    const body = await response.json();

    if (!response.ok || !body.success) {
      battle.finishing = false;
      toast(body.message || '최종 결과를 확인하지 못했습니다.');
      return;
    }

    const finalResult = body.data;

    // 서버 시간이 아직 종료 시각에 도달하지 않았다면 다시 진행
    if (finalResult.status === 'ACTIVE') {
      battle.finishing = false;
      battle.remainingSeconds =
        Number(finalResult.remainingSeconds || 0);

      startCrewBattleTimer();
      return;
    }

    battle.status = finalResult.status;
    battle.remainingSeconds = 0;
    battle.winnerCrewId = finalResult.winnerCrewId;

    await loadCrewBattleParticipants(battle.id);

    updateBattleUI();

    disconnectCrewBattleSocket();
    stopCrewBattleCamera();

    const myCrewId = Number(state.crew.id);
    const winnerCrewId =
      finalResult.winnerCrewId == null
        ? null
        : Number(finalResult.winnerCrewId);

    if (winnerCrewId === null) {
      battle.result = 'draw';
    } else if (winnerCrewId === myCrewId) {
      battle.result = 'win';
    } else {
      battle.result = 'lose';
    }

    showCrewBattleResult(battle.result);

  } catch (error) {
    console.error('크루대전 종료 처리 오류:', error);

    battle.finishing = false;
    toast('크루대전 최종 결과를 불러오지 못했습니다.');
  }
}


function stopCrewBattleCamera() {
  clearInterval(state.exercise.timerId);

  if (state.exercise.camStream) {
    state.exercise.camStream
      .getTracks()
      .forEach(track => track.stop());

    state.exercise.camStream = null;
  }

  if (typeof exRAF !== 'undefined') {
    cancelAnimationFrame(exRAF);
  }

  const statusElement =
    document.getElementById('cam-status');

  if (statusElement) {
    statusElement.textContent = '대전 종료';
  }
}


function showCrewBattleResult(result) {
  const oldResult =
    document.getElementById('crew-battle-result');

  if (oldResult) {
    oldResult.remove();
  }

  const scoreboard =
    document.querySelector('.battle-scoreboard');

  if (!scoreboard) {
    return;
  }

  let title = '무승부입니다';
  let description =
    '양쪽 크루의 최종 운동 횟수가 같습니다.';

  if (result === 'win') {
    title = '🎉 우리 크루 승리!';
    description =
      '우리 크루가 더 많은 유효 스쿼트를 기록했습니다.';
  }

  if (result === 'lose') {
    title = '아쉽게 패배했습니다';
    description =
      '다음 크루대전에서 다시 도전해보세요.';
  }

  const resultElement =
    document.createElement('div');

  resultElement.id = 'crew-battle-result';
  resultElement.className = 'card';

  resultElement.style.marginBottom = '16px';
  resultElement.style.textAlign = 'center';

  resultElement.innerHTML = `
    <h2 style="margin:0 0 6px;">
      ${title}
    </h2>

    <p class="desc" style="margin:0 0 14px;">
      ${description}
    </p>

    <button
      class="btn btn-primary"
      onclick="exitLiveCrewBattle()">
      크루로 돌아가기
    </button>
  `;

  scoreboard.insertAdjacentElement(
    'afterend',
    resultElement
  );
}