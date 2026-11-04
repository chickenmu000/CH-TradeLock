# CH-TradeLock

로어에 **"거래불가"** 가 들어간 아이템을 거래 / 유출 / 사망 손실로부터 보호하는 Paper 플러그인입니다.
(구 이름: IsakLackitem)

- 서버: Paper 1.21+ / Java 21
- 선택 플러그인: AxTrade (거래 GUI 차단에 활용)

## 기능

| 기능 | 설명 |
|---|---|
| 드롭 차단 | 거래불가 아이템은 버릴 수 없습니다. |
| 줍기 차단 | 거래불가 아이템은 바닥에서 주울 수 없습니다. |
| 이동 차단 | 상자 등 다른 인벤토리로 옮길 수 없습니다. (클릭 / 쉬프트클릭 / 드래그 / 숫자키 / F키 포함) |
| 명령어 차단 | 거래불가 아이템을 손에 들고 있으면 `blocked-commands` 의 명령어를 쓸 수 없습니다. |
| **사망 시 보호** | 사망해도 거래불가 아이템은 떨어지지 않고, 리스폰 후 인벤토리로 돌아옵니다. |

- 거래불가 판별: 아이템 로어의 한 줄에라도 `거래불가` 가 들어 있으면 거래불가 아이템입니다.
- 인벤토리를 열지 않은 상태에서 F키(양손 바꾸기)로 왼손에 드는 것은 허용됩니다.
- 상자 등을 연 상태에서 F키로 넣는 것은 막습니다.
- `whitelisted-guis` 에 GUI 제목(부분 일치)을 넣으면 그 GUI에서는 이동을 허용합니다.
- 사망 시 보호는 리스폰 후 **원래 슬롯이 아니라 빈 칸**에 들어옵니다. (Paper 기본 동작)

## 명령어 / 권한

| 명령어 | 설명 | 권한 |
|---|---|---|
| `/tradelock reload` | 설정(config.yml) 리로드 | `chtradelock.admin` (기본 OP) |

## 설정 (config.yml)

```yaml
# 이 문자열이 GUI 제목에 들어 있으면 거래불가 아이템 이동 허용 (부분 일치)
whitelisted-guis:
  - "예시 GUI"

# 거래불가 아이템을 손에 들고 있을 때 차단할 명령어 (슬래시 없이, 대소문자 구분 없음)
blocked-commands:
  - "ah"
```

## 빌드

JDK 21 + Maven 이 필요합니다.

```
mvn package
```

`target/CH-TradeLock.jar` 를 서버 `plugins/` 폴더에 넣으세요.

## 이전(IsakLackitem)에서 옮길 때

- 설정 폴더가 `plugins/IsakLackitem` -> `plugins/CH-TradeLock` 으로 바뀝니다. 기존 `config.yml` 내용을 복사해 주세요.
- 명령어가 `/isakLackitem reload` -> `/tradelock reload`, 권한이 `isakLackitem.admin` -> `chtradelock.admin` 으로 바뀌었습니다.
- 기존 `IsakLackitem.jar` 는 삭제하고 새 jar 만 넣으세요.

## 변경 내역

- **1.4** 이름을 CH-TradeLock 으로 변경 / 사망 시 거래불가 아이템 보호 추가 / 인벤토리를 열지 않은 상태의 F키 허용, 상자에서의 F키 이동만 차단
- 1.3 이하: IsakLackitem (드롭 / 줍기 / 이동 / 명령어 차단)
