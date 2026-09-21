// bootstrap.js — 페이지 최초 로드 시 실행되는 시작 스크립트. index.html에서 가장 마지막에 로드된다.
// [담당] 특정 카테고리 없음 — 앱 부팅(저장된 로그인 세션 복원) + 첫 render() 호출 + 공용 이벤트 리스너.
// [백엔드 연동] localStorage에 토큰이 남아있으면 로그인과 동일한 초기 데이터 로드(loadMyProfile,
//              loadMyCrew 등)를 재사용해서 자동 로그인 상태로 복원한다.
// [주의] 이 파일은 반드시 다른 모든 js 파일보다 나중에 로드돼야 한다(index.html 스크립트 순서 마지막).
document.addEventListener('click', e => {
  if (e.target && e.target.id === 'confirm-yes' && state.confirm) { state.confirm.onYes(); }
});

(async function initApp() {
  const params = new URLSearchParams(window.location.search);
  const code = params.get('code');
  const providerState = params.get('state');
  if (code) {
    window.history.replaceState({}, '', window.location.pathname);
    if (providerState === 'google') await handleGoogleRedirect(code);
    else await handleKakaoRedirect(code);
  } else {
    // 새로고침하면 랜딩페이지로 돌아가던 문제 — state가 메모리에만 있어서 새로고침할 때마다
    // 초기값(screen:'intro')부터 다시 시작했던 것. 저장된 토큰이 있으면 그걸로 로그인 상태와
    // 마지막으로 보던 메뉴를 복원한다.
    const savedToken = loadSessionToken();
    if (savedToken) {
      state.token = savedToken;
      await loadMyProfile();
      await loadExerciseHistory();
      await loadMyCrew();
      await loadTodayMissions();
      await loadShopItems();
      if (state.user.id) { // loadMyProfile()은 토큰이 만료/무효해도 던지지 않고 조용히 실패한다
        state.guestMode = false;
        state.screen = 'app';
        state.menu = loadSessionMenu() || 'main';
        if (typeof loadMyRegionRank === 'function') await loadMyRegionRank();
        if (typeof autoClaimAttendance === 'function') autoClaimAttendance(); // 새로고침으로 세션이 복원돼도 오늘 출석은 기록한다
        // 온보딩(닉네임·캐릭터·동네)을 마치기 전에 새로고침한 소셜 로그인 계정도 다시
        // 이 화면부터 채우게 한다 — auth.js maybeOpenSocialOnboarding 참고.
        if (typeof maybeOpenSocialOnboarding === 'function') maybeOpenSocialOnboarding();
      } else {
        clearSession(); // 만료된 토큰이면 지우고 랜딩페이지로 보낸다
      }
    }
  }
  render();
})();
