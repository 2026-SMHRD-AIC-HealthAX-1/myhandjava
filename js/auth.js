// auth.js — 로그인(SNS 전용)/소셜로그인/소셜 온보딩 화면과 로직.
// [담당] 로그인 화면 + 소셜 로그인 콜백 처리 + 최초 로그인 시 온보딩(닉네임/성별/지역 설정).
// [백엔드 연동] POST /api/auth/kakao/login, /api/auth/google/login, /api/auth/social/{provider},
//              GET /api/users/me(loadMyProfile), PATCH /api/users/me/onboarding
//              → DB: users, calibration_profiles 테이블까지 이어짐.
// [주의] doSocialLogin()은 실제 카카오/구글 OAuth 리다이렉트라, 로컬 개발 환경에서는
//        OAUTH_REDIRECT_URI가 실제 서버 설정(카카오/구글 개발자 콘솔)과 일치해야 동작한다.

/* ---------- 캘리브레이션 모달 (MediaPipe Pose) ---------- */
// (FR-AC-002) 이 구간(calStartCamera ~ calComputeProfile)은 브라우저 안에서 도는
// MediaPipe Pose(WASM) 계산이라 그대로 프론트엔드에 남습니다 — 백엔드가 필요 없는 부분.
//   카메라 영상(JS) > MediaPipe Pose(WASM, 브라우저 내 실행) > 체형 프로필 계산(JS)
// 계산된 결과를 실제로 "저장"하는 시점(아래 calApply())부터만 서버 연동이 필요합니다.
// 중간 프로젝트 단계에서 아이디/비밀번호 회원가입·로그인 폼을 없애고 SNS 계정(카카오/구글)
// 로그인만 남겼다 — 아이디/비밀번호 관련 백엔드 API(/api/auth/signup, /api/auth/login,
// check-id 등)는 그대로 남아있지만(기존 아이디/비밀번호 계정도 여전히 유효), 프론트에서
// 더 이상 이 화면들로 진입할 방법이 없어 사실상 SNS 전용이 된다.
function renderLogin() {
  return `
  <div class="center-shell">
    <div class="auth-card login-card">
      <div class="brand" style="padding:0;margin-bottom:16px;cursor:default;">
        <div class="brand-mark" style="width:88px;height:88px;"><img src="assets/오운홈 로고.png" alt="오운홈"></div>
        <div class="brand-name"><small style="font-size:14px;margin-top:0;word-break:keep-all;color:#344563;font-weight:600;">오늘 운동은 집에서</small></div>
      </div>
      <h1 class="auth-title">로그인</h1>
      <p class="auth-sub">${state.user.nickname ? state.user.nickname + '님, 다시 오신 것을 환영해요' : 'SNS 계정으로 간편하게 시작해요'}</p>
      <button class="btn btn-block login-social" style="background:#FEE500;border-color:var(--outline);color:#241A00;margin-bottom:8px;" onclick="doSocialLogin('카카오')">
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path fill="#191919" d="M12 3C6.48 3 2 6.48 2 10.77c0 2.76 1.86 5.18 4.66 6.56l-.94 3.5c-.08.3.25.54.51.37l4.09-2.7c.55.07 1.11.1 1.68.1 5.52 0 10-3.49 10-7.83S17.52 3 12 3Z"/></svg>
        <span>카카오로 계속하기</span>
      </button>
      <button class="btn btn-secondary btn-block login-social" style="background:#fff;" onclick="doSocialLogin('구글')">
        <svg viewBox="0 0 48 48" aria-hidden="true" focusable="false"><path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5Z"/><path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6C44.4 38.02 46.98 31.86 46.98 24.55Z"/><path fill="#FBBC05" d="M10.53 28.59A14.4 14.4 0 0 1 9.75 24c0-1.59.27-3.13.78-4.59l-7.98-6.19A23.87 23.87 0 0 0 0 24c0 3.87.93 7.52 2.56 10.78l7.97-6.19Z"/><path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.8l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48Z"/></svg>
        <span>Google로 계속하기</span>
      </button>
      <p class="hint" style="text-align:center;margin-top:16px;color:#465570;line-height:1.6;">처음이신가요? SNS 계정으로 바로 시작할 수 있어요.</p>
      <button class="btn btn-ghost login-back" onclick="backToLanding()">← 뒤로가기</button>
    </div>
  </div>`;
}

async function loadMyProfile() {
  if (!state.token) return;
  try {
    const res = await fetch(`${API_BASE}/api/users/me`, {
      headers: { 'Authorization': 'Bearer ' + state.token }
    });
    const body = await res.json();
    if (!body.success) return;
    const u = body.data;
    state.user.id = u.id;
    state.user.role = u.role || 'USER'; // 'ADMIN'이면 고객센터에 전체 문의 관리 화면이 뜬다(support.js)
    state.user.nickname = u.nickname;
    state.user.points = u.points;
    state.user.exp = u.exp;
    state.user.currentExp = u.currentExp ?? u.exp;
    state.user.nextLevelExp = u.nextLevelExp ?? calculatedNextLevelExp(u.level);
    state.user.level = u.level;
    state.user.gender = normalizeGender(u.gender, loadSessionGender());
    saveSessionGender(state.user.gender);
    state.user.avatar = u.avatar ?? state.user.avatar;
    state.user.grade = u.grade; // 'IRON'~'CHALLENGER' — userLevelBadge()의 배지 색을 정하는 값
    state.user.gradeName = u.gradeName; // '아이언'~'챌린저' — 배지 title 툴팁용
    state.user.streak = u.streak;
    state.user.retakeTickets = u.retakeTickets;
    state.user.nicknameTickets = u.nicknameTickets;
    state.user.setsUsedToday = u.setsUsedToday;
    state.user.freeWorkoutsUsed = u.freeWorkoutsUsed ?? u.setsUsedToday ?? state.user.freeWorkoutsUsed;
    state.user.freeWorkoutDate = u.freeWorkoutDate || state.user.freeWorkoutDate;
    // 서버 값을 그대로 덮어썼으니, 자정(KST) 지난 뒤 처음 불러온 프로필이라면 여기서 바로
    // 0회로 되돌린다 — 서버가 날짜를 안 보내주거나(freeWorkoutDate 없음) 갱신을 놓친 경우에도
    // 프론트에서 항상 "오늘" 기준으로 맞춰지게 하기 위함.
    if (typeof syncDailyFreeWorkouts === 'function') syncDailyFreeWorkouts();
    state.user.bio = u.bio || '';
    state.user.region = (u.regionCity && u.regionGu && u.regionDong) ? `${u.regionCity} ${u.regionGu} ${u.regionDong}` : '';
    state.settings.account.regionCity = u.regionCity;
    state.settings.account.regionGu = u.regionGu;
    state.settings.account.regionDong = u.regionDong;
    state.settings.account.profilePublic = u.profilePublic !== false;

        // 캘리브레이션은 계정에 저장돼 있어도 로그인할 때 자동으로 안 불러와지고 있었다 —
    // 그래서 매번 다시 하라고 뜬 것. 여기서 서버에 저장된 값을 가져와 채워준다.
    try {
      const calRes = await fetch(`${API_BASE}/api/users/me/calibration`, {
        headers: { 'Authorization': 'Bearer ' + state.token }
      });
      const calBody = await calRes.json();
      if (calBody.success && calBody.data && calBody.data.profileJson) {
        state.user.calibration = JSON.parse(calBody.data.profileJson);
      }
      state.user.calibrated = !!(calBody.success && calBody.data && (calBody.data.calibrated || calBody.data.profileJson));
    } catch (err) {
      console.error('캘리브레이션 불러오기 실패', err);
    }



    if (!state.user.region) {
      askConfirm('동네를 설정해주세요', '지역별 랭킹에 참여하려면 사는 동네를 등록해야 해요.', () => { closeConfirm(); goToAccountSettings(); }, '지금 설정하기');
    }
  } catch (err) {
    console.error('프로필 불러오기 실패', err);
  }
}

function goToAccountSettings() {
  state.screen = 'app';
  state.menu = 'profile';
  state.subtabs.profile = 3;
  render();
}


// [백엔드 연동 필요 구간] doSocialLogin() — 실제로는 각 사(카카오/네이버/구글) OAuth 인가 코드를
// 받아 Java 서버로 넘기고 > 서버가 토큰 교환 + 사용자 조회/생성(DB 연결, SQL INSERT or SELECT)을
// 수행한 뒤 세션을 발급하는 흐름이 필요하다. 여기서는 버튼 클릭 시 바로 로그인된 것처럼 목업 처리.
function doSocialLogin(provider) {
  const redirectUri = OAUTH_REDIRECT_URI;
  if (provider === '카카오') {
    const clientId = '6cdf06ee0d4469694b390e529f65f741';
    window.location.href = `https://kauth.kakao.com/oauth/authorize?client_id=${clientId}&redirect_uri=${encodeURIComponent(redirectUri)}&response_type=code&state=kakao`;
    return;
  }
  if (provider === '구글') {
    const clientId = '117788279159-b9fv1cg9mivijt2r79680mua8u2a6o78.apps.googleusercontent.com';
    window.location.href = `https://accounts.google.com/o/oauth2/v2/auth?client_id=${clientId}&redirect_uri=${encodeURIComponent(redirectUri)}&response_type=code&scope=${encodeURIComponent('email profile')}&state=google`;
    return;
  }
}


async function handleKakaoRedirect(code) {
  try {
    const res = await fetch(`${API_BASE}/api/auth/kakao/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code, redirectUri: OAUTH_REDIRECT_URI })
    });
    const body = await res.json();
    if (!body.success) { toast(body.message || '카카오 로그인에 실패했습니다'); return; }
    state.token = body.data.accessToken;
    saveSession(state.token);
    await loadMyProfile();
    await loadExerciseHistory();
    await loadMyCrew();
    await loadShopItems();
    if (typeof loadMyRegionRank === 'function') await loadMyRegionRank();
    if (typeof autoClaimAttendance === 'function') autoClaimAttendance();
    state.user.id = body.data.userId;
    state.user.nickname = body.data.nickname;
    state.guestMode = false;
    state.screen = 'app';
    state.menu = 'main';
    saveSessionMenu('main');
    toast('카카오 계정으로 로그인했습니다');
    maybeOpenSocialOnboarding();
  } catch (err) {
    toast('서버에 연결할 수 없습니다 (백엔드가 켜져 있는지 확인해주세요)');
  }
}

async function handleGoogleRedirect(code) {
  try {
    const res = await fetch(`${API_BASE}/api/auth/google/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code, redirectUri: OAUTH_REDIRECT_URI })
    });
    const body = await res.json();
    if (!body.success) { toast(body.message || '구글 로그인에 실패했습니다'); return; }
    state.token = body.data.accessToken;
    saveSession(state.token);
    await loadMyProfile();
    await loadExerciseHistory();
    await loadMyCrew();
    await loadShopItems();
    if (typeof loadMyRegionRank === 'function') await loadMyRegionRank();
    if (typeof autoClaimAttendance === 'function') autoClaimAttendance();
    state.user.id = body.data.userId;
    state.user.nickname = body.data.nickname;
    state.guestMode = false;
    state.screen = 'app';
    state.menu = 'main';
    saveSessionMenu('main');
    toast('구글 계정으로 로그인했습니다');
    maybeOpenSocialOnboarding();
  } catch (err) {
    toast('서버에 연결할 수 없습니다 (백엔드가 켜져 있는지 확인해주세요)');
  }
}

// 소셜 로그인은 회원가입 화면(닉네임·성별·동네)을 안 거치고 바로 앱에 들어오므로, 아직 동네를
// 한 번도 안 정한 계정(state.user.region이 비어있음)이면 메인 화면 대신 이 설정 화면부터
// 채우게 막는다. 이미 설정을 마친 계정(재로그인)은 당연히 건너뛴다.
function maybeOpenSocialOnboarding() {
  if (state.user.region) return;
  // loadMyProfile()이 이미 "동네를 설정해주세요" 확인창을 띄워둔 상태일 수 있다 — 이
  // 온보딩 팝업이 그걸 완전히 대체하니 뒤에 같이 떠 있지 않게 먼저 닫는다.
  state.confirm = null;
  state.socialOnboarding = {
    open: true,
    nickname: state.user.nickname || '',
    gender: normalizeGender(state.user.gender, 'male'),
    regionCity: '서울시', regionGu: '강남구', regionDong: '역삼동',
  };
  render();
}
function setOnboardingGender(v) { state.socialOnboarding.gender = v; render(); }
function setOnboardingCity(v) {
  state.socialOnboarding.regionCity = v;
  const gus = Object.keys(REGION_DATA[v]);
  state.socialOnboarding.regionGu = gus[0];
  state.socialOnboarding.regionDong = REGION_DATA[v][gus[0]][0];
  render();
}
function setOnboardingGu(v) {
  state.socialOnboarding.regionGu = v;
  state.socialOnboarding.regionDong = REGION_DATA[state.socialOnboarding.regionCity][v][0];
  render();
}
function setOnboardingDong(v) { state.socialOnboarding.regionDong = v; render(); }
async function checkOnboardingNickDup() {
  const nick = document.getElementById('ob-nick').value.trim();
  const msg = document.getElementById('ob-nick-msg');
  if (!nick) { msg.style.color = 'var(--danger)'; msg.textContent = '닉네임을 입력해주세요'; msg.style.display = 'block'; return; }
  // 소셜 로그인이 지어준 임시 닉네임이 입력창에 기본으로 채워져 있어서, 그대로 두고
  // 중복확인을 누르면 본인 것과 충돌 체크를 하게 돼 항상 "사용중"으로 나온다 — 지금 내
  // 닉네임 그대로면 굳이 서버까지 안 묻고 바로 통과시킨다(백엔드도 같은 기준으로 통과시킴).
  if (nick === state.user.nickname) {
    msg.style.color = 'var(--accent)'; msg.textContent = '지금 쓰고 있는 닉네임이에요. 그대로 사용할 수 있어요'; msg.style.display = 'block';
    return;
  }
  try {
    const res = await fetch(`${API_BASE}/api/auth/check-nickname`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nickname: nick })
    });
    const body = await res.json();
    const dup = body.data.duplicate;
    msg.style.color = dup ? 'var(--danger)' : 'var(--accent)';
    msg.textContent = dup ? '이미 사용중인 닉네임입니다' : '사용 가능한 닉네임입니다';
    msg.style.display = 'block';
  } catch (err) {
    msg.style.color = 'var(--danger)'; msg.textContent = '서버에 연결할 수 없습니다'; msg.style.display = 'block';
  }
}
function drawOnboardingAvatar() {
  const canvas = document.getElementById('onboarding-avatar-canvas');
  if (!canvas) return;
  drawPixelCharacter(canvas, {}, state.socialOnboarding.gender);
}
async function submitSocialOnboarding() {
  const nickEl = document.getElementById('ob-nick');
  const nickname = nickEl ? nickEl.value.trim() : state.socialOnboarding.nickname.trim();
  if (!nickname) { toast('닉네임을 입력해주세요'); return; }
  const o = state.socialOnboarding;
  try {
    const res = await fetch(`${API_BASE}/api/users/me/onboarding`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + state.token },
      body: JSON.stringify({
        nickname, gender: o.gender,
        regionCity: o.regionCity, regionGu: o.regionGu, regionDong: o.regionDong,
      })
    });
    const body = await res.json();
    if (!body.success) { toast(body.message || '설정에 실패했습니다'); return; }
    const d = body.data;
    state.user.nickname = d.nickname;
    state.user.gender = normalizeGender(d.gender, o.gender);
    saveSessionGender(state.user.gender);
    state.user.region = (d.regionCity && d.regionGu && d.regionDong) ? `${d.regionCity} ${d.regionGu} ${d.regionDong}` : '';
    state.settings.account.regionCity = d.regionCity;
    state.settings.account.regionGu = d.regionGu;
    state.settings.account.regionDong = d.regionDong;
    state.socialOnboarding.open = false;
    toast('설정이 완료됐어요! 오운홈을 시작해볼까요?');
    render();
  } catch (err) {
    toast('서버에 연결할 수 없습니다 (백엔드가 켜져 있는지 확인해주세요)');
  }
}
function renderSocialOnboardingModal() {
  const o = state.socialOnboarding;
  if (!o.open) return '';
  return `
  <div class="confirm-backdrop">
    <div class="confirm-box" style="max-width:420px;text-align:left;">
      <p class="auth-eyebrow" style="margin:0 0 4px;">오운홈</p>
      <h3 style="margin:0 0 4px;">시작하기 전에</h3>
      <p class="hint" style="margin:0 0 14px;">캐릭터와 닉네임, 동네를 설정해주세요.</p>
      <canvas id="onboarding-avatar-canvas" width="144" height="176" style="width:120px;height:146px;display:block;margin:0 auto 14px;image-rendering:auto;"></canvas>
      <div class="field">
        <label for="ob-nick">닉네임</label>
        <div class="field-row">
          <input id="ob-nick" type="text" placeholder="홈트에서 사용할 닉네임" style="flex:1;min-width:0;" value="${escapeHtml(o.nickname)}" oninput="state.socialOnboarding.nickname=this.value">
          <button type="button" class="btn btn-secondary btn-sm" style="flex:none;white-space:nowrap;" onclick="checkOnboardingNickDup()">중복확인</button>
        </div>
        <p class="hint" id="ob-nick-msg" style="display:none;"></p>
      </div>
      <div class="field">
        <label>캐릭터</label>
        <div class="field-row">
          <button type="button" class="btn btn-sm ${o.gender!=='female'?'btn-primary':'btn-secondary'}" style="flex:1;" onclick="setOnboardingGender('male')">남성</button>
          <button type="button" class="btn btn-sm ${o.gender==='female'?'btn-primary':'btn-secondary'}" style="flex:1;" onclick="setOnboardingGender('female')">여성</button>
        </div>
      </div>
      <div class="field">
        <label>활동 지역 (랭킹 산정 기준)</label>
        <div class="field-row">
          <select onchange="setOnboardingCity(this.value)" style="flex:1;min-width:0;">
            ${Object.keys(REGION_DATA).map(c => `<option ${c === o.regionCity ? 'selected' : ''}>${c}</option>`).join('')}
          </select>
          <select onchange="setOnboardingGu(this.value)" style="flex:1;min-width:0;">
            ${Object.keys(REGION_DATA[o.regionCity]).map(g => `<option ${g === o.regionGu ? 'selected' : ''}>${g}</option>`).join('')}
          </select>
          <select onchange="setOnboardingDong(this.value)" style="flex:1;min-width:0;">
            ${REGION_DATA[o.regionCity][o.regionGu].map(d => `<option ${d === o.regionDong ? 'selected' : ''}>${d}</option>`).join('')}
          </select>
        </div>
        <p class="hint">랭킹은 동 단위로 집계됩니다.</p>
      </div>
      <button class="btn btn-primary btn-block" style="margin-top:6px;" onclick="submitSocialOnboarding()">시작하기</button>
    </div>
  </div>`;
}





