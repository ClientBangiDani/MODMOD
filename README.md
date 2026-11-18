# AETHERFLUX Combo Aura (Fabric, Minecraft 1.21.11)

Modul WYLACZNIE wizualny (klient). Nie atakuje, nie celuje, nie modyfikuje ataku.

## Budowanie (GitHub Actions)
Repozytorium na GitHubie -> zakladka Actions -> "Build AETHERFLUX Combo Aura" -> Run workflow.
Po zakonczeniu: wejdz w przebieg -> sekcja Artifacts -> `AETHERFLUX-Combo-Aura` (zawiera `AETHERFLUX-Combo-Aura.jar`).
Workflow sam dobiera aktualne wersje Fabric (`scripts/resolve_versions.py`) i generuje Gradle Wrapper.

## Dzialanie licznika
- Proba ataku (klikniecie na encje) != trafienie. Trafienie jest "potwierdzone" dopiero gdy ten sam cel w ciagu
  `confirmWindowTicks` pokaze nowa animacje obrazen (rosnacy `hurtTime`). Jedna proba = max jedno trafienie.
- Kazdy cel ma osobny stan (UUID). Reset: wygasniecie (`timeoutTicks`), smierc/znikniecie celu, zmiana celu
  (opcja `resetOnTargetSwitch`), smierc gracza, opcjonalnie obrazenia gracza, wejscie/wyjscie z serwera.
- Licznik nie jest zapisywany na dysk.
- OGRANICENIE: klient nie wie, kto zadal obrazenia; obrazenia od kogos innego w oknie potwierdzenia
  moga zostac zaliczone po Twojej probie ataku.

## Efekty
| Trafienia | Efekt |
|---|---|
| 4-10 | `HYPER COMBO` nad celem, kolor AETHERFLUX (cyan #3FD8FF) |
| 11-13 | przejscie fiolet #9B5CFF <-> cyan |
| 14-20 | + subtelne iskry |
| 21, 27, 33, 39, 45, ... (co 6) | jednorazowy wybuch "fajerwerkow" |

## Komendy
`/aetherflux_combo toggle | timeout <sekundy> | targetreset | particles`

Konfiguracja: `config/aetherflux_combo_aura.json`.
