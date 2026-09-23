// tier3d.js — 마이프로필 등급 배지 3D 회전 미리보기(턴테이블 회전).
// profile.js/router.js는 일반 <script>(전역 스코프)라 ES 모듈을 직접 import할 수 없으므로,
// 이 파일이 window.renderTier3D()를 전역에 노출해서 그쪽에서 평범한 함수처럼 호출하게 한다.
//
// 캔버스 엘리먼트는 router.js의 render()가 화면을 다시 그릴 때마다(innerHTML 교체) 통째로
// 새로 만들어지므로, 매번 새 WebGLRenderer를 그 시점의 캔버스에 새로 붙인다. 대신 등급별 glb는
// 한 번 내려받으면 캐시해두고 재사용한다(이 SPA는 배지 캔버스가 한 번에 하나만 떠 있어서
// clone() 없이 재파킹해도 안전하다 — Object3D를 새 Scene에 add()하면 이전 부모에서 자동으로
// 떨어져 나간다).
import * as THREE from "three";
import { GLTFLoader } from "three/addons/loaders/GLTFLoader.js";
import { RoomEnvironment } from "three/addons/environments/RoomEnvironment.js";

const ROTATE_SPEED = 0.6; // 라디안/초 — 정면에서 계속 오른쪽으로 도는 턴테이블 회전

const MODEL_PATH_BY_GRADE = {
  IRON: 'assets/models/tiers/iron.glb',
  BRONZE: 'assets/models/tiers/bronze.glb',
  SILVER: 'assets/models/tiers/silver.glb',
  GOLD: 'assets/models/tiers/gold.glb',
  PLATINUM: 'assets/models/tiers/platinum.glb',
  EMERALD: 'assets/models/tiers/emerald.glb',
  DIAMOND: 'assets/models/tiers/diamond.glb',
  MASTER: 'assets/models/tiers/master.glb',
  GRANDMASTER: 'assets/models/tiers/grandmaster.glb',
  CHALLENGER: 'assets/models/tiers/challenger.glb',
};

const cachedScenes = {}; // grade -> Object3D
const loadingPromises = {}; // grade -> Promise

// PBR 금속 재질(base_basic_pbr.glb)은 반사시킬 환경(IBL)이 없으면 조명을 아무리 세게 줘도
// 어둡고 밋밋하게 보인다 — RoomEnvironment로 만든 환경맵을 한 번만 생성해서 모든 배지가
// 공유한다(렌더러가 매번 새로 생겨도 텍스처 자체는 재사용 가능).
let cachedEnvTexture = null;
function getEnvTexture(renderer) {
  if (cachedEnvTexture) return cachedEnvTexture;
  const pmrem = new THREE.PMREMGenerator(renderer);
  cachedEnvTexture = pmrem.fromScene(new RoomEnvironment(), 0.04).texture;
  pmrem.dispose();
  return cachedEnvTexture;
}

function loadTierModel(grade) {
  const path = MODEL_PATH_BY_GRADE[grade] || MODEL_PATH_BY_GRADE.IRON;
  if (cachedScenes[grade]) return Promise.resolve(cachedScenes[grade]);
  if (loadingPromises[grade]) return loadingPromises[grade];
  loadingPromises[grade] = new Promise((resolve, reject) => {
    new GLTFLoader().load(
      path,
      (gltf) => { cachedScenes[grade] = gltf.scene; resolve(gltf.scene); },
      undefined,
      reject
    );
  });
  return loadingPromises[grade];
}

// 프로필 화면을 벗어나면(다른 메뉴로 이동, 다음 render() 등) 이전 루프를 반드시 멈춰야
// 한다 — 안 그러면 프로필 탭을 들락거릴 때마다 rAF 루프와 리사이즈 리스너가 계속 쌓인다.
let currentRenderer = null;
let currentRafId = null;
let currentResizeHandler = null;

function stopPreviousLoop() {
  if (currentRafId !== null) { cancelAnimationFrame(currentRafId); currentRafId = null; }
  if (currentRenderer) { currentRenderer.dispose(); currentRenderer = null; }
  if (currentResizeHandler) { window.removeEventListener('resize', currentResizeHandler); currentResizeHandler = null; }
}

window.renderTier3D = async function renderTier3D(canvasId, grade) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return;

  stopPreviousLoop();

  const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true });
  currentRenderer = renderer;
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  // ACES 필름 톤매핑은 중간톤을 눌러 오히려 더 어둡게 보였다 — 작은 배지 미리보기에는
  // 그냥 노출값만 높인 선형 톤매핑이 더 밝고 또렷하게 나온다.
  renderer.toneMapping = THREE.NoToneMapping;
  renderer.outputColorSpace = THREE.SRGBColorSpace;

  const scene = new THREE.Scene();
  scene.environment = getEnvTexture(renderer);
  const camera = new THREE.PerspectiveCamera(35, 1, 0.1, 50);

  // AmbientLight는 각도와 무관하게 모든 면을 균일하게 밝혀서, 회전 중 어느 각도에서도
  // "이쪽만 어둡게" 보이는 걸 막아주는 밝기 최저선 역할을 한다.
  scene.add(new THREE.AmbientLight(0xffffff, 2.2));
  scene.add(new THREE.HemisphereLight(0xffffff, 0x9aa7c9, 1.6));
  const key = new THREE.DirectionalLight(0xffffff, 2.4);
  key.position.set(2, 3, 2);
  scene.add(key);
  const fill = new THREE.DirectionalLight(0xffffff, 1.4);
  fill.position.set(-2, 1, -2);
  scene.add(fill);

  function resize() {
    const w = canvas.clientWidth || canvas.width || 120;
    const h = canvas.clientHeight || canvas.height || 120;
    renderer.setSize(w, h, false);
    camera.aspect = w / h;
    camera.updateProjectionMatrix();
  }

  let model;
  try {
    model = await loadTierModel(grade);
  } catch (err) {
    console.error('등급 배지 3D 로드 실패:', grade, err);
    return;
  }
  // 로딩 중에 다른 화면으로 이동해서 이 캔버스가 더 이상 활성 렌더러가 아니면 조용히 멈춘다.
  if (currentRenderer !== renderer) return;

  const badgeGroup = new THREE.Group();
  badgeGroup.add(model);
  scene.add(badgeGroup);

  // 등급마다 실제 모델 크기·중심이 제각각이라, 카메라 거리를 매번 하드코딩하는 대신
  // 바운딩 스피어를 계산해서 항상 캔버스에 꽉 차게 자동으로 맞춘다.
  const box = new THREE.Box3().setFromObject(badgeGroup);
  const center = box.getCenter(new THREE.Vector3());
  const sphere = box.getBoundingSphere(new THREE.Sphere());
  badgeGroup.position.sub(center);
  const fitDist = (sphere.radius / Math.sin((camera.fov * Math.PI / 180) / 2)) * 1.35;
  camera.position.set(0, 0, fitDist);
  camera.lookAt(0, 0, 0);

  resize();
  currentResizeHandler = resize;
  window.addEventListener('resize', currentResizeHandler);

  const clock = new THREE.Clock();
  function tick() {
    currentRafId = requestAnimationFrame(tick);
    badgeGroup.rotation.y += ROTATE_SPEED * clock.getDelta();
    renderer.render(scene, camera);
  }
  tick();
};
