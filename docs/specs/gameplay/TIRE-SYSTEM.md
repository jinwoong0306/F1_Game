# TIRE-SYSTEM.md

## Overview
F1 스타일의 타이어 선택 및 마모 시스템 명세서입니다. 3종의 타이어 컴파운드(Soft/Medium/Hard)와 실시간 성능 저하 메카닉을 정의합니다.

**Owner**: Gameplay Lead  
**Status**: Draft  
**Last Updated**: 2025-01-15  
**Related Specs**: `VEHICLE-PHYSICS.md`, `PITSTOP-MINIGAME.md`, `DAMAGE-MODEL.md`

---

## Dependencies
- **Physics System**: `VEHICLE-PHYSICS.md` (타이어 그립이 차량 성능에 직접 영향)
- **Pit Stop**: `PITSTOP-MINIGAME.md` (타이어 교체 메카닉)
- **HUD**: `HUD-SPECIFICATION.md` (타이어 상태 표시)

---

## 1. Tire Types (타이어 종류)

### 1.1 Soft Compound (빨강)
**특징**: 최고 성능, 짧은 수명

| 속성 | 값 | 설명 |
|------|------|------|
| **최고 속도 보너스** | +10% | 83.33 m/s → 91.66 m/s |
| **가속도 보너스** | +10% | 기본 가속력 × 1.1 |
| **코너링 그립** | +15% | 측면 마찰력 × 1.15 |
| **내구도 (시간)** | 30초 | 30초 후 급격한 성능 저하 시작 |
| **마모율** | 3.33% / 초 | 30초에 100% 도달 |

**사용 시나리오**:
- 예선 랩 (단일 플라잉 랩)
- 레이스 초반 오버테이킹
- 짧은 스틴트 (2-3 랩)

---

### 1.2 Medium Compound (노랑)
**특징**: 균형잡힌 성능, 중간 수명

| 속성 | 값 | 설명 |
|------|------|------|
| **최고 속도 보너스** | +5% | 83.33 m/s → 87.50 m/s |
| **가속도 보너스** | +5% | 기본 가속력 × 1.05 |
| **코너링 그립** | +5% | 측면 마찰력 × 1.05 |
| **내구도 (시간)** | 60초 | 60초 후 서서히 성능 저하 시작 |
| **마모율** | 1.67% / 초 | 60초에 100% 도달 |

**사용 시나리오**:
- 표준 레이스 전략 (중간 스틴트)
- 초보자에게 권장
- 변화무쌍한 레이스 (안전 선택)

---

### 1.3 Hard Compound (흰색)
**특징**: 낮은 성능, 긴 수명

| 속성 | 값 | 설명 |
|------|------|------|
| **최고 속도 보너스** | 0% | 기본 속도 유지 |
| **가속도 보너스** | 0% | 기본 가속력 유지 |
| **코너링 그립** | 0% | 기본 그립 유지 |
| **내구도 (시간)** | 100초 | 100초 후 아주 느리게 성능 저하 |
| **마모율** | 1.0% / 초 | 100초에 100% 도달 |

**사용 시나리오**:
- 긴 스틴트 전략 (원스톱 레이스)
- 트래픽이 많은 상황 (오버테이킹 어려움)
- 안정적인 랩타임 유지

---

## 2. Tire Degradation System (타이어 마모 시스템)

### 2.1 마모 계산 공식

```java
public class TireDegradation {
    private float baseWearRate;      // 타이어 종류별 기본 마모율
    private float currentCondition;  // 0.0 ~ 1.0 (100% ~ 0%)
    
    public void update(float delta, float speed, float steeringInput) {
        // 마모 계수 계산
        float speedFactor = 1.0f + (speed / maxSpeed) * 0.5f;        // 속도가 높을수록 마모 증가
        float steeringFactor = 1.0f + Math.abs(steeringInput) * 0.3f; // 조향 중 마모 증가
        float offTrackFactor = isOffTrack() ? 2.0f : 1.0f;            // 오프로드 시 2배 마모
        
        // 최종 마모율 = 기본 마모율 × 각 계수
        float wearRate = baseWearRate * speedFactor * steeringFactor * offTrackFactor;
        
        // 컨디션 감소
        currentCondition -= wearRate * delta;
        currentCondition = Math.max(0, currentCondition); // 0 이하로 내려가지 않음
    }
    
    public float getPerformanceModifier() {
        // 컨디션에 따른 성능 보정
        if (currentCondition > 0.8f) {
            return 1.0f; // 80% 이상: 최고 성능
        } else if (currentCondition > 0.5f) {
            return 0.8f + (currentCondition - 0.5f) * 0.6f; // 50-80%: 선형 감소
        } else if (currentCondition > 0.2f) {
            return 0.6f + (currentCondition - 0.2f) * 0.6f; // 20-50%: 급격한 감소
        } else {
            return 0.3f + currentCondition * 1.5f; // 0-20%: 최악의 상태
        }
    }
}
```

### 2.2 성능 저하 그래프

```
Performance
100% ┤──────────────────╮
     │                  │
 80% ┤                  │
     │                  ╲
 60% ┤                   ╲
     │                    ╲
 40% ┤                     ╲___
     │                         ╲___
 20% ┤                             ╲___
     │                                 ╲
  0% └────────────────────────────────────
     0%   20%   40%   60%   80%   100%
              Tire Condition
```

---

## 3. Performance Impact (성능 영향)

### 3.1 차량 성능에 미치는 영향

```java
public class VehicleController {
    private TireManager tireManager;
    
    public float getEffectiveAcceleration() {
        float baseAccel = BASE_ACCELERATION;
        float tireBonus = tireManager.getCurrentTireType().getAccelBonus();
        float tireCondition = tireManager.getPerformanceModifier();
        
        return baseAccel * (1.0f + tireBonus) * tireCondition;
    }
    
    public float getEffectiveMaxSpeed() {
        float baseSpeed = MAX_SPEED;
        float tireBonus = tireManager.getCurrentTireType().getSpeedBonus();
        float tireCondition = tireManager.getPerformanceModifier();
        
        return baseSpeed * (1.0f + tireBonus) * tireCondition;
    }
    
    public float getEffectiveGrip() {
        float baseGrip = BASE_GRIP;
        float tireBonus = tireManager.getCurrentTireType().getGripBonus();
        float tireCondition = tireManager.getPerformanceModifier();
        
        return baseGrip * (1.0f + tireBonus) * tireCondition;
    }
}
```

### 3.2 실제 성능 예시

**Soft 타이어 마모 시나리오**:
```
0초 (신품):     최고 속도 330 km/h, 그립 1.15
15초 (50%):     최고 속도 330 km/h, 그립 1.15 (유지)
30초 (100%):    최고 속도 320 km/h, 그립 1.05 (10% 저하)
45초 (마모):    최고 속도 290 km/h, 그립 0.85 (40% 저하)
```

**Medium 타이어 마모 시나리오**:
```
0초 (신품):     최고 속도 315 km/h, 그립 1.05
30초 (50%):     최고 속도 315 km/h, 그립 1.05 (유지)
60초 (100%):    최고 속도 310 km/h, 그립 1.00 (5% 저하)
90초 (마모):    최고 속도 285 km/h, 그립 0.85 (20% 저하)
```

---

## 4. UI Integration (UI 통합)

### 4.1 HUD 타이어 게이지

```
┌─────────────────────┐
│ Tire: SOFT [●]      │  ← 타이어 종류 + 색상 아이콘
│ [████████░░] 80%    │  ← 컨디션 바 + 퍼센트
│ Lap 3 / 15          │  ← 이 타이어로 주행한 랩
└─────────────────────┘
```

#### 색상 코딩
- **초록** (100-60%): 타이어 상태 양호
- **노랑** (60-30%): 성능 저하 시작, 주의 필요
- **빨강** (30-0%): 심각한 마모, 피트인 권장
- **깜빡임**: 20% 이하 시 경고 깜빡임

---

## 5. Strategy & Gameplay Balance

### 5.1 레이스 거리별 권장 전략

**짧은 레이스 (5 랩, ~5분)**:
- **Soft**: 원스톱 불가, 트윈스톱 필요 (초반 공격 → 중반 교체)
- **Medium**: 원스톱 가능 (안전한 선택)
- **Hard**: 노스톱 가능하지만 느림

**중간 레이스 (10 랩, ~10분)**:
- **Soft**: 트윈스톱 (랩 3, 7에 교체)
- **Medium**: 원스톱 (랩 5에 교체)
- **Hard**: 노스톱 또는 레이트 스톱 (랩 8)

**긴 레이스 (20 랩, ~20분)**:
- **Soft**: 트리플스톱 (공격적이지만 위험)
- **Medium**: 트윈스톱 (랩 7, 14)
- **Hard**: 원스톱 (랩 10-12)

### 5.2 피트 스톱 시간 vs 트랙 포지션

```
피트 스톱 시간 (평균): 5초
트랙 포지션 손실: ~3-4 위치

트레이드오프:
- 신선한 타이어 → 2-3초/랩 빠름 × 남은 랩 수
- 피트 스톱 시간 → 5초 손실
- 순위 복구에 필요한 오버테이킹
```

**예시 계산**:
```
남은 랩: 10
예상 랩타임 개선: 2초/랩
총 이득: 10 × 2 = 20초
피트 스톱 비용: 5초
순이득: 20 - 5 = 15초 ✅ 피트인 가치 있음
```

---

## 6. Implementation Details

### 6.1 TireManager Class

```java
public class TireManager {
    private TireType currentTire;
    private float condition; // 0.0 - 1.0
    private float ageInSeconds;
    
    public enum TireType {
        SOFT(0.10f, 0.10f, 0.15f, 30f, 0.0333f, Color.RED),
        MEDIUM(0.05f, 0.05f, 0.05f, 60f, 0.0167f, Color.YELLOW),
        HARD(0.0f, 0.0f, 0.0f, 100f, 0.01f, Color.WHITE);
        
        public final float speedBonus;
        public final float accelBonus;
        public final float gripBonus;
        public final float durabilitySeconds;
        public final float baseWearRate;
        public final Color displayColor;
        
        TireType(float speed, float accel, float grip, float durability, float wear, Color color) {
            this.speedBonus = speed;
            this.accelBonus = accel;
            this.gripBonus = grip;
            this.durabilitySeconds = durability;
            this.baseWearRate = wear;
            this.displayColor = color;
        }
    }
    
    public TireManager() {
        currentTire = TireType.MEDIUM; // 기본 타이어
        condition = 1.0f;
        ageInSeconds = 0f;
    }
    
    public void update(float delta, VehicleState vehicleState) {
        ageInSeconds += delta;
        
        // 마모 계산
        float speedFactor = 1.0f + (vehicleState.speed / vehicleState.maxSpeed) * 0.5f;
        float steeringFactor = 1.0f + Math.abs(vehicleState.steeringInput) * 0.3f;
        float offTrackFactor = vehicleState.isOffTrack ? 2.0f : 1.0f;
        
        float wearRate = currentTire.baseWearRate * speedFactor * steeringFactor * offTrackFactor;
        condition -= wearRate * delta;
        condition = Math.max(0, condition);
    }
    
    public void changeTire(TireType newType) {
        currentTire = newType;
        condition = 1.0f;
        ageInSeconds = 0f;
    }
    
    public float getPerformanceModifier() {
        if (condition > 0.8f) return 1.0f;
        if (condition > 0.5f) return 0.8f + (condition - 0.5f) * 0.67f;
        if (condition > 0.2f) return 0.6f + (condition - 0.2f) * 0.67f;
        return 0.3f + condition * 1.5f;
    }
    
    public boolean isWornOut() {
        return condition < 0.2f;
    }
    
    public Color getConditionColor() {
        if (condition > 0.6f) return Color.GREEN;
        if (condition > 0.3f) return Color.YELLOW;
        return Color.RED;
    }
}
```

---

## 7. Testing & Validation

### 7.1 Unit Tests

```java
@Test
@DisplayName("Soft tire should degrade to 50% in 15 seconds")
public void testSoftTireDegradation() {
    TireManager tireManager = new TireManager();
    tireManager.changeTire(TireType.SOFT);
    
    VehicleState state = new VehicleState();
    state.speed = 50f; // 중간 속도
    state.steeringInput = 0f;
    state.isOffTrack = false;
    
    // 15초 시뮬레이션
    for (int i = 0; i < 900; i++) { // 60 FPS
        tireManager.update(1/60f, state);
    }
    
    assertThat(tireManager.getCondition())
        .isCloseTo(0.5f, within(0.1f));
}

@Test
@DisplayName("Off-track driving should double wear rate")
public void testOffTrackWear() {
    TireManager tireManager = new TireManager();
    float initialCondition = tireManager.getCondition();
    
    VehicleState state = new VehicleState();
    state.isOffTrack = true;
    
    tireManager.update(1.0f, state);
    float wearOffTrack = initialCondition - tireManager.getCondition();
    
    tireManager.changeTire(TireType.MEDIUM);
    state.isOffTrack = false;
    tireManager.update(1.0f, state);
    float wearOnTrack = 1.0f - tireManager.getCondition();
    
    assertThat(wearOffTrack).isCloseTo(wearOnTrack * 2, within(0.01f));
}
```

### 7.2 Balance Testing Checklist

- [ ] Soft 타이어로 3랩 주행 가능한가?
- [ ] Medium 타이어로 5랩 이상 버틸 수 있는가?
- [ ] Hard 타이어로 10랩 완주 가능한가?
- [ ] 타이어 교체가 전략적 이점을 제공하는가?
- [ ] 마모된 타이어의 성능 저하가 체감되는가?

---

## 8. Future Enhancements

### Phase 6+ 추가 기능
- [ ] **웨더 시스템**: 비 오는 날 웨트 타이어
- [ ] **타이어 온도**: 워밍업/쿨다운 메카닉
- [ ] **플랫 스팟**: 락업 시 타이어 손상
- [ ] **타이어 데이터 텔레메트리**: 실시간 온도/압력 표시

---

**Version**: 1.0.0  
**Status**: Ready for Implementation  
**Next Review**: After Phase 3 completion
